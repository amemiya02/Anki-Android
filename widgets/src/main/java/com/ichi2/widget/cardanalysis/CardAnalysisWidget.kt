// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget.cardanalysis

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
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
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.WidgetConfigDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.libanki.DeckId
import com.ichi2.widget.AnalyticsWidgetProvider
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.PaperWidget
import com.ichi2.widget.WidgetAppearance
import com.ichi2.widget.WidgetSheet
import com.ichi2.widget.WidgetText
import com.ichi2.widget.deckpicker.CompactDeckCounts
import com.ichi2.widget.deckpicker.DeckCount
import com.ichi2.widget.deckpicker.DeckWidgetSnapshot
import com.ichi2.widget.deckpicker.EmptyDeckWidget
import com.ichi2.widget.deckpicker.counts
import com.ichi2.widget.deckpicker.getDeckWidgetSnapshot
import com.ichi2.widget.requestWidgetUpdate

/** One Deck with its new, learning and due counts. */
class CardAnalysisWidget : AnalyticsWidgetProvider() {
    override val usesRecurringAlarm = true
    override val glanceAppWidget: PaperWidget<*> get() = OneDeck

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        val preferences = CardAnalysisWidgetPreferences(context)
        appWidgetIds.forEach { preferences.deleteDeckData(AppWidgetId(it)) }
    }

    companion object {
        fun updateCardAnalysisWidgets(context: Context) = OneDeck.refresh(context)

        fun updateWidget(
            context: Context,
            appWidgetId: AppWidgetId,
        ) = context.requestWidgetUpdate(CardAnalysisWidget::class.java, appWidgetId)
    }
}

private data class OneDeckDisplay(
    val id: AppWidgetId,
    val selected: DeckId?,
    val snapshot: DeckWidgetSnapshot,
)

private object OneDeck : PaperWidget<OneDeckDisplay>(CardAnalysisWidget::class.java) {
    override suspend fun load(
        context: Context,
        id: AppWidgetId,
    ): OneDeckDisplay {
        val selected = CardAnalysisWidgetPreferences(context).getSelectedDeckIdFromPreferences(id)
        return OneDeckDisplay(id, selected, getDeckWidgetSnapshot(listOfNotNull(selected)))
    }

    @Composable
    @GlanceComposable
    override fun Content(
        appearance: WidgetAppearance,
        data: OneDeckDisplay,
    ) {
        val deck = data.snapshot.decks.firstOrNull()
        if (deck == null || data.snapshot.emptyCollection) {
            EmptyDeckWidget(
                appearance,
                with(DeferredNavigation) { WidgetConfigDestination.CardAnalysis(data.id.id).toIntent() },
                snapshot = data.snapshot,
                selectedDeckMissing = data.selected != null && data.selected != DEFAULT_DECK_ID,
                singleDeck = true,
            )
            return
        }

        val size = LocalSize.current
        val narrow = size.width < 125.dp
        val detailedNameFont = appearance.textSize(if (narrow) 13 else 16, 22)
        val detailedCountFont = appearance.textSize(20, 22)
        val detailHeight = (24f + appearance.textHeight(detailedNameFont) + 3 * appearance.textHeight(detailedCountFont)).dp
        val compact = size.width < 105.dp || size.height < detailHeight
        val colors = appearance.colors
        WidgetSheet(appearance, actionStartActivity(deck.studyIntent())) {
            Column(
                GlanceModifier.fillMaxSize().padding(
                    horizontal = if (narrow) 6.dp else 14.dp,
                    vertical = if (compact) 5.dp else 12.dp,
                ),
                verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
            ) {
                WidgetText(
                    deck.fullName.replace("::", " › "),
                    colors.onBackground,
                    if (compact) appearance.textSize(if (narrow) 13 else 16, 16) else detailedNameFont,
                    GlanceModifier.fillMaxWidth(),
                    weight = FontWeight.Medium,
                )
                if (compact) {
                    Spacer(GlanceModifier.height(3.dp))
                    CompactDeckCounts(
                        appearance,
                        deck,
                        columnWidth = if (narrow) 23.dp else 36.dp,
                        font = appearance.textSize(if (narrow) 12 else 15, 16),
                    )
                } else {
                    Spacer(GlanceModifier.defaultWeight())
                    for (count in appearance.counts(deck)) {
                        Row(
                            GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val width = if (narrow) 28.dp else 40.dp
                            Row(GlanceModifier.width(width)) { DeckCount(appearance, count, detailedCountFont, width) }
                            Spacer(GlanceModifier.width(4.dp))
                            WidgetText(count.label, colors.onBackgroundMuted, appearance.textSize(if (narrow) 11 else 13, 17))
                        }
                    }
                }
            }
        }
    }
}
