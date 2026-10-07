package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.opt
import kotlin.math.roundToInt
import kotlin.random.Random

object ToolSpecs {
    val all: List<WidgetSpec> = listOf(CounterSpec, WaterSpec, DiceSpec, QuickSettingsSpec)
}

/** Sayaç ve su takibi aynı mantığı kullanır; "günlük sıfırla" açıksa sayaç her gün 0'dan başlar. */
abstract class BaseCounter : WidgetSpec {
    protected fun value(d: WidgetDesign, env: RenderEnv): Int =
        if (d.opt("dailyReset") && d.stateDay != env.epochDay) 0 else d.counter

    protected fun valueText(d: WidgetDesign, v: Int): String =
        if (d.unit.isBlank()) "$v" else "$v ${d.unit}"

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? {
        val step = d.step.coerceAtLeast(1)
        val v = value(d, env)
        val next = when (action) {
            "inc" -> v + step
            "dec" -> (v - step).coerceAtLeast(0)
            "reset" -> 0
            else -> return null
        }
        return d.copy(counter = next, stateDay = env.epochDay)
    }
}

object CounterSpec : BaseCounter() {
    override val type = WidgetType.COUNTER
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(label = "Sayaç", accentColor = 0xFF94E2D5)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val v = value(d, env)
        return buildList<Block> {
            add(title(d.label.ifBlank { "Sayaç" }))
            add(big(valueText(d, v), 40f))
            if (d.goal > 0) {
                add(Block.Gap(2))
                add(Block.Meter((v.toFloat() / d.goal).coerceIn(0f, 1f)))
                add(muted("hedef: ${d.goal}", 11f))
            }
            add(Block.Gap(6))
            add(
                Block.Group(
                    listOf(
                        Block.Btn("−${d.step}", Tap("dec")),
                        Block.Btn("+${d.step}", Tap("inc")),
                        Block.Btn("↺", Tap("reset")),
                    ),
                    horizontal = true,
                ),
            )
        }
    }
}

object WaterSpec : BaseCounter() {
    override val type = WidgetType.WATER
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(
        label = "Su takibi", unit = "bardak", goal = 8, step = 1,
        bgColor = 0xFF12304A, accentColor = 0xFF74C7EC,
    ).let { it.copy(opts = it.opts + ("dailyReset" to true)) }

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val v = value(d, env)
        val goal = d.goal.coerceAtLeast(1)
        val drops = if (goal <= 12) "💧".repeat(v.coerceAtMost(goal)) + "▫️".repeat((goal - v).coerceAtLeast(0)) else ""
        return buildList<Block> {
            add(Block.Group(listOf(title("💧 ${d.label.ifBlank { "Su" }}")), tap = Tap("inc"), center = true))
            add(
                Block.Group(
                    listOf(
                        big("$v / $goal", 34f),
                        body(d.unit.ifBlank { "bardak" }, 12f, Tone.MUTED),
                        if (drops.isNotEmpty()) Block.Label(drops, 14f) else Block.Gap(0),
                        Block.Gap(2),
                        Block.Meter((v.toFloat() / goal).coerceIn(0f, 1f)),
                        muted("dokun → +${d.step}  ·  %${((v.toFloat() / goal) * 100).roundToInt().coerceAtMost(999)}", 10f),
                    ),
                    tap = Tap("inc"),
                    center = true,
                ),
            )
            add(Block.Gap(4))
            add(Block.Group(listOf(Block.Btn("−", Tap("dec")), Block.Btn("↺", Tap("reset"))), horizontal = true))
        }
    }
}

object DiceSpec : WidgetSpec {
    override val type = WidgetType.DICE
    override val wide = false

    private val FACES = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    override fun defaults(base: WidgetDesign) = base.copy(step = 6, accentColor = 0xFFF9E2AF)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val sides = d.step.coerceIn(2, 100)
        val rolled = d.counter in 1..sides
        val result = when {
            !rolled -> "🎲"
            sides == 2 -> if (d.counter == 1) "Yazı" else "Tura"
            sides == 6 -> FACES[d.counter - 1]
            else -> d.counter.toString()
        }
        return listOf(
            Block.Group(
                listOf(
                    Block.Label(result, if (sides == 6 && rolled) 56f else 44f, Tone.TEXT),
                    body(if (sides == 2) "yazı-tura" else "d$sides", 13f, Tone.ACCENT),
                    if (d.text.isNotBlank()) muted(d.text, 11f) else muted("dokun → at", 11f),
                ),
                tap = Tap("roll"),
                center = true,
            ),
        )
    }

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? {
        if (action != "roll") return null
        val sides = d.step.coerceIn(2, 100)
        val value = Random(env.nowMs xor d.id.hashCode().toLong()).nextInt(1, sides + 1)
        val history = (listOf(value.toString()) + d.text.split(" ").filter { it.isNotBlank() }).take(5)
        return d.copy(counter = value, text = history.joinToString(" "))
    }
}

/** Ayar sayfalarını açan kısayol butonları. Hiçbir izin gerektirmez. */
object QuickSettingsSpec : WidgetSpec {
    override val type = WidgetType.QUICK_SETTINGS

    data class Shortcut(val key: String, val label: String, val action: String, val default: Boolean)

    val SHORTCUTS = listOf(
        Shortcut("wifi", "📶 Wi-Fi", "android.settings.WIFI_SETTINGS", true),
        Shortcut("bluetooth", "🔷 Bluetooth", "android.settings.BLUETOOTH_SETTINGS", true),
        Shortcut("display", "☀️ Ekran", "android.settings.DISPLAY_SETTINGS", true),
        Shortcut("sound", "🔊 Ses", "android.settings.SOUND_SETTINGS", true),
        Shortcut("battery", "🔋 Pil", "android.intent.action.POWER_USAGE_SUMMARY", false),
        Shortcut("location", "📍 Konum", "android.settings.LOCATION_SOURCE_SETTINGS", false),
        Shortcut("airplane", "✈️ Uçak modu", "android.settings.AIRPLANE_MODE_SETTINGS", false),
        Shortcut("apps", "🧩 Uygulamalar", "android.settings.APPLICATION_SETTINGS", false),
        Shortcut("alarm", "⏰ Alarmlar", "android.intent.action.SHOW_ALARMS", false),
        Shortcut("camera", "📷 Kamera", "android.media.action.STILL_IMAGE_CAMERA", false),
        Shortcut("settings", "⚙️ Ayarlar", "android.settings.SETTINGS", false),
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val chosen = SHORTCUTS.filter { d.opt("sc_${it.key}", it.default) }
        if (chosen.isEmpty()) return listOf(body("Düzenleyiciden en az bir kısayol seç"))
        val rows = chosen.chunked(2).map { pair ->
            pair.map { Block.Btn(it.label, launch = it.action) as Block } +
                if (pair.size == 1) listOf(Block.Gap(0)) else emptyList()
        }
        return listOf(Block.Table(rows))
    }
}
