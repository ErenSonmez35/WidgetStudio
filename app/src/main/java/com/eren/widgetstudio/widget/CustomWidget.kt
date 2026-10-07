package com.eren.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eren.widgetstudio.ConfigureActivity
import com.eren.widgetstudio.MainActivity
import com.eren.widgetstudio.R
import com.eren.widgetstudio.data.DATE_FORMAT
import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.SystemInfo
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.accentArgb
import com.eren.widgetstudio.data.bgArgb
import com.eren.widgetstudio.data.clockFormat
import com.eren.widgetstudio.data.textArgb
import kotlin.math.roundToInt
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent

/**
 * Ana ekrana eklenen tek widget türü. Her örneği, uygulamada seçilen
 * tasarımı (WidgetDesign) okuyup çizer.
 */
class CustomWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Durumu okumak bu içeriği ona abone eder: WidgetUpdater sürümü
            // artırdığında widget tasarımı yeniden okuyup yeniden çizilir.
            @Suppress("UNUSED_VARIABLE")
            val version = currentState<Preferences>()[WidgetUpdater.VERSION]

            val design = DesignRepository.designFor(context, appWidgetId)
            if (design == null) {
                EmptyContent(context, appWidgetId)
            } else {
                DesignContent(context, design)
            }
        }
    }
}

@Composable
private fun EmptyContent(context: Context, appWidgetId: Int) {
    val intent = Intent(context, ConfigureActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xCC1E1E2E))
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(startActivityWithIntent(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Tasarım seçmek için dokun",
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp),
        )
    }
}

@Composable
private fun DesignContent(context: Context, d: WidgetDesign) {
    val alignTop = d.type == WidgetType.TODO || !d.centered
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(d.bgArgb()))
            .cornerRadius(d.cornerRadius.dp)
            .padding(d.padding.dp)
            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
        contentAlignment = if (alignTop) Alignment.TopStart else Alignment.Center,
    ) {
        when (d.type) {
            WidgetType.CLOCK -> ClockContent(context, d)
            WidgetType.TODO -> TodoContent(d)
            WidgetType.WEATHER -> WeatherContent(d)
            WidgetType.SYSTEM -> SystemContent(context, d)
        }
    }
}

private fun clockViews(context: Context, layout: Int, format: String, sizeSp: Float, color: Int) =
    RemoteViews(context.packageName, layout).apply {
        setCharSequence(R.id.clock, "setFormat24Hour", format)
        setCharSequence(R.id.clock, "setFormat12Hour", format)
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.clock, color)
    }

@Composable
private fun ClockContent(context: Context, d: WidgetDesign) {
    Column(
        horizontalAlignment = if (d.centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        AndroidRemoteViews(
            clockViews(context, R.layout.clock_big, d.clockFormat(), 44f * d.fontScale, d.textArgb()),
        )
        if (d.clockShowDate) {
            Spacer(GlanceModifier.height(4.dp))
            AndroidRemoteViews(
                clockViews(context, R.layout.clock_small, DATE_FORMAT, 14f * d.fontScale, d.accentArgb()),
            )
        }
    }
}

@Composable
private fun TodoContent(d: WidgetDesign) {
    val text = ColorProvider(Color(d.textArgb()))
    val accent = ColorProvider(Color(d.accentArgb()))
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = d.todoTitle,
            style = TextStyle(color = accent, fontSize = (15 * d.fontScale).sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(6.dp))
        if (d.todos.isEmpty()) {
            Text("Görev yok 🎉", style = TextStyle(color = text, fontSize = (13 * d.fontScale).sp))
        } else {
            LazyColumn {
                itemsIndexed(d.todos) { index, item ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                actionRunCallback<ToggleTodoAction>(
                                    actionParametersOf(DesignIdKey to d.id, IndexKey to index),
                                ),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (item.done) "☑" else "☐",
                            style = TextStyle(color = accent, fontSize = (16 * d.fontScale).sp),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = item.text,
                            maxLines = 2,
                            style = TextStyle(
                                color = text,
                                fontSize = (14 * d.fontScale).sp,
                                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(d: WidgetDesign) {
    val text = ColorProvider(Color(d.textArgb()))
    val accent = ColorProvider(Color(d.accentArgb()))
    val w = d.weather
    val horizontal = if (d.centered) Alignment.CenterHorizontally else Alignment.Start

    if (w == null) {
        Column(
            horizontalAlignment = horizontal,
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        ) {
            Text(
                text = if (d.city.isBlank()) "Şehir seçilmedi" else d.city,
                style = TextStyle(color = accent, fontSize = (14 * d.fontScale).sp),
            )
            Text(
                text = "Güncellemek için dokun",
                style = TextStyle(color = text, fontSize = (12 * d.fontScale).sp),
            )
        }
    } else {
        WeatherLoaded(d, w, horizontal, text, accent)
    }
}

@Composable
private fun WeatherLoaded(
    d: WidgetDesign,
    w: com.eren.widgetstudio.data.WeatherData,
    horizontal: Alignment.Horizontal,
    text: ColorProvider,
    accent: ColorProvider,
) {
    val (emoji, desc) = WeatherApi.describe(w.code)
    Column(
        horizontalAlignment = horizontal,
        modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = TextStyle(fontSize = (30 * d.fontScale).sp))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "${w.temp.roundToInt()}°",
                style = TextStyle(color = text, fontSize = (38 * d.fontScale).sp, fontWeight = FontWeight.Medium),
            )
        }
        Text(
            text = d.city,
            style = TextStyle(color = accent, fontSize = (14 * d.fontScale).sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = "$desc · ↑${w.max.roundToInt()}° ↓${w.min.roundToInt()}°",
            style = TextStyle(color = text, fontSize = (12 * d.fontScale).sp),
        )
    }
}

@Composable
private fun SystemContent(context: Context, d: WidgetDesign) {
    val text = ColorProvider(Color(d.textArgb()))
    val accent = ColorProvider(Color(d.accentArgb()))
    val track = ColorProvider(Color(d.textArgb()).copy(alpha = 0.2f))
    val lines = SystemInfo.lines(context, d)

    Column(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionRunCallback<RefreshAction>()),
    ) {
        if (lines.isEmpty()) {
            Text("Gösterilecek bilgi seçilmedi", style = TextStyle(color = text, fontSize = 12.sp))
        }
        lines.forEachIndexed { i, line ->
            if (i > 0) Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = line.label,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = text, fontSize = (13 * d.fontScale).sp),
                )
                Text(
                    text = line.value,
                    style = TextStyle(color = accent, fontSize = (13 * d.fontScale).sp, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = line.fraction,
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = accent,
                backgroundColor = track,
            )
        }
    }
}
