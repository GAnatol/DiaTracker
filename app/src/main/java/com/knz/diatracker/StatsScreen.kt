package com.knz.diatracker

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knz.diatracker.data.DiabetesRecord
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val statsState by viewModel.statsUiState.collectAsState()

    val selectedPeriod = statsState.selectedPeriod
    val chartTypes = statsState.availableMetrics
    val currentMetricIndex = statsState.currentMetricIndex
    val currentMetric = statsState.currentMetric
    val records = statsState.periodRecords
    val metricValues = statsState.metricValues

    val showExportDialog = statsState.showExportDialog

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2c3e6b))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Статистика и аналитика",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                Button(
                    onClick = { viewModel.setStatsPeriod(period) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.White else Color(0xFF4c5e8b)
                    ),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = period.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF2c3e6b) else Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        val nextIndex = if (currentMetricIndex > 0) currentMetricIndex - 1 else chartTypes.size - 1
                        viewModel.setMetricIndex(nextIndex)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color(0xFF2c3e6b))
                    }

                    Text(
                        text = currentMetric?.title ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2c3e6b)
                    )

                    IconButton(onClick = {
                        val nextIndex = if (currentMetricIndex < chartTypes.size - 1) currentMetricIndex + 1 else 0
                        viewModel.setMetricIndex(nextIndex)
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Вперед", tint = Color(0xFF2c3e6b))
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (metricValues.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Недостаточно данных за выбранный период (нужно от 2 записей)",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (currentMetric != null) {
                    MetricLineChart(data = metricValues, unit = currentMetric.unit, period = selectedPeriod)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Сводные показатели (${selectedPeriod.title.lowercase()})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2c3e6b)
                )
                Spacer(Modifier.height(4.dp))
                StatRowItem("Всего записей:", "${statsState.totalRecords}")
                StatRowItem("Средний сахар:", String.format(Locale.US, "%.1f ммоль/л", statsState.avgBloodSugar))
                StatRowItem("Всего ХЕ:", String.format(Locale.US, "%.1f ХЕ", statsState.totalBreadUnits))

                if (statsState.totalGeneralInsulin > 0f || chartTypes.any { it.title == "Инсулин" }) {
                    StatRowItem("Инсулин:", String.format(Locale.US, "%.1f ед", statsState.totalGeneralInsulin))
                }
                if (statsState.totalShortInsulin > 0f || chartTypes.any { it.title == "Короткий инсулин" }) {
                    StatRowItem("Короткий инсулин:", String.format(Locale.US, "%.1f ед", statsState.totalShortInsulin))
                }
                if (statsState.totalLongInsulin > 0f || chartTypes.any { it.title == "Продленный инсулин" }) {
                    StatRowItem("Продленный инсулин:", String.format(Locale.US, "%.1f ед", statsState.totalLongInsulin))
                }
                if (statsState.totalMixedInsulin > 0f || chartTypes.any { it.title == "Смешанный инсулин" }) {
                    StatRowItem("Смешанный инсулин:", String.format(Locale.US, "%.1f ед", statsState.totalMixedInsulin))
                }

                StatRowItem("Средний вес:", String.format(Locale.US, "%.1f кг", statsState.avgWeight))
                StatRowItem("Активность (всего):", "%.0f мин".format(statsState.totalActivityMinutes))
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                if (records.isEmpty()) {
                    Toast.makeText(context, "Нет данных за выбранный период для экспорта", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setShowExportDialog(true)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c5e8b)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Экспортировать отчет (${selectedPeriod.title})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showExportDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.setShowExportDialog(false) }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Экспорт отчета (${selectedPeriod.title})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2c3e6b))
                    Text(text = "Выберите формат файла:", fontSize = 14.sp, color = Color.Gray)

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.setShowExportDialog(false)
                            generatePdfReport(context, records, selectedPeriod.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2c3e6b)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сохранить в PDF", color = Color.White)
                    }

                    Button(
                        onClick = {
                            viewModel.setShowExportDialog(false)
                            exportToExcel(context, records, selectedPeriod.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2c3e6b)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сохранить в Excel (.xls)", color = Color.White)
                    }

                    Button(
                        onClick = {
                            viewModel.setShowExportDialog(false)
                            exportToCsv(context, records, selectedPeriod.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c5e8b)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сохранить в CSV", color = Color.White)
                    }

                    TextButton(onClick = { viewModel.setShowExportDialog(false) }) {
                        Text("Отмена", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricLineChart(data: List<Pair<DiabetesRecord, Float>>, unit: String, period: StatsPeriod) {
    if (data.isEmpty()) return

    val values = data.map { it.second }
    val rawMax = values.maxOrNull() ?: 10f
    val maxValue = if (rawMax <= 5f) 10f else (rawMax * 1.2f).coerceAtLeast(5f)
    val minValue = 0f

    val lineColor = Color(0xFF2c3e6b)
    val pointColor = Color(0xFF4c5e8b)
    val gridColor = Color.LightGray
    val textColor = AndroidColor.DKGRAY

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 16.dp, top = 25.dp, bottom = 30.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = if (data.size > 1) width / (data.size - 1) else width

            val nativeCanvas = drawContext.canvas.nativeCanvas
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 22f
                color = textColor
            }

            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * i / gridLines
                val labelValue = maxValue - (maxValue - minValue) * i / gridLines

                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )

                val labelText = String.format(Locale.US, "%.0f", labelValue)
                nativeCanvas.drawText(
                    labelText,
                    -34f,
                    y + 8f,
                    paint.apply { textAlign = Paint.Align.RIGHT; textSize = 22f }
                )
            }

            val path = Path()
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, (_, value) ->
                val x = index * stepX
                val y = height - (value - minValue) / (maxValue - minValue) * height
                points.add(Offset(x, y))

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f)
            )

            val dateFormat = when (period) {
                StatsPeriod.DAY -> SimpleDateFormat("HH:mm", Locale.getDefault())
                StatsPeriod.WEEK -> SimpleDateFormat("E, dd.MM", Locale.getDefault())
                StatsPeriod.MONTH, StatsPeriod.ALL -> SimpleDateFormat("dd.MM", Locale.getDefault())
            }

            points.forEachIndexed { index, point ->
                drawCircle(
                    color = pointColor,
                    radius = 5f,
                    center = point
                )

                val exactValText = String.format(Locale.US, "%.1f", data[index].second)
                val textY = (point.y - 22f).coerceAtLeast(15f)

                nativeCanvas.drawText(
                    exactValText,
                    point.x,
                    textY,
                    paint.apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 24f
                        isFakeBoldText = true
                        color = AndroidColor.BLACK
                    }
                )

                val record = data[index].first
                val recordMillis = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                val timeStr = dateFormat.format(Date(recordMillis))
                nativeCanvas.drawText(
                    timeStr,
                    point.x,
                    height + 25f,
                    paint.apply { textAlign = Paint.Align.CENTER; textSize = 18f; isFakeBoldText = false; color = textColor }
                )
            }
        }
    }
}

