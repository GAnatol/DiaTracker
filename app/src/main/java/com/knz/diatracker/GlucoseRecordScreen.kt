package com.knz.diatracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knz.diatracker.data.AppDatabase
import com.knz.diatracker.data.DiabetesRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MeasurementItem(
    val id: Int,
    val description: String,
    val icon: ImageVector? = null,
    val textLabel: String? = null,
    val isVisible: Boolean = true
)

@Composable
fun GlucoseRecordScreen(
    showGeneralInsulin: Boolean,
    showShortInsulin: Boolean,
    showLongInsulin: Boolean,
    showMixedInsulin: Boolean,
    dateTimeText: String,
    existingRecord: DiabetesRecord? = null,
    onRecordSavedOrUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.diabetesDao()

    var currentDateTimeText by remember(existingRecord) {
        mutableStateOf(existingRecord?.dateTimeText ?: dateTimeText)
    }

    val openInternalDateTimePicker = {
        val calendarNow = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, y, m, d ->
                val isToday = (y == calendarNow.get(Calendar.YEAR) &&
                        m == calendarNow.get(Calendar.MONTH) &&
                        d == calendarNow.get(Calendar.DAY_OF_MONTH))

                TimePickerDialog(
                    context,
                    { _, h, min ->
                        val finalHour: Int
                        val finalMin: Int
                        if (isToday) {
                            val selectedMinutesTotal = h * 60 + min
                            val currentMinutesTotal = calendarNow.get(Calendar.HOUR_OF_DAY) * 60 + calendarNow.get(Calendar.MINUTE)
                            if (selectedMinutesTotal > currentMinutesTotal) {
                                finalHour = calendarNow.get(Calendar.HOUR_OF_DAY)
                                finalMin = calendarNow.get(Calendar.MINUTE)
                            } else {
                                finalHour = h
                                finalMin = min
                            }
                        } else {
                            finalHour = h
                            finalMin = min
                        }

                        val cal = Calendar.getInstance().apply {
                            set(y, m, d, finalHour, finalMin)
                        }
                        val formatter = SimpleDateFormat(
                            "d MMMM yyyy HH:mm", Locale("ru")
                        )
                        currentDateTimeText = formatter.format(cal.time)
                    },
                    calendarNow.get(Calendar.HOUR_OF_DAY),
                    calendarNow.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendarNow.get(Calendar.YEAR),
            calendarNow.get(Calendar.MONTH),
            calendarNow.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = calendarNow.timeInMillis
        datePickerDialog.show()
    }

    var selectedMeasurement by remember { mutableStateOf(0) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    var bloodSugar by rememberRecordFloat(existingRecord, resetTrigger) { it.bloodSugar }
    var breadUnits by rememberRecordFloat(existingRecord, resetTrigger) { it.breadUnits }
    var generalInsulin by rememberRecordFloat(existingRecord, resetTrigger) { it.generalInsulin }
    var shortInsulin by rememberRecordFloat(existingRecord, resetTrigger) { it.shortInsulin }
    var longInsulin by rememberRecordFloat(existingRecord, resetTrigger) { it.longInsulin }
    var mixedInsulin by rememberRecordFloat(existingRecord, resetTrigger) { it.mixedInsulin }
    var weight by rememberRecordFloat(existingRecord, resetTrigger) { it.weight }
    var activityMinutes by rememberRecordFloat(existingRecord, resetTrigger) { it.activityMinutes }

    var noteText by remember(existingRecord, resetTrigger) {
        mutableStateOf(existingRecord?.note ?: "")
    }

    var selectedMealTime by remember(existingRecord, resetTrigger) {
        mutableIntStateOf(
            when (existingRecord?.mealType) {
                "до еды" -> 1
                "после еды" -> 2
                else -> 0
            }
        )
    }

    var selectedActivity by remember(
        existingRecord, resetTrigger
    ) { mutableStateOf(existingRecord?.activityName ?: "") }
    val scrollState = rememberScrollState()

    val currentSliderVal = when (selectedMeasurement) {
        0 -> bloodSugar
        1 -> breadUnits
        2 -> generalInsulin
        3 -> shortInsulin
        4 -> longInsulin
        5 -> mixedInsulin
        6 -> weight
        8 -> activityMinutes
        else -> 0f
    }

    val currentHeaderIcon = when(selectedMeasurement) {
        0 -> Icons.Default.Bloodtype
        1 -> Icons.Default.EmojiFoodBeverage
        2, 3, 4, 5 -> Icons.Default.MedicalServices
        6 -> Icons.Default.Scale
        7 -> Icons.Default.Comment
        8 -> Icons.Default.DirectionsWalk
        else -> Icons.Default.Bloodtype
    }

    Column (
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            if (existingRecord != null) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            dao.deleteRecord(existingRecord)
                            onRecordSavedOrUpdated()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить запись",
                        tint = Color.Red
                    )
                }
            } else {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = currentHeaderIcon,
                        contentDescription = "icon"
                    )
                }
            }

            Text(
                text = when(selectedMeasurement) {
                    0 -> "Сахар"
                    1 -> "Еда (ХЕ)"
                    2 -> "Инсулин"
                    3 -> "Инсулин короткий"
                    4 -> "Инсулин продленный"
                    5 -> "Инсулин смешанный"
                    6 -> "Вес"
                    7 -> "Заметка"
                    8 -> "Физическая активность"
                    else -> "Запись"
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = {
                    resetTrigger++
                    noteText = ""
                    selectedActivity = ""
                    selectedMealTime = 0
                    bloodSugar = 0f
                    breadUnits = 0f
                    generalInsulin = 0f
                    shortInsulin = 0f
                    longInsulin = 0f
                    mixedInsulin = 0f
                    weight = 0f
                    activityMinutes = 0f
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "reset"
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 10.dp)
        ) {
            val items = listOf(
                MeasurementItem(0, "Сахар", icon = Icons.Default.Bloodtype),
                MeasurementItem(1, "Еда", icon = Icons.Default.EmojiFoodBeverage),
                MeasurementItem(2, "Инсулин", icon = Icons.Default.MedicalServices, isVisible = showGeneralInsulin),
                MeasurementItem(3, "Инсулин короткий", textLabel = "И кор.", isVisible = showShortInsulin),
                MeasurementItem(4, "Инсулин продлённый", textLabel = "И пр.", isVisible = showLongInsulin),
                MeasurementItem(5, "Инсулин смешанный", textLabel = "mix", isVisible = showMixedInsulin),
                MeasurementItem(6, "Вес", icon = Icons.Default.Scale),
                MeasurementItem(7, "Заметка", icon = Icons.Default.Comment),
                MeasurementItem(8, "Активность", icon = Icons.Default.DirectionsWalk)
            )

            items.filter { it.isVisible }.forEach { item ->
                CategoryButton(
                    onClick = { selectedMeasurement = item.id },
                    isSelected = selectedMeasurement == item.id,
                    icon = item.icon,
                    textLabel = item.textLabel
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Text(
            text = currentDateTimeText,
            modifier = Modifier
                .clickable { openInternalDateTimePicker() }
                .border(shape = RoundedCornerShape(10.dp), width = 1.dp, color = Color.Black)
                .padding(horizontal = 25.dp, vertical = 4.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(10.dp))

        if (selectedMeasurement <= 6) {
            Sliders(
                selectedTypeIndex = selectedMeasurement,
                currentValue = currentSliderVal,
                resetTrigger = resetTrigger,
                onTypeChanged = { selectedMeasurement = it },
                onValueChanged = { newValue ->
                    when (selectedMeasurement) {
                        0 -> bloodSugar = newValue
                        1 -> breadUnits = newValue
                        2 -> generalInsulin = newValue
                        3 -> shortInsulin = newValue
                        4 -> longInsulin = newValue
                        5 -> mixedInsulin = newValue
                        6 -> weight = newValue
                    }
                }
            )
        } else if (selectedMeasurement == 8) {
            val activitiesList = listOf(
                Triple("Ходьба", Icons.Default.DirectionsWalk, "Ходьба"),
                Triple("Бег", Icons.Default.DirectionsRun, "Бег"),
                Triple("Йога", Icons.Default.SelfImprovement, "Йога"),
                Triple("Велосипед", Icons.Default.DirectionsBike, "Велосипед"),
                Triple("Плавание", Icons.Default.Pool, "Плавание")
            )

            if (selectedActivity.isNotEmpty()) {
                Sliders(
                    selectedTypeIndex = selectedMeasurement,
                    currentValue = activityMinutes,
                    resetTrigger = resetTrigger,
                    onTypeChanged = { selectedMeasurement = it },
                    onValueChanged = { newValue -> activityMinutes = newValue }
                )
            } else {
                Text(
                    text = "Выберите активность",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp)
            ) {
                activitiesList.forEach { (id, icon, label) ->
                    val isSelected = selectedActivity == id
                    Column(
                        modifier = Modifier
                            .clickable { selectedActivity = if (selectedActivity == id) "" else id }
                            .background(
                                color = if (isSelected) Color(0xFF2c3e6b) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp, color = Color(0xFF2c3e6b),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(40.dp),
                            color = if (isSelected) Color.White else Color(0xFF4c5e8b),
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color(0xFF2c3e6b) else Color.White)
                            }
                        }
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        if (selectedMeasurement == 7) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Введите комментарий...") },
                singleLine = false,
                minLines = 2,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        Row {
            MealText(
                "до еды",
                selectedMealTime == 1,
                onClick = { selectedMealTime = if (selectedMealTime == 1) 0 else 1 }
            )
            Spacer(Modifier.width(20.dp))
            MealText(
                "после еды",
                selectedMealTime == 2,
                onClick = { selectedMealTime = if (selectedMealTime == 2) 0 else 2 }
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (existingRecord == null) "подтвердить" else "сохранить изменения",
            modifier = Modifier
                .background(Color(0xFF4c5e8b), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    val hasData = bloodSugar > 0f || breadUnits > 0f ||
                            generalInsulin > 0f || shortInsulin > 0f ||
                            longInsulin > 0f || mixedInsulin > 0f ||
                            weight > 0f || noteText.isNotBlank() ||
                            selectedActivity.isNotEmpty()

                    if (!hasData) return@clickable

                    coroutineScope.launch {
                        val parsedTimestamp =
                            parseRecordTimestamp(
                                currentDateTimeText, System.currentTimeMillis()
                            )

                        val record = DiabetesRecord(
                            id = existingRecord?.id ?: 0L,
                            timestamp = parsedTimestamp,
                            dateTimeText = currentDateTimeText,
                            mealType = when (selectedMealTime) {
                                1 -> "до еды"
                                2 -> "после еды"
                                else -> null
                            },
                            bloodSugar = if (bloodSugar > 0f) bloodSugar else null,
                            breadUnits = if (breadUnits > 0f) breadUnits else null,
                            generalInsulin = if (generalInsulin > 0f) generalInsulin else null,
                            shortInsulin = if (shortInsulin > 0f) shortInsulin else null,
                            longInsulin = if (longInsulin > 0f) longInsulin else null,
                            mixedInsulin = if (mixedInsulin > 0f) mixedInsulin else null,
                            weight = if (weight > 0f) weight else null,
                            note = if (noteText.isNotBlank()) noteText else null,
                            activityName = if (selectedActivity.isNotEmpty()) selectedActivity else null,
                            activityMinutes = if (selectedActivity.isNotEmpty()) activityMinutes else null
                        )

                        if (existingRecord == null) {
                            dao.insertRecord(record)
                        } else {
                            dao.updateRecord(record)
                        }

                        resetTrigger++
                        noteText = ""
                        selectedActivity = ""
                        selectedMealTime = 0
                        bloodSugar = 0f
                        breadUnits = 0f
                        generalInsulin = 0f
                        shortInsulin = 0f
                        longInsulin = 0f
                        mixedInsulin = 0f
                        weight = 0f
                        activityMinutes = 0f

                        onRecordSavedOrUpdated()
                    }
                },
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
inline fun rememberRecordFloat(
    existingRecord: DiabetesRecord?,
    resetTrigger: Int,
    crossinline selector: (DiabetesRecord) -> Float?
): MutableFloatState {
    val initial = existingRecord?.let(selector) ?: 0f
    return remember(existingRecord, resetTrigger) { mutableFloatStateOf(initial) }
}

@Composable
fun CategoryButton(
    onClick: () -> Unit,
    isSelected: Boolean,
    icon: ImageVector? = null,
    textLabel: String? = null
) {
    Surface(
        modifier = Modifier.size(45.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(60.dp),
        color = if (isSelected) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    tint = Color.White,
                    contentDescription = null
                )
            } else if (textLabel != null) {
                Text(
                    text = textLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MealText(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text,
        modifier = Modifier
            .background(
                color = if (isSelected) Color(0xFF2c3e6b) else Color(0xFFe8e8e8),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = if (isSelected) Color.White else Color(0xFF808080),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}