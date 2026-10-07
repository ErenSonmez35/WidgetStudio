package com.eren.widgetstudio.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

/** [SystemProbe]'un gerçek cihaz uygulaması. Hiçbir veri cihazdan dışarı çıkmaz. */
class AndroidSystemProbe(context: Context) : SystemProbe {
    private val ctx = context.applicationContext

    override fun battery(): BatteryInfo? {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "İyi"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Aşırı ısınma"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Bozuk"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Aşırı voltaj"
            BatteryManager.BATTERY_HEALTH_COLD -> "Soğuk"
            else -> "Bilinmiyor"
        }
        return BatteryInfo(
            percent = (level * 100 / scale).coerceIn(0, 100),
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "Şarj aleti"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz"
                else -> ""
            },
            temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0,
            voltageV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0,
            health = health,
        )
    }

    override fun storage(): StorageInfo? = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        StorageInfo(usedBytes = stat.totalBytes - stat.availableBytes, totalBytes = stat.totalBytes)
    }.getOrNull()

    override fun ram(): RamInfo? = runCatching {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        RamInfo(usedBytes = mi.totalMem - mi.availMem, totalBytes = mi.totalMem)
    }.getOrNull()

    override fun network(): NetworkInfo {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
            ?: return NetworkInfo(NetworkKind.NONE, false, 0)
        val kind = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.MOBILE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.ETHERNET
            else -> NetworkKind.OTHER
        }
        return NetworkInfo(
            kind = kind,
            hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            downMbps = caps.linkDownstreamBandwidthKbps / 1000,
        )
    }

    override fun device(): DeviceInfo {
        val metrics = ctx.resources.displayMetrics
        return DeviceInfo(
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            uptimeMs = SystemClock.elapsedRealtime(),
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
        )
    }
}
