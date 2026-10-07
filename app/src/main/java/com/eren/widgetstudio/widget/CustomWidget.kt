package com.eren.widgetstudio.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eren.widgetstudio.ConfigureActivity
import com.eren.widgetstudio.MainActivity
import com.eren.widgetstudio.catalog.Block
import com.eren.widgetstudio.catalog.RenderEnv
import com.eren.widgetstudio.catalog.WidgetSpecs
import com.eren.widgetstudio.data.AndroidSystemProbe
import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.bgArgb
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent

/**
 * Ana ekrana eklenen tek widget sağlayıcısı. Her örneği, uygulamada seçilen
 * tasarımı (WidgetDesign) okuyup ilgili WidgetSpec'in ürettiği blokları çizer.
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
    val spec = WidgetSpecs.of(d.type)
    val env = RenderEnv(probe = AndroidSystemProbe(context))
    val blocks = runCatching { spec.render(d, env) }
        .getOrElse { listOf(Block.Label("Widget çizilemedi", 12f)) }
    val alignTop = spec.alignTop || !d.centered
    val scrolls = blocks.any { it is Block.Scroll }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(d.bgArgb()))
            .cornerRadius(d.cornerRadius.dp)
            .padding(d.padding.dp)
            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
        contentAlignment = if (alignTop) Alignment.TopStart else Alignment.Center,
    ) {
        Column(
            modifier = if (scrolls) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth(),
            horizontalAlignment = if (d.centered && !spec.alignTop) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            GlanceBlocks(blocks, GlanceStyle(context, d))
        }
    }
}
