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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

data class MeasurementType(
    val title: String,
    val unit: String,
    val minWhole: Float,
    val maxWhole: Float,
    val wholeSteps: Int,
    val hasDecimal: Boolean
)

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
            MeasurementType("Сахар", "ммоль/л", 0f, 20f, 19, true),        // 0
            MeasurementType("ХЕ", "хе", 0f, 15f, 14, true),               // 1
            MeasurementType("Инсулин", "ед", 0f, 20f, 19, true),          // 2
            MeasurementType("Инсулин кор.", "ед", 0f, 20f, 19, true),     // 3
            MeasurementType("Инсулин прод.", "ед", 0f, 90f, 89, false),   // 4
            MeasurementType("Инсулин смеш.", "ед", 0f, 20f, 19, true),    // 5
            MeasurementType("Вес", "кг", 0f, 150f, 149, true),           // 6
            MeasurementType("Физическая активность", "мин", 0f, 120f, 11, false) // 8
        )
    }

    val safeIndex = selectedTypeIndex.coerceIn(0, measurementTypes.size - 1)
    val currentType = measurementTypes[safeIndex]

    var internalValue by remember(safeIndex, resetTrigger) { mutableFloatStateOf(currentValue) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(safeIndex) {
        focusManager.clearFocus()
    }

    LaunchedEffect(currentValue, safeIndex, resetTrigger) {
        if (internalValue != currentValue) {
            internalValue = currentValue
        }
    }

    val wholePart = internalValue.toInt().toFloat()

    val decimalPart = if (currentType.hasDecimal) {
        (((internalValue - wholePart) * 10).roundToInt() % 10).toFloat().coerceIn(0f, 9f)
    } else 0f

    var textState by remember(safeIndex, resetTrigger) {
        val formatted = if (currentType.hasDecimal) String.format(Locale.US, "%.1f", internalValue) else "${internalValue.toInt()}"
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    LaunchedEffect(internalValue, safeIndex, resetTrigger) {
        val formatted = if (currentType.hasDecimal) String.format(Locale.US, "%.1f", internalValue) else "${internalValue.toInt()}"
        if (textState.text != formatted) {
            val textStr = textState.text.replace(',', '.').trim()
            val currentFloat = textStr.toFloatOrNull()
            if (currentFloat == null || (currentFloat * 10).roundToInt() / 10f != internalValue) {
                textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
            }
        }
    }

    var wasAtMax by remember(safeIndex) { mutableStateOf(false) }

    fun updateValueFromSlider(newVal: Float) {
        val roundVal = (newVal * 10).roundToInt() / 10f
        if (internalValue != roundVal) {
            internalValue = roundVal
            val formatted = if (currentType.hasDecimal) String.format(Locale.US, "%.1f", roundVal) else "${roundVal.toInt()}"
            textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
            onValueChanged(roundVal)
        }

        val isAtMax = roundVal >= currentType.maxWhole
        if (isAtMax != wasAtMax) {
            wasAtMax = isAtMax
            if (isAtMax) {
                if (thresholdMode == ThresholdKeyboardMode.AUTO_KEYBOARD) {
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
            } else {
                focusManager.clearFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
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
                            val valid = parsed.coerceAtLeast(0f)
                            internalValue = (valid * 10).roundToInt() / 10f
                            onValueChanged(internalValue)
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
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.width(110.dp).focusRequester(focusRequester)
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
                    val diff = newWhole - wholePart
                    val updated = (internalValue + diff).coerceAtLeast(0f)
                    updateValueFromSlider(updated)
                },
                valueRange = currentType.minWhole..currentType.maxWhole,
                steps = currentType.wholeSteps,
                modifier = Modifier.weight(1f)
            )

            if (thresholdMode == ThresholdKeyboardMode.PLUS_BUTTON) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        try { focusRequester.requestFocus() } catch (_: Exception) {}
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
                    val updated = (wholePart + (newDec / 10f)).coerceAtLeast(0f)
                    updateValueFromSlider(updated)
                },
                valueRange = 0f..9f,
                steps = 8
            )
        }
    }
}
