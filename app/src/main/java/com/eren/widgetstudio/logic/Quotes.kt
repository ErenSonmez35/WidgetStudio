package com.eren.widgetstudio.logic

/** Günün sözü için hazır atasözleri. Kullanıcı kendi listesini de girebilir. */
object Quotes {
    val BUILT_IN = listOf(
        "Damlaya damlaya göl olur.",
        "Sabreden derviş muradına ermiş.",
        "Bugünün işini yarına bırakma.",
        "Acele işe şeytan karışır.",
        "Yuvarlanan taş yosun tutmaz.",
        "Ayağını yorganına göre uzat.",
        "Sakla samanı, gelir zamanı.",
        "Her yokuşun bir inişi vardır.",
        "Rüzgâr eken fırtına biçer.",
        "Tatlı dil yılanı deliğinden çıkarır.",
        "Geç olsun, güç olmasın.",
        "Bir elin nesi var, iki elin sesi var.",
        "Üzüm üzüme baka baka kararır.",
        "İyilik yap denize at, balık bilmezse Halik bilir.",
        "Ak akçe kara gün içindir.",
        "Komşu komşunun külüne muhtaç.",
        "Gülü seven dikenine katlanır.",
        "Ne ekersen onu biçersin.",
        "Dost acı söyler.",
        "Bir fincan kahvenin kırk yıl hatırı vardır.",
        "Bin düşün, bir söyle.",
        "Er meydanında belli olur.",
        "Başlamak, işin yarısıdır.",
    )

    /** Özel liste (satır başına bir söz) boş değilse onu, değilse hazır listeyi döndürür. */
    fun pool(custom: String): List<String> =
        custom.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { BUILT_IN }

    fun pick(pool: List<String>, epochDay: Long, offset: Int): String {
        val n = pool.size
        val idx = (((epochDay + offset) % n) + n) % n
        return pool[idx.toInt()]
    }
}
