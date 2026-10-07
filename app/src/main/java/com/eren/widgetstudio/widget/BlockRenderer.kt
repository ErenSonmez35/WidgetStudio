package com.eren.widgetstudio.widget

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.os.SystemClock
import com.eren.widgetstudio.R
import com.eren.widgetstudio.catalog.Block
import com.eren.widgetstudio.catalog.Tap
import com.eren.widgetstudio.catalog.Tone
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.accentArgb
import com.eren.widgetstudio.data.textArgb
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent

/** [Block] listesini Glance (ana ekran widget'ı) ile çizer. Önizlemedeki karşılığı ui/BlockPreview.kt. */
class GlanceStyle(val context: Context, val design: WidgetDesign) {
    private val text = Color(design.textArgb())
    private val accent = Color(design.accentArgb())
    val onAccent: Color = if (accent.luminance() > 0.5f) Color.Black else Color.White

    fun color(tone: Tone): Color = when (tone) {
        Tone.TEXT -> text
        Tone.ACCENT -> accent
        Tone.MUTED -> text.copy(alpha = 0.65f)
        Tone.ON_ACCENT -> onAccent
    }

    fun provider(tone: Tone) = ColorProvider(color(tone))

    fun sp(size: Float) = (size * design.fontScale).sp
}

@Composable
fun GlanceBlocks(blocks: List<Block>, s: GlanceStyle) {
    blocks.forEach { GlanceBlock(it, s) }
}

private fun tapAction(s: GlanceStyle, tap: Tap) = actionRunCallback<BlockAction>(
    actionParametersOf(DesignIdKey to s.design.id, ActionKey to tap.action, IndexKey to tap.arg),
)

private fun GlanceModifier.tappable(s: GlanceStyle, tap: Tap?): GlanceModifier =
    if (tap == null) this else clickable(tapAction(s, tap))

@Composable
private fun GlanceBlock(b: Block, s: GlanceStyle) {
    when (b) {
        is Block.Label -> Text(
            text = b.text,
            maxLines = if (b.maxLines > 0) b.maxLines else Int.MAX_VALUE,
            style = TextStyle(
                color = s.provider(b.tone),
                fontSize = s.sp(b.size),
                fontWeight = if (b.bold) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (b.strike) TextDecoration.LineThrough else TextDecoration.None,
            ),
        )

        is Block.Meter -> LinearProgressIndicator(
            progress = b.fraction,
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = s.provider(b.tone),
            backgroundColor = ColorProvider(s.color(Tone.TEXT).copy(alpha = 0.2f)),
        )

        is Block.Gap -> if (b.dp > 0) Spacer(GlanceModifier.height(b.dp.dp))

        is Block.Clock -> AndroidRemoteViews(
            clockViews(s.context, R.layout.clock_big, b.format, b.size * s.design.fontScale, s.color(b.tone), b.zone),
        )

        is Block.Chrono -> AndroidRemoteViews(
            chronoViews(s.context, b, b.size * s.design.fontScale, s.color(b.tone)),
        )

        is Block.Btn -> {
            val click = when {
                b.launch != null -> startActivityWithIntent(Intent(b.launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                b.tap != null -> tapAction(s, b.tap)
                else -> null
            }
            var mod = GlanceModifier
                .background(s.color(Tone.ACCENT))
                .cornerRadius(10.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
            if (click != null) mod = mod.clickable(click)
            Box(modifier = mod, contentAlignment = Alignment.Center) {
                Text(
                    text = b.label,
                    maxLines = 1,
                    style = TextStyle(color = s.provider(Tone.ON_ACCENT), fontSize = s.sp(13f), fontWeight = FontWeight.Medium),
                )
            }
        }

        is Block.Group -> {
            var mod = GlanceModifier.tappable(s, b.tap)
            if (b.fill != null) mod = mod.background(s.color(b.fill)).cornerRadius(8.dp).padding(horizontal = 4.dp, vertical = 2.dp)
            if (b.horizontal) {
                Row(modifier = mod, verticalAlignment = Alignment.CenterVertically) {
                    b.children.forEachIndexed { i, child ->
                        if (i > 0 && child is Block.Btn) Spacer(GlanceModifier.width(6.dp))
                        GlanceBlock(child, s)
                    }
                }
            } else {
                Column(
                    modifier = mod,
                    horizontalAlignment = if (b.center) Alignment.CenterHorizontally else Alignment.Start,
                ) { GlanceBlocks(b.children, s) }
            }
        }

        is Block.Split -> Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.defaultWeight()) { GlanceBlock(b.left, s) }
            GlanceBlock(b.right, s)
        }

        is Block.Table -> Column(modifier = GlanceModifier.fillMaxWidth()) {
            b.rows.forEach { row ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    row.forEach { cell ->
                        Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            GlanceBlock(cell, s)
                        }
                    }
                }
            }
        }

        is Block.Scroll -> LazyColumn {
            itemsIndexed(b.children) { _, child -> GlanceBlock(child, s) }
        }
    }
}

/** TextClock: sistem tarafından kendiliğinden güncellenir, pil yemez. */
private fun clockViews(context: Context, layout: Int, format: String, sizeSp: Float, color: Color, zone: String?) =
    RemoteViews(context.packageName, layout).apply {
        setCharSequence(R.id.clock, "setFormat24Hour", format)
        setCharSequence(R.id.clock, "setFormat12Hour", format)
        if (zone != null) setString(R.id.clock, "setTimeZone", zone)
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.clock, color.toArgb())
    }

private fun chronoViews(context: Context, b: Block.Chrono, sizeSp: Float, color: Color) =
    RemoteViews(context.packageName, R.layout.chronometer_big).apply {
        val now = SystemClock.elapsedRealtime()
        // Kronometre: base = şimdi - geçen süre. Geri sayım: base = şimdi + kalan süre.
        val base = if (b.countDown) now + b.ms else now - b.ms
        setChronometerCountDown(R.id.chrono, b.countDown)
        setChronometer(R.id.chrono, base, null, b.running)
        setTextViewTextSize(R.id.chrono, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.chrono, color.toArgb())
    }
