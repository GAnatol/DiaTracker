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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MeasurementItem(
    val id: Int,
    val description: String,
    val icon: ImageVector? = null,
    val textLabel: String? = null,
    val isVisible: Boolean = true
)

private data class ActivityOption(
    val id: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun GlucoseRecordScreen(
    showGeneralInsulin: Boolean,
    showShortInsulin: Boolean,
    showLongInsulin: Boolean,
    showMixedInsulin: Boolean,
    dateTimeText: String,
    onDateTimeClick: () -> Unit,
    inputState: GlucoseInputState,
    onMeasurementChanged: (Int) -> Unit,
    onValueChanged: (Int, Float) -> Unit,
    onMealTimeChanged: (Int) -> Unit,
    onActivityChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onReset: () -> Unit,
    onSaveRecord: () -> Unit,
    onDeleteRecord: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    thresholdMode: ThresholdKeyboardMode = ThresholdKeyboardMode.AUTO_KEYBOARD
) {
    val scrollState = rememberScrollState()
    val activityScrollState = rememberScrollState()

    val selectedMeasurement = inputState.selectedMeasurement

    val currentSliderVal = remember(selectedMeasurement, inputState) {
        when (selectedMeasurement) {
            0 -> inputState.bloodSugar
            1 -> inputState.breadUnits
            2 -> inputState.generalInsulin
            3 -> inputState.shortInsulin
            4 -> inputState.longInsulin
            5 -> inputState.mixedInsulin
            6 -> inputState.weight
            8 -> inputState.activityMinutes
            else -> 0f
        }
    }

    val currentHeaderIcon = remember(selectedMeasurement) {
        when (selectedMeasurement) {
            0 -> Icons.Default.Bloodtype
            1 -> Icons.Default.EmojiFoodBeverage
            2, 3, 4, 5 -> Icons.Default.MedicalServices
            6 -> Icons.Default.Scale
            7 -> Icons.Default.Comment
            8 -> Icons.Default.DirectionsWalk
            else -> Icons.Default.Bloodtype
        }
    }

    val headerTitle = remember(selectedMeasurement) {
        when (selectedMeasurement) {
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
        }
    }

    val visibleCategoryItems = remember(
        showGeneralInsulin,
        showShortInsulin,
        showLongInsulin,
        showMixedInsulin
    ) {
        listOf(
            MeasurementItem(0, "Сахар", icon = Icons.Default.Bloodtype),
            MeasurementItem(1, "Еда", icon = Icons.Default.EmojiFoodBeverage),
            MeasurementItem(2, "Инсулин", icon = Icons.Default.MedicalServices, isVisible = showGeneralInsulin),
            MeasurementItem(3, "Инсулин короткий", textLabel = "И кор.", isVisible = showShortInsulin),
            MeasurementItem(4, "Инсулин продлённый", textLabel = "И пр.", isVisible = showLongInsulin),
            MeasurementItem(5, "Инсулин смешанный", textLabel = "mix", isVisible = showMixedInsulin),
            MeasurementItem(6, "Вес", icon = Icons.Default.Scale),
            MeasurementItem(7, "Заметка", icon = Icons.Default.Comment),
            MeasurementItem(8, "Активность", icon = Icons.Default.DirectionsWalk)
        ).filter { it.isVisible }
    }

    val activitiesList = remember {
        listOf(
            ActivityOption("Ходьба", Icons.Default.DirectionsWalk, "Ходьба"),
            ActivityOption("Бег", Icons.Default.DirectionsRun, "Бег"),
            ActivityOption("Йога", Icons.Default.SelfImprovement, "Йога"),
            ActivityOption("Велосипед", Icons.Default.DirectionsBike, "Велосипед"),
            ActivityOption("Плавание", Icons.Default.Pool, "Плавание")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            if (isEditMode && onDeleteRecord != null) {
                IconButton(
                    onClick = onDeleteRecord,
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
                text = headerTitle,
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
                onClick = onReset,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "reset"
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 10.dp)
        ) {
            visibleCategoryItems.forEach { item ->
                CategoryButton(
                    onClick = { onMeasurementChanged(item.id) },
                    isSelected = selectedMeasurement == item.id,
                    icon = item.icon,
                    textLabel = item.textLabel
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Text(
            text = dateTimeText,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDateTimeClick)
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
                resetTrigger = inputState.resetTrigger,
                thresholdMode = thresholdMode,
                onTypeChanged = onMeasurementChanged,
                onValueChanged = { newValue ->
                    onValueChanged(selectedMeasurement, newValue)
                }
            )
        } else if (selectedMeasurement == 8) {
            if (inputState.selectedActivity.isNotEmpty()) {
                Sliders(
                    selectedTypeIndex = selectedMeasurement,
                    currentValue = inputState.activityMinutes,
                    resetTrigger = inputState.resetTrigger,
                    thresholdMode = thresholdMode,
                    onTypeChanged = onMeasurementChanged,
                    onValueChanged = { newValue ->
                        onValueChanged(selectedMeasurement, newValue)
                    }
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
                    .horizontalScroll(activityScrollState)
                    .padding(horizontal = 10.dp)
            ) {
                activitiesList.forEach { option ->
                    val isSelected = inputState.selectedActivity == option.id
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onActivityChanged(if (inputState.selectedActivity == option.id) "" else option.id)
                            }
                            .background(
                                color = if (isSelected) Color(0xFF2c3e6b) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF2c3e6b),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (isSelected) Color.White else Color(0xFF4c5e8b),
                                    shape = RoundedCornerShape(40.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) Color(0xFF2c3e6b) else Color.White
                            )
                        }
                        Text(
                            text = option.label,
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
                value = inputState.noteText,
                onValueChange = onNoteChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                text = "до еды",
                isSelected = inputState.selectedMealTime == 1,
                onClick = {
                    onMealTimeChanged(if (inputState.selectedMealTime == 1) 0 else 1)
                }
            )
            Spacer(Modifier.width(20.dp))
            MealText(
                text = "после еды",
                isSelected = inputState.selectedMealTime == 2,
                onClick = {
                    onMealTimeChanged(if (inputState.selectedMealTime == 2) 0 else 2)
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (!isEditMode) "подтвердить" else "сохранить изменения",
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF4c5e8b))
                .clickable(role = Role.Button) {
                    val hasData = inputState.bloodSugar > 0f ||
                            inputState.breadUnits > 0f ||
                            inputState.generalInsulin > 0f ||
                            inputState.shortInsulin > 0f ||
                            inputState.longInsulin > 0f ||
                            inputState.mixedInsulin > 0f ||
                            inputState.weight > 0f ||
                            inputState.noteText.isNotBlank() ||
                            inputState.selectedActivity.isNotEmpty()

                    if (hasData) {
                        onSaveRecord()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryButton(
    onClick: () -> Unit,
    isSelected: Boolean,
    icon: ImageVector? = null,
    textLabel: String? = null
) {
    Surface(
        modifier = Modifier.size(45.dp),
        onClick = onClick,
        shape = RoundedCornerShape(60.dp),
        color = if (isSelected) Color(0xFF2c3e6b) else Color(0xFF4c5e8b),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                color = if (isSelected) Color(0xFF2c3e6b) else Color(0xFFe8e8e8)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = if (isSelected) Color.White else Color(0xFF808080),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}