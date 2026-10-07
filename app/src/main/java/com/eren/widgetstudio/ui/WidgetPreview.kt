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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eren.widgetstudio.catalog.Block
import com.eren.widgetstudio.catalog.RenderEnv
import com.eren.widgetstudio.catalog.Tone
import com.eren.widgetstudio.catalog.WidgetSpecs
import com.eren.widgetstudio.data.AndroidSystemProbe
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.accentArgb
import com.eren.widgetstudio.data.bgArgb
import com.eren.widgetstudio.data.textArgb
import com.eren.widgetstudio.logic.DateMath
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val TR = Locale.forLanguageTag("tr-TR")

/** Uygulama içindeki canlı önizleme: ana ekrandaki widget'ın benzeri (aynı blok listesini çizer). */
@Composable
fun WidgetPreview(d: WidgetDesign, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val spec = WidgetSpecs.of(d.type)
    val blocks = remember(d) {
        runCatching { spec.render(d, RenderEnv(probe = AndroidSystemProbe(context))) }
            .getOrElse { listOf(Block.Label("Önizleme çizilemedi", 12f)) }
    }
    val alignTop = spec.alignTop || !d.centered
    val scrolls = blocks.any { it is Block.Scroll }
    val style = remember(d) { PreviewStyle(d) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(d.cornerRadius.dp))
            .background(Color(d.bgArgb()))
            .padding(d.padding.dp),
        contentAlignment = if (alignTop) Alignment.TopStart else Alignment.Center,
    ) {
        Column(
            modifier = if (scrolls) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            horizontalAlignment = if (d.centered && !spec.alignTop) Alignment.CenterHorizontally else Alignment.Start,
        ) { PreviewBlocks(blocks, style) }
    }
}

private class PreviewStyle(val d: WidgetDesign) {
    private val text = Color(d.textArgb())
    private val accent = Color(d.accentArgb())
    private val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

    fun color(tone: Tone): Color = when (tone) {
        Tone.TEXT -> text
        Tone.ACCENT -> accent
        Tone.MUTED -> text.copy(alpha = 0.65f)
        Tone.ON_ACCENT -> onAccent
    }

    fun sp(size: Float) = (size * d.fontScale).sp
}

@Composable
private fun PreviewBlocks(blocks: List<Block>, s: PreviewStyle) {
    blocks.forEach { PreviewBlock(it, s) }
}

@Composable
private fun PreviewBlock(b: Block, s: PreviewStyle) {
    when (b) {
        is Block.Label -> Text(
            text = b.text,
            color = s.color(b.tone),
            fontSize = s.sp(b.size),
            fontWeight = if (b.bold) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (b.strike) TextDecoration.LineThrough else null,
            maxLines = if (b.maxLines > 0) b.maxLines else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )

        is Block.Meter -> LinearProgressIndicator(
            progress = { b.fraction },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = s.color(b.tone),
            trackColor = s.color(Tone.TEXT).copy(alpha = 0.2f),
        )

        is Block.Gap -> Spacer(Modifier.height(b.dp.dp))

        is Block.Clock -> {
            var now by remember { mutableStateOf(Date()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = Date()
                    delay(1000)
                }
            }
            val fmt = remember(b.format, b.zone) {
                SimpleDateFormat(b.format, TR).apply { b.zone?.let { timeZone = TimeZone.getTimeZone(it) } }
            }
            Text(fmt.format(now), color = s.color(b.tone), fontSize = s.sp(b.size), fontWeight = FontWeight.Medium, maxLines = 1)
        }

        is Block.Chrono -> {
            val start = remember(b) { System.currentTimeMillis() }
            var now by remember(b) { mutableStateOf(start) }
            LaunchedEffect(b) {
                while (b.running) {
                    now = System.currentTimeMillis()
                    delay(500)
                }
            }
            val delta = if (b.running) now - start else 0
            val shown = if (b.countDown) b.ms - delta else b.ms + delta
            Text(DateMath.formatDuration(shown), color = s.color(b.tone), fontSize = s.sp(b.size), fontWeight = FontWeight.Medium, maxLines = 1)
        }

        is Block.Btn -> Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(s.color(Tone.ACCENT))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(b.label, color = s.color(Tone.ON_ACCENT), fontSize = s.sp(13f), fontWeight = FontWeight.Medium, maxLines = 1)
        }

        is Block.Group -> {
            var mod: Modifier = Modifier
            if (b.fill != null) {
                mod = mod.clip(RoundedCornerShape(8.dp)).background(s.color(b.fill)).padding(horizontal = 4.dp, vertical = 2.dp)
            }
            if (b.horizontal) {
                Row(mod, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    b.children.forEach { PreviewBlock(it, s) }
                }
            } else {
                Column(mod, horizontalAlignment = if (b.center) Alignment.CenterHorizontally else Alignment.Start) {
                    PreviewBlocks(b.children, s)
                }
            }
        }

        is Block.Split -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { PreviewBlock(b.left, s) }
            PreviewBlock(b.right, s)
        }

        is Block.Table -> Column(Modifier.fillMaxWidth()) {
            b.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    row.forEach { cell ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { PreviewBlock(cell, s) }
                    }
                }
            }
        }

        is Block.Scroll -> Column { b.children.take(8).forEach { PreviewBlock(it, s) } }
    }
}
