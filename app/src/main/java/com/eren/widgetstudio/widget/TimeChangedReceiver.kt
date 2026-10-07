package com.eren.widgetstudio.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Gün değişince, saat ya da saat dilimi değişince tarih tabanlı widget'ları (takvim, geri sayım…) yeniler. */
class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetUpdater.refreshAll(app)
            } finally {
                pending.finish()
            }
        }
    }
}
