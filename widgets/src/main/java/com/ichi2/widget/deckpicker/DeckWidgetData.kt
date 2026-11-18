// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Anoop <xenonnn4w@gmail.com>

package com.ichi2.widget.deckpicker

import android.content.Intent
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.common.destinations.DeckOptionsDestination
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.ReviewDeckDestination
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.sched.DeckNode
import kotlinx.coroutines.CancellationException
import timber.log.Timber

data class DeckWidgetData(
    val deckId: DeckId,
    val name: String,
    val reviewCount: Int,
    val learnCount: Int,
    val newCount: Int,
    internal val isFiltered: Boolean = false,
    internal val fullName: String = name,
) {
    internal val total = newCount + learnCount + reviewCount

    internal fun studyIntent(): Intent =
        with(DeferredNavigation) {
            if (total > 0) {
                // Glance gives each PendingIntent a URI so that rows remain distinct.
                // This explicit launch is a review request even with a data URI attached.
                ReviewDeckDestination.ExternalLaunch(deckId).toIntent().setAction(null)
            } else {
                DeckOptionsDestination(deckId, isFiltered).toIntent()
            }
        }
}

internal data class DeckWidgetSnapshot(
    val decks: List<DeckWidgetData> = emptyList(),
    val totalDue: Int = 0,
    val emptyCollection: Boolean = false,
    val unavailable: Boolean = false,
)

suspend fun getDeckNamesAndStats(deckIds: List<DeckId>): List<DeckWidgetData> = readDeckWidgetSnapshot(deckIds).decks

internal suspend fun getDeckWidgetSnapshot(deckIds: List<DeckId>): DeckWidgetSnapshot =
    try {
        readDeckWidgetSnapshot(deckIds)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Could not load Decks for the widget")
        DeckWidgetSnapshot(unavailable = true)
    }

private suspend fun readDeckWidgetSnapshot(deckIds: List<DeckId>): DeckWidgetSnapshot {
    if (deckIds.isEmpty()) return DeckWidgetSnapshot()
    return withCol {
        val tree = sched.deckDueTree()
        val onlyDefaultDeck = tree.children.singleOrNull()?.did == DEFAULT_DECK_ID
        val selectedIds = deckIds.toSet()
        val byId = tree.filter { it.did in selectedIds }.associateBy(DeckNode::did)
        DeckWidgetSnapshot(
            decks =
                deckIds.mapNotNull { id ->
                    byId[id]?.let { node ->
                        DeckWidgetData(
                            deckId = node.did,
                            name = node.lastDeckNameComponent,
                            reviewCount = node.revCount,
                            learnCount = node.lrnCount,
                            newCount = node.newCount,
                            isFiltered = node.filtered,
                            fullName = node.fullDeckName,
                        )
                    }
                },
            // Aggregate top-level Decks only. A parent already includes its children's counts.
            totalDue = tree.children.sumOf { it.newCount + it.lrnCount + it.revCount },
            emptyCollection = onlyDefaultDeck && decks.cardCount(DEFAULT_DECK_ID, includeSubdecks = true) == 0,
        )
    }
}
