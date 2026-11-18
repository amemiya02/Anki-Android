// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs.customstudy

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.widget.AdapterView
import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import anki.scheduler.CustomStudyDefaultsResponse
import anki.scheduler.CustomStudyRequest.Cram.CramKind
import anki.scheduler.copy
import anki.scheduler.customStudyRequest
import anki.search.SearchNode
import anki.search.searchNode
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.asyncIO
import com.ichi2.anki.browser.search.CardState
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.EXTEND_NEW
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.EXTEND_REV
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.STUDY_AHEAD
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.STUDY_FORGOT
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.STUDY_PREVIEW
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.STUDY_TAGS
import com.ichi2.anki.dialogs.tags.TagsDialog
import com.ichi2.anki.dialogs.tags.TagsDialogListener.Companion.REQUEST_SELECT_TAGS
import com.ichi2.anki.dialogs.tags.TagsDialogListener.Companion.RESULT_SELECTED_TAGS
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.requireActivity
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.anki.utils.ext.getIntOrNull
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.withProgress
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.ProgressDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.coMeasureTime
import com.ichi2.utils.title
import kotlinx.coroutines.Deferred
import kotlinx.parcelize.Parcelize
import net.ankiweb.rsdroid.BackendException
import timber.log.Timber

/**
 * Implements custom studying either by:
 * 1. modifying the limits of the current selected deck for when the user has reached the daily
 *    deck limits and wishes to study more
 * 2. creating a filtered "Custom study deck" where a user can study outside the typical schedule
 *
 *
 * ## UI
 * [CustomStudyDialog] represents either:
 * * A [main menu][buildContextMenu], offering [methods of custom study][ContextMenuOption]
 * * An [input dialog][buildInputDialog] to input additional constraints for a [ContextMenuOption]
 *    * Example: changing the number of new cards
 *
 * Note: when studying by tags the input dialog will also display a state selector and on user
 * action will show one extra dialog(for actual tag selection, see [TagsDialog])
 *
 * ## Nomenclature
 * Filtered decks were previously known as 'dynamic' decks, and before that: 'cram' decks
 *
 * ## Links
 * * [https://docs.ankiweb.net/filtered-decks.html#custom-study](https://docs.ankiweb.net/filtered-decks.html#custom-study)
 * * [com.ichi2.anki.libanki.sched.Scheduler.customStudyDefaults]
 *     * [sched.proto: CustomStudyDefaultsResponse](https://github.com/search?q=repo%3Aankitects%2Fanki+CustomStudyDefaultsResponse+language%3A%22Protocol+Buffer%22&type=code&l=Protocol+Buffer)
 * * [com.ichi2.anki.libanki.sched.Scheduler.customStudy]
 *     * [sched.proto: CustomStudyRequest](https://github.com/search?q=repo%3Aankitects%2Fanki+CustomStudyRequest+language%3A%22Protocol+Buffer%22&type=code&l=Protocol+Buffer)
 * * [https://github.com/ankitects/anki/blob/main/qt/aqt/customstudy.py](https://github.com/ankitects/anki/blob/main/qt/aqt/customstudy.py)
 * * [https://github.com/ankitects/anki/blob/main/qt/aqt/taglimit](https://github.com/ankitects/anki/blob/main/qt/aqt/taglimit.py)
 *
 * @see TagsDialog
 */
class CustomStudyDialog : AnalyticsDialogFragment() {
    @VisibleForTesting(otherwise = PRIVATE)
    val viewModel by viewModels<CustomStudyViewModel>()

    /**
     * `null` initially when the main view is shown
     * otherwise, the [ContextMenuOption] representing the current sub-dialog
     */
    private val selectedSubDialog: ContextMenuOption?
        get() = requireArguments().getIntOrNull(ARG_SUB_DIALOG_ID)?.let { ContextMenuOption.entries[it] }

    private val userInputValue: Int?
        get() = viewModel.userInputText?.toIntOrNull()

