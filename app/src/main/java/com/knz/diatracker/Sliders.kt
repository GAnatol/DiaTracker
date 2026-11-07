package com.knz.diatracker

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onTypeChanged: (Int) -> Unit,
    onValueChanged: (Float) -> Unit
) {
    val measurementTypes = listOf(
        MeasurementType("Сахар", "ммоль/л", 0f, 20f, 19, true),        // 0
        MeasurementType("ХЕ", "хе", 0f, 15f, 14, true),               // 1
        MeasurementType("Инсулин", "ед", 0f, 20f, 19, true),          // 2
        MeasurementType("Инсулин кор.", "ед", 0f, 20f, 19, true),     // 3
        MeasurementType("Инсулин прод.", "ед", 0f, 90f, 89, false),   // 4
        MeasurementType("Инсулин смеш.", "ед", 0f, 20f, 19, true),    // 5
        MeasurementType("Вес", "кг", 30f, 120f, 89, true),            // 6
        MeasurementType("Физическая активность", "мин", 0f, 120f, 11, false) // 8
    )

    val safeIndex = selectedTypeIndex.coerceIn(0, measurementTypes.size - 1)
    val currentType = measurementTypes[safeIndex]

    var internalValue by remember(safeIndex, resetTrigger) { mutableFloatStateOf(currentValue) }

    val focusManager = LocalFocusManager.current

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
        val formatted = if (currentType.hasDecimal) String.format(java.util.Locale.US, "%.1f", internalValue) else "${internalValue.toInt()}"
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    LaunchedEffect(safeIndex, resetTrigger) {
        val formatted = if (currentType.hasDecimal) String.format(java.util.Locale.US, "%.1f", internalValue) else "${internalValue.toInt()}"
        textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
    }

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            BasicTextField(
                value = textState,
                onValueChange = { newValue ->
                    textState = newValue

                    val filtered = newValue.text.replace(',', '.')
                    val parsed = filtered.toFloatOrNull()
                    if (parsed != null) {
                        val valid = parsed.coerceAtLeast(0f)
                        internalValue = (valid * 10).roundToInt() / 10f
                        onValueChanged(internalValue)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

        Slider(
            value = wholePart.coerceIn(currentType.minWhole, currentType.maxWhole),
            onValueChange = { newWhole ->
                val diff = newWhole - wholePart
                val updated = (internalValue + diff).coerceAtLeast(0f)
                internalValue = (updated * 10).roundToInt() / 10f

                val formatted = if (currentType.hasDecimal) String.format(java.util.Locale.US, "%.1f", internalValue) else "${internalValue.toInt()}"
                textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))

                onValueChanged(internalValue)

                if (newWhole >= currentType.maxWhole) {
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
            },
            valueRange = currentType.minWhole..currentType.maxWhole,
            steps = currentType.wholeSteps
        )

        if (currentType.hasDecimal) {
            Spacer(Modifier.height(4.dp))
            Slider(
                value = decimalPart,
                onValueChange = { newDec ->
                    val updated = (wholePart + (newDec / 10f)).coerceAtLeast(0f)
                    internalValue = (updated * 10).roundToInt() / 10f

                    val formatted = String.format(java.util.Locale.US, "%.1f", internalValue)
                    textState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))

                    onValueChanged(internalValue)
                },
                valueRange = 0f..9f,
                steps = 8
            )
        }
    }
}