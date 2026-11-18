// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.annotation.CheckResult
import com.ichi2.widget.AppWidgetId.Companion.INVALID_APPWIDGET_ID

/**
 * Each widget has a unique identifier. This id allows to associate to each widget the preferences associated to this widget.
 * This class encapsulate such an id.
 *
 * This ID may not be valid, check for [INVALID_APPWIDGET_ID] when using this value.
 */
@JvmInline
value class AppWidgetId(
    val id: Int,
) {
    companion object {
        // TODO: consider a different design, Intent classes/nullables
        /**
         * Returns the [com.ichi2.widget.AppWidgetId] extra, or [INVALID_APPWIDGET_ID]
         *
         * @see AppWidgetManager.EXTRA_APPWIDGET_ID
         */
        @CheckResult
        fun Intent.getAppWidgetId() = AppWidgetId(getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))

        /**
         * Sets the [`EXTRA_APPWIDGET_ID`][AppWidgetManager.EXTRA_APPWIDGET_ID] for the intent
         */
        fun Intent.updateWidget(appWidgetId: AppWidgetId) = putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId.id)

        /**
         * A sentinel value that the AppWidget manager will never return as a appWidgetId.
         */
        val INVALID_APPWIDGET_ID = AppWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
    }
}