@Composable
fun History() {

}

@Composable
fun StatRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 15.sp, color = Color(0xFF2c3e6b), fontWeight = FontWeight.Bold)
    }
}

fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Поделиться отчетом")
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Не удалось открыть меню поделиться: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun generatePdfReport(context: Context, records: List<DiabetesRecord>, periodTitle: String) {
    val pdfDocument = PdfDocument()
    val titlePaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 18f
        isFakeBoldText = true
    }
    val regularPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 12f
    }

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    var yPos = 50f
    val xMargin = 40f

    canvas.drawText("Дневник диабета — Отчет ($periodTitle)", xMargin, yPos, titlePaint)
    yPos += 20f

    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText("Дата формирования: $dateStr", xMargin, yPos, regularPaint)
    yPos += 40f

    records.forEachIndexed { index, record ->
        if (yPos > 780f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
        }

        val recordHeader = "Запись #${records.size - index} от ${record.dateTimeText} (${record.mealType ?: "время не указано"})"
        canvas.drawText(recordHeader, xMargin, yPos, titlePaint.apply { textSize = 12f; isFakeBoldText = true })
        yPos += 18f

        val details = buildString {
            record.bloodSugar?.let { append("Сахар: $it ммоль/л  ") }
            record.breadUnits?.let { append("ХЕ: $it  ") }
            record.generalInsulin?.let { append("Инс: $it ед  ") }
            record.shortInsulin?.let { append("ИК: $it ед  ") }
            record.longInsulin?.let { append("ИП: $it ед  ") }
            record.mixedInsulin?.let { append("Mix: $it ед  ") }
            record.weight?.let { append("Вес: $it кг  ") }
            record.activityName?.let { append("Активность: $it (${record.activityMinutes?.toInt() ?: 0} мин)") }
        }

        if (details.isNotBlank()) {
            canvas.drawText(details, xMargin, yPos, regularPaint)
            yPos += 16f
        }

        if (!record.note.isNullOrBlank()) {
            canvas.drawText("Заметка: ${record.note}", xMargin, yPos, regularPaint)
            yPos += 16f
        }

        yPos += 10f
    }

    pdfDocument.finishPage(page)

    try {
        val fileName = "Diabetes_Report_${periodTitle}_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = if (downloadsDir.exists()) File(downloadsDir, fileName) else File(context.filesDir, fileName)

        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        Toast.makeText(context, "PDF сохранен в: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        shareFile(context, file, "application/pdf")
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка сохранения PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        pdfDocument.close()
    }
}

