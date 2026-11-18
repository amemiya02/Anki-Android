// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget.deckpicker

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.ichi2.anki.CommonString
import com.ichi2.anki.widgets.R
import com.ichi2.widget.WidgetAppearance
import com.ichi2.widget.WidgetSheet
import com.ichi2.widget.WidgetText
import com.ichi2.anki.common.android.R as CommonR

internal data class DeckWidgetCount(
    val count: Int,
    val label: String,
    val color: Color,
)

internal fun WidgetAppearance.counts(deck: DeckWidgetData): List<DeckWidgetCount> =
    with(localizedContext) {
        listOf(
            DeckWidgetCount(deck.newCount, getString(R.string.widget_count_new), colors.countNew),
            DeckWidgetCount(deck.learnCount, getString(R.string.widget_count_learning), colors.countLearning),
            DeckWidgetCount(deck.reviewCount, getString(R.string.widget_count_due), colors.countDue),
        )
    }

@Composable
@GlanceComposable
internal fun CompactDeckCounts(
    appearance: WidgetAppearance,
    deck: DeckWidgetData,
    columnWidth: Dp,
    font: TextUnit,
) {
    Row {
        for (count in appearance.counts(deck)) {
            Box(GlanceModifier.width(columnWidth), contentAlignment = Alignment.Center) {
                DeckCount(appearance, count, font, columnWidth)
            }
        }
    }
}

@Composable
@GlanceComposable
internal fun DeckCount(
    appearance: WidgetAppearance,
    count: DeckWidgetCount,
    font: TextUnit,
    width: Dp,
) {
    val text = appearance.number(count.count)
    val limit = (width.value * 1.55f / text.length).toInt().coerceAtLeast(8)
    WidgetText(
        text,
        if (count.count == 0) appearance.colors.onBackgroundMuted else count.color,
        appearance.textSize(font.value.toInt(), limit),
        GlanceModifier.semantics {
            contentDescription = appearance.localizedContext.getString(CommonString.deck_list_count_description, count.label, count.count)
        },
        weight = if (count.count == 0) FontWeight.Normal else FontWeight.Bold,
    )
}

@Composable
@GlanceComposable
internal fun EmptyDeckWidget(
    appearance: WidgetAppearance,
    configIntent: Intent,
    snapshot: DeckWidgetSnapshot,
    selectedDeckMissing: Boolean,
    singleDeck: Boolean = false,
) {
    val compact = LocalSize.current.height < 80.dp
    val smallWidth = LocalSize.current.width < 130.dp
    WidgetSheet(appearance, actionStartActivity(configIntent)) {
        Column(
            GlanceModifier.fillMaxSize().padding(horizontal = if (smallWidth) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Image(
                ImageProvider(CommonR.drawable.symbol_warning),
                contentDescription = null,
                modifier = GlanceModifier.size(if (compact) 13.dp else 20.dp),
                colorFilter = ColorFilter.tint(ColorProvider(appearance.colors.emphasis)),
            )
            Spacer(GlanceModifier.height(2.dp))
            WidgetText(
                appearance.localizedContext.getString(
                    when {
                        snapshot.unavailable -> R.string.widget_collection_unavailable
                        selectedDeckMissing -> R.string.widget_deck_missing
                        snapshot.emptyCollection -> R.string.widget_collection_empty
                        singleDeck -> CommonString.select_deck_title
                        else -> R.string.widget_choose_decks
                    },
                ),
                appearance.colors.onBackground,
                appearance.textSize(if (smallWidth) 10 else 14, if (compact) 12 else 20),
                modifier = GlanceModifier.fillMaxWidth(),
                weight = FontWeight.Medium,
                maxLines = if (smallWidth) 2 else 1,
            )
            WidgetText(
                appearance.localizedContext.getString(
                    when {
                        snapshot.unavailable -> R.string.widget_tap_to_open
                        selectedDeckMissing && smallWidth -> R.string.widget_choose_decks_hint_short
                        selectedDeckMissing -> R.string.widget_choose_decks_hint
                        snapshot.emptyCollection -> R.string.widget_collection_empty_hint
                        else -> R.string.widget_tap_to_open
                    },
                ),
                appearance.colors.onBackgroundMuted,
                appearance.textSize(if (smallWidth) 9 else 12, if (compact) 10 else 17),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = if (compact) 1 else 2,
            )
        }
    }
}
