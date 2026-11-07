package com.knz.diatracker

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knz.diatracker.data.AppDatabase
import com.knz.diatracker.data.DiabetesRecord
import kotlinx.coroutines.launch

@Composable
fun GlucoseRecordScreen(
    showGeneralInsulin: Boolean,
    showShortInsulin: Boolean,
    showLongInsulin: Boolean,
    showMixedInsulin: Boolean,
    dateTimeText: String,
    onDateTimeClick: () -> Unit,
    existingRecord: DiabetesRecord? = null,
    onRecordSavedOrUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.diabetesDao()

    var selectedMeasurement by remember { mutableStateOf(0) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    var bloodSugar by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.bloodSugar ?: 0f) }
    var breadUnits by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.breadUnits ?: 0f) }
    var generalInsulin by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.generalInsulin ?: 0f) }
    var shortInsulin by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.shortInsulin ?: 0f) }
    var longInsulin by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.longInsulin ?: 0f) }
    var mixedInsulin by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.mixedInsulin ?: 0f) }
    var weight by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.weight ?: 0f) }
    var activityMinutes by remember(existingRecord, resetTrigger) { mutableFloatStateOf(existingRecord?.activityMinutes ?: 0f) }


    var noteText by remember(existingRecord, resetTrigger) { mutableStateOf(existingRecord?.note ?: "") }

    var selectedMealTime by remember(existingRecord, resetTrigger) {
        mutableIntStateOf(
            when (existingRecord?.mealType) {
                "до еды" -> 1
                "после еды" -> 2
                else -> 0
            }
        )
    }

    var selectedActivity by remember(existingRecord, resetTrigger) { mutableStateOf(existingRecord?.activityName ?: "") }
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
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (existingRecord != null) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            dao.deleteRecord(existingRecord)
                            onRecordSavedOrUpdated()
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить запись",
                        tint = Color.Red
                    )
                }
            } else {
                Icon(
                    imageVector = currentHeaderIcon,
                    contentDescription = "icon",
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Spacer(Modifier.weight(1f))

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
                modifier = Modifier.weight(4f),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                softWrap = false
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "reset",
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clickable {
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
                    }
            )
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
            // Сахар
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 0 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 0) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bloodtype, tint = Color.White, contentDescription = null) } }
            // Еда
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 1 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 1) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.EmojiFoodBeverage, tint = Color.White, contentDescription = null) } }



            if (showGeneralInsulin) {
                Surface(
                    modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 2 },
                    shape = RoundedCornerShape(60.dp),
                    color = if (selectedMeasurement == 2) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                    shadowElevation = 2.dp
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MedicalServices, tint = Color.White, contentDescription = null) } }
            }
            if (showShortInsulin) {
                Surface(
                    modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 3 },
                    shape = RoundedCornerShape(60.dp),
                    color = if (selectedMeasurement == 3) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                    shadowElevation = 2.dp
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ИК", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
            }
            if (showLongInsulin) {
                Surface(
                    modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 4 },
                    shape = RoundedCornerShape(60.dp),
                    color = if (selectedMeasurement == 4) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                    shadowElevation = 2.dp
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ИП", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
            }
            if (showMixedInsulin) {
                Surface(
                    modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 5 },
                    shape = RoundedCornerShape(60.dp),
                    color = if (selectedMeasurement == 5) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                    shadowElevation = 2.dp
                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("mix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
            }


            // Вес
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 6 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 6) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Scale, tint = Color.White, contentDescription = null) } }
            // Заметка
            Surface(
                modifier = Modifier.size(45.dp).clickable {
                    selectedMeasurement = 7
                },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 7) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Comment, tint = Color.White, contentDescription = null) } }
            // Активность
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 8 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 8) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsWalk, tint = Color.White, contentDescription = null) } }
        }

        Spacer(Modifier.height(15.dp))

        Text(
            text = dateTimeText,
            modifier = Modifier
                .clickable { onDateTimeClick() }
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
                            .border(width = 1.dp, color = Color(0xFF2c3e6b), shape = RoundedCornerShape(14.dp))
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
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color(0xFF2c3e6b) else Color.White)
                            }
                        }
                        Text(text = label, fontSize = 12.sp, color = if (isSelected) Color.White else Color.Black)
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
                maxLines = 5
            )
        }

        Spacer(Modifier.height(10.dp))



        Row {
            Text(
                "до еды",
                modifier = Modifier
                    .background(
                        color = if (selectedMealTime == 1) Color(0xFF2c3e6b) else Color(0xFFe8e8e8),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedMealTime = if (selectedMealTime == 1) 0 else 1 }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (selectedMealTime == 1) Color.White else Color(0xFF808080),
                fontWeight = if (selectedMealTime == 1) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(Modifier.width(20.dp))
            Text(
                "после еды",
                modifier = Modifier
                    .background(
                        color = if (selectedMealTime == 2) Color(0xFF2c3e6b) else Color(0xFFe8e8e8),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedMealTime = if (selectedMealTime == 2) 0 else 2 }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (selectedMealTime == 2) Color.White else Color(0xFF808080),
                fontWeight = if (selectedMealTime == 2) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(Modifier.height(20.dp))



        Text(
            text = if (existingRecord == null) "подтвердить" else "сохранить изменения",
            modifier = Modifier
                .background(Color(0xFF4c5e8b), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    // Защита от пустых записей
                    val hasData = bloodSugar > 0f || breadUnits > 0f ||
                            generalInsulin > 0f || shortInsulin > 0f ||
                            longInsulin > 0f || mixedInsulin > 0f ||
                            weight > 0f || noteText.isNotBlank() ||
                            selectedActivity.isNotEmpty()

                    if (!hasData) return@clickable

                    coroutineScope.launch {
                        val record = DiabetesRecord(
                            id = existingRecord?.id ?: 0L,
                            timestamp = existingRecord?.timestamp ?: System.currentTimeMillis(),
                            dateTimeText = dateTimeText,
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

    @Composable
    fun OptionalRecords() {
        if (showGeneralInsulin) {
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 2 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 2) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.MedicalServices, tint = Color.White, contentDescription = null) } }
        }
        if (showShortInsulin) {
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 3 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 3) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ИК", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
        }
        if (showLongInsulin) {
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 4 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 4) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ИП", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
        }
        if (showMixedInsulin) {
            Surface(
                modifier = Modifier.size(45.dp).clickable { selectedMeasurement = 5 },
                shape = RoundedCornerShape(60.dp),
                color = if (selectedMeasurement == 5) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
                shadowElevation = 2.dp
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("mix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
        }
    }
}