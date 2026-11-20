// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import anki.collection.OpChanges
import anki.scheduler.UnburyDeckRequest
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.ReviewDeckDestination
import com.ichi2.anki.common.destinations.StudyOptionsDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.destinations.toIntent
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.CustomStudyAction
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.pages.CongratsRefreshState
import com.ichi2.anki.pages.CongratsViewModel
import com.ichi2.anki.pages.UnburyState
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The finished screen ([FinishedScreen]): shown when a study session runs out of Cards and when
 * a finished Deck is tapped on the deck list. It replaced the Engine's congratulations page.
 */
class FinishedFragment :
    Fragment(),
    ChangeManager.Subscriber {
    private val viewModel by viewModels<CongratsViewModel>()
    private val studyAll: Boolean
        get() = arguments?.getBoolean(ARG_STUDY_ALL) == true

    init {
        ChangeManager.subscribe(this)
    }

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        // typically due to 'day rollover'. handler !== viewModel excludes changes this
        // screen caused itself (eg. unburying), which are already handled directly
        if (changes.studyQueues && handler !== viewModel) {
            Timber.i("study queues updated")
            viewModel.onStudyQueuesChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    val state by viewModel.finishedState.collectAsStateWithLifecycle()
                    state?.let {
                        FinishedScreen(
                            state = it,
                            onBack = ::closeFinished,
                            onUnbury = { viewModel.onUnbury() },
                            onCustomStudy = { onCustomStudy() },
                            onDeckOptions = { viewModel.onDeckOptions() },
                            onBackToDecks = ::closeFinished,
                            closeLabel = if (studyAll) CommonString.finished_done else CommonString.finished_back_to_decks,
                        )
                    }
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (studyAll) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { closeFinished() }
        }

        viewModel.onError
            .flowWithLifecycle(lifecycle)
            .onEach { errorMessage ->
                createPaperDialog(requireContext()) { dialog ->
                    ErrorDialog(getString(CommonString.vague_error), errorMessage, onDismissRequest = dialog::dismiss)
                }.show()
            }.launchIn(lifecycleScope)

        viewModel.unburyState
            .onEach { state ->
                when (state) {
                    UnburyState.OpenStudy -> openStudyScreenAndFinish()
                    UnburyState.SelectMode -> showUnburyModes()
                }
            }.launchIn(lifecycleScope)

        viewModel.deckOptionsDestination
            .flowWithLifecycle(lifecycle)
            .onEach { destination -> navigate(destination) }
            .launchIn(lifecycleScope)

        // a rebuild triggered elsewhere (eg. deck options) while this screen wasn't visible may
        // have been missed, so recheck on every STARTED entry instead of trusting only the
        // opExecuted callback. Launched from inside repeatOnLifecycle so the collector is
        // guaranteed active before it emits (congratsRefreshState has no replay cache, so
        // emitting with no active collector silently drops the event).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // read once per STARTED, not on a timer: a polling loop never lets a
                    // test scheduler's virtual time settle. The page's minute-by-minute refresh
                    // of "the next card is ready in" is dropped; returning to the screen updates it.
                    viewModel.loadFinishedState()
                    viewModel.refreshIfNeeded()
                }
                viewModel.congratsRefreshState.collect { state ->
                    when (state) {
                        CongratsRefreshState.ReloadPage -> viewModel.loadFinishedState()
                        CongratsRefreshState.DeckHasCardsToStudy -> openStudyOptionsAndFinish()
                    }
                }
            }
        }

        setFragmentResultListener(CustomStudyAction.REQUEST_KEY) { _, bundle ->
            when (CustomStudyAction.fromBundle(bundle)) {
                CustomStudyAction.CUSTOM_STUDY_SESSION,
                CustomStudyAction.EXTEND_STUDY_LIMITS,
                -> openStudyOptionsAndFinish()
            }
        }
    }

    private fun closeFinished() {
        // Opening Custom Study or Study Options ends Study All.
        finishScreen(if (studyAll) Activity.RESULT_OK else Activity.RESULT_CANCELED)
    }

    private fun showUnburyModes() {
        val unburyOptions =
            listOf(
                TR.studyingManuallyBuriedCards(),
                TR.studyingBuriedSiblings(),
                TR.studyingAllBuriedCards(),
            )
        val modes = listOf(UnburyDeckRequest.Mode.USER_ONLY, UnburyDeckRequest.Mode.SCHED_ONLY, UnburyDeckRequest.Mode.ALL)
        createPaperDialog(requireContext()) { dialog ->
            SingleChoiceDialog(
                title = TR.studyingWhatWouldYouLikeToUnbury(),
                choices = unburyOptions.map { DialogChoice(it) },
                selectedIndex = null,
                onSelect = { index ->
                    dialog.dismiss()
                    viewModel.onUnburyModeSelected(modes[index])
                },
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    /** Unburying returns straight to studying the Deck. */
    private fun openStudyScreenAndFinish() {
        Timber.i("opening the study screen")
        val host = requireActivity()
        finishScreen()
        host.startActivity(with(DeferredNavigation) { ReviewDeckDestination.CurrentDeck.toIntent() })
    }

    private fun openStudyOptionsAndFinish() {
        Timber.i("opening study options")
        val host = requireActivity()
        finishScreen()
        host.startActivity(with(DeferredNavigation) { StudyOptionsDestination.toIntent() })
    }

    @androidx.annotation.VisibleForTesting
    internal fun onCustomStudy() {
        launchCatchingTask {
            val customStudy = CustomStudyDialog.createInstance(deckId = withCol { decks.selected() })
            requireActivity().showDialogFragment(customStudy)
        }
    }

    companion object {
        private const val ARG_STUDY_ALL = "study_all"

        fun getIntent(
            context: Context,
            studyAll: Boolean = false,
        ): Intent =
            SingleFragmentActivity.getIntent(
                context,
                fragmentClass = FinishedFragment::class,
                arguments = if (studyAll) bundleOf(ARG_STUDY_ALL to true) else null,
            )
    }
}
