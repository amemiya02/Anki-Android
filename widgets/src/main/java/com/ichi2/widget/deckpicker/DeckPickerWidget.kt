// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget.deckpicker

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.WidgetConfigDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.widgets.R
import com.ichi2.widget.AnalyticsWidgetProvider
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.PaperWidget
import com.ichi2.widget.WidgetAppearance
import com.ichi2.widget.WidgetSheet
import com.ichi2.widget.WidgetText
import com.ichi2.widget.requestWidgetUpdate

/** A configurable list of Decks with their new, learning and due counts. */
class DeckPickerWidget : AnalyticsWidgetProvider() {
    override val usesRecurringAlarm = true
    override val glanceAppWidget: PaperWidget<*> get() = DeckList

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        val preferences = DeckPickerWidgetPreferences(context)
        appWidgetIds.forEach { preferences.deleteDeckData(AppWidgetId(it)) }
    }

    companion object {
        fun updateDeckPickerWidgets(context: Context) = DeckList.refresh(context)

        fun updateWidget(
            context: Context,
            appWidgetId: AppWidgetId,
        ) = context.requestWidgetUpdate(DeckPickerWidget::class.java, appWidgetId)
    }
}

private data class DeckListDisplay(
    val id: AppWidgetId,
    val snapshot: DeckWidgetSnapshot,
)

private object DeckList : PaperWidget<DeckListDisplay>(DeckPickerWidget::class.java) {
    override suspend fun load(
        context: Context,
        id: AppWidgetId,
    ): DeckListDisplay {
        val selected = DeckPickerWidgetPreferences(context).getSelectedDeckIdsFromPreferences(id).take(5)
        return DeckListDisplay(id, getDeckWidgetSnapshot(selected))
    }

    @Composable
    @GlanceComposable
    override fun Content(
        appearance: WidgetAppearance,
        data: DeckListDisplay,
    ) {
        val snapshot = data.snapshot
        if (snapshot.decks.isEmpty() || snapshot.emptyCollection) {
            EmptyDeckWidget(
                appearance,
                with(DeferredNavigation) { WidgetConfigDestination.DeckPicker(data.id.id).toIntent() },
                snapshot = snapshot,
                selectedDeckMissing = false,
            )
            return
        }

        val size = LocalSize.current
        val narrow = size.width < 190.dp
        val horizontalPadding = if (narrow) 7.dp else 14.dp
        val verticalPadding = if (size.height < 80.dp) 5.dp else 10.dp
        val compact = size.height < 80.dp
        val head = appearance.textSize(if (narrow) 13 else 17, if (compact) 14 else 22)
        val summary = appearance.textSize(if (narrow) 10 else 13, if (compact) 11 else 18)
        val name = appearance.textSize(if (narrow) 12 else 16, if (compact) 13 else 23)
        val countColumn = if (narrow) 21.dp else 36.dp
        val count = appearance.textSize(if (narrow) 11 else 15, if (compact) 13 else 21)
        val headerHeight = maxOf(appearance.textHeight(head) + 1f, if (compact) 18f else 24f).dp
        val rowHeight = maxOf(appearance.textHeight(name) + 3f, if (compact) 19f else 32f).dp
        val rows = if (compact) 1 else ((size.height - verticalPadding * 2 - headerHeight).value / rowHeight.value).toInt().coerceIn(1, 5)
        val colors = appearance.colors
        WidgetSheet(appearance) {
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
            ) {
                Row(
                    GlanceModifier.fillMaxWidth().height(headerHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetText(
                        appearance.localizedContext.getString(R.string.widget_brand),
                        colors.onBackground,
                        head,
                        GlanceModifier.defaultWeight(),
                        weight = FontWeight.Medium,
                    )
                    Spacer(GlanceModifier.width(3.dp))
                    val due = snapshot.totalDue
                    val strings = appearance.localizedContext.resources
                    val summaryText =
                        if (narrow) {
                            strings.getString(R.string.widget_due_today_compact, due)
                        } else {
                            strings.getQuantityString(R.plurals.widget_due_today, due, due)
                        }
                    WidgetText(
                        summaryText,
                        colors.onBackgroundMuted,
                        summary,
                    )
                }
                val showRules = !compact
                for (deck in snapshot.decks.take(rows)) {
                    Column(GlanceModifier.fillMaxWidth()) {
                        if (showRules) {
                            Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(colors.outline)))
                        }
                        Row(
                            GlanceModifier
                                .fillMaxWidth()
                                .height(rowHeight - if (showRules) 1.dp else 0.dp)
                                .clickable(actionStartActivity(deck.studyIntent())),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WidgetText(deck.name, colors.onBackground, name, GlanceModifier.defaultWeight())
                            Spacer(GlanceModifier.width(3.dp))
                            CompactDeckCounts(appearance, deck, countColumn, count)
                        }
                    }
                }
            }
        }
    }
}