fun exportToExcel(context: Context, records: List<DiabetesRecord>, periodTitle: String) {
    try {
        val fileName = "Diabetes_Report_${periodTitle}_${System.currentTimeMillis()}.xls"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = if (downloadsDir.exists()) File(downloadsDir, fileName) else File(context.filesDir, fileName)

        val htmlContent = buildString {
            append("<html><head><meta charset=\"UTF-8\"></head><body>")
            append("<h3>Отчет: Дневник диабета ($periodTitle)</h3>")
            append("<table border='1'>")
            append("<tr style='background-color: #2c3e6b; color: white;'>")
            append("<th>Дата и время</th><th>Тип приема</th><th>Сахар (ммоль/л)</th><th>ХЕ</th><th>Инсулин общ.</th><th>Инсулин кор.</th><th>Инсулин прод.</th><th>Инсулин смеш.</th><th>Вес (кг)</th><th>Активность</th><th>Минуты</th><th>Заметка</th>")
            append("</tr>")

            records.forEach { record ->
                append("<tr>")
                append("<td>${record.dateTimeText}</td>")
                append("<td>${record.mealType ?: ""}</td>")
                append("<td>${record.bloodSugar ?: ""}</td>")
                append("<td>${record.breadUnits ?: ""}</td>")
                append("<td>${record.generalInsulin ?: ""}</td>")
                append("<td>${record.shortInsulin ?: ""}</td>")
                append("<td>${record.longInsulin ?: ""}</td>")
                append("<td>${record.mixedInsulin ?: ""}</td>")
                append("<td>${record.weight ?: ""}</td>")
                append("<td>${record.activityName ?: ""}</td>")
                append("<td>${record.activityMinutes?.toInt() ?: ""}</td>")
                append("<td>${record.note ?: ""}</td>")
                append("</tr>")
            }
            append("</table></body></html>")
        }

        file.writeText(htmlContent)
        Toast.makeText(context, "Excel сохранен в: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        shareFile(context, file, "application/vnd.ms-excel")
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка экспорта в Excel: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun exportToCsv(context: Context, records: List<DiabetesRecord>, periodTitle: String) {
    try {
        val fileName = "Diabetes_Data_${periodTitle}_${System.currentTimeMillis()}.csv"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = if (downloadsDir.exists()) File(downloadsDir, fileName) else File(context.filesDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Дата и время;Тип приема пищи;Сахар (ммоль/л);ХЕ;Инсулин общ.;Инсулин кор.;Инсулин прод.;Инсулин смеш.;Вес (кг);Активность;Минуты;Заметка\n")

            records.forEach { record ->
                val line = listOf(
                    record.dateTimeText,
                    record.mealType ?: "",
                    record.bloodSugar?.toString() ?: "",
                    record.breadUnits?.toString() ?: "",
                    record.generalInsulin?.toString() ?: "",
                    record.shortInsulin?.toString() ?: "",
                    record.longInsulin?.toString() ?: "",
                    record.mixedInsulin?.toString() ?: "",
                    record.weight?.toString() ?: "",
                    record.activityName ?: "",
                    record.activityMinutes?.toString() ?: "",
                    (record.note ?: "").replace(";", ",")
                ).joinToString(";")
                writer.append("$line\n")
            }
        }

        Toast.makeText(context, "CSV сохранен в: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        shareFile(context, file, "text/csv")
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка экспорта в CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
