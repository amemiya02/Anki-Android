// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import com.ichi2.anki.common.time.SECONDS_PER_DAY
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.sched.Counts

/**
 * What the deck list shows.
 *
 * @param decks the visible Decks, in tree order (Decks inside a collapsed Deck are left out)
 * @param isEmptyCollection a Newcomer's collection: only the Default deck, and no Cards.
 *  The deck list then explains what to do first.
 * @param queuesToday new, learning and due Cards left today, over every Deck
 * @param studiedToday Cards answered today
 * @param currentStudyDeck the current Deck when it has Cards ready to study, even under a collapsed parent
 */
data class DeckListScreenState(
    val decks: List<DisplayDeckNode>,
    val isEmptyCollection: Boolean,
    val queuesToday: Counts,
    val studiedToday: Int,
    val currentStudyDeck: DisplayDeckNode? = null,
) {
    val dueToday: Int
        get() = queuesToday.count()
}

/**
 * The number of Cards answered today, as the Engine counts it for its "Studied N cards" line:
 * every review since the day started, except manual rescheduling.
 */
fun Collection.cardsStudiedToday(): Int {
    val dayStartMillis = (sched.dayCutoff - SECONDS_PER_DAY) * 1000
    // RevlogEntry kinds 4 (manual) and 5 (rescheduled) are not studying
    return db.queryScalar("select count() from revlog where id > ? and type not in (4, 5)", dayStartMillis)
}
