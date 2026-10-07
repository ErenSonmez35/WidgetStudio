package com.eren.widgetstudio.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.eren.widgetstudio.catalog.ACTION_REFRESH
import com.eren.widgetstudio.catalog.RenderEnv
import com.eren.widgetstudio.catalog.WidgetSpecs
import com.eren.widgetstudio.data.AndroidSystemProbe
import com.eren.widgetstudio.data.DesignRepository

val DesignIdKey = ActionParameters.Key<String>("designId")
val ActionKey = ActionParameters.Key<String>("action")
val IndexKey = ActionParameters.Key<Int>("index")

/** Widget üzerindeki her dokunmayı ilgili WidgetSpec.onAction'a iletir ve widget'ları yeniler. */
class BlockAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val designId = parameters[DesignIdKey] ?: return
        val action = parameters[ActionKey] ?: return
        val arg = parameters[IndexKey] ?: 0

        if (action == ACTION_REFRESH) {
            WidgetUpdater.refreshAll(context)
            WidgetUpdater.refreshNow(context)
            return
        }
        val env = RenderEnv(probe = AndroidSystemProbe(context))
        DesignRepository.update(context, designId) { d ->
            runCatching { WidgetSpecs.of(d.type).onAction(d, action, arg, env) }.getOrNull() ?: d
        }
        WidgetUpdater.refreshAll(context)
    }
}

/** Eski sürümlerde yerleştirilmiş widget'ların dokunma eylemi bozulmasın diye korunuyor. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetUpdater.refreshAll(context)
        WidgetUpdater.refreshNow(context)
    }
}
