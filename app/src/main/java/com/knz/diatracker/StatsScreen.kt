package com.knz.diatracker

import android.content.Context
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
import com.knz.diatracker.data.AppDatabase
import com.knz.diatracker.data.DiabetesRecord
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

enum class StatsPeriod(val title: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    ALL("Все")
}

fun parseRecordTimestamp(dateTimeText: String?, fallbackTimestamp: Long): Long {
    if (dateTimeText.isNullOrBlank()) return fallbackTimestamp

    val patterns = listOf(
        Pair("d MMMM yyyy HH:mm", Locale("ru")),
        Pair("d MMMM yyyy", Locale("ru")),
        Pair("dd.MM.yyyy HH:mm", Locale.getDefault()),
        Pair("dd.MM.yyyy HH-mm", Locale.getDefault()),
        Pair("yyyy-MM-dd HH:mm", Locale.getDefault()),
        Pair("dd.MM.yyyy", Locale.getDefault()),
        Pair("yyyy-MM-dd", Locale.getDefault())
    )

    for ((pattern, locale) in patterns) {
        try {
            val formatter = SimpleDateFormat(pattern, locale).apply {
                isLenient = false
            }
            val date = formatter.parse(dateTimeText)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return fallbackTimestamp
}

@Composable
fun StatsScreen(settings: UserSettings) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = database.diabetesDao()

    val allRecords by dao.getAllRecords().collectAsState(initial = emptyList())

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }

    val records = remember(allRecords, selectedPeriod) {
        if (allRecords.isEmpty()) return@remember emptyList<DiabetesRecord>()

        val sortedAll = allRecords.sortedBy { parseRecordTimestamp(it.dateTimeText, it.timestamp) }
        val latestRecordTime = parseRecordTimestamp(sortedAll.last().dateTimeText, sortedAll.last().timestamp)
        val calendar = Calendar.getInstance()

        when (selectedPeriod) {
            StatsPeriod.DAY -> {
                calendar.timeInMillis = latestRecordTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfDay..endOfDay
                }
            }
            StatsPeriod.WEEK -> {
                calendar.timeInMillis = latestRecordTime
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startOfWeek = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfWeek..(latestRecordTime + 86400000L)
                }
            }
            StatsPeriod.MONTH -> {
                calendar.timeInMillis = latestRecordTime
                calendar.add(Calendar.MONTH, -1)
                val startOfMonth = calendar.timeInMillis

                sortedAll.filter { record ->
                    val recordTime = parseRecordTimestamp(record.dateTimeText, record.timestamp)
                    recordTime in startOfMonth..(latestRecordTime + 86400000L)
                }
            }
            StatsPeriod.ALL -> sortedAll
        }
    }

    val chartTypes = remember(settings) {
        mutableListOf<ChartMetric>().apply {
            add(ChartMetric("Уровень сахара", "ммоль/л") { it.bloodSugar })
            add(ChartMetric("Хлебные единицы (ХЕ)", "ХЕ") { it.breadUnits })

            if (settings.general) {
                add(ChartMetric("Инсулин", "ед") { it.generalInsulin })
            }
            if (settings.short) {
                add(ChartMetric("Короткий инсулин", "ед") { it.shortInsulin })
            }
            if (settings.long) {
                add(ChartMetric("Продленный инсулин", "ед") { it.longInsulin })
            }
            if (settings.mixed) {
                add(ChartMetric("Смешанный инсулин", "ед") { it.mixedInsulin })
            }

            add(ChartMetric("Вес", "кг") { it.weight })
            add(ChartMetric("Физическая активность", "мин") { it.activityMinutes })
        }
    }

    var currentMetricIndex by remember { mutableStateOf(0) }
    if (currentMetricIndex >= chartTypes.size) {
        currentMetricIndex = 0
    }
    val currentMetric = chartTypes[currentMetricIndex]
    var showExportDialog by remember { mutableStateOf(false) }

    val totalRecords = records.size
    val avgBloodSugar = if (records.isNotEmpty()) {
        records.mapNotNull { it.bloodSugar }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }
    } else 0f

    val totalBreadUnits = records.mapNotNull { it.breadUnits }.sum()
    val totalGeneralInsulin = records.mapNotNull { it.generalInsulin }.sum()
    val totalShortInsulin = records.mapNotNull { it.shortInsulin }.sum()
    val totalLongInsulin = records.mapNotNull { it.longInsulin }.sum()
    val totalMixedInsulin = records.mapNotNull { it.mixedInsulin }.sum()
    val avgWeight = if (records.isNotEmpty()) {
        records.mapNotNull { it.weight }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }
    } else 0f
    val totalActivityMinutes = records.mapNotNull { it.activityMinutes }.sum()

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
                    onClick = { selectedPeriod = period },
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
                        currentMetricIndex = if (currentMetricIndex > 0) currentMetricIndex - 1 else chartTypes.size - 1
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color(0xFF2c3e6b))
                    }

                    Text(
                        text = currentMetric.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2c3e6b)
                    )

                    IconButton(onClick = {
                        currentMetricIndex = if (currentMetricIndex < chartTypes.size - 1) currentMetricIndex + 1 else 0
                    }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Вперед", tint = Color(0xFF2c3e6b))
                    }
                }

                Spacer(Modifier.height(10.dp))

                val sortedRecords = records.sortedBy { parseRecordTimestamp(it.dateTimeText, it.timestamp) }
                val metricValues = sortedRecords.mapNotNull { record ->
                    currentMetric.extractor(record)?.let { valPair -> record to valPair }
                }.sortedBy { parseRecordTimestamp(it.first.dateTimeText, it.first.timestamp) }

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
                } else {
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
                StatRowItem("Всего записей:", "$totalRecords")
                StatRowItem("Средний сахар:", String.format(Locale.US, "%.1f ммоль/л", avgBloodSugar))
                StatRowItem("Всего ХЕ:", String.format(Locale.US, "%.1f ХЕ", totalBreadUnits))

                if (settings.general) {
                    StatRowItem("Инсулин:", String.format(Locale.US, "%.1f ед", totalGeneralInsulin))
                }
                if (settings.short) {
                    StatRowItem("Короткий инсулин:", String.format(Locale.US, "%.1f ед", totalShortInsulin))
                }
                if (settings.long) {
                    StatRowItem("Продленный инсулин:", String.format(Locale.US, "%.1f ед", totalLongInsulin))
                }
                if (settings.mixed) {
                    StatRowItem("Смешанный инсулин:", String.format(Locale.US, "%.1f ед", totalMixedInsulin))
                }

                StatRowItem("Средний вес:", String.format(Locale.US, "%.1f кг", avgWeight))
                StatRowItem("Активность (всего):", "%.0f мин".format(totalActivityMinutes))
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                if (records.isEmpty()) {
                    Toast.makeText(context, "Нет данных за выбранный период для экспорта", Toast.LENGTH_SHORT).show()
                } else {
                    showExportDialog = true
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
        androidx.compose.ui.window.Dialog(onDismissRequest = { showExportDialog = false }) {
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
                            showExportDialog = false
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
                            showExportDialog = false
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
                            showExportDialog = false
                            exportToCsv(context, records, selectedPeriod.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c5e8b)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сохранить в CSV", color = Color.White)
                    }

                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Отмена", color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class ChartMetric(
    val title: String,
    val unit: String,
    val extractor: (DiabetesRecord) -> Float?
)

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
fun StatRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 15.sp, color = Color(0xFF2c3e6b), fontWeight = FontWeight.Bold)
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
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка экспорта в CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}