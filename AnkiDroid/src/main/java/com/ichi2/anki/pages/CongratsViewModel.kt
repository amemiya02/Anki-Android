// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import androidx.lifecycle.ViewModel
import anki.scheduler.UnburyDeckRequest
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.OnErrorListener
import com.ichi2.anki.common.destinations.DeckOptionsDestination
import com.ichi2.anki.launchCatchingIO
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.study.FinishedState
import com.ichi2.anki.study.StudyAvailability
import com.ichi2.anki.study.currentDeckStudyState
import com.ichi2.anki.study.finishedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The logic behind the finished screen ([com.ichi2.anki.study.FinishedFragment]), which
 * replaced the Engine's congratulations page; the name is the page's.
 */
class CongratsViewModel :
    ViewModel(),
    OnErrorListener {
    override val onError = MutableSharedFlow<String>()
    val unburyState = MutableSharedFlow<UnburyState>()
    val deckOptionsDestination = MutableSharedFlow<DeckOptionsDestination>()

    private val _finishedState = MutableStateFlow<FinishedState?>(null)

    /** What the screen shows; null until first loaded. */
    val finishedState: StateFlow<FinishedState?> = _finishedState.asStateFlow()

    /** Reads [finishedState] again from the collection. */
    suspend fun loadFinishedState() {
        _finishedState.value = withCol { finishedState() }
    }

    fun onUnbury() {
        launchCatchingIO {
            // https://github.com/ankitects/anki/blob/acaeee91fa853e4a7a78dcddbb832d009ec3529a/qt/aqt/overview.py#L154
            val congratsInfo = withCol { sched.congratulationsInfo() }
            if (congratsInfo.haveSchedBuried && congratsInfo.haveUserBuried) {
                unburyState.emit(UnburyState.SelectMode)
                return@launchCatchingIO
            }
            unburyAndStudy(UnburyDeckRequest.Mode.ALL)
        }
    }

    fun onUnburyModeSelected(mode: UnburyDeckRequest.Mode) {
        launchCatchingIO {
            unburyAndStudy(mode)
        }
    }

    private suspend fun unburyAndStudy(mode: UnburyDeckRequest.Mode) {
        undoableOp(handler = this) {
            sched.unburyDeck(decks.getCurrentId(), mode)
        }
        unburyState.emit(UnburyState.OpenStudy)
    }

    fun onDeckOptions() {
        launchCatchingIO {
            val deckId = withCol { decks.getCurrentId() }
            val isFiltered = withCol { decks.isFiltered(deckId) }
            deckOptionsDestination.emit(DeckOptionsDestination(deckId, isFiltered))
        }
    }

    val congratsRefreshState = MutableSharedFlow<CongratsRefreshState>()

    // set when onStudyQueuesChanged() has nothing to emit to (screen not STARTED, so
    // congratsRefreshState has no collector and would silently drop the event) - consumed
    // and cleared by refreshIfNeeded() on the next STARTED
    private var missedRefresh = false

    fun onStudyQueuesChanged() {
        launchCatchingIO {
            if (congratsRefreshState.subscriptionCount.value == 0) {
                missedRefresh = true
                return@launchCatchingIO
            }
            emitRefreshState()
        }
    }

    /** Called every time the screen becomes STARTED. [isDeckFinished] is always checked,
     * but reload/navigation only happens if something was actually
     * missed while unwatched, or the deck no longer needs the finished screen. */
    suspend fun refreshIfNeeded() {
        // desktop's Overview re-checks sched._is_finished() on every refresh instead of
        // assuming the deck is still done - do the same before reloading the finished screen
        val stillFinished = isDeckFinished()
        if (!missedRefresh && stillFinished) return
        missedRefresh = false
        congratsRefreshState.emit(
            if (stillFinished) CongratsRefreshState.ReloadPage else CongratsRefreshState.DeckHasCardsToStudy,
        )
    }

    private suspend fun emitRefreshState() {
        missedRefresh = false
        val stillFinished = isDeckFinished()
        congratsRefreshState.emit(
            if (stillFinished) CongratsRefreshState.ReloadPage else CongratsRefreshState.DeckHasCardsToStudy,
        )
    }

    private suspend fun isDeckFinished(): Boolean = withCol { currentDeckStudyState().availability == StudyAvailability.NoCardsAvailable }
}

sealed class UnburyState {
    data object OpenStudy : UnburyState()

    data object SelectMode : UnburyState()
}

sealed class CongratsRefreshState {
    /** The Deck is still finished, but what the screen says may have changed: read it again. */
    data object ReloadPage : CongratsRefreshState()

    data object DeckHasCardsToStudy : CongratsRefreshState()
}
