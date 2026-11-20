// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import anki.collection.OpChangesAfterUndo
import anki.scheduler.CardAnswer
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.UndoStepCounter
import com.ichi2.anki.study.ReviewSessionSummary
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.util.UUID
import java.util.WeakHashMap

/** Tracks successful grades and their undo steps for one visit to the reviewer. */
internal class ReviewSession(
    savedStateHandle: SavedStateHandle,
) {
    private val restoredState =
        savedStateHandle.get<Bundle>(KEY)?.let { BundleCompat.getParcelable(it, KEY, State::class.java) }
    private var historyId = restoredState?.historyId
    private val reviews = restoredState?.reviews.orEmpty().toMutableList()

    init {
        savedStateHandle.setSavedStateProvider(KEY) { Bundle().apply { putParcelable(KEY, saveState()) } }
    }

    @Synchronized
    private fun saveState() = State(historyId, reviews.toList())

    @Synchronized
    fun recordAnswer(
        collection: Collection,
        answer: CardAnswer,
    ) {
        updateHistory(collection)
        reviews.removeAll { !it.applied }
        val step =
            runCatching { collection.undoStatus() }
                .onFailure { Timber.w(it, "Could not identify the review's undo step") }
                .getOrNull()
                ?.takeIf { it.undoAvailable }
                ?.lastStep
        reviews.add(Review(answer.rating, answer.millisecondsTaken, step))
    }

    @Synchronized
    fun recordUndoOrRedo(
        collection: Collection,
        result: OpChangesAfterUndo,
        redo: Boolean,
    ) {
        updateHistory(collection)
        val index = reviews.indexOfLast { it.step == result.counter && it.applied != redo }
        if (index >= 0) {
            reviews[index] = reviews[index].copy(step = result.newStatus.lastStep, applied = redo)
        }
    }

    @Synchronized
    fun summary(): ReviewSessionSummary? {
        val applied = reviews.filter { it.applied }
        if (applied.isEmpty()) return null
        return ReviewSessionSummary(
            again = applied.count { it.rating == Rating.AGAIN },
            hard = applied.count { it.rating == Rating.HARD },
            good = applied.count { it.rating == Rating.GOOD },
            easy = applied.count { it.rating == Rating.EASY },
            elapsedMillis = applied.sumOf { it.milliseconds.coerceAtLeast(0).toLong() },
        )
    }

    /** Engine undo counters are scoped to the open collection and start fresh after process death. */
    private fun updateHistory(collection: Collection) {
        val currentHistoryId = synchronized(historyIds) { historyIds.getOrPut(collection) { UUID.randomUUID().toString() } }
        if (currentHistoryId == historyId) return
        historyId = currentHistoryId
        reviews.removeAll { !it.applied }
        reviews.replaceAll { it.copy(step = null) }
    }

    @Parcelize
    internal data class Review(
        val rating: Rating,
        val milliseconds: Int,
        val step: UndoStepCounter?,
        val applied: Boolean = true,
    ) : Parcelable

    @Parcelize
    internal data class State(
        val historyId: String?,
        val reviews: List<Review>,
    ) : Parcelable

    companion object {
        private const val KEY = "reviewSession"
        private val historyIds = WeakHashMap<Collection, String>()
    }
}
