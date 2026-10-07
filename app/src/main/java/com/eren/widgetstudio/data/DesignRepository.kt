package com.eren.widgetstudio.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tasarımları ve "hangi ana ekran widget'ı hangi tasarımı gösteriyor"
 * eşleşmesini SharedPreferences içinde JSON olarak saklar.
 */
object DesignRepository {
    private const val PREFS = "widget_studio"
    private const val KEY_DESIGNS = "designs"
    private const val ASSIGN_PREFIX = "assign_"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(context: Context): List<WidgetDesign> = synchronized(lock) {
        val raw = prefs(context).getString(KEY_DESIGNS, null) ?: return@synchronized emptyList()
        runCatching { json.decodeFromString<List<WidgetDesign>>(raw) }.getOrDefault(emptyList())
    }

    fun get(context: Context, id: String): WidgetDesign? = all(context).find { it.id == id }

    fun save(context: Context, design: WidgetDesign) = synchronized(lock) {
        val list = all(context).toMutableList()
        val index = list.indexOfFirst { it.id == design.id }
        if (index >= 0) list[index] = design else list.add(design)
        write(context, list)
    }

    fun update(context: Context, id: String, transform: (WidgetDesign) -> WidgetDesign) =
        synchronized(lock) {
            get(context, id)?.let { save(context, transform(it)) }
        }

    fun delete(context: Context, id: String) = synchronized(lock) {
        write(context, all(context).filterNot { it.id == id })
    }

    fun assign(context: Context, appWidgetId: Int, designId: String) {
        prefs(context).edit().putString(ASSIGN_PREFIX + appWidgetId, designId).commit()
    }

    fun unassign(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(ASSIGN_PREFIX + appWidgetId).commit()
    }

    fun designFor(context: Context, appWidgetId: Int): WidgetDesign? =
        prefs(context).getString(ASSIGN_PREFIX + appWidgetId, null)?.let { get(context, it) }

    /** Bu tasarımı kaç ana ekran widget'ı kullanıyor. */
    fun usageCount(context: Context, designId: String): Int =
        prefs(context).all.count { (key, value) -> key.startsWith(ASSIGN_PREFIX) && value == designId }

    private fun write(context: Context, list: List<WidgetDesign>) {
        prefs(context).edit().putString(KEY_DESIGNS, json.encodeToString(list)).commit()
    }
}
