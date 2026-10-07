package com.eren.widgetstudio.data

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.util.Locale

/** Widget ve önizlemede gösterilecek tek satır: etiket, değer, doluluk oranı (0..1). */
data class InfoLine(val label: String, val value: String, val fraction: Float)

object SystemInfo {

    fun lines(context: Context, design: WidgetDesign): List<InfoLine> {
        val result = mutableListOf<InfoLine>()

        if (design.showBattery) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            val charging = if (bm.isCharging) " ⚡" else ""
            result += InfoLine("🔋 Pil", "%$level$charging", level / 100f)
        }

        if (design.showStorage) {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes.toDouble()
            val used = total - stat.availableBytes
            val value = String.format(Locale.US, "%.0f / %.0f GB", used / 1e9, total / 1e9)
            result += InfoLine("💾 Depolama", value, (used / total).toFloat())
        }

        if (design.showRam) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val usedPct = ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt()
            result += InfoLine("🧠 RAM", "%$usedPct", usedPct / 100f)
        }

        return result
    }
}
