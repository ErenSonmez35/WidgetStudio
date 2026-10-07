package com.eren.widgetstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eren.widgetstudio.data.DATE_FORMAT
import com.eren.widgetstudio.data.SystemInfo
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.accentArgb
import com.eren.widgetstudio.data.bgArgb
import com.eren.widgetstudio.data.clockFormat
import com.eren.widgetstudio.data.textArgb
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val TR = Locale.forLanguageTag("tr-TR")

/** Uygulama içindeki canlı önizleme: ana ekrandaki widget'ın birebir benzeri. */
@Composable
fun WidgetPreview(d: WidgetDesign, modifier: Modifier = Modifier) {
    val alignTop = d.type == WidgetType.TODO || !d.centered
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(d.cornerRadius.dp))
            .background(Color(d.bgArgb()))
            .padding(d.padding.dp),
        contentAlignment = if (alignTop) Alignment.TopStart else Alignment.Center,
    ) {
        when (d.type) {
            WidgetType.CLOCK -> ClockPreview(d)
            WidgetType.TODO -> TodoPreview(d)
            WidgetType.WEATHER -> WeatherPreview(d)
            WidgetType.SYSTEM -> SystemPreview(d)
        }
    }
}

@Composable
private fun ClockPreview(d: WidgetDesign) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    Column(horizontalAlignment = if (d.centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text = SimpleDateFormat(d.clockFormat(), TR).format(now),
            color = Color(d.textArgb()),
            fontSize = (44 * d.fontScale).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (d.clockShowDate) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat(DATE_FORMAT, TR).format(now),
                color = Color(d.accentArgb()),
                fontSize = (14 * d.fontScale).sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TodoPreview(d: WidgetDesign) {
    val text = Color(d.textArgb())
    val accent = Color(d.accentArgb())
    Column(Modifier.fillMaxSize()) {
        Text(d.todoTitle, color = accent, fontSize = (15 * d.fontScale).sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (d.todos.isEmpty()) {
            Text("Görev yok 🎉", color = text, fontSize = (13 * d.fontScale).sp)
        }
        d.todos.take(8).forEach { item ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.done) "☑" else "☐", color = accent, fontSize = (16 * d.fontScale).sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.text,
                    color = text,
                    fontSize = (14 * d.fontScale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

@Composable
private fun WeatherPreview(d: WidgetDesign) {
    val text = Color(d.textArgb())
    val accent = Color(d.accentArgb())
    val horizontal = if (d.centered) Alignment.CenterHorizontally else Alignment.Start
    val w = d.weather
    Column(horizontalAlignment = horizontal) {
        if (w == null) {
            Text(if (d.city.isBlank()) "Şehir seçilmedi" else d.city, color = accent, fontSize = (14 * d.fontScale).sp)
            Text(
                if (d.city.isBlank()) "Aşağıdan bir şehir ara" else "Veri yükleniyor…",
                color = text, fontSize = (12 * d.fontScale).sp,
            )
        } else {
            val (emoji, desc) = WeatherApi.describe(w.code)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = (30 * d.fontScale).sp)
                Spacer(Modifier.width(6.dp))
                Text("${w.temp.roundToInt()}°", color = text, fontSize = (38 * d.fontScale).sp, fontWeight = FontWeight.Medium)
            }
            Text(d.city, color = accent, fontSize = (14 * d.fontScale).sp, fontWeight = FontWeight.Bold)
            Text(
                "$desc · ↑${w.max.roundToInt()}° ↓${w.min.roundToInt()}°",
                color = text, fontSize = (12 * d.fontScale).sp,
            )
        }
    }
}

@Composable
private fun SystemPreview(d: WidgetDesign) {
    val context = LocalContext.current
    val text = Color(d.textArgb())
    val accent = Color(d.accentArgb())
    val lines = remember(d.showBattery, d.showStorage, d.showRam) { SystemInfo.lines(context, d) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (lines.isEmpty()) Text("Gösterilecek bilgi seçilmedi", color = text, fontSize = 12.sp)
        lines.forEach { line ->
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text(line.label, color = text, fontSize = (13 * d.fontScale).sp, modifier = Modifier.weight(1f))
                    Text(line.value, color = accent, fontSize = (13 * d.fontScale).sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { line.fraction },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = accent,
                    trackColor = text.copy(alpha = 0.2f),
                )
            }
        }
    }
}
