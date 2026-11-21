package com.knz.diatracker

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knz.diatracker.data.AppDatabase
import com.knz.diatracker.data.DiabetesRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThresholdKeyboardMode(val title: String) {
    DISABLED("Выключено"),
    AUTO_KEYBOARD("Автоматическая клавиатура"),
    PLUS_BUTTON("Кнопка «+» рядом со слайдером")
}

data class UserSettings(
    val general: Boolean = true,
    val short: Boolean = false,
    val long: Boolean = false,
    val mixed: Boolean = false,
    val thresholdMode: ThresholdKeyboardMode = ThresholdKeyboardMode.AUTO_KEYBOARD
)

enum class StatsPeriod(val title: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    ALL("Все")
}

data class ChartMetric(
    val title: String,
    val unit: String,
    val extractor: (DiabetesRecord) -> Float?
)

fun parseRecordTimestamp(dateTimeText: String?, fallbackTimestamp: Long): Long {
    if (dateTimeText.isNullOrBlank()) return fallbackTimestamp

    val patterns = listOf(
        Pair("d MMMM yyyy HH:mm", Locale("ru")),
        Pair("d MMMM yyyy", Locale("ru")),
        Pair("dd.MM.yyyy HH:mm", Locale.getDefault()),
        Pair("dd.MM.yyyy HH-mm", Locale.getDefault()),
        Pair("yyyy-MM-dd HH:mm", Locale.getDefault()),
        Pair("dd.MM.yyyy", Locale.getDefault()),
        Pair("yyyy-MM-dd", Locale.getDefault())
    )

    for ((pattern, locale) in patterns) {
        try {
            val formatter = SimpleDateFormat(pattern, locale).apply {
                isLenient = false
            }
            val date = formatter.parse(dateTimeText)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return fallbackTimestamp
}

data class GlucoseInputState(
    val selectedMeasurement: Int = 0,
    val bloodSugar: Float = 0.0f,
    val breadUnits: Float = 0f,
    val generalInsulin: Float = 0f,
    val shortInsulin: Float = 0f,
    val longInsulin: Float = 0f,
    val mixedInsulin: Float = 0f,
    val weight: Float = 0f,
    val activityMinutes: Float = 0f,
    val selectedMealTime: Int = 0, // 0 = none, 1 = до еды, 2 = после еды
    val selectedActivity: String = "",
    val noteText: String = "",
    val isNoteVisible: Boolean = false,
    val resetTrigger: Int = 0
)

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEK,
    val availableMetrics: List<ChartMetric> = emptyList(),
    val currentMetricIndex: Int = 0,
    val currentMetric: ChartMetric? = null,
    val periodRecords: List<DiabetesRecord> = emptyList(),
    val metricValues: List<Pair<DiabetesRecord, Float>> = emptyList(),
    val totalRecords: Int = 0,
    val avgBloodSugar: Float = 0f,
    val totalBreadUnits: Float = 0f,
    val totalGeneralInsulin: Float = 0f,
    val totalShortInsulin: Float = 0f,
    val totalLongInsulin: Float = 0f,
    val totalMixedInsulin: Float = 0f,
    val avgWeight: Float = 0f,
    val totalActivityMinutes: Float = 0f,
    val isDataReady: Boolean = false,
    val showExportDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).diabetesDao()
    private val context = application.applicationContext
    private val dateTimeFormatter = SimpleDateFormat("d MMMM yyyy HH:mm", Locale("ru"))

    val recordsList: StateFlow<List<DiabetesRecord>> = dao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private object PreferencesKeys {
        val GENERAL_INSULIN = booleanPreferencesKey("show_general_insulin")
        val SHORT_INSULIN = booleanPreferencesKey("show_short_insulin")
        val LONG_INSULIN = booleanPreferencesKey("show_long_insulin")
        val MIXED_INSULIN = booleanPreferencesKey("show_mixed_insulin")
        val THRESHOLD_MODE = stringPreferencesKey("threshold_keyboard_mode")
    }

    val uiState: StateFlow<UserSettings> = context.dataStore.data
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.THRESHOLD_MODE]
            val mode = ThresholdKeyboardMode.entries.find { it.name == modeStr } ?: ThresholdKeyboardMode.AUTO_KEYBOARD
            UserSettings(
                general = preferences[PreferencesKeys.GENERAL_INSULIN] ?: true,
                short = preferences[PreferencesKeys.SHORT_INSULIN] ?: false,
                long = preferences[PreferencesKeys.LONG_INSULIN] ?: false,
                mixed = preferences[PreferencesKeys.MIXED_INSULIN] ?: false,
                thresholdMode = mode
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings(
                general = true,
                short = false,
                long = false,
                mixed = false,
                thresholdMode = ThresholdKeyboardMode.AUTO_KEYBOARD
            )
        )

    fun toggleGeneralInsulin(value: Boolean) {
        viewModelScope.launch { context.dataStore.edit { it[PreferencesKeys.GENERAL_INSULIN] = value } }
    }

    fun toggleShortInsulin(value: Boolean) {
        viewModelScope.launch { context.dataStore.edit { it[PreferencesKeys.SHORT_INSULIN] = value } }
    }

    fun toggleLongInsulin(value: Boolean) {
        viewModelScope.launch { context.dataStore.edit { it[PreferencesKeys.LONG_INSULIN] = value } }
    }

    fun toggleMixedInsulin(value: Boolean) {
        viewModelScope.launch { context.dataStore.edit { it[PreferencesKeys.MIXED_INSULIN] = value } }
    }

    fun setThresholdKeyboardMode(mode: ThresholdKeyboardMode) {
        viewModelScope.launch { context.dataStore.edit { it[PreferencesKeys.THRESHOLD_MODE] = mode.name } }
    }

    var currentScreen by mutableStateOf(Screen.MAIN)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    var draftInputState by mutableStateOf(GlucoseInputState())
        private set

    fun updateDraftMeasurement(measurement: Int) {
        draftInputState = draftInputState.copy(selectedMeasurement = measurement)
    }

    fun updateDraftValue(measurementIndex: Int, value: Float) {
        draftInputState = when (measurementIndex) {
            0 -> draftInputState.copy(bloodSugar = value)
            1 -> draftInputState.copy(breadUnits = value)
            2 -> draftInputState.copy(generalInsulin = value)
            3 -> draftInputState.copy(shortInsulin = value)
            4 -> draftInputState.copy(longInsulin = value)
            5 -> draftInputState.copy(mixedInsulin = value)
            6 -> draftInputState.copy(weight = value)
            8 -> draftInputState.copy(activityMinutes = value)
            else -> draftInputState
        }
    }

    fun updateDraftMealTime(mealTime: Int) {
        draftInputState = draftInputState.copy(selectedMealTime = mealTime)
    }

    fun updateDraftActivity(activity: String) {
        draftInputState = draftInputState.copy(selectedActivity = activity)
    }

    fun updateDraftNote(note: String) {
        draftInputState = draftInputState.copy(noteText = note)
    }

    fun resetDraftInputState() {
        draftInputState = GlucoseInputState(resetTrigger = draftInputState.resetTrigger + 1)
        resetDateTimeToNow()
    }

    var selectedRecordForEdit by mutableStateOf<DiabetesRecord?>(null)
        private set

    var isEditDialogVisible by mutableStateOf(false)
        private set

    var editInputState by mutableStateOf<GlucoseInputState?>(null)
        private set

    fun startEditingRecord(record: DiabetesRecord) {
        selectedRecordForEdit = record
        editInputState = GlucoseInputState(
            selectedMeasurement = 0,
            bloodSugar = record.bloodSugar ?: 0.0f,
            breadUnits = record.breadUnits ?: 0f,
            generalInsulin = record.generalInsulin ?: 0f,
            shortInsulin = record.shortInsulin ?: 0f,
            longInsulin = record.longInsulin ?: 0f,
            mixedInsulin = record.mixedInsulin ?: 0f,
            weight = record.weight ?: 0f,
            activityMinutes = record.activityMinutes ?: 0f,
            selectedMealTime = when (record.mealType) {
                "до еды" -> 1
                "после еды" -> 2
                else -> 0
            },
            selectedActivity = record.activityName ?: "",
            noteText = record.note ?: "",
            isNoteVisible = !record.note.isNullOrBlank()
        )
        isEditDialogVisible = true
    }

    fun updateEditMeasurement(measurement: Int) {
        editInputState = editInputState?.copy(selectedMeasurement = measurement)
    }

    fun updateEditValue(measurementIndex: Int, value: Float) {
        editInputState = editInputState?.let { current ->
            when (measurementIndex) {
                0 -> current.copy(bloodSugar = value)
                1 -> current.copy(breadUnits = value)
                2 -> current.copy(generalInsulin = value)
                3 -> current.copy(shortInsulin = value)
                4 -> current.copy(longInsulin = value)
                5 -> current.copy(mixedInsulin = value)
                6 -> current.copy(weight = value)
                8 -> current.copy(activityMinutes = value)
                else -> current
            }
        }
    }

    fun updateEditMealTime(mealTime: Int) {
        editInputState = editInputState?.copy(selectedMealTime = mealTime)
    }

    fun updateEditActivity(activity: String) {
        editInputState = editInputState?.copy(selectedActivity = activity)
    }

    fun updateEditNote(note: String) {
        editInputState = editInputState?.copy(noteText = note)
    }

    fun closeEditDialog() {
        isEditDialogVisible = false
        selectedRecordForEdit = null
        editInputState = null
    }

    fun deleteRecord(record: DiabetesRecord) {
        viewModelScope.launch {
            dao.deleteRecord(record)
            closeEditDialog()
        }
    }

    fun saveDraftRecord(dateTimeText: String) {
        saveRecordInternal(draftInputState, dateTimeText, null)
    }

    fun saveEditRecord(dateTimeText: String) {
        val state = editInputState ?: return
        val recordToEdit = selectedRecordForEdit ?: return
        saveRecordInternal(state, dateTimeText, recordToEdit)
    }

    private fun saveRecordInternal(
        inputState: GlucoseInputState,
        dateTimeText: String,
        existingRecord: DiabetesRecord?
    ) {
        viewModelScope.launch {
            val parsedTimestamp = parseRecordTimestamp(dateTimeText, System.currentTimeMillis())

            val record = DiabetesRecord(
                id = existingRecord?.id ?: 0L,
                timestamp = parsedTimestamp,
                dateTimeText = dateTimeText,
                mealType = when (inputState.selectedMealTime) {
                    1 -> "до еды"
                    2 -> "после еды"
                    else -> null
                },
                bloodSugar = if (inputState.bloodSugar > 0f) inputState.bloodSugar else null,
                breadUnits = if (inputState.breadUnits > 0f) inputState.breadUnits else null,
                generalInsulin = if (inputState.generalInsulin > 0f) inputState.generalInsulin else null,
                shortInsulin = if (inputState.shortInsulin > 0f) inputState.shortInsulin else null,
                longInsulin = if (inputState.longInsulin > 0f) inputState.longInsulin else null,
                mixedInsulin = if (inputState.mixedInsulin > 0f) inputState.mixedInsulin else null,
                weight = if (inputState.weight > 0f) inputState.weight else null,
                note = if (inputState.noteText.isNotBlank()) inputState.noteText else null,
                activityName = if (inputState.selectedActivity.isNotEmpty()) inputState.selectedActivity else null,
                activityMinutes = if (inputState.selectedActivity.isNotEmpty()) inputState.activityMinutes else null
            )

            if (existingRecord == null) {
                dao.insertRecord(record)
                resetDraftInputState()
            } else {
                dao.updateRecord(record)
                closeEditDialog()
            }
        }
    }

    private val initialCalendar = Calendar.getInstance()
    var selectedYear by mutableStateOf(initialCalendar.get(Calendar.YEAR))
        private set
    var selectedMonth by mutableStateOf(initialCalendar.get(Calendar.MONTH))
        private set
    var selectedDay by mutableStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH))
        private set
    var selectedHour by mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY))
        private set
    var selectedMinute by mutableStateOf(initialCalendar.get(Calendar.MINUTE))
        private set

    var formattedDateTimeText by mutableStateOf(formatCurrentSelection())
        private set

    private fun formatCurrentSelection(): String {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
        }
        return dateTimeFormatter.format(cal.time)
    }

    fun updateDate(year: Int, month: Int, day: Int) {
        selectedYear = year
        selectedMonth = month
        selectedDay = day
        formattedDateTimeText = formatCurrentSelection()
    }

    fun updateTime(hour: Int, minute: Int) {
        selectedHour = hour
        selectedMinute = minute
        formattedDateTimeText = formatCurrentSelection()
    }

    fun resetDateTimeToNow() {
        val now = Calendar.getInstance()
        selectedYear = now.get(Calendar.YEAR)
        selectedMonth = now.get(Calendar.MONTH)
        selectedDay = now.get(Calendar.DAY_OF_MONTH)
        selectedHour = now.get(Calendar.HOUR_OF_DAY)
        selectedMinute = now.get(Calendar.MINUTE)
        formattedDateTimeText = dateTimeFormatter.format(now.time)
    }

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private val _currentMetricIndex = MutableStateFlow(0)
    val currentMetricIndex: StateFlow<Int> = _currentMetricIndex.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    fun setStatsPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    fun setMetricIndex(index: Int) {
        _currentMetricIndex.value = index
    }

    fun setShowExportDialog(show: Boolean) {
        _showExportDialog.value = show
    }

    val statsUiState: StateFlow<StatsUiState> = combine(
        recordsList,
        uiState,
        _selectedPeriod,
        _currentMetricIndex,
        _showExportDialog
    ) { allRecords: List<DiabetesRecord>, settings: UserSettings, period: StatsPeriod, metricIndex: Int, showExport: Boolean ->
        if (allRecords.isEmpty()) {
            return@combine StatsUiState(
                selectedPeriod = period,
                isDataReady = true,
                showExportDialog = showExport
            )
        }

        val sortedAll = allRecords.sortedBy { parseRecordTimestamp(it.dateTimeText, it.timestamp) }
        val latestRecordTime = parseRecordTimestamp(sortedAll.last().dateTimeText, sortedAll.last().timestamp)
        val calendar = Calendar.getInstance()

        val records = when (period) {
            StatsPeriod.DAY -> {
                calendar.timeInMillis = latestRecordTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfDay..endOfDay
                }
            }
            StatsPeriod.WEEK -> {
                calendar.timeInMillis = latestRecordTime
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startOfWeek = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfWeek..(latestRecordTime + 86400000L)
                }
            }
            StatsPeriod.MONTH -> {
                calendar.timeInMillis = latestRecordTime
                calendar.add(Calendar.MONTH, -1)
                val startOfMonth = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfMonth..(latestRecordTime + 86400000L)
                }
            }
            StatsPeriod.ALL -> sortedAll
        }

        val chartTypes = mutableListOf<ChartMetric>().apply {
            add(ChartMetric("Уровень сахара", "ммоль/л") { it.bloodSugar })
            add(ChartMetric("Хлебные единицы (ХЕ)", "ХЕ") { it.breadUnits })

            if (settings.general) {
                add(ChartMetric("Инсулин", "ед") { it.generalInsulin })
            }
            if (settings.short) {
                add(ChartMetric("Короткий инсулин", "ед") { it.shortInsulin })
            }
            if (settings.long) {
                add(ChartMetric("Продленный инсулин", "ед") { it.longInsulin })
            }
            if (settings.mixed) {
                add(ChartMetric("Смешанный инсулин", "ед") { it.mixedInsulin })
            }

            add(ChartMetric("Вес", "кг") { it.weight })
            add(ChartMetric("Физическая активность", "мин") { it.activityMinutes })
        }

        val safeIndex = if (metricIndex in chartTypes.indices) metricIndex else 0
        val currentMetric = chartTypes.getOrNull(safeIndex)

        val metricValues = if (currentMetric != null) {
            records.mapNotNull { record ->
                currentMetric.extractor(record)?.let { valPair -> record to valPair }
            }.sortedBy { parseRecordTimestamp(it.first.dateTimeText, it.first.timestamp) }
        } else emptyList()

        val totalRecords = records.size
        val avgBloodSugar = if (records.isNotEmpty()) {
            records.mapNotNull { it.bloodSugar }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }
        } else 0f

        val totalBreadUnits = records.mapNotNull { it.breadUnits }.sum()
        val totalGeneralInsulin = records.mapNotNull { it.generalInsulin }.sum()
        val totalShortInsulin = records.mapNotNull { it.shortInsulin }.sum()
        val totalLongInsulin = records.mapNotNull { it.longInsulin }.sum()
        val totalMixedInsulin = records.mapNotNull { it.mixedInsulin }.sum()
        val avgWeight = if (records.isNotEmpty()) {
            records.mapNotNull { it.weight }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }
        } else 0f
        val totalActivityMinutes = records.mapNotNull { it.activityMinutes }.sum()

        StatsUiState(
            selectedPeriod = period,
            availableMetrics = chartTypes,
            currentMetricIndex = safeIndex,
            currentMetric = currentMetric,
            periodRecords = records,
            metricValues = metricValues,
            totalRecords = totalRecords,
            avgBloodSugar = avgBloodSugar,
            totalBreadUnits = totalBreadUnits,
            totalGeneralInsulin = totalGeneralInsulin,
            totalShortInsulin = totalShortInsulin,
            totalLongInsulin = totalLongInsulin,
            totalMixedInsulin = totalMixedInsulin,
            avgWeight = avgWeight,
            totalActivityMinutes = totalActivityMinutes,
            isDataReady = true,
            showExportDialog = showExport
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}