// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.analytics.AnalyticsEvent
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.common.storage.StorageDecision
import com.ichi2.widget.AppWidgetId.Companion.getAppWidgetId
import timber.log.Timber

/** The launcher-facing receiver for a Hayaki Glance widget. */
abstract class AnalyticsWidgetProvider : GlanceAppWidgetReceiver() {
    abstract override val glanceAppWidget: PaperWidget<*>

    protected open val requiresCollection = true
    protected open val usesRecurringAlarm = false

    @CallSuper
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Analytics.send(AnalyticsEvent.WidgetEnabled(javaClass.simpleName))
        if (usesRecurringAlarm) DayRolloverAlarm.scheduleNext(context)
    }

    @CallSuper
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Analytics.send(AnalyticsEvent.WidgetDisabled(javaClass.simpleName))
    }

    final override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (!storageReady(context)) return
        if (usesRecurringAlarm) {
            appWidgetIds.forEach { setRecurringAlarm(context, AppWidgetId(it), javaClass) }
        }
        onReadyToUpdate(context)
        glanceAppWidget.invalidate()
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    final override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        if (!storageReady(context)) return
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_UPDATE_WIDGET && usesRecurringAlarm) {
            val manager = getAppWidgetManager(context) ?: return
            val id = intent.getAppWidgetId().id
            if (id in manager.getAppWidgetIds(ComponentName(context, javaClass))) {
                onUpdate(context, manager, intArrayOf(id))
            }
            return
        }
        if (getAppWidgetManager(context) == null) return
        super.onReceive(context, intent)
    }

    @CallSuper
    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        if (usesRecurringAlarm) {
            appWidgetIds.forEach { cancelRecurringAlarm(context, AppWidgetId(it), javaClass) }
        }
        super.onDeleted(context, appWidgetIds)
    }

    protected open fun onReadyToUpdate(context: Context) = Unit

    private fun storageReady(context: Context): Boolean {
        if (requiresCollection && CollectionHelper.storageDecision(context.sharedPrefs()) == StorageDecision.Undecided) {
            Timber.w("Skipping widget %s until storage is ready", javaClass.simpleName)
            return false
        }
        return true
    }
}

/** Configuration activities must request the first update themselves. */
internal fun Context.requestWidgetUpdate(
    provider: Class<out AnalyticsWidgetProvider>,
    widgetId: AppWidgetId,
) = sendBroadcast(
    Intent(this, provider)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId.id)),
)
