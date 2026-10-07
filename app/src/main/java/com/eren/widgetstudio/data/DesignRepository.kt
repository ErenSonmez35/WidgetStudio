package com.eren.widgetstudio.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * Tasarımları ve "hangi ana ekran widget'ı hangi tasarımı gösteriyor"
 * eşleşmesini SharedPreferences içinde JSON olarak saklar. Her şey yalnızca
 * cihazda durur; dışa/içe aktarma kullanıcının seçtiği bir dosyayla yapılır.
 */
object DesignRepository {
    private const val PREFS = "widget_studio"
    private const val KEY_DESIGNS = "designs"
    private const val KEY_BACKUP = "designs_unreadable_backup"
    private const val ASSIGN_PREFIX = "assign_"

    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val lock = Any()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(context: Context): List<WidgetDesign> = synchronized(lock) {
        val raw = prefs(context).getString(KEY_DESIGNS, null) ?: return@synchronized emptyList()
        val result = decodeLenient(raw)
        if (result.skipped > 0 || result.corrupt) {
            // Okunamayan veri varsa kaybolmasın diye ham halini bir kez yedekle.
            prefs(context).edit().putString(KEY_BACKUP, raw).commit()
        }
        result.designs
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

    /** Aynı tasarımın yeni bir kimlikle kopyasını ekler. */
    fun duplicate(context: Context, design: WidgetDesign): WidgetDesign {
        val copy = design.copy(id = UUID.randomUUID().toString(), name = design.name + " (kopya)")
        save(context, copy)
        return copy
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

    // ---- Dışa / içe aktarma ----

    /** Tüm tasarımları okunabilir bir JSON metni olarak verir. */
    fun exportJson(context: Context): String = encodeBackup(all(context))

    /**
     * Yedek dosyasındaki tasarımları ekler. Aynı kimlikli olanların üzerine yazar.
     * Eklenen/güncellenen tasarım sayısını döndürür; dosya geçersizse -1.
     */
    fun importJson(context: Context, text: String): Int = synchronized(lock) {
        val incoming = decodeBackup(text) ?: return@synchronized -1
        val merged = all(context).associateBy { it.id }.toMutableMap()
        incoming.forEach { merged[it.id] = it }
        write(context, merged.values.toList())
        incoming.size
    }

    private fun write(context: Context, list: List<WidgetDesign>) {
        prefs(context).edit().putString(KEY_DESIGNS, json.encodeToString(list)).commit()
    }

    // ---- Saf (Android'siz) kodlama yardımcıları; testlerde kullanılır ----

    internal data class Decoded(val designs: List<WidgetDesign>, val skipped: Int, val corrupt: Boolean)

    /** Tek tek çözer: bozuk bir kayıt diğerlerini kaybettirmez. */
    internal fun decodeLenient(raw: String): Decoded {
        val array = runCatching { json.parseToJsonElement(raw).jsonArray }.getOrNull()
            ?: return Decoded(emptyList(), 0, corrupt = true)
        var skipped = 0
        val designs = array.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(WidgetDesign.serializer(), element) }
                .onFailure { skipped++ }
                .getOrNull()
        }
        return Decoded(designs, skipped, corrupt = false)
    }

    internal fun encodeBackup(designs: List<WidgetDesign>): String {
        val pretty = Json(json) { prettyPrint = true }
        val root = JsonObject(
            mapOf(
                "app" to JsonPrimitive("widget-studio"),
                "schemaVersion" to JsonPrimitive(SCHEMA_VERSION),
                "designs" to JsonArray(designs.map { json.encodeToJsonElement(WidgetDesign.serializer(), it) }),
            ),
        )
        return pretty.encodeToString(JsonObject.serializer(), root)
    }

    /** Hem yedek biçimini ({"designs":[...]}) hem düz tasarım dizisini kabul eder. */
    internal fun decodeBackup(text: String): List<WidgetDesign>? {
        val element = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return null
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> element["designs"] as? JsonArray ?: return null
            else -> return null
        }
        val decoded = decodeLenient(array.toString())
        return if (decoded.corrupt) null else decoded.designs
    }
}
