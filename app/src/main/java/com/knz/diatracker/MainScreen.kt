package com.knz.diatracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.room.util.TableInfo
import com.knz.diatracker.data.DiabetesRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
) {
    val viewModel: MainViewModel = viewModel()
    val settings by viewModel.uiState.collectAsState()
    val showGeneralInsulin = settings.general
    val showShortInsulin = settings.short
    val showLongInsulin = settings.long
    val showMixedInsulin = settings.mixed

    val recordsList by viewModel.recordsList.collectAsState()

    var selectedRecordForEdit by remember { mutableStateOf<DiabetesRecord?>(null) }
    var isEditDialogVisible by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    val formattedDateTime = remember(year, month, day, hour, minute) {
        val cal = Calendar.getInstance().apply {
            set(year, month, day, hour, minute)
        }
        val formatter = SimpleDateFormat("d MMMM yyyy HH:mm", Locale("ru"))
        formatter.format(cal.time)
    }

    val context = LocalContext.current

    val openDateTimePicker = {
        val calendarNow = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, y, m, d ->
                year = y
                month = m
                day = d
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
                                hour = calendarNow.get(Calendar.HOUR_OF_DAY)
                                minute = calendarNow.get(Calendar.MINUTE)
                            } else {
                                hour = h
                                minute = min
                            }
                        } else {
                            hour = h
                            minute = min
                        }
                    },
                    hour,
                    minute,
                    true
                ).show()
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = calendarNow.timeInMillis
        datePickerDialog.show()
    }

    val mainScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
                .verticalScroll(mainScrollState)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

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
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            // Карточка ввода нового показателя
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // Высота подстраивается под содержимое
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
                        onDateTimeClick = openDateTimePicker
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
                        modifier = Modifier.fillMaxWidth().height(600.dp),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recordsList.forEach { record ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRecordForEdit = record
                                            isEditDialogVisible = true
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF5F6F8),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Верхняя строка: Дата и тип приёма пищи
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
                                                text = "Сахар: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } ммоль/л", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.breadUnits?.let {
                                            Text(
                                                text = "ХЕ: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                }", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.generalInsulin?.let {
                                            Text(
                                                text = "Инсулин: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } ед", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.shortInsulin?.let {
                                            Text(
                                                text = "Инсулин кор.: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } ед", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.longInsulin?.let {
                                            Text(
                                                text = "Инсулин прод.: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } ед", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.mixedInsulin?.let {
                                            Text(
                                                text = "Инсулин смеш.: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } ед", fontSize = 14.sp, color = Color.DarkGray
                                            )
                                        }
                                        record.weight?.let {
                                            Text(
                                                text = "Вес: ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f",
                                                        it
                                                    )
                                                } кг", fontSize = 14.sp, color = Color.DarkGray
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
                        }
                    }
                }
            }

        }

            Spacer(Modifier.height(40.dp))
    }


    if (isEditDialogVisible && selectedRecordForEdit != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {}
        )

        AlertDialog(
            onDismissRequest = { isEditDialogVisible = false },
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
                        dateTimeText = selectedRecordForEdit!!.dateTimeText,
                        onDateTimeClick = openDateTimePicker,
                        existingRecord = selectedRecordForEdit,
                        onRecordSavedOrUpdated = {
                            isEditDialogVisible = false
                        }
                    )
                }
            }
        )
    }
}
