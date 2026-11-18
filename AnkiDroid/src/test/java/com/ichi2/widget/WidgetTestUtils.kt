// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.View.MeasureSpec
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import com.ichi2.widget.AppWidgetId.Companion.updateWidget
import com.ichi2.anki.common.utils.dp as pixelsDp

fun widgetUpdateIntent(appWidgetId: Int = 1) =
    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    }

/** Composes the production widget and lays out the RemoteViews the launcher receives. */
suspend fun Context.renderWidget(
    receiver: AnalyticsWidgetProvider,
    width: Int,
    height: Int,
    id: AppWidgetId = AppWidgetId(1),
): View {
    val glanceId = requireNotNull(GlanceAppWidgetManager(this).getGlanceIdBy(Intent().updateWidget(id)))
    val views = receiver.glanceAppWidget.compose(this, id = glanceId, size = DpSize(width.dp, height.dp))
    return views.apply(this, null).also { view ->
        val w = width.pixelsDp.toPx(this)
        val h = height.pixelsDp.toPx(this)
        view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
    }
}
