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

    /** Hava, sistem bilgisi ve gün/tarih tabanlı widget'lar için 15 dakikada bir arka plan yenilemesi. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Verileri hemen tazele (ör. şehir değiştiğinde veya widget'a dokunulduğunda). */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        var failed = false
        DesignRepository.all(ctx)
            .filter { it.type.needsWeather && it.city.isNotBlank() }
            .forEach { design ->
                runCatching { WeatherApi.current(design.lat, design.lon) }
                    .onSuccess { weather ->
                        DesignRepository.update(ctx, design.id) { it.copy(weather = weather) }
                    }
                    .onFailure { failed = true }
            }
        WidgetUpdater.refreshAll(ctx)
        // Ağ yoksa eski veri korunur; WorkManager daha sonra yeniden dener.
        return if (failed) Result.retry() else Result.success()
    }
}
