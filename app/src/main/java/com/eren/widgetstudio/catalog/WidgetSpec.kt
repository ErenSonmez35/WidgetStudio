package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.SystemProbe
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Çizim sırasında widget'ın ihtiyaç duyduğu "dış dünya": saat ve cihaz bilgileri. */
class RenderEnv(
    val nowMs: Long = System.currentTimeMillis(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val probe: SystemProbe = SystemProbe.None,
) {
    val today: LocalDate get() = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val epochDay: Long get() = today.toEpochDay()
    val secondOfDay: Int get() = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime().toSecondOfDay()
}

/**
 * Bir widget türünün tanımı: varsayılan görünüm, içerik ve dokunma eylemleri.
 * Android'e bağımlı DEĞİLDİR (ayar ekranları ui/TypeOptions.kt içindedir).
 */
interface WidgetSpec {
    val type: WidgetType

    /** İçerik varsayılan olarak sol üste mi yaslansın (listeler gibi)? */
    val alignTop: Boolean get() = false

    /** Önizlemede geniş (dikdörtgen) mi gösterilsin, yoksa kare mi? */
    val wide: Boolean get() = true

    /** Yeni tasarım için başlangıç değerleri. */
    fun defaults(base: WidgetDesign): WidgetDesign = base

    fun render(d: WidgetDesign, env: RenderEnv): List<Block>

    /**
     * Bir [Tap] eylemini tasarıma uygular. Değişiklik yoksa null döndür.
     * Çağrı ana ekrandan arka planda gelir; kalıcı kaydı çağıran yapar.
     */
    fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? = null
}

object WidgetSpecs {
    private val specs: Map<WidgetType, WidgetSpec> by lazy {
        (TimeSpecs.all + PlanSpecs.all + ToolSpecs.all + DeviceSpecs.all + NatureSpecs.all)
            .associateBy { it.type }
    }

    fun of(type: WidgetType): WidgetSpec =
        specs[type] ?: error("$type için WidgetSpec kayıtlı değil (catalog/WidgetSpec.kt)")

    /** Hiçbir tür kayıtsız kalmasın diye testte kullanılır. */
    fun missing(): List<WidgetType> = WidgetType.entries.filter { it !in specs }
}

/** Yeni tasarım için türüne uygun başlangıç değerleri. */
fun newDesign(type: WidgetType): WidgetDesign =
    WidgetSpecs.of(type).defaults(WidgetDesign(name = type.label, type = type))
