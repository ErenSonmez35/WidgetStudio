package com.eren.widgetstudio.catalog

/** Metin / vurgu / soluk metin / vurgu rengi üzerindeki yazı rengi. */
enum class Tone { TEXT, ACCENT, MUTED, ON_ACCENT }

/** Dokunma eylemi. [action] ilgili widget tanımının `onAction` fonksiyonuna gider. */
data class Tap(val action: String, val arg: Int = 0)

const val ACTION_REFRESH = "refresh"

/**
 * Bir widget'ın ekranda göstereceği içeriğin platformdan bağımsız tarifi.
 * Hem ana ekran widget'ı (Glance) hem uygulama içi önizleme (Compose) aynı listeyi çizer.
 * Boyutlar `sp` cinsindendir ve tasarımın yazı boyutu çarpanıyla ölçeklenir.
 */
sealed interface Block {
    data class Label(
        val text: String,
        val size: Float = 13f,
        val tone: Tone = Tone.TEXT,
        val bold: Boolean = false,
        val strike: Boolean = false,
        val maxLines: Int = 0,
    ) : Block

    /** İlerleme çubuğu (0..1). */
    data class Meter(val fraction: Float, val tone: Tone = Tone.ACCENT) : Block

    /** Sistemin kendiliğinden güncellediği canlı saat. [format] SimpleDateFormat kalıbıdır. */
    data class Clock(val format: String, val size: Float, val tone: Tone = Tone.TEXT, val zone: String? = null) : Block

    /** Canlı kronometre / geri sayım. [ms] gösterilecek süredir (geri sayımda kalan süre, negatif olabilir). */
    data class Chrono(
        val ms: Long,
        val running: Boolean,
        val countDown: Boolean,
        val size: Float,
        val tone: Tone = Tone.TEXT,
    ) : Block

    data class Gap(val dp: Int = 6) : Block

    /** Buton: [tap] widget'ın kendi eylemini, [launch] bir Android intent action'ını (ör. Wi-Fi ayarları) çalıştırır. */
    data class Btn(val label: String, val tap: Tap? = null, val launch: String? = null) : Block

    data class Group(
        val children: List<Block>,
        val horizontal: Boolean = false,
        val tap: Tap? = null,
        val fill: Tone? = null,
        val center: Boolean = false,
    ) : Block

    /** Sol blok genişleyip boşluğu doldurur, sağ blok sağa yaslanır. */
    data class Split(val left: Block, val right: Block) : Block

    /** Her hücresi eşit genişlikte, ortalanmış satır/sütun tablosu. */
    data class Table(val rows: List<List<Block>>) : Block

    /** Kaydırılabilir liste (widget'ın tüm yüksekliğini kaplar). */
    data class Scroll(val children: List<Block>) : Block
}

// Sık kullanılan kısayollar -------------------------------------------------

fun title(text: String, size: Float = 15f) = Block.Label(text, size, Tone.ACCENT, bold = true)

fun body(text: String, size: Float = 13f, tone: Tone = Tone.TEXT) = Block.Label(text, size, tone)

fun big(text: String, size: Float = 38f, tone: Tone = Tone.TEXT) = Block.Label(text, size, tone)

fun muted(text: String, size: Float = 12f) = Block.Label(text, size, Tone.MUTED)

/** "Etiket ......... değer" satırı ve altında ilerleme çubuğu. */
fun meterRow(label: String, value: String, fraction: Float): Block = Block.Group(
    listOf(
        Block.Split(body(label), Block.Label(value, 13f, Tone.ACCENT, bold = true)),
        Block.Gap(3),
        Block.Meter(fraction.coerceIn(0f, 1f)),
    ),
)

/** "Etiket ......... değer" satırı. */
fun infoRow(label: String, value: String): Block =
    Block.Split(body(label), Block.Label(value, 13f, Tone.ACCENT, bold = true))

/** Blokları dikey aralıklarla art arda dizer. */
fun spaced(items: List<Block>, gap: Int = 8): List<Block> =
    items.flatMapIndexed { i, b -> if (i == 0) listOf(b) else listOf(Block.Gap(gap), b) }
