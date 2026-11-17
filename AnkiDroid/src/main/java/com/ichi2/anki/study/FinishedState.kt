// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import com.ichi2.anki.common.time.SECONDS_PER_DAY
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Decks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What the finished screen says about the selected Deck once it has nothing left to study:
 * everything the Engine's congratulations page showed, plus the Deck path and today's summary.
 */
data class FinishedState(
    val deckPath: List<String>,
    val isFiltered: Boolean,
    /** Learning Cards that come back later today, or null if none do. */
    val learningLater: LearningLater?,
    /** New Cards are waiting, but the Deck's daily limit for new Cards was reached. */
    val newLimitReached: Boolean,
    /** Reviews are waiting, but the Deck's daily review limit was reached. */
    val reviewLimitReached: Boolean,
    /** Some Cards are buried, by the person or as siblings, until the next day. */
    val hasBuried: Boolean,
    /** When the Engine's next day starts and today's limits reset. */
    val nextDayStartsAtMillis: Long,
    /** The Engine's summary of today's studying, on one line. */
    val studiedToday: String,
    /** The Deck's description as HTML, when the Deck shows it on this screen; otherwise empty. */
    val deckDescription: String,
) {
    /** The one reason the screen leads with; the others are still listed. */
    val reason: FinishedReason
        get() =
            when {
                newLimitReached || reviewLimitReached -> FinishedReason.DailyLimit
                learningLater != null -> FinishedReason.LearningLater
                hasBuried -> FinishedReason.Buried
                else -> FinishedReason.Done
            }

    data class LearningLater(
        val remaining: Int,
        val nextIn: Duration,
    )
}

/** The four ways a Deck can be finished, as the Mockups draw them. */
enum class FinishedReason {
    DailyLimit,
    LearningLater,
    Buried,
    Done,
}

/** Read under the collection lock so every fact refers to the same Deck and moment. */
fun Collection.finishedState(): FinishedState {
    val deck = decks.current()
    val info = sched.congratulationsInfo()
    val learningLater =
        info.secsUntilNextLearn
            .takeIf { info.learnRemaining > 0 && it < SECONDS_PER_DAY }
            ?.let { FinishedState.LearningLater(remaining = info.learnRemaining, nextIn = it.seconds) }
    return FinishedState(
        deckPath = Decks.path(deck.name),
        isFiltered = deck.isFiltered,
        learningLater = learningLater,
        newLimitReached = info.newRemaining,
        reviewLimitReached = info.reviewRemaining,
        hasBuried = info.haveSchedBuried || info.haveUserBuried,
        nextDayStartsAtMillis = sched.dayCutoff * 1000,
        // the Engine breaks the line for HTML
        studiedToday = sched.studiedToday().replace("\n", " "),
        deckDescription = info.deckDescription,
    )
}
