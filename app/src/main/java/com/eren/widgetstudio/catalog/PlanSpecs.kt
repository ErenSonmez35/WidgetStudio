package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.TodoItem
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.logic.DateMath
import com.eren.widgetstudio.logic.Quotes
import java.time.LocalDate
import java.time.format.TextStyle

object PlanSpecs {
    val all: List<WidgetSpec> = listOf(
        TodoSpec, HabitSpec, CountdownSpec, DaysSinceSpec, AgeSpec, NoteSpec, QuoteSpec,
    )
}

private fun checkRow(text: String, done: Boolean, index: Int, size: Float): Block = Block.Group(
    listOf(
        Block.Label(if (done) "☑" else "☐", size + 2, Tone.ACCENT),
        Block.Label(text, size, Tone.TEXT, strike = done, maxLines = 2),
    ),
    horizontal = true,
    tap = Tap("toggle", index),
)

object TodoSpec : WidgetSpec {
    override val type = WidgetType.TODO
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(
        centered = false,
        todos = listOf(TodoItem("İlk görevim"), TodoItem("Widget'a dokunarak işaretle")),
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = buildList<Block> {
        add(title(d.todoTitle))
        add(Block.Gap(6))
        if (d.todos.isEmpty()) {
            add(body("Görev yok 🎉"))
        } else {
            add(Block.Scroll(d.todos.mapIndexed { i, t -> checkRow(t.text, t.done, i, 14f) }))
        }
    }

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? {
        if (action != "toggle") return null
        return d.copy(todos = d.todos.mapIndexed { i, t -> if (i == arg) t.copy(done = !t.done) else t })
    }
}

/** Yapılacaklar gibi, ama işaretler her gün kendiliğinden sıfırlanır. */
object HabitSpec : WidgetSpec {
    override val type = WidgetType.HABIT
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(
        centered = false,
        todoTitle = "Bugünkü alışkanlıklar",
        accentColor = 0xFFA6E3A1,
        todos = listOf(TodoItem("Su iç"), TodoItem("Yürüyüş"), TodoItem("Kitap oku")),
    )

    /** Bugüne ait işaretler; gün değişmişse hepsi boş sayılır. */
    private fun effective(d: WidgetDesign, env: RenderEnv): List<TodoItem> =
        if (d.stateDay == env.epochDay) d.todos else d.todos.map { it.copy(done = false) }

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val items = effective(d, env)
        val done = items.count { it.done }
        return buildList<Block> {
            add(Block.Split(title(d.todoTitle), Block.Label("$done/${items.size}", 13f, Tone.MUTED)))
            add(Block.Gap(4))
            if (items.isEmpty()) {
                add(body("Alışkanlık eklenmedi"))
            } else {
                add(Block.Meter(done.toFloat() / items.size))
                add(Block.Gap(4))
                add(Block.Scroll(items.mapIndexed { i, t -> checkRow(t.text, t.done, i, 14f) }))
            }
        }
    }

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? {
        if (action != "toggle") return null
        val items = effective(d, env).mapIndexed { i, t -> if (i == arg) t.copy(done = !t.done) else t }
        return d.copy(todos = items, stateDay = env.epochDay)
    }
}

private fun dateText(date: LocalDate): String =
    "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, TR)} ${date.year}, " +
        date.dayOfWeek.getDisplayName(TextStyle.FULL, TR)

object CountdownSpec : WidgetSpec {
    override val type = WidgetType.COUNTDOWN
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(
        label = "Büyük gün",
        targetEpochDay = LocalDate.now().plusDays(30).toEpochDay(),
        accentColor = 0xFFFAB387,
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val target = LocalDate.ofEpochDay(d.targetEpochDay)
        val days = DateMath.daysBetween(env.today, target)
        val (number, caption) = when {
            days > 0 -> days.toString() to "gün kaldı"
            days == 0L -> "🎉" to "Bugün!"
            else -> (-days).toString() to "gün önce"
        }
        return listOf(
            title(d.label.ifBlank { "Geri sayım" }),
            big(number, 52f),
            body(caption),
            Block.Gap(2),
            muted(dateText(target), 11f),
        )
    }
}

object DaysSinceSpec : WidgetSpec {
    override val type = WidgetType.DAYS_SINCE
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(
        label = "Başladığımdan beri",
        targetEpochDay = LocalDate.now().toEpochDay(),
        accentColor = 0xFFA6E3A1,
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val start = LocalDate.ofEpochDay(d.targetEpochDay)
        val days = DateMath.daysBetween(start, env.today)
        return listOf(
            title(d.label.ifBlank { "Gün sayacı" }),
            big(days.coerceAtLeast(0).toString(), 52f),
            body(if (days < 0) "gün sonra başlıyor" else "gün geçti"),
            Block.Gap(2),
            muted(dateText(start), 11f),
        )
    }
}

object AgeSpec : WidgetSpec {
    override val type = WidgetType.AGE
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(
        label = "Yaş",
        targetEpochDay = LocalDate.of(2000, 1, 1).toEpochDay(),
        accentColor = 0xFFF5C2E7,
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val birth = LocalDate.ofEpochDay(d.targetEpochDay)
        val age = DateMath.age(birth, env.today)
        val untilBirthday = DateMath.daysBetween(env.today, DateMath.nextBirthday(birth, env.today))
        return listOf(
            title(d.label.ifBlank { "Yaş" }),
            big(age.years.toString(), 52f),
            body("yaşında"),
            muted("${age.months} ay ${age.days} gün", 12f),
            Block.Gap(2),
            muted(if (untilBirthday == 0L) "Doğum günün kutlu olsun! 🎂" else "Doğum gününe $untilBirthday gün", 11f),
        )
    }
}

object NoteSpec : WidgetSpec {
    override val type = WidgetType.NOTE
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(
        centered = false,
        label = "Not",
        text = "Notunu buraya yaz…",
        bgColor = 0xFFF9E2AF,
        textColor = 0xFF1E1E2E,
        accentColor = 0xFFB4620B,
    )

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = buildList<Block> {
        if (d.label.isNotBlank()) {
            add(title(d.label))
            add(Block.Gap(4))
        }
        add(body(d.text.ifBlank { "(boş not)" }, 15f))
    }
}

object QuoteSpec : WidgetSpec {
    override val type = WidgetType.QUOTE

    override fun defaults(base: WidgetDesign) = base.copy(accentColor = 0xFFCBA6F7)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val quote = Quotes.pick(Quotes.pool(d.text), env.epochDay, d.counter)
        return listOf(
            Block.Group(
                listOf(
                    Block.Label("❝", 26f, Tone.ACCENT),
                    Block.Label(quote, 16f, Tone.TEXT, bold = true),
                    Block.Gap(4),
                    muted("dokun → başka söz", 10f),
                ),
                tap = Tap("next"),
                center = true,
            ),
        )
    }

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv): WidgetDesign? =
        if (action == "next") d.copy(counter = d.counter + 1) else null
}
