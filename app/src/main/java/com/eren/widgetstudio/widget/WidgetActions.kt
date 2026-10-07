package com.eren.widgetstudio.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.eren.widgetstudio.data.DesignRepository

val DesignIdKey = ActionParameters.Key<String>("designId")
val IndexKey = ActionParameters.Key<Int>("index")

/** Yapılacaklar widget'ında bir maddeye dokununca işaretler / işareti kaldırır. */
class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val designId = parameters[DesignIdKey] ?: return
        val index = parameters[IndexKey] ?: return
        DesignRepository.update(context, designId) { d ->
            d.copy(todos = d.todos.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item })
        }
        WidgetUpdater.refreshAll(context)
    }
}

/** Hava durumu / sistem widget'ına dokununca verileri hemen tazeler. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetUpdater.refreshAll(context)
        WidgetUpdater.refreshNow(context)
    }
}
