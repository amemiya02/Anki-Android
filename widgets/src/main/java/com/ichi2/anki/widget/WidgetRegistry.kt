// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget

import android.content.Context
import com.ichi2.widget.AddNoteWidget
import com.ichi2.widget.AnalyticsWidgetProvider
import com.ichi2.widget.AnkiDroidWidgetSmall
import com.ichi2.widget.cardanalysis.CardAnalysisWidget
import com.ichi2.widget.deckpicker.DeckPickerWidget

val RECURRING_WIDGETS: List<Class<out AnalyticsWidgetProvider>> =
    listOf(
        DeckPickerWidget::class.java,
        CardAnalysisWidget::class.java,
    )

val NON_RECURRING_WIDGETS: List<Class<out AnalyticsWidgetProvider>> =
    listOf(
        AddNoteWidget::class.java,
        AnkiDroidWidgetSmall::class.java,
    )

/** Reapply the app locale and appearance to widgets already on the home screen. */
fun refreshHomeScreenWidgets(context: Context) {
    AddNoteWidget.refresh(context)
    AnkiDroidWidgetSmall.refresh(context)
    CardAnalysisWidget.updateCardAnalysisWidgets(context)
    DeckPickerWidget.updateDeckPickerWidgets(context)
}
