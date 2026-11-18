// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import anki.collection.OpChanges
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.backend.stripHTMLScriptAndStyleTags
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.utils.showDialogFragment
import com.ichi2.compose.theme.AnkiDroidTheme
import org.intellij.lang.annotations.Language
import timber.log.Timber

/**
 * Displays an overview of a deck (title, counts, description) and allows studying or modification
 * of the deck (Unbury, Deck Options, Custom Study)
 *
 * Filtered decks may be emptied or rebuilt
 *
 * On a tablet, this is the primary screen to study a deck and appears inside [DeckPicker]
 * On a phone, this is hosted inside [StudyOptionsActivity], opened via the the [DeckPicker] counts
 */
class StudyOptionsFragment :
    Fragment(),
    ChangeManager.Subscriber,
    MenuProvider {
    @VisibleForTesting
    internal val viewModel: StudyOptionsViewModel by viewModels()

    private var shownState: StudyOptionsState by mutableStateOf(StudyOptionsState.Loading)

    @VisibleForTesting
    val shownDeckId: DeckId? get() = shownState.deckIdOrNull()

    private var scrollDeckId: DeckId? = null
    private var scrollState by mutableStateOf(ScrollState(0))

    private var fragmented = false
    private var hasResumedView = false

    private var descriptionLayoutBeforeResizing: DescriptionLayoutSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scrollDeckId = savedInstanceState?.getLong(STATE_DISPLAYED_DECK_ID)?.takeIf { it != 0L }
        scrollState = ScrollState(savedInstanceState?.getInt(STATE_SCROLL_OFFSET) ?: 0)
        // Subscribe once per fragment: returning from another panel can recreate its view.
        ChangeManager.subscribe(this)
        observeProgress(viewModel) { progress -> getString(progress.messageRes) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val flow = parentFragment as? StudyOptionsFlowFragment
        fragmented = flow == null
        descriptionLayoutBeforeResizing = null
        return ComposeView(requireContext()).apply {
            id = R.id.studyoptions_main
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    Column {
                        flow?.Toolbar(this@StudyOptionsFragment)
                        StudyOptionsScreen(
                            state = shownState,
                            scrollState = scrollState,
                            inPane = fragmented,
                            mediaDir = viewModel.mediaDir,
                            onStart = { if (flow != null) flow.startStudy() else parentFragmentManager.setStudyOptionsStudyResult() },
                            onCustomStudy = ::showCustomStudyContextMenu,
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (fragmented) requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        hasResumedView = false
        viewModel.flowOfState.collectInViewLifecycle(Lifecycle.State.STARTED) { state ->
            state.deckIdOrNull()?.let { deckId ->
                if (scrollDeckId != deckId) scrollState = ScrollState(0)
                scrollDeckId = deckId
            }
            shownState = state
            activity?.invalidateMenu()
        }
        refreshInterface()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        scrollDeckId?.let { outState.putLong(STATE_DISPLAYED_DECK_ID, it) }
        outState.putInt(STATE_SCROLL_OFFSET, scrollState.value)
    }

    /** Uses cheaper wrapping during a divider drag, then restores the view's original settings. */
    fun onResizingChanged(isResizing: Boolean) {
        val description = view?.findViewById<TextView>(R.id.studyoptions_deck_description) ?: return
        if (isResizing) {
            if (descriptionLayoutBeforeResizing != null) return
            descriptionLayoutBeforeResizing =
                DescriptionLayoutSettings(
                    breakStrategy = description.breakStrategy,
                    hyphenationFrequency = description.hyphenationFrequency,
                )
            // Avoid whole-paragraph optimization while the width changes. Setters invalidate the
            // text layout, so only change these settings at the start and end of the gesture.
            @SuppressLint("WrongConstant")
            description.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
            description.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        } else {
            val originalSettings = descriptionLayoutBeforeResizing ?: return
            description.breakStrategy = originalSettings.breakStrategy
            description.hyphenationFrequency = originalSettings.hyphenationFrequency
            descriptionLayoutBeforeResizing = null
        }
    }

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.study_options_fragment, menu)
        if (fragmented) {
            // Primary action in the split-pane DeckPicker toolbar; when hosted in
            // StudyOptionsActivity the XML default (ifRoom) applies.
            menu.findItem(R.id.action_custom_study)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.findItem(R.id.action_rebuild)?.title = TR.actionsRebuild()
        menu.findItem(R.id.action_custom_study)?.title = TR.sentenceCase.customStudy
        menu.findItem(R.id.action_unbury)?.title = TR.studyingUnbury()
    }

    override fun onResume() {
        super.onResume()
        // onViewCreated has already requested this view's initial data.
        if (hasResumedView) refreshInterface()
        hasResumedView = true
    }

    private fun closeStudyOptions(result: Int) {
        val a: Activity? = activity
        if (!fragmented && a != null) {
            finishScreen(result)
        } else if (a == null) {
            // getActivity() can return null if reference to fragment lingers after parent activity has been closed,
            // which is particularly relevant when using AsyncTasks.
            Timber.e("closeStudyOptions() failed due to getActivity() returning null")
        }
    }

    private fun showCustomStudyContextMenu() {
        val dialog = CustomStudyDialog.createInstance(deckId = viewModel.selectedDeckId)
        if (parentFragment is StudyOptionsFlowFragment) {
            parentFragmentManager.showDialogFragment(
                dialog,
            )
        } else {
            requireActivity().showDialogFragment(dialog)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_deck_or_study_options -> {
                Timber.i("StudyOptionsFragment:: Deck or study options button pressed")
                val deckId = viewModel.selectedDeckId
                if (viewModel.isFilteredDeck) {
                    val i = FilteredDeckOptionsFragment.getIntent(requireActivity(), did = deckId)
                    Timber.i("Opening filtered deck options")
                    onDeckOptionsActivityResult.launch(i)
                } else {
                    val i =
                        com.ichi2.anki.pages.DeckOptions
                            .getIntent(requireContext(), deckId)
                    Timber.i("Opening deck options for activity result")
                    onDeckOptionsActivityResult.launch(i)
                }
                return true
            }
            R.id.action_custom_study -> {
                Timber.i("StudyOptionsFragment:: custom study button pressed")
                showCustomStudyContextMenu()
                return true
            }
            R.id.action_schedule_reminders -> {
                Timber.i("StudyOptionsFragment:: schedule reminders button pressed")
                val flow = parentFragment as? StudyOptionsFlowFragment
                if (flow !=
                    null
                ) {
                    flow.showReminders(viewModel.selectedDeckId)
                } else {
                    parentFragmentManager.setStudyOptionsAddEditReminderResult(viewModel.selectedDeckId)
                }
                return true
            }
            R.id.action_unbury -> {
                Timber.i("StudyOptionsFragment:: unbury button pressed")
                val deckId = shownDeckId ?: return false
                viewModel.unbury(deckId)
                item.isVisible = false
                return true
            }
            R.id.action_rebuild -> {
                Timber.i("StudyOptionsFragment:: rebuild cram deck button pressed")
                val deckId = shownDeckId?.takeIf { shownState.isFiltered() } ?: return false
                launchCatchingTask { viewModel.rebuildCram(deckId) }
                return true
            }
            R.id.action_empty -> {
                Timber.i("StudyOptionsFragment:: empty cram deck button pressed")
                val deckId = shownDeckId?.takeIf { shownState.isFiltered() } ?: return false
                launchCatchingTask { viewModel.emptyCram(deckId) }
                return true
            }
            else -> return false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        Timber.i("configureToolbarInternal()")
        val state = viewModel.state
        if (state is StudyOptionsState.Loading) return

        val showsFilteredDeck = shownState.isFiltered()
        menu.findItem(R.id.action_rebuild)?.isVisible = showsFilteredDeck
        menu.findItem(R.id.action_empty)?.isVisible = showsFilteredDeck
        if (viewModel.isFilteredDeck) {
            menu.findItem(R.id.action_custom_study)?.isVisible = false
            menu.findItem(R.id.action_deck_or_study_options)?.setTitle(CommonString.menu__study_options)
        } else {
            menu.findItem(R.id.action_custom_study)?.isVisible = true
            menu.findItem(R.id.action_deck_or_study_options)?.title = TR.sentenceCase.deckOptions
        }
        if (state is StudyOptionsState.Congrats || state is StudyOptionsState.Empty) {
            menu.findItem(R.id.action_custom_study)?.isVisible = false
        }
        menu.findItem(R.id.action_unbury)?.isVisible = viewModel.haveBuried
    }

    private var onDeckOptionsActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Timber.i("StudyOptionsFragment::mOnDeckOptionsActivityResult")

            if (!isAdded) {
                Timber.d("Fragment not added to the activity")
                CrashReportService.sendExceptionReport("Fragment is not added to activity", "StudyOptionsFragment")
                return@registerForActivityResult
            }

            Timber.d(
                "Handling onActivityResult for StudyOptionsFragment (deckOptions/filteredDeckOptions, resultCode = %d)",
                result.resultCode,
            )
            activity?.invalidateMenu()
            if (result.resultCode == DeckPicker.RESULT_MEDIA_EJECTED) {
                closeStudyOptions(result.resultCode)
                return@registerForActivityResult
            }
        }

    fun refreshInterface() {
        Timber.d("Refreshing StudyOptionsFragment")
        launchCatchingViewTask { viewModel.refreshData() }
    }

    private data class DescriptionLayoutSettings(
        val breakStrategy: Int,
        val hyphenationFrequency: Int,
    )

    companion object {
        private const val STATE_DISPLAYED_DECK_ID = "studyOptionsDisplayedDeckId"
        private const val STATE_SCROLL_OFFSET = "studyOptionsScrollOffset"

        /**
         * Identifier for a fragment result request to study(open the reviewer). Activities using
         * this fragment need to handle this request and initialize the study screen as they see fit.
         */
        private const val REQUEST_STUDY_OPTIONS_STUDY = "request_study_option_study"

        /**
         * Identifier for a fragment result request to open the review reminders screen for the selected deck.
         * Activities using this fragment need to handle this request and open the review reminders screen as they see fit.
         */
        private const val REQUEST_STUDY_OPTIONS_REVIEW_REMINDERS = "request_study_option_review_reminders"

        /**
         * Bundle key for the deck ID whose study options are being displayed, used when the review reminders screen is being opened.
         */
        private const val RESULT_REVIEW_REMINDERS_DECK_ID = "result_review_reminders_deck_id"

        @VisibleForTesting
        fun formatDescription(
            @Language("HTML") desc: String,
            imageGetter: Html.ImageGetter? = null,
        ): Spanned {
            // #5715: In deck description, ignore what is in style and script tag
            // Since we don't currently execute the JS/CSS, it's not worth displaying.
            val withStrippedTags = stripHTMLScriptAndStyleTags(desc)
            // #5188 - compat.fromHtml converts newlines into spaces.
            val withoutWindowsLineEndings = withStrippedTags.replace("\r\n", "<br/>")
            val withoutLinuxLineEndings = withoutWindowsLineEndings.replace("\n", "<br/>")

            return withoutLinuxLineEndings.parseAsHtml(
                flags = HtmlCompat.FROM_HTML_MODE_LEGACY,
                imageGetter = imageGetter,
            )
        }

        fun FragmentActivity.registerStudyOptionsStudyHandler(action: () -> Unit) {
            setFragmentResultListener(REQUEST_STUDY_OPTIONS_STUDY) { _, _ ->
                Timber.i("Received fragment result from study options fragment to start studying")
                action()
            }
        }

        fun FragmentActivity.registerStudyOptionsAddEditReminderHandler(action: (did: DeckId) -> Unit) {
            setFragmentResultListener(REQUEST_STUDY_OPTIONS_REVIEW_REMINDERS) { _, bundle ->
                Timber.i("Received fragment result from study options fragment for adding / editing review reminders")
                val did = bundle.getLong(RESULT_REVIEW_REMINDERS_DECK_ID)
                action(did)
            }
        }

        private fun FragmentManager.setStudyOptionsStudyResult() {
            setFragmentResult(REQUEST_STUDY_OPTIONS_STUDY, Bundle())
        }

        private fun FragmentManager.setStudyOptionsAddEditReminderResult(did: DeckId) {
            setFragmentResult(
                REQUEST_STUDY_OPTIONS_REVIEW_REMINDERS,
                Bundle().apply { putLong(RESULT_REVIEW_REMINDERS_DECK_ID, did) },
            )
        }
    }

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        if (activity != null) {
            refreshInterface()
        }
    }
}

@get:StringRes
private val StudyOptionsProgress.messageRes: Int
    get() =
        when (this) {
            StudyOptionsProgress.REBUILDING_FILTERED_DECK -> CommonString.rebuild_filtered_deck
            StudyOptionsProgress.EMPTYING_FILTERED_DECK -> CommonString.empty_filtered_deck
        }
