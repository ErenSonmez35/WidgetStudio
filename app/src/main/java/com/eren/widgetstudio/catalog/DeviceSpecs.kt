package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.NetworkKind
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.logic.DateMath
import java.util.Locale

object DeviceSpecs {
    val all: List<WidgetSpec> = listOf(SystemSpec, BatterySpec, NetworkSpec, DeviceInfoSpec)
}

private fun gb(bytes: Long) = String.format(Locale.US, "%.0f", bytes / 1e9)

object SystemSpec : WidgetSpec {
    override val type = WidgetType.SYSTEM
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(centered = false, accentColor = 0xFFA6E3A1)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val rows = buildList<Block> {
            if (d.showBattery) env.probe.battery()?.let {
                add(meterRow("🔋 Pil", "%${it.percent}" + if (it.charging) " ⚡" else "", it.percent / 100f))
            }
            if (d.showStorage) env.probe.storage()?.let {
                add(meterRow("💾 Depolama", "${gb(it.usedBytes)} / ${gb(it.totalBytes)} GB", it.usedBytes.toFloat() / it.totalBytes))
            }
            if (d.showRam) env.probe.ram()?.let {
                val pct = (it.usedBytes * 100 / it.totalBytes).toInt()
                add(meterRow("🧠 RAM", "%$pct", pct / 100f))
            }
        }
        return if (rows.isEmpty()) listOf(body("Gösterilecek bilgi seçilmedi", 12f))
        else listOf(Block.Group(spaced(rows), tap = Tap(ACTION_REFRESH)))
    }
}

object BatterySpec : WidgetSpec {
    override val type = WidgetType.BATTERY
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(accentColor = 0xFFA6E3A1)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val b = env.probe.battery() ?: return listOf(body("Pil bilgisi okunamadı"))
        val state = when {
            b.charging && b.source.isNotEmpty() -> "⚡ Şarj oluyor · ${b.source}"
            b.charging -> "⚡ Şarj oluyor"
            else -> "Pil kullanılıyor"
        }
        return listOf(
            Block.Group(
                listOf(
                    big("%${b.percent}", 40f),
                    Block.Gap(2),
                    Block.Meter(b.percent / 100f),
                    Block.Gap(4),
                    body(state, 12f, Tone.ACCENT),
                    muted("%.1f°C · %.2f V · %s".format(Locale.US, b.temperatureC, b.voltageV, b.health), 11f),
                ),
                tap = Tap(ACTION_REFRESH),
                center = true,
            ),
        )
    }
}

object NetworkSpec : WidgetSpec {
    override val type = WidgetType.NETWORK
    override val wide = false

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val n = env.probe.network()
        val icon = when (n.kind) {
            NetworkKind.WIFI -> "📶"
            NetworkKind.MOBILE -> "📱"
            NetworkKind.ETHERNET -> "🔌"
            NetworkKind.OTHER -> "🌐"
            NetworkKind.NONE -> "🚫"
        }
        return listOf(
            Block.Group(
                listOf(
                    Block.Label(icon, 36f),
                    title(n.kind.label),
                    when {
                        n.kind == NetworkKind.NONE -> muted("İnternet yok", 12f)
                        n.hasInternet -> muted("İnternet var" + if (n.downMbps > 0) " · ~${n.downMbps} Mbps" else "", 12f)
                        else -> muted("Bağlı ama internet doğrulanamadı", 12f)
                    },
                ),
                tap = Tap(ACTION_REFRESH),
                center = true,
            ),
        )
    }
}

object DeviceInfoSpec : WidgetSpec {
    override val type = WidgetType.DEVICE
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(centered = false, accentColor = 0xFFF9E2AF)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val i = env.probe.device()
        val rows = buildList<Block> {
            add(infoRow("Model", i.model))
            add(infoRow("Android", "${i.androidVersion} (API ${i.sdk})"))
            if (i.securityPatch.isNotBlank()) add(infoRow("Güvenlik yaması", i.securityPatch))
            add(infoRow("Ekran", "${i.screenWidthPx}×${i.screenHeightPx}"))
            add(infoRow("Açık kalma", DateMath.formatDuration(i.uptimeMs)))
        }
        return listOf(title("📱 Cihaz"), Block.Gap(6), Block.Group(spaced(rows, 5), tap = Tap(ACTION_REFRESH)))
    }
}
