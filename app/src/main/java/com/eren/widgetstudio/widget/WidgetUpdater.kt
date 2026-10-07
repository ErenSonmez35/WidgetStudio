package com.eren.widgetstudio.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetType
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    val VERSION = longPreferencesKey("design_version")

    private const val PERIODIC = "widget_refresh_periodic"
    private const val ONCE = "widget_refresh_once"

    /** Ana ekrandaki tüm widget'ları güncel tasarımlarıyla yeniden çizer. */
    suspend fun refreshAll(context: Context) {
        val widget = CustomWidget()
        GlanceAppWidgetManager(context).getGlanceIds(CustomWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { prefs ->
                prefs[VERSION] = System.currentTimeMillis()
            }
            widget.update(context, id)
        }
    }

    /** Hava durumu ve sistem bilgisi için 30 dakikada bir arka plan yenilemesi. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Hava durumunu hemen çek (ör. şehir değiştiğinde veya widget'a dokunulduğunda). */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        DesignRepository.all(ctx)
            .filter { it.type == WidgetType.WEATHER && it.city.isNotBlank() }
            .forEach { design ->
                runCatching { WeatherApi.current(design.lat, design.lon) }
                    .onSuccess { weather ->
                        DesignRepository.update(ctx, design.id) { it.copy(weather = weather) }
                    }
            }
        WidgetUpdater.refreshAll(ctx)
        return Result.success()
    }
}
