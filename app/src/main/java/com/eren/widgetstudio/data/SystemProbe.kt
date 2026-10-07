package com.eren.widgetstudio.data

/** Cihazdan okunan değerler. Widget tanımları bu arayüzü kullanır; böylece Android'siz test edilebilir. */
interface SystemProbe {
    fun battery(): BatteryInfo?
    fun storage(): StorageInfo?
    fun ram(): RamInfo?
    fun network(): NetworkInfo
    fun device(): DeviceInfo

    /** Testler ve Android dışı ortamlar için boş uygulama. */
    object None : SystemProbe {
        override fun battery(): BatteryInfo? = null
        override fun storage(): StorageInfo? = null
        override fun ram(): RamInfo? = null
        override fun network() = NetworkInfo(NetworkKind.NONE, false, 0)
        override fun device() = DeviceInfo("", "", 0, "", 0, 0, 0)
    }
}

data class BatteryInfo(
    val percent: Int,
    val charging: Boolean,
    val source: String,        // "USB", "Şarj aleti", "Kablosuz" ya da ""
    val temperatureC: Double,
    val voltageV: Double,
    val health: String,
)

data class StorageInfo(val usedBytes: Long, val totalBytes: Long)

data class RamInfo(val usedBytes: Long, val totalBytes: Long)

enum class NetworkKind(val label: String) {
    WIFI("Wi-Fi"), MOBILE("Mobil veri"), ETHERNET("Ethernet"), OTHER("Diğer"), NONE("Bağlantı yok"),
}

data class NetworkInfo(val kind: NetworkKind, val hasInternet: Boolean, val downMbps: Int)

data class DeviceInfo(
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val securityPatch: String,
    val uptimeMs: Long,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
)
