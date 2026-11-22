package com.knz.diatracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knz.diatracker.data.DiabetesRecord
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private fun formatOneDec(value: Float): String {
    val totalTenths = (value * 10f).roundToInt()
    val whole = totalTenths / 10
    val dec = abs(totalTenths % 10)
    return if (dec == 0) "$whole" else "$whole.$dec"
}

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.uiState.collectAsState()
    val showGeneralInsulin = settings.general
    val showShortInsulin = settings.short
    val showLongInsulin = settings.long
    val showMixedInsulin = settings.mixed

    val recordsList by viewModel.recordsList.collectAsState()

    val draftState = viewModel.draftInputState
    val editState = viewModel.editInputState
    val isEditDialogVisible = viewModel.isEditDialogVisible
    val selectedRecordForEdit = viewModel.selectedRecordForEdit
    val formattedDateTime = viewModel.formattedDateTimeText

    val context = LocalContext.current

    val openDateTimePicker = remember(
        context,
        viewModel.selectedYear,
        viewModel.selectedMonth,
        viewModel.selectedDay,
        viewModel.selectedHour,
        viewModel.selectedMinute
    ) {
        {
            val calendarNow = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, y, m, d ->
                    viewModel.updateDate(y, m, d)
                    val isToday = (y == calendarNow.get(Calendar.YEAR) &&
                            m == calendarNow.get(Calendar.MONTH) &&
                            d == calendarNow.get(Calendar.DAY_OF_MONTH))

                    TimePickerDialog(
                        context,
                        { _, h, min ->
                            if (isToday) {
                                val selectedMinutesTotal = h * 60 + min
                                val currentMinutesTotal = calendarNow.get(Calendar.HOUR_OF_DAY) * 60 + calendarNow.get(Calendar.MINUTE)
                                if (selectedMinutesTotal > currentMinutesTotal) {
                                    viewModel.updateTime(calendarNow.get(Calendar.HOUR_OF_DAY), calendarNow.get(Calendar.MINUTE))
                                } else {
                                    viewModel.updateTime(h, min)
                                }
                            } else {
                                viewModel.updateTime(h, min)
                            }
                        },
                        viewModel.selectedHour,
                        viewModel.selectedMinute,
                        true
                    ).show()
                },
                viewModel.selectedYear,
                viewModel.selectedMonth,
                viewModel.selectedDay
            )
            datePickerDialog.datePicker.maxDate = calendarNow.timeInMillis
            datePickerDialog.show()
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
                .verticalScroll(scrollState)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Мой дневник",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(15.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(15.dp),
                color = Color.White,
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp)) {
                    GlucoseRecordScreen(
                        showGeneralInsulin = showGeneralInsulin,
                        showShortInsulin = showShortInsulin,
                        showLongInsulin = showLongInsulin,
                        showMixedInsulin = showMixedInsulin,
                        dateTimeText = formattedDateTime,
                        onDateTimeClick = openDateTimePicker,
                        inputState = draftState,
                        onMeasurementChanged = viewModel::updateDraftMeasurement,
                        onValueChanged = viewModel::updateDraftValue,
                        onMealTimeChanged = viewModel::updateDraftMealTime,
                        onActivityChanged = viewModel::updateDraftActivity,
                        onNoteChanged = viewModel::updateDraftNote,
                        onReset = viewModel::resetDraftInputState,
                        onSaveRecord = { viewModel.saveDraftRecord(formattedDateTime) },
                        isEditMode = false,
                        thresholdMode = settings.thresholdMode
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp)
            ) {
                Text(
                    text = "История записей",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (recordsList.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("История пуста", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recordsList.forEach { record ->
                                RecordItemRow(
                                    record = record,
                                    onClick = { viewModel.startEditingRecord(record) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (isEditDialogVisible && editState != null && selectedRecordForEdit != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {}
        )

        AlertDialog(
            onDismissRequest = viewModel::closeEditDialog,
            confirmButton = {},
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Редактирование записи",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF2c3e6b)
                    )
                    Spacer(Modifier.height(10.dp))

                    GlucoseRecordScreen(
                        showGeneralInsulin = showGeneralInsulin,
                        showShortInsulin = showShortInsulin,
                        showLongInsulin = showLongInsulin,
                        showMixedInsulin = showMixedInsulin,
                        dateTimeText = selectedRecordForEdit.dateTimeText,
                        onDateTimeClick = openDateTimePicker,
                        inputState = editState,
                        onMeasurementChanged = viewModel::updateEditMeasurement,
                        onValueChanged = viewModel::updateEditValue,
                        onMealTimeChanged = viewModel::updateEditMealTime,
                        onActivityChanged = viewModel::updateEditActivity,
                        onNoteChanged = viewModel::updateEditNote,
                        onReset = { viewModel.startEditingRecord(selectedRecordForEdit) },
                        onSaveRecord = { viewModel.saveEditRecord(selectedRecordForEdit.dateTimeText) },
                        onDeleteRecord = { viewModel.deleteRecord(selectedRecordForEdit) },
                        isEditMode = true,
                        thresholdMode = settings.thresholdMode
                    )
                }
            }
        )
    }
}

@Composable
private fun RecordItemRow(
    record: DiabetesRecord,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF5F6F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.dateTimeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2c3e6b)
                )
                record.mealType?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            record.bloodSugar?.let {
                Text(
                    text = "Сахар: ${formatOneDec(it)} ммоль/л",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.breadUnits?.let {
                Text(
                    text = "ХЕ: ${formatOneDec(it)}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.generalInsulin?.let {
                Text(
                    text = "Инсулин: ${formatOneDec(it)} ед",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.shortInsulin?.let {
                Text(
                    text = "Инсулин кор.: ${formatOneDec(it)} ед",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.longInsulin?.let {
                Text(
                    text = "Инсулин прод.: ${formatOneDec(it)} ед",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.mixedInsulin?.let {
                Text(
                    text = "Инсулин смеш.: ${formatOneDec(it)} ед",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.weight?.let {
                Text(
                    text = "Вес: ${formatOneDec(it)} кг",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            record.activityName?.let { activity ->
                val mins = record.activityMinutes?.toInt() ?: 0
                Text(
                    text = "🏃 $activity ($mins мин)",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            if (!record.note.isNullOrBlank()) {
                Text(
                    text = "💬 ${record.note}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