    /** Whether a menu availability check or transition is already in progress. */
    private var menuSelectionInProgress = false

    /** Defaults captured for this input dialog when its main menu finishes loading. */
    private lateinit var defaults: CustomStudyDefaults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val option = selectedSubDialog ?: return
        if (option == STUDY_TAGS && viewModel.selectedCardStateIndex == AdapterView.INVALID_POSITION) {
            viewModel.selectedCardStateIndex = 0
        }
        parentFragmentManager.setFragmentResultListener(REQUEST_SELECT_TAGS, this) { _, bundle ->
            val tagsToInclude = bundle.getStringArrayList(RESULT_SELECTED_TAGS) ?: emptyList<String>()
            val selectedCardStateIndex = viewModel.selectedCardStateIndex
            if (selectedCardStateIndex == AdapterView.INVALID_POSITION) return@setFragmentResultListener
            val kind = viewModel.selectedKind
            val cardsAmount = userInputValue ?: 100 // the default value
            launchCustomStudy(option, cardsAmount, kind, tagsToInclude, emptyList())
        }
    }

    /** @see customStudy */
    private fun launchCustomStudy(
        option: ContextMenuOption,
        cardsAmount: Int,
        kind: CramKind = CramKind.CRAM_KIND_NEW,
        tagsToInclude: List<String> = emptyList(),
        tagsToExclude: List<String> = emptyList(),
    ) {
        // The activity owns this operation, which can outlive the dialog.
        val activity = requireActivity(simpleName = true)
        val fragmentManager = parentFragmentManager
        val deckId = viewModel.deckId
        val preferences = activity.sharedPrefs()
        activity.launchCatchingTask(
            // net.ankiweb.rsdroid.BackendException: No cards matched the criteria you provided.
            // TODO (Backend#256: make this BackendCustomStudyException)
            skipCrashReport = { it is BackendException },
        ) {
            activity.withProgress {
                try {
                    val action = customStudy(deckId, option, cardsAmount, kind, tagsToInclude, tagsToExclude)
                    fragmentManager.setFragmentResult(
                        CustomStudyAction.REQUEST_KEY,
                        Bundle().apply { putInt(CustomStudyAction.RESULT_CUSTOM_STUDY_ACTION, action.ordinal) },
                    )

                    // Save the default values (not in upstream).
                    when (option) {
                        STUDY_FORGOT -> preferences.edit { putInt("forgottenDays", cardsAmount) }
                        STUDY_AHEAD -> preferences.edit { putInt("aheadDays", cardsAmount) }
                        STUDY_PREVIEW -> preferences.edit { putInt("previewDays", cardsAmount) }
                        STUDY_TAGS -> preferences.edit { putInt("amountOfCards", cardsAmount) }
                        EXTEND_NEW, EXTEND_REV -> {
                            // The backend provides the default value.
                        }
                    }
                } finally {
                    activity.dismissAllDialogFragments()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val option = selectedSubDialog
        val ready = readyInitialization()
        return if (option == null || (option.checkAvailability != null && ready?.deckId != viewModel.deckId)) {
            Timber.i("Showing Custom Study main menu")
            val initialization = loadInitialization()
            deferredInitialization = initialization
            buildContextMenu(initialization)
        } else {
            Timber.i("Showing Custom Study dialog: $option")
            if (ready?.deckId == viewModel.deckId) {
                defaults = ready.defaults
            }
            buildInputDialog(option)
        }
    }

    /**
     * Continues the custom study process by showing an input dialog where the user can enter an
     * amount specific to that type of custom study(eg. cards, days etc).
     */
    private suspend fun onMenuItemSelected(
        menuDialog: Dialog,
        item: ContextMenuOption,
        defaults: CustomStudyDefaults,
        onEmptyDeck: () -> Unit,
    ) {
        if (menuSelectionInProgress) return
        menuSelectionInProgress = true
        try {
            // The deck may have changed since this menu loaded.
            val isUnavailable = withProgress { viewModel.isDeckEmptyOrHidden() }
            // The fragment can outlive this dialog and recreate it while the check is queued.
            if (
                this.dialog !== menuDialog ||
                !menuDialog.isShowing ||
                !isResumed ||
                requireActivity().isFinishing ||
                parentFragmentManager.isStateSaved
            ) {
                return
            }
            if (isUnavailable) {
                onEmptyDeck()
                return
            }
            if (item.checkAvailability?.invoke(defaults) == false) {
                showSnackbar(getString(CommonString.studyoptions_no_cards_due))
                return
            }

            val dialog: CustomStudyDialog = createSubDialog(viewModel.deckId, item)
            requireActivity().showDialogFragment(dialog)
        } finally {
            menuSelectionInProgress = false
        }
    }

    private fun buildContextMenu(initialization: Deferred<CustomStudyInitialization>): Dialog =
        createPaperDialog(requireContext()) { dialog ->
            var state by remember { mutableStateOf<CustomStudyInitialization>(CustomStudyInitialization.Loading) }
            LaunchedEffect(initialization) { state = initialization.await() }
            when (val current = state) {
                CustomStudyInitialization.Loading ->
                    ProgressDialog(title = TR.sentenceCase.customStudy, onCancel = dialog::cancel)
                CustomStudyInitialization.Empty ->
                    LaunchedEffect(Unit) {
                        lifecycle.withResumed {
                            val host = activity ?: return@withResumed
                            if (!dialog.isShowing || host.isFinishing || parentFragmentManager.isStateSaved) return@withResumed
                            dismiss()
                            host.showSnackbar(CommonString.empty_deck)
                        }
                    }
                is CustomStudyInitialization.Ready ->
                    SingleChoiceDialog(
                        title = TR.sentenceCase.customStudy,
                        choices =
                            ContextMenuOption.entries.map { option ->
                                DialogChoice(
                                    option.getTitle(resources),
                                    enabled =
                                        option.checkAvailability?.invoke(current.defaults) != false,
                                )
                            },
                        selectedIndex = null,
                        onSelect = { index ->
                            launchCatchingTask {
                                onMenuItemSelected(dialog, ContextMenuOption.entries[index], current.defaults) {
                                    state = CustomStudyInitialization.Empty
                                }
                            }
                        },
                        onDismissRequest = ::dismiss,
                    )
            }
        }.also { dialog ->
            // Native dismissal stops the window before FragmentManager delivers onDismiss.
            dialog.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        initialization.cancel()
                    }
                },
            )
        }

    private fun buildInputDialog(option: ContextMenuOption): Dialog = createPaperDialog(requireContext()) { StudyInputContent(option) }

    @Composable
    private fun StudyInputContent(option: ContextMenuOption) {
        val initial = viewModel.userInputText ?: defaultValue
        var input by remember {
            mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length)))
        }
        var selectedState by rememberSaveable {
            mutableIntStateOf(viewModel.selectedCardStateIndex.coerceAtLeast(0))
        }
        var selectingState by rememberSaveable { mutableStateOf(false) }
        BackHandler(selectingState) { selectingState = false }
        val range = option.validRange
        val value = input.text.toIntOrNull()
        val extends = option == EXTEND_NEW || option == EXTEND_REV
        val days = option == STUDY_AHEAD || option == STUDY_PREVIEW
        val requiresSearch = days && value != null && value in range
        var searchResult by remember(value) { mutableStateOf<Boolean?>(null) }
        if (requiresSearch) {
            LaunchedEffect(value) {
                val n = requireNotNull(value)
                searchResult = if (option == STUDY_PREVIEW) hasPreviewCards(n) else hasCardsDueWithin(n)
            }
        }
        val canSubmit = value != null && value != 0 && value in range && (!days || searchResult == true)
        var submitted by remember { mutableStateOf(false) }

        fun updateInput(text: TextFieldValue) {
            input = text
            viewModel.userInputText = text.text
        }

        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        val options = CustomStudyCardState.entries.map { it.labelProducer() }
        if (selectingState) {
            SingleChoiceDialog(
                title = getString(CommonString.custom_study_tags),
                choices = options.map { DialogChoice(it) },
                selectedIndex = selectedState,
                onSelect = {
                    selectedState = it
                    viewModel.selectedCardStateIndex = it
                    selectingState = false
                },
                onDismissRequest = { selectingState = false },
            )
            return
        }

        val confirmLabel =
            when {
                option == STUDY_TAGS -> TR.sentenceCase.chooseTags
                days -> getString(CommonString.dialog_positive_create)
                extends -> getString(CommonString.custom_study_increase)
                else -> getString(CommonString.dialog_ok)
            }

        fun submit() {
            val n = value ?: return
            if (!canSubmit || submitted) return
            if (option == STUDY_TAGS) {
                chooseTags(option, n)
            } else {
                submitted = true
                launchCustomStudy(option, n)
            }
        }

        PaperDialog(
            title = if (extends) getString(CommonString.custom_study_extend_limits_title) else option.getTitle(resources),
            onDismissRequest = ::dismiss,
            actions =
                listOf(
                    DialogAction(getString(CommonString.dialog_cancel), { requireActivity().dismissAllDialogFragments() }),
                    DialogAction(confirmLabel, ::submit, DialogActionStyle.Filled, enabled = canSubmit && !submitted),
                ),
        ) {
            val validationError =
                when {
                    input.text.isEmpty() || input.text == "-" -> null
                    value == null || value > range.last -> getString(CommonString.maximum_value_is, range.last)
                    value < range.first -> getString(CommonString.minimum_value_is, range.first)
                    searchResult == false -> TR.customStudyNoCardsMatchedTheCriteriaYou()
                    else -> null
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (extends) {
                    TextButton(onClick = {
                        val updated = ((value ?: 0) - 1).coerceIn(range).toString()
                        updateInput(TextFieldValue(updated, selection = TextRange(updated.length)))
                    }) { Text("−") }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { edited ->
                        val proposed = edited.text
                        val digits = if (extends) proposed.removePrefix("-") else proposed
                        if (!digits.all { it in '0'..'9' }) return@OutlinedTextField
                        if (days && digits.length > 1 && digits.startsWith('0')) return@OutlinedTextField
                        if (days && digits.length > 5) {
                            // Keep digits outside the edited range when a paste exceeds the field limit.
                            val prefix = input.text.commonPrefixWith(proposed)
                            val suffix =
                                input.text
                                    .drop(prefix.length)
                                    .commonSuffixWith(proposed.drop(prefix.length))
                                    .takeLast(5)
                            val end = 5 - suffix.length
                            val limited = proposed.take(end) + suffix
                            updateInput(TextFieldValue(limited, selection = TextRange(end)))
                        } else {
                            updateInput(edited)
                        }
                    },
                    label = { Text(text2) },
                    supportingText = { if (validationError != null) Text(validationError) },
                    suffix =
                        if (days) {
                            (
                                {
                                    Text(
                                        resources.getQuantityString(CommonPlurals.set_due_date_label_suffix, value ?: 1),
                                    )
                                }
                            )
                        } else {
                            null
                        },
                    isError = validationError != null,
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = if (extends) KeyboardType.Decimal else KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier =
                        Modifier.weight(1f).focusRequester(focus).semantics {
                            if (validationError != null) error(validationError)
                        },
                )
                if (extends) {
                    TextButton(onClick = {
                        val updated = ((value ?: 0) + 1).coerceIn(range).toString()
                        updateInput(TextFieldValue(updated, selection = TextRange(updated.length)))
                    }) { Text("+") }
                }
            }
            if (text1.isNotEmpty()) DialogDetail(text1)
            if (option == STUDY_TAGS) {
                DialogChoiceRow(DialogChoice(options[selectedState]), checked = true, onClick = { selectingState = true }, multiple = false)
            }
            LaunchedEffect(focus) {
                focus.requestFocus()
                keyboard?.show()
            }
        }
    }

    private fun chooseTags(
        option: ContextMenuOption,
        amount: Int,
    ) {
        launchCatchingTask {
            val nids =
                withCol {
                    val currentDeckName = decks.name(viewModel.deckId)
                    // this allows us to skip the tag selection dialog entirely
                    // if there are no tags available to choose from in this deck
                    val searchNodes =
                        buildList {
                            add(SearchNode.newBuilder().setDeck(currentDeckName).build())
                            add(
                                SearchNode
                                    .newBuilder()
                                    .setNegated(SearchNode.newBuilder().setTag("none").build())
                                    .build(),
                            )
                        }
                    findNotes(buildSearchString(searchNodes))
                }
            // skip tag selection if there's no tags to select
            if (nids.isEmpty()) {
                launchCustomStudy(option, amount, viewModel.selectedKind)
                return@launchCatchingTask
            }
            if (isAdded) {
                TagsDialog()
                    .withArguments(
                        requireContext(),
                        TagsDialog.DialogType.CUSTOM_STUDY,
                        nids,
                    ).show(parentFragmentManager, "TagsDialog")
            }
        }
    }

    /**
     * Whether there are new cards added within [days] days in the deck for previewing.
     *
     * Mirrors the upstream logic:
     * [https://github.com/ankitects/anki/blob/main/qt/aqt/customstudy.py](https://github.com/ankitects/anki/blob/main/qt/aqt/customstudy.py)
     */
    private suspend fun hasPreviewCards(days: Int): Boolean =
        withCol {
            val search =
                listOf(
                    searchNode { deck = decks.name(viewModel.deckId) },
                    CardState.New.toSearchNode(),
                    searchNode { addedInDays = days },
                )
            findCards(buildSearchString(search)).isNotEmpty()
        }

    /** Whether the deck has cards due in the next [days] days, as 'review ahead' would select them */
    private suspend fun hasCardsDueWithin(days: Int): Boolean =
        withCol {
            val search =
                listOf(
                    SearchNode.newBuilder().setDeck(decks.name(viewModel.deckId)).build(),
                    // prop:due<=days
                    SearchNode.newBuilder().setDueInDays(days).build(),
                )
            findCards(buildSearchString(search)).isNotEmpty()
        }

    // TODO cram kind and the included/excluded tags lists are only relevant for STUDY_TAGS and
    //  should be included in the option to not leak in the method's api
    private suspend fun customStudy(
        deckId: DeckId,
        contextMenuOption: ContextMenuOption,
        userEntry: Int,
        cramKind: CramKind,
        tagsSelectedForInclude: List<String>,
        tagsSelectedForExclude: List<String>,
    ): CustomStudyAction {
        Timber.i("Custom study: $contextMenuOption; input = $userEntry")

        val request =
            customStudyRequest {
                this.deckId = deckId
                when (contextMenuOption) {
                    EXTEND_NEW -> newLimitDelta = userEntry
                    EXTEND_REV -> reviewLimitDelta = userEntry
                    STUDY_FORGOT -> forgotDays = userEntry
                    STUDY_AHEAD -> reviewAheadDays = userEntry
                    STUDY_PREVIEW -> previewDays = userEntry
                    STUDY_TAGS -> {
                        // https://github.com/ankitects/anki/blob/acaeee91fa853e4a7a78dcddbb832d009ec3529a/qt/aqt/customstudy.py#L169-L177
                        cram =
                            cram.copy {
                                kind = cramKind
                                cardLimit = userEntry
                                tagsToInclude.addAll(tagsSelectedForInclude)
                                tagsToExclude.addAll(tagsSelectedForExclude)
                            }
                    }
                }
            }

        undoableOp { sched.customStudy(request) }
        return when (contextMenuOption) {
            EXTEND_NEW, EXTEND_REV -> CustomStudyAction.EXTEND_STUDY_LIMITS
            STUDY_FORGOT, STUDY_AHEAD, STUDY_PREVIEW, STUDY_TAGS -> CustomStudyAction.CUSTOM_STUDY_SESSION
        }
    }

    /**
     * Loads [CustomStudyDefaults] from the backend
     *
     * This method may be slow (> 1s)
     */
    private fun loadInitialization(): Deferred<CustomStudyInitialization> {
        val host = requireActivity()
        return lifecycleScope.asyncIO {
            coMeasureTime("loadCustomStudyDefaults") {
                viewModel.initialize { !host.isFinishing }
            }
        }
    }

    /**
     * Line 1 of the number entry dialog
     *
     * e.g. "Review forgotten cards"
     *
     * Requires [ContextMenuOption.checkAvailability] to be null/return true
     */
    private val text1: String
        get() =
            when (selectedSubDialog) {
                EXTEND_NEW -> defaults.labelForNewQueueAvailable()
                EXTEND_REV -> defaults.labelForReviewQueueAvailable()
                STUDY_FORGOT,
                STUDY_AHEAD,
                STUDY_PREVIEW,
                STUDY_TAGS,
                null,
                -> ""
            }

    /** Line 2 of the number entry dialog */
    private val text2: String
        get() {
            val res = resources
            return when (selectedSubDialog) {
                EXTEND_NEW -> TR.customStudyIncreaseTodaysNewCardLimitBy()
                EXTEND_REV -> TR.customStudyIncreaseTodaysReviewLimitBy()
                STUDY_FORGOT -> res.getString(CommonString.custom_study_forgotten)
                STUDY_AHEAD -> res.getString(CommonString.custom_study_ahead_description)
                STUDY_PREVIEW -> res.getString(CommonString.custom_study_preview)
                STUDY_TAGS -> res.getString(CommonString.custom_study_tags)
                null -> ""
            }
        }

    /**
     * Initial value of the number entry dialog
     *
     * Requires [ContextMenuOption.checkAvailability] to be null/return true
     */
    private val defaultValue: String
        get() {
            val prefs = requireActivity().sharedPrefs()
            return when (selectedSubDialog) {
                EXTEND_NEW -> defaults.extendNew.initialValue.toString()
                EXTEND_REV -> defaults.extendReview.initialValue.toString()
                STUDY_FORGOT -> prefs.getInt("forgottenDays", 1).toString()
                STUDY_AHEAD -> prefs.getInt("aheadDays", 1).toString()
                STUDY_PREVIEW -> prefs.getInt("previewDays", 1).toString()
                // currently(as of Anki 25.02) not upstream
                STUDY_TAGS -> prefs.getInt("amountOfCards", 100).toString()
                null,
                -> ""
            }
        }

    /**
     * Represents actions for managing custom study sessions and extending study limits.
     * These actions are passed between fragments and activities via the FragmentResult API.
     */
    enum class CustomStudyAction {
        EXTEND_STUDY_LIMITS,
        CUSTOM_STUDY_SESSION,
        ;

        companion object {
            const val REQUEST_KEY = "CustomStudyDialog"
            const val RESULT_CUSTOM_STUDY_ACTION = "action"

            /** Extracts a [CustomStudyAction] from a [Bundle] */
            fun fromBundle(bundle: Bundle): CustomStudyAction =
                bundle.getInt(CustomStudyAction.RESULT_CUSTOM_STUDY_ACTION).let { actionOrdinal ->
                    entries.first { it.ordinal == actionOrdinal }
                }
        }
    }

    /**
     * Context menu options shown in the custom study dialog.
     *
     * @param checkAvailability Whether the menu option is available
     */
    @VisibleForTesting(otherwise = PRIVATE)
    enum class ContextMenuOption(
        val getTitle: Resources.() -> String,
        val validRange: IntRange,
        val checkAvailability: ((CustomStudyDefaults) -> Boolean)? = null,
    ) {
        /** Increase today's new card limit */
        EXTEND_NEW(
            { TR.customStudyIncreaseTodaysNewCardLimit() },
            validRange = -Consts.DYN_MAX_SIZE..Consts.DYN_MAX_SIZE,
            checkAvailability = { it.extendNew.isUsable },
        ),

        /** Increase today's review card limit */
        EXTEND_REV(
            { TR.customStudyIncreaseTodaysReviewCardLimit() },
            validRange = -Consts.DYN_MAX_SIZE..Consts.DYN_MAX_SIZE,
            checkAvailability = { it.extendReview.isUsable },
        ),

        /** Review forgotten cards */
        STUDY_FORGOT({ TR.customStudyReviewForgottenCards() }, validRange = 1..FORGOT_MAX_DAYS),

        /** Review ahead */
        STUDY_AHEAD({ TR.customStudyReviewAhead() }, validRange = 1..Consts.DYN_MAX_SIZE),

        /** Preview new cards */
        STUDY_PREVIEW({ TR.customStudyPreviewNewCards() }, validRange = 1..Consts.DYN_MAX_SIZE),

        /** Limit to particular tags */
        STUDY_TAGS({ TR.customStudyStudyByCardStateOrTag() }, validRange = 1..Consts.DYN_MAX_SIZE),
    }

    @Parcelize
    enum class CustomStudyCardState(
        val labelProducer: () -> String,
        val kind: CramKind,
    ) : Parcelable {
        NewCardsOnly({ TR.customStudyNewCardsOnly() }, CramKind.CRAM_KIND_NEW),
        DueCardsOnly({ TR.customStudyDueCardsOnly() }, CramKind.CRAM_KIND_DUE),
        ReviewCardsRandom({ TR.customStudyAllReviewCardsInRandomOrder() }, CramKind.CRAM_KIND_REVIEW),
        AllCardsRandom({ TR.customStudyAllCardsInRandomOrderDont() }, CramKind.CRAM_KIND_ALL),
    }

    /**
     * Default values for extending deck limits, and default tag selection
     *
     * Adapter which documents [anki.scheduler.CustomStudyDefaultsResponse]
     *
     * Upstream: [sched.proto: CustomStudyDefaultsResponse](https://github.com/search?q=repo%3Aankitects%2Fanki+CustomStudyDefaultsResponse+language%3A%22Protocol+Buffer%22&type=code&l=Protocol+Buffer)
     */
    @VisibleForTesting
    class CustomStudyDefaults(
        val extendNew: ExtendLimits,
        val extendReview: ExtendLimits,
        @Suppress("unused")
        val tags: List<CustomStudyDefaultsResponse.Tag>,
    ) {
        /** Available new cards: 1 (2 in subdecks) */
        fun labelForNewQueueAvailable(): String = TR.customStudyAvailableNewCards2(extendNew.labelForCountWithChildren())

        /** Available review cards: 1 (2 in subdecks) */
        fun labelForReviewQueueAvailable(): String = TR.customStudyAvailableReviewCards2(extendReview.labelForCountWithChildren())

        /**
         * Data displayed to a user wanting to temporarily extend the daily limits of
         * either new/review cards for a deck
         *
         * Displays `Available new cards: 1 (2 in subdecks)` when a limit is reached with remaining cards:
         * ```
         * Deck (1)
         *   Deck::Child1 (1)
         *   Deck::Child2 (1)
         * ```
         */
        class ExtendLimits(
            /** The initial value to display in the input dialog */
            val initialValue: Int,
            /**
             * The number of pending cards in only the parent deck
             *
             * **Example**
             * Returns **1** when a limit is reached with remaining cards:
             * ```
             * Deck (1)
             *   Deck::Child1 (1)
             *   Deck::Child2 (1)
             * ```
             */
            val available: Int,
            /**
             * The sum of cards in only the child decks
             *
             * **Example**
             * * Returns **2** when a limit is reached  with remaining cards:
             * ```
             * Deck (1)
             *   Deck::Child1 (1)
             *   Deck::Child2 (1)
             * ```
             */
            val availableInChildren: Int,
        ) {
            /**
             * **Temporarily Disabled** - logic may be incorrect
             *
             * "Extend" only has an effect if there are pending cards in the target deck
             *
             * The number of pending cards in child decks is only informative
             */
            val isUsable
                get() = true // TODO: Confirm `available > 0` is correct; user feedback states subdecks are taken into account

            /**
             * A string representing the count of cards which have exceeded a deck limit
             *
             * `123 (456 in subdecks)` or `123`
             *
             * For use in either
             * [net.ankiweb.rsdroid.Translations.customStudyAvailableReviewCards2] or
             * [net.ankiweb.rsdroid.Translations.customStudyAvailableNewCards2]
             *
             */
            fun labelForCountWithChildren(): String =
                if (availableInChildren == 0) {
                    available.toString()
                } else {
                    "$available ${TR.customStudyAvailableChildCount(availableInChildren)}"
                }
        }

        companion object {
            fun CustomStudyDefaultsResponse.toDomainModel(): CustomStudyDefaults =
                CustomStudyDefaults(
                    extendNew =
                        ExtendLimits(
                            initialValue = extendNew,
                            available = availableNew,
                            availableInChildren = availableNewInChildren,
                        ),
                    extendReview =
                        ExtendLimits(
                            initialValue = extendReview,
                            available = availableReview,
                            availableInChildren = availableReviewInChildren,
                        ),
                    tags = this.tagsList,
                )
        }
    }

    companion object {
        /**
         * @see CustomStudyDefaults
         *
         * Singleton; initialized when the main screen is loaded
         * This exists so we don't need to pass an unbounded object between fragments
         */
        private lateinit var deferredInitialization: Deferred<CustomStudyInitialization>

        /**
         * Completed defaults shared with subdialogs. Restore the main menu if the loading
         * dialog was cancelled or its process died.
         */
        private fun readyInitialization(): CustomStudyInitialization.Ready? {
            if (!::deferredInitialization.isInitialized || !deferredInitialization.isCompleted ||
                deferredInitialization.isCancelled
            ) {
                return null
            }
            return deferredInitialization.getCompleted() as? CustomStudyInitialization.Ready
        }

        /**
         * Creates an instance of the Custom Study Dialog: a user can select a custom study type
         */
        fun createInstance(deckId: DeckId): CustomStudyDialog =
            CustomStudyDialog().apply {
                arguments =
                    Bundle().apply {
                        putLong(CustomStudyViewModel.ARG_DECK_ID, deckId)
                    }
            }

        /**
         * Creates an instance of the Custom Study sub-dialog for a user to configure
         * a selected custom study type
         *
         * e.g. After selecting "Study Ahead", entering the number of days to study ahead by
         */
        fun createSubDialog(
            deckId: DeckId,
            contextMenuAttribute: ContextMenuOption,
        ): CustomStudyDialog =
            CustomStudyDialog().apply {
                arguments =
                    Bundle().apply {
                        putLong(CustomStudyViewModel.ARG_DECK_ID, deckId)
                        putInt(ARG_SUB_DIALOG_ID, contextMenuAttribute.ordinal)
                    }
            }

        const val FORGOT_MAX_DAYS = 30

        /**
         * (optional) Key for the ordinal of the [ContextMenuOption] to display.
         * @see CustomStudyDialog.selectedSubDialog
         */
        private const val ARG_SUB_DIALOG_ID = "subDialogId"
    }
}
