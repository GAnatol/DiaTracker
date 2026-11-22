package com.knz.diatracker

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
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

private val SoftGray = Color(0xFFB0B0B0)

private fun formatValue(value: Float, hasDecimal: Boolean, isInitial: Boolean): String {
    if (!hasDecimal) return value.roundToInt().toString()

    val totalTenths = (value * 10f).roundToInt()
    if (totalTenths == 0) return "0.0"

    val whole = totalTenths / 10
    val dec = abs(totalTenths % 10)

    return if (dec == 0) {
        if (isInitial) "0.0" else whole.toString()
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
        roundToTenths(currentValue)
    } else {
        currentValue.roundToInt().toFloat()
    }

    var internalValue by remember(safeIndex, resetTrigger) {
        mutableFloatStateOf(initialRounded)
    }

    var isInitial by remember(safeIndex, resetTrigger) {
        mutableStateOf(currentValue == 0f)
    }

    LaunchedEffect(currentValue, safeIndex, resetTrigger) {
        if (abs(internalValue - currentValue) > 0.05f) {
            internalValue = currentValue
            isInitial = (currentValue == 0f)
        }
    }

    var isFieldFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(safeIndex) {
        focusManager.clearFocus()
    }

    val wholePart = internalValue.toInt().toFloat()

    val decimalPart = if (currentType.hasDecimal) {
        (((internalValue - wholePart) * 10f).roundToInt() % 10).toFloat().coerceIn(0f, 9f)
    } else {
        0f
    }

    var textState by remember(safeIndex, resetTrigger) {
        val formatted = formatValue(internalValue, currentType.hasDecimal, isInitial)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    fun syncTextFromValue(value: Float) {
        if (isFieldFocused) return
        val formatted = formatValue(value, currentType.hasDecimal, isInitial)
        if (textState.text != formatted) {
            textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }
    }

    var wasAtMax by remember(safeIndex) { mutableStateOf(false) }

    fun updateValueFromSlider(newVal: Float, commitExternal: Boolean = false) {
        val clamped = newVal.coerceAtLeast(0f)
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
        }

        val isAtMax = roundVal >= currentType.maxWhole
        if (isAtMax != wasAtMax) {
            wasAtMax = isAtMax
            if (isAtMax) {
                if (thresholdMode == ThresholdKeyboardMode.AUTO_KEYBOARD && !isFieldFocused) {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                }
            } else {
                if (isFieldFocused) {
                    focusManager.clearFocus()
                }
            }
        }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color(0xFF2c3e6b),
        activeTrackColor = Color(0xFF2c3e6b)
    )

    val textColor = if (internalValue == 0f && isInitial && !isFieldFocused) SoftGray else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
                        isInitial = true
                        onValueChanged(0f)
                    } else {
                        val parsed = filtered.toFloatOrNull()
                        if (parsed != null) {
                            val valid = parsed.coerceAtLeast(0f)
                            val updated = if (currentType.hasDecimal) {
                                roundToTenths(valid)
                            } else {
                                valid.roundToInt().toFloat()
                            }
                            internalValue = updated
                            isInitial = false
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
                    color = textColor,
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
                value = wholePart.coerceIn(currentType.minWhole, currentType.maxWhole),
                onValueChange = { newWhole ->
                    if (isFieldFocused) {
                        focusManager.clearFocus()
                    }
                    isInitial = false
                    val updated = newWhole + (decimalPart / 10f)
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
                    isInitial = false
                    val updated = wholePart + (newDec / 10f)
                    updateValueFromSlider(updated, commitExternal = false)
                },
                onValueChangeFinished = {
                    updateValueFromSlider(internalValue, commitExternal = true)
                },
                enabled = true,
                valueRange = 0f..9f,
                steps = 8,
                colors = sliderColors
            )
        }
    }
}
