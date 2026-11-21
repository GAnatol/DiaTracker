package com.knz.diatracker

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

data class MeasurementType(
    val title: String,
    val unit: String,
    val minWhole: Float,
    val maxWhole: Float,
    val wholeSteps: Int,
    val hasDecimal: Boolean
)

private fun formatValue(value: Float, hasDecimal: Boolean): String {
    if (!hasDecimal) return value.roundToInt().toString()

    val totalTenths = (value * 10f).roundToInt()
    if (totalTenths == 0) return "0.0"

    val whole = totalTenths / 10
    val dec = abs(totalTenths % 10)

    return if (dec == 0) {
        whole.toString()
    } else {
        "$whole.$dec"
    }
}

private fun roundToTenths(value: Float): Float = (value * 10f).roundToInt() / 10f

@Composable
fun Sliders(
    selectedTypeIndex: Int,
    currentValue: Float,
    resetTrigger: Int,
    thresholdMode: ThresholdKeyboardMode = ThresholdKeyboardMode.AUTO_KEYBOARD,
    onTypeChanged: (Int) -> Unit,
    onValueChanged: (Float) -> Unit
) {
    val measurementTypes = remember {
        listOf(
            MeasurementType("Сахар", "ммоль/л", 0f, 20f, 19, true),              // 0
            MeasurementType("ХЕ", "хе", 0f, 15f, 14, true),                     // 1
            MeasurementType("Инсулин", "ед", 0f, 20f, 19, true),                // 2
            MeasurementType("Инсулин кор.", "ед", 0f, 20f, 19, true),           // 3
            MeasurementType("Инсулин прод.", "ед", 0f, 90f, 89, false),         // 4
            MeasurementType("Инсулин смеш.", "ед", 0f, 20f, 19, true),          // 5
            MeasurementType("Вес", "кг", 0f, 150f, 149, true),                  // 6
            MeasurementType("Физическая активность", "мин", 0f, 120f, 11, false) // 7
        )
    }

    val safeIndex = selectedTypeIndex.coerceIn(0, measurementTypes.size - 1)
    val currentType = measurementTypes[safeIndex]

    val initialRounded = if (currentType.hasDecimal) {
        roundToTenths(currentValue.coerceIn(currentType.minWhole, currentType.maxWhole))
    } else {
        currentValue.coerceIn(currentType.minWhole, currentType.maxWhole).roundToInt().toFloat()
    }

    var internalValue by remember(safeIndex, resetTrigger) {
        mutableFloatStateOf(initialRounded)
    }

    var isFieldFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(safeIndex) {
        focusManager.clearFocus()
    }

    val wholePart = internalValue.toInt().toFloat()
        .coerceIn(currentType.minWhole, currentType.maxWhole)

    val decimalPart = if (currentType.hasDecimal) {
        (((internalValue - wholePart) * 10f).roundToInt() % 10).toFloat().coerceIn(0f, 9f)
    } else {
        0f
    }

    var textState by remember(safeIndex, resetTrigger) {
        val formatted = formatValue(internalValue, currentType.hasDecimal)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    fun syncTextFromValue(value: Float) {
        if (isFieldFocused) return
        val formatted = formatValue(value, currentType.hasDecimal)
        if (textState.text != formatted) {
            textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }
    }

    fun handleThresholdFinished(value: Float) {
        if (thresholdMode == ThresholdKeyboardMode.AUTO_KEYBOARD && value >= currentType.maxWhole) {
            if (internalValue != currentType.maxWhole) {
                internalValue = currentType.maxWhole
                syncTextFromValue(currentType.maxWhole)
                onValueChanged(currentType.maxWhole)
            }
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    fun updateValueFromSlider(newVal: Float, commitExternal: Boolean = false) {
        val clamped = newVal.coerceIn(currentType.minWhole, currentType.maxWhole)
        val roundVal = if (currentType.hasDecimal) {
            roundToTenths(clamped)
        } else {
            clamped.roundToInt().toFloat()
        }

        if (internalValue != roundVal) {
            internalValue = roundVal
            syncTextFromValue(roundVal)
        }

        if (commitExternal) {
            onValueChanged(internalValue)
            handleThresholdFinished(internalValue)
        }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color(0xFF2c3e6b),
        activeTrackColor = Color(0xFF2c3e6b)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            BasicTextField(
                value = textState,
                onValueChange = { newValue ->
                    textState = newValue
                    val filtered = newValue.text.replace(',', '.').trim()
                    if (filtered.isEmpty()) {
                        internalValue = 0f
                        onValueChanged(0f)
                    } else {
                        val parsed = filtered.toFloatOrNull()
                        if (parsed != null) {
                            val valid = parsed.coerceIn(currentType.minWhole, currentType.maxWhole)
                            val updated = if (currentType.hasDecimal) {
                                roundToTenths(valid)
                            } else {
                                valid.roundToInt().toFloat()
                            }
                            internalValue = updated
                            onValueChanged(updated)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                textStyle = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .width(110.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFieldFocused = it.isFocused }
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = currentType.unit,
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = wholePart,
                onValueChange = { newWhole ->
                    if (isFieldFocused) {
                        focusManager.clearFocus()
                    }
                    val updated = if (newWhole >= currentType.maxWhole || !currentType.hasDecimal) {
                        newWhole
                    } else {
                        (newWhole + (decimalPart / 10f)).coerceAtMost(currentType.maxWhole)
                    }
                    updateValueFromSlider(updated, commitExternal = false)
                },
                onValueChangeFinished = {
                    updateValueFromSlider(internalValue, commitExternal = true)
                },
                valueRange = currentType.minWhole..currentType.maxWhole,
                steps = currentType.wholeSteps,
                colors = sliderColors,
                modifier = Modifier.weight(1f)
            )

            if (thresholdMode == ThresholdKeyboardMode.AUTO_KEYBOARD) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (internalValue >= currentType.maxWhole) {
                        Color(0xFF2c3e6b)
                    } else {
                        Color.Gray
                    }
                )
            } else if (thresholdMode == ThresholdKeyboardMode.PLUS_BUTTON) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ввести вручную",
                        tint = Color(0xFF2c3e6b)
                    )
                }
            }
        }

        if (currentType.hasDecimal) {
            Spacer(Modifier.height(4.dp))
            Slider(
                value = decimalPart,
                onValueChange = { newDec ->
                    if (isFieldFocused) {
                        focusManager.clearFocus()
                    }
                    val updated = if (wholePart >= currentType.maxWhole) {
                        currentType.maxWhole
                    } else {
                        (wholePart + (newDec / 10f)).coerceAtMost(currentType.maxWhole)
                    }
                    updateValueFromSlider(updated, commitExternal = false)
                },
                onValueChangeFinished = {
                    updateValueFromSlider(internalValue, commitExternal = true)
                },
                valueRange = 0f..9f,
                steps = 8,
                colors = sliderColors
            )
        }
    }
}