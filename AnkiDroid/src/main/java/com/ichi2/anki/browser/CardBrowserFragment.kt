// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import anki.collection.OpChanges
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.AnkiActivityProvider
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.getColUnsafe
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.android.input.ShortcutGroup
import com.ichi2.anki.android.input.shortcut
import com.ichi2.anki.browser.CardBrowserViewModel.ChangeNoteTypeResponse
import com.ichi2.anki.browser.CardBrowserViewModel.NoteEditorCommand
import com.ichi2.anki.browser.CardBrowserViewModel.SearchState
import com.ichi2.anki.browser.RepositionCardFragment.Companion.REQUEST_REPOSITION_NEW_CARDS
import com.ichi2.anki.browser.RepositionCardsRequest.NoRepositionableCardsError
import com.ichi2.anki.browser.RepositionCardsRequest.RepositionData
import com.ichi2.anki.browser.search.CardBrowserSearchViewModel
import com.ichi2.anki.browser.search.CardBrowserSearchViewModel.UserMessage
import com.ichi2.anki.browser.search.SearchRequest
import com.ichi2.anki.browser.search.SearchString
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.common.utils.ext.ifNotZero
import com.ichi2.anki.dialogs.BrowserOptionsDialog
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.anki.dialogs.DeckSelectionDialog.Companion.RESULT_SELECTED_DECK
import com.ichi2.anki.dialogs.FlagRenameDialog
import com.ichi2.anki.dialogs.GradeNowDialog
import com.ichi2.anki.dialogs.ManageSavedSearchAction
import com.ichi2.anki.dialogs.SaveBrowserSearchDialogFragment
import com.ichi2.anki.dialogs.SavedBrowserSearchesDialogFragment
import com.ichi2.anki.dialogs.SimpleMessageDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.registerSaveSearchHandler
import com.ichi2.anki.dialogs.registerSavedSearchActionHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.dialogs.tags.TagsDialogFactory
import com.ichi2.anki.dialogs.tags.TagsDialogListener
import com.ichi2.anki.export.ExportDialogFragment
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.formatCardCount
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.exception.InvalidSearchException
import com.ichi2.anki.libanki.undoAvailable
import com.ichi2.anki.libanki.undoLabel
import com.ichi2.anki.model.CardStateFilter
import com.ichi2.anki.model.CardsOrNotes.CARDS
import com.ichi2.anki.model.CardsOrNotes.NOTES
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.pages.ChangeNoteTypePage
import com.ichi2.anki.previewer.PreviewerFragment
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.requireAnkiActivity
import com.ichi2.anki.requireNavigationDrawerActivity
import com.ichi2.anki.scheduling.ForgetCardsDialog
import com.ichi2.anki.scheduling.SetDueDateDialog
import com.ichi2.anki.scheduling.registerOnForgetHandler
import com.ichi2.anki.setBrowserFullScreen
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.undoAndShowSnackbar
import com.ichi2.anki.utils.ext.collectInLifecycle
import com.ichi2.anki.utils.ext.getParcelableCompat
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.utils.showSearchError
import com.ichi2.anki.withProgress
import com.ichi2.compose.theme.AnkiDroidTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.ankiweb.rsdroid.Translations
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

// Minor BUG: 'don't keep activities' and huge selection
// At some point, starting between 35k and 60k selections, the scroll position is lost on recreation
// This occurred on a Pixel 9 Pro, Android 15
class CardBrowserFragment :
    Fragment(),
    AnkiActivityProvider,
    ChangeManager.Subscriber,
    TagsDialogListener {
    private val browserPane: CardBrowserPaneFragment?
        get() = parentFragment as? CardBrowserPaneFragment

    private val hostBrowserViewModel: CardBrowserViewModel by viewModels(ownerProducer = { requireActivity() }) {
        val host = requireActivity()
        if (host is DeckPicker) {
            CardBrowserViewModel.factory(
                lastDeckIdRepository = AnkiDroidApp.instance.sharedPrefsLastDeckIdRepository,
                idsFileDirectory = IdsFile.getDirectory(),
                options = null,
                isFragmented = false,
                initialDeckId = null,
            )
        } else {
            host.defaultViewModelProviderFactory
        }
    }

    val activityViewModel: CardBrowserViewModel
        get() = browserPane?.browserViewModel ?: hostBrowserViewModel

    val searchViewModel: CardBrowserSearchViewModel by viewModels(ownerProducer = { browserPane ?: requireActivity() })

    // TODO: remove AnkiActivityProvider, use context parameters instead
    override val ankiActivity: AnkiActivity
        get() = requireAnkiActivity()

    private lateinit var tagsDialogFactory: TagsDialogFactory

    private var undoSnackbar: Snackbar? = null

    private val onAddNoteActivityResult =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            Timber.d("onAddNoteActivityResult: resultCode=%d", result.resultCode)
            if (result.resultCode == Activity.RESULT_OK) {
                activityViewModel.launchSearchForCards(feedbackMode = BrowserSearchFeedbackMode.COUNT_ONLY)
            }
        }

    /** Whether the browser is hosted by the phone's Browse tab. */
    private fun isHostedInBottomNavigation() = activity !is CardBrowser && browserPane == null

    /**
     * Returns the current deck name, "All Decks" if all decks are selected, or "Unknown"
     * Do not use this for any business logic, as this will return inconsistent data
     * with the collection.
     */
    private val selectedDeckNameForUi: String
        get() =
            activityViewModel.searchRequestFlow.value.filters.decks
                .firstOrNull()
                ?.name
                ?: TR.sentenceCase.allDecks

    internal val flagNameRevision = MutableStateFlow(0)
    internal val sheetRequests = MutableSharedFlow<BrowserSheet>(extraBufferCapacity = 1)

    internal fun openSearch() {
        if (!isVisible || activityViewModel.isInMultiSelectMode) return
        launchCatchingTask {
            val request = activityViewModel.searchRequestFlow.value
            val text = if (request.isEmpty) "" else withCol { request.toSearchString() }.getOrNull()?.value ?: request.query
            searchViewModel.openBrowserSearch(text)
        }
    }

    internal fun replaceSearchDraft(request: SearchRequest) {
        launchCatchingTask {
            val text = if (request.isEmpty) "" else withCol { request.toSearchString() }.getOrNull()?.value ?: request.query
            searchViewModel.updateBrowserDraft(text)
        }
    }

    internal fun browserChromeChanged(takingOver: Boolean) {
        if (isHostedInBottomNavigation()) {
            (activity as? DeckPicker)?.setBrowserFullScreen(takingOver && !isHidden)
        }
    }

    internal fun browserBack() {
        browserPane?.navigateBack() ?: requireNavigationDrawerActivity().onNavigationPressed()
    }

    internal fun showsStandaloneBack() = browserPane != null || !activityViewModel.isFragmented

    private fun saveSubmittedSearch(search: SearchRequest = activityViewModel.searchRequestFlow.value) {
        if (search.isEmpty) return
        launchCatchingTask {
            val query = withCol { search.toSearchString() }.getOrThrow().value
            SaveBrowserSearchDialogFragment.newInstance(query).show(childFragmentManager, null)
        }
    }

    internal fun openSearchManual() = ankiActivity.openUrl("https://docs.ankiweb.net/searching.html")

    internal fun renameFlags() {
        FlagRenameDialog().show(childFragmentManager, FLAG_RENAME_TAG)
    }

    private fun invalidateFlagNames() {
        flagNameRevision.value++
    }

    internal fun browserMenu(
        selecting: Boolean,
        state: SearchState,
        search: SearchRequest,
    ): List<BrowserMenuEntry> =
        buildList {
            fun addAction(
                label: String,
                action: () -> Unit,
            ) = add(BrowserMenuEntry(label, action))

            if (state is SearchState.Completed && getColUnsafe().undoAvailable()) {
                getColUnsafe().undoLabel()?.let { addAction(it) { onUndo() } }
            }
            if (selecting) {
                addAction(getString(CommonString.card_browser_select_none)) { activityViewModel.selectNone() }
                return@buildList
            }
            if (activityViewModel.rowCount > 0 && !activityViewModel.isFragmented) {
                addAction(getString(CommonString.card_editor_preview_card), ::onPreview)
            }
            if (activityViewModel.rowCount > 0) {
                addAction(getString(CommonString.card_browser_select_all)) { activityViewModel.selectAll() }
            }
            if (search.query.isNotBlank() || search.filters.activeFilters.isNotEmpty()) {
                addAction(getString(CommonString.card_browser_list_my_searches_save)) { saveSubmittedSearch(search) }
            }
            addAction(getString(CommonString.new_dynamic_deck), ::showFilteredDeckScreen)
            addAction(TR.sentenceCase.findAndReplace, ::showFindAndReplaceDialog)
            addAction(getString(CommonString.browse_manage_columns_main_heading)) {
                BrowserColumnSelectionFragment.createInstance(activityViewModel.cardsOrNotes).show(parentFragmentManager, null)
            }
            addAction(getString(CommonString.rename_flag), ::renameFlags)
            addAction(TR.sentenceCase.browserOptions, ::showOptionsDialog)
        }.map { item ->
            item.copy(action = {
                prepareForUndoableOperation()
                item.action()
            })
        }

    internal fun selectionShortcuts(
        onFlag: () -> Unit,
        onMore: () -> Unit,
    ): List<BrowserSelectionShortcut> =
        listOf(
            BrowserSelectionShortcut(
                getString(R.string.browse_action_deck),
                TR.sentenceCase.changeDeck,
                R.drawable.symbol_move_selection_right,
            ) {
                prepareForUndoableOperation()
                showChangeDeckDialog()
            },
            BrowserSelectionShortcut(
                getString(R.string.browse_action_flag),
                TR.sentenceCase.flagCard,
                R.drawable.symbol_flag,
                onFlag,
            ),
            BrowserSelectionShortcut(
                getString(R.string.browse_action_tags),
                getString(CommonString.menu_edit_tags),
                R.drawable.symbol_label,
            ) {
                prepareForUndoableOperation()
                showEditTagsDialog()
            },
            BrowserSelectionShortcut(
                getString(R.string.browse_action_suspend),
                TR.sentenceCase.toggleSuspend,
                R.drawable.symbol_pause_circle,
            ) { toggleSuspendCards() },
            BrowserSelectionShortcut(
                getString(R.string.browse_action_more),
                getString(R.string.browse_action_more),
                R.drawable.symbol_more_horiz,
                onMore,
            ),
        )

    internal fun selectionActions(count: Int): List<BrowserSelectionAction> {
        val mode = activityViewModel.cardsOrNotes
        val editDetail =
            when {
                count == 1 -> null
                mode == CARDS -> getString(R.string.browse_edit_single_selection)
                else -> getString(R.string.browse_edit_single_note_selection)
            }
        return listOf(
            BrowserSelectionAction(
                getString(CommonString.cardeditor_title_edit_card),
                R.drawable.symbol_edit_note,
                detail = editDetail,
                enabled = count == 1,
                action = ::openNoteEditorForCurrentlySelectedRow,
            ),
            BrowserSelectionAction(getString(CommonString.card_editor_preview_card), R.drawable.symbol_visibility, action = ::onPreview),
            BrowserSelectionAction(TR.sentenceCase.cardInfo, R.drawable.symbol_info) { displayCardInfo() },
            BrowserSelectionAction(
                getString(R.string.browse_action_mark),
                R.drawable.symbol_star,
                detail = getString(R.string.browse_mark_selection_note),
            ) { toggleMark() },
            BrowserSelectionAction(
                TR.sentenceCase.changeNoteType,
                R.drawable.symbol_swap_horiz,
            ) { activityViewModel.requestChangeNoteType() },
            BrowserSelectionAction(TR.sentenceCase.findAndReplace, R.drawable.symbol_find_replace, action = ::showFindAndReplaceDialog),
            BrowserSelectionAction(
                resources.getQuantityString(
                    if (mode == CARDS) CommonPlurals.card_browser_export_cards else CommonPlurals.card_browser_export_notes,
                    count,
                ),
                R.drawable.symbol_file_export,
                action = ::exportSelected,
            ),
            BrowserSelectionAction(
                TR.sentenceCase.setDueDate,
                R.drawable.symbol_calendar_today,
                startsGroup = true,
                action = ::rescheduleSelectedCards,
            ),
            BrowserSelectionAction(
                getString(CommonString.card_editor_reposition_card),
                R.drawable.symbol_low_priority,
                detail = getString(R.string.browse_reposition_new_cards),
            ) { repositionSelectedCards() },
            BrowserSelectionAction(TR.sentenceCase.gradeNow, R.drawable.symbol_grading) { openGradeNow() },
            BrowserSelectionAction(
                getString(CommonString.card_editor_reset_card),
                R.drawable.symbol_restart_alt,
                action = ::onResetProgress,
            ),
            BrowserSelectionAction(getString(R.string.browse_action_bury), R.drawable.symbol_visibility_off) { toggleBury() },
            BrowserSelectionAction(
                resources.getQuantityString(CommonPlurals.card_browser_delete_notes, count),
                CommonR.drawable.symbol_delete,
                destructive = true,
                startsGroup = true,
            ) { deleteSelectedNotes() },
        ).map { item ->
            item.copy(action = {
                prepareForUndoableOperation()
                item.action()
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isHostedInBottomNavigation()) ChangeManager.subscribe(this)
        tagsDialogFactory = TagsDialogFactory(listener = this).attachToActivity<TagsDialogFactory>(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            id = R.id.card_browser_compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    CardBrowserScreen(this@CardBrowserFragment, embedded = isHostedInBottomNavigation())
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            // Selected cards may not be ready yet on activity recreation,
            // so it is necessary to dismiss the change deck dialog
            (parentFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG) as? DeckSelectionDialog)?.dismiss()
        }

        observeProgress(activityViewModel) { progress -> getString(progress.messageRes) }

        // onSearchForDecks starts deck selection using childFragmentManager
        childFragmentManager.setFragmentResultListener(DeckSelectionDialog.REQUEST_SELECT_DECK, this) { _, bundle ->
            val selectedDeck = bundle.getParcelableCompat<SelectableDeck>(RESULT_SELECTED_DECK)
            activityViewModel.setSelectedDeck(requireNotNull(selectedDeck) { "Expected non-null deck" })
        }

        registerDeckSelectedHandler(REQUEST_DECK_SELECTION_CHANGE_DECK) { selectedDeck ->
            require(selectedDeck is SelectableDeck.Deck) { "Expected non-null deck" }
            moveSelectedCardsToDeck(selectedDeck.deckId)
        }

        childFragmentManager.setFragmentResultListener(FlagRenameDialog.REQUEST_RENAMED_FLAGS, viewLifecycleOwner) { _, _ ->
            invalidateFlagNames()
        }
        registerSaveSearchHandler { searchViewModel.addSavedSearch(it) }
        registerSavedSearchActionHandler { action ->
            when (action) {
                is ManageSavedSearchAction.SelectSearch -> searchViewModel.submitBrowserSearch(action.search.query)
                is ManageSavedSearchAction.Delete -> searchViewModel.deleteSavedSearch(action.search)
            }
        }
        if (activity !is CardBrowser) {
            registerOnForgetHandler { activityViewModel.queryAllSelectedCardIds() }
            registerBrowserFindReplaceHandler { result ->
                launchCatchingTask {
                    showSnackbar(TR.browsingNotesUpdated(activityViewModel.findAndReplace(result).await()))
                }
            }
        }

        setupComposeFlows()
        setupFragmentResultListeners()
    }

    private fun setupComposeFlows() {
        activityViewModel.flowOfSearchState.collectInLifecycle { state ->
            if (state is SearchState.Error) requireContext().showSearchError(state.error)
        }
        activityViewModel.flowOfSearchFeedback.filterNotNull().collectInLifecycle { state ->
            if (!activityViewModel.consumeSearchFeedback(state)) return@collectInLifecycle
            val feedback =
                browserSearchFeedback(
                    feedbackMode = state.feedbackMode,
                    rowCount = state.rowCount,
                    allDecksSelected = state.allDecksSelected,
                    isHeaderCountVisible = !activityViewModel.isInMultiSelectMode,
                )
            val message =
                when (feedback) {
                    BrowserSearchFeedback.NONE -> return@collectInLifecycle
                    BrowserSearchFeedback.COUNT, BrowserSearchFeedback.COUNT_WITH_SEARCH_ALL_DECKS -> state.formatCardCount(resources)
                    BrowserSearchFeedback.NO_CARDS_IN_SELECTED_DECK ->
                        getString(CommonString.card_browser_no_cards_in_deck, selectedDeckNameForUi)
                }
            showSnackbar(message, Snackbar.LENGTH_SHORT) {
                if (feedback.offersSearchAllDecks) {
                    setAction(CommonString.card_browser_search_all_decks) {
                        activityViewModel.setSelectedDeck(SelectableDeck.AllDecks, feedbackMode = BrowserSearchFeedbackMode.SEARCH_RESULTS)
                    }
                }
            }
        }
        if (isHostedInBottomNavigation()) {
            activityViewModel.flowOfNoteEditorCommand.filterIsInstance<NoteEditorCommand.LaunchActivity>().collectInLifecycle {
                navigate(it.destination)
            }
        }
        searchViewModel.submittedSearchFlow.filterNotNull().collectInLifecycle { value ->
            if (activity?.isFinishing != true) {
                activityViewModel.launchSearchForCards(value, forceRefresh = false, feedbackMode = BrowserSearchFeedbackMode.SEARCH_RESULTS)
            }
        }
        searchViewModel.userMessageFlow.filterNotNull().collectInLifecycle { message ->
            val label =
                when (message) {
                    UserMessage.SEARCH_SAVED -> CommonString.card_browser_list_my_searches_successful_save
                    UserMessage.SAVED_SEARCH_DUPLICATE_ADDED -> CommonString.card_browser_list_my_searches_new_search_error_dup
                    UserMessage.SAVED_SEARCH_DELETED -> CommonString.card_browser_list_my_searches_successful_deleted
                    UserMessage.SAVED_SEARCH_NAME_DOES_NOT_EXIST -> CommonString.something_wrong
                }
            showSnackbar(label, Snackbar.LENGTH_SHORT)
        }
        activityViewModel.flowOfChangeNoteType.collectInLifecycle { result ->
            when (result) {
                ChangeNoteTypeResponse.NoSelection -> Timber.w("change note type: no selection")
                ChangeNoteTypeResponse.MixedSelection -> showSnackbar(CommonString.different_note_types_selected)
                is ChangeNoteTypeResponse.ChangeNoteType -> {
                    val noteType = withCol { getNote(result.noteIds.first()).noteTypeId }
                    startActivity(ChangeNoteTypePage.getIntent(requireContext(), result.noteIds, noteType))
                }
            }
        }
        activityViewModel.flowOfSaveSearchNamePrompt.collectInLifecycle { query ->
            SaveBrowserSearchDialogFragment.newInstance(query).show(childFragmentManager, null)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            closeSearch()
        }
        browserChromeChanged(!hidden && activityViewModel.isInMultiSelectMode)
    }

    internal fun closeSearch() = searchViewModel.closeBrowserSearch(activityViewModel.searchRequestFlow.value)

    override fun onResume() {
        super.onResume()
        invalidateFlagNames()
    }

    private fun setupFragmentResultListeners() {
        ankiActivity.setFragmentResultListener(REQUEST_REPOSITION_NEW_CARDS) { _, bundle ->
            repositionCardsNoValidation(
                position = bundle.getInt(RepositionCardFragment.RESULT_POSITION),
                step = bundle.getInt(RepositionCardFragment.RESULT_STEP),
                shuffle = bundle.getBoolean(RepositionCardFragment.RESULT_RANDOM),
                shift = bundle.getBoolean(RepositionCardFragment.RESULT_SHIFT),
            )
        }
    }

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        // TODO: dismiss undoSnackbar if it would undo a new action
        if (handler === this || handler === activityViewModel) {
            return
        }

        if (changes.browserSidebar ||
            changes.browserTable ||
            changes.noteText ||
            changes.card
        ) {
            if (isHostedInBottomNavigation()) activityViewModel.launchSearchForCards()
        }
    }

    fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        // This method is called even when the user is typing in the search text field.
        // So we must ensure that all shortcuts uses a modifier.
        // A shortcut without modifier would be triggered while the user types, which is not what we want.
        when (keyCode) {
            KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_DEL -> {
                if (searchViewModel.isScreenOpenFlow.value) {
                    Timber.i("Delete pressed - Search active, deleting character")
                    // the search box is available and could potentially receive input so handle the
                    // DEL as a simple text deletion and not as a keyboard shortcut
                    return false
                }
                Timber.i("Delete pressed - Delete Selected Note")
                deleteSelectedNotes()
                return true
            }
            KeyEvent.KEYCODE_A -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+A - Show edit tags dialog")
                    showEditTagsDialog()
                    return true
                } else if (event.isCtrlPressed) {
                    Timber.i("Ctrl+A - Select All")
                    activityViewModel.selectAll()
                    return true
                }
            }
            KeyEvent.KEYCODE_F -> {
                if (event.isCtrlPressed && event.isAltPressed) {
                    showFindAndReplaceDialog()
                    return true
                } else if (event.isCtrlPressed) {
                    openSearch()
                    return true
                }
            }
            KeyEvent.KEYCODE_E -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+E: Export selected cards")
                    exportSelected()
                    return true
                } else if (event.isCtrlPressed) {
                    Timber.i("Ctrl+E: Add Note")
                    addNote()
                    return true
                } else if (!event.isCtrlPressed) {
                    if (!searchViewModel.isScreenOpenFlow.value) {
                        // search box is not available so treat the event as a shortcut
                        // Disable 'E' edit shortcut in split mode as the integrated NoteEditor
                        // is already available in the split view, making the shortcut redundant
                        if (activityViewModel.isFragmented) {
                            Timber.i("E: Ignored in split mode")
                            return true
                        }
                        Timber.i("E: Edit note")
                        openNoteEditorForCurrentlySelectedRow()
                        return true
                    }
                    Timber.i("E: Character added")
                    // search box might be available and receiving input so treat this as usual text
                    return false
                }
            }
            KeyEvent.KEYCODE_D -> {
                if (event.isCtrlPressed) {
                    Timber.i("Ctrl+D: Change Deck")
                    showChangeDeckDialog()
                    return true
                }
            }
            KeyEvent.KEYCODE_K -> {
                if (event.isCtrlPressed) {
                    Timber.i("Ctrl+K: Toggle Mark")
                    toggleMark()
                    return true
                }
            }
            KeyEvent.KEYCODE_R -> {
                if (event.isCtrlPressed && event.isAltPressed) {
                    Timber.i("Ctrl+Alt+R - Reschedule")
                    rescheduleSelectedCards()
                    return true
                }
            }
            KeyEvent.KEYCODE_N -> {
                if (event.isCtrlPressed && event.isAltPressed) {
                    Timber.i("Ctrl+Alt+N: Reset card progress")
                    onResetProgress()
                    return true
                }
            }
            KeyEvent.KEYCODE_T -> {
                if (event.isCtrlPressed && event.isAltPressed) {
                    val mode = if (activityViewModel.cardsOrNotes == CARDS) NOTES else CARDS
                    activityViewModel.setCardsOrNotes(mode)
                    return true
                } else if (event.isCtrlPressed) {
                    sheetRequests.tryEmit(BrowserSheet.TAGS)
                    return true
                }
            }
            KeyEvent.KEYCODE_S -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+S: Reposition selected cards")
                    repositionSelectedCards()
                    return true
                } else if (event.isCtrlPressed && event.isAltPressed) {
                    Timber.i("Ctrl+Alt+S: Show saved searches")
                    showSavedSearches()
                    return true
                } else if (event.isCtrlPressed) {
                    saveSubmittedSearch()
                    return true
                } else if (event.isAltPressed) {
                    Timber.i("Alt+S: Show suspended cards")
                    activityViewModel.searchForSuspendedCards()
                    return true
                }
            }
            KeyEvent.KEYCODE_J -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+J: Toggle bury cards")
                    toggleBury()
                    return true
                } else if (event.isCtrlPressed) {
                    Timber.i("Ctrl+J: Toggle suspended cards")
                    toggleSuspendCards()
                    return true
                }
            }
            KeyEvent.KEYCODE_O -> {
                if (event.isCtrlPressed) {
                    sheetRequests.tryEmit(BrowserSheet.SORT)
                    return true
                }
            }
            KeyEvent.KEYCODE_P -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+P - Preview")
                    onPreview()
                    return true
                }
            }
            KeyEvent.KEYCODE_M -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+M: Change Note Type")
                    activityViewModel.requestChangeNoteType()
                    return true
                } else if (event.isCtrlPressed) {
                    Timber.i("Ctrl+M: Search marked notes")
                    activityViewModel.searchForMarkedNotes()
                    return true
                }
            }
            KeyEvent.KEYCODE_G -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+G - Grade Now")
                    openGradeNow()
                    return true
                }
            }
            KeyEvent.KEYCODE_I -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    Timber.i("Ctrl+Shift+I: Card info")
                    displayCardInfo()
                    return true
                }
            }
            KeyEvent.KEYCODE_Z -> {
                if (event.isCtrlPressed) {
                    Timber.i("Ctrl+Z: Undo")
                    onUndo()
                    return true
                }
            }
            in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_7 -> {
                if (event.isCtrlPressed) {
                    Timber.i("Update flag")
                    updateFlag(keyCode)
                    return true
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                Timber.i("ESC: Select none")
                activityViewModel.selectNone()
                return true
            }
        }
        return false
    }

    fun showChangeDeckDialog() {
        if (!activityViewModel.hasSelectedAnyRows()) {
            Timber.i("Not showing Change Deck - No Cards")
            return
        }
        startDeckSelection(
            title = getString(CommonString.move_all_to_deck),
            allowAll = false,
            allowFiltered = false,
            requestKey = REQUEST_DECK_SELECTION_CHANGE_DECK,
        )
    }

    @VisibleForTesting
    fun onUndo() =
        launchCatchingTask {
            undoAndShowSnackbar()
        }

    fun displayCardInfo() =
        launchCatchingTask {
            activityViewModel.queryCardInfoDestination()?.let { destination ->
                navigate(destination)
            }
        }

    /**
     * @see CardBrowserViewModel.openNoteEditorForCurrentlySelectedRow
     */
    fun openNoteEditorForCurrentlySelectedRow() {
        if (!activityViewModel.openNoteEditorForCurrentlySelectedRow()) {
            showSnackbar(CommonString.no_note_to_edit)
        }
    }

    fun openGradeNow() =
        launchCatchingTask {
            val cardIds = activityViewModel.queryAllSelectedCardIds()
            GradeNowDialog.showDialog(requireAnkiActivity(), cardIds)
        }

    fun showSavedSearches() =
        launchCatchingTask {
            SavedBrowserSearchesDialogFragment
                .newInstance(activityViewModel.savedSearches())
                .show(childFragmentManager, SavedBrowserSearchesDialogFragment.TAG)
        }

    /** All the notes of the selected cards will be marked
     * If one or more card is unmarked, all will be marked,
     * otherwise, they will be unmarked  */
    @NeedsTest("Test that the mark get toggled as expected for a list of selected cards")
    @VisibleForTesting
    fun toggleMark() =
        launchCatchingTask {
            activityViewModel.toggleMark()
        }

    fun toggleSuspendCards() =
        launchCatchingTask {
            prepareForUndoableOperation()
            activityViewModel.toggleSuspendCards().join()
        }

    /** @see CardBrowserViewModel.toggleBury */
    fun toggleBury() =
        launchCatchingTask {
            val result = activityViewModel.toggleBury() ?: return@launchCatchingTask
            // show a snackbar as there's currently no colored background for buried cards
            val message =
                when (result.wasBuried) {
                    true -> TR.studyingCardsBuried(result.count)
                    false -> resources.getQuantityString(CommonPlurals.unbury_cards_feedback, result.count, result.count)
                }
            showUndoSnackbar(message)
        }

    fun onPreview() {
        launchCatchingTask {
            val intent = activityViewModel.queryPreviewIntentData().toIntent(requireContext())
            startActivity(intent)
        }
    }

    fun rescheduleSelectedCards() {
        if (!activityViewModel.hasSelectedAnyRows()) {
            Timber.i("Attempted reschedule - no cards selected")
            return
        }

        launchCatchingTask {
            val allCardIds = activityViewModel.queryAllSelectedCardIds()
            Timber.i(
                "Reschedule: mode=%s, selected rows=%d, cards=%d",
                activityViewModel.cardsOrNotes,
                activityViewModel.selectedRows.size,
                allCardIds.size,
            )
            SetDueDateDialog.show(requireActivity(), allCardIds)
        }
    }

    /** @see repositionCardsNoValidation */
    fun repositionSelectedCards(): Boolean {
        Timber.i("CardBrowser:: Reposition button pressed")
        launchCatchingTask {
            when (val repositionCardsResult = activityViewModel.prepareToRepositionCards()) {
                is NoRepositionableCardsError -> {
                    // No selected cards can be repositioned
                    showDialogFragment(
                        SimpleMessageDialog.newInstance(
                            title = getString(CommonString.vague_error),
                            message = getString(CommonString.reposition_card_not_new_error),
                            reload = false,
                        ),
                    )
                    return@launchCatchingTask
                }
                is RepositionData -> {
                    val top = repositionCardsResult.queueTop
                    val bottom = repositionCardsResult.queueBottom
                    if (top == null || bottom == null) {
                        showSnackbar(CommonString.something_wrong)
                        return@launchCatchingTask
                    }
                    val repositionDialog =
                        RepositionCardFragment.newInstance(
                            queueTop = top,
                            queueBottom = bottom,
                            random = repositionCardsResult.random,
                            shift = repositionCardsResult.shift,
                        )
                    showDialogFragment(repositionDialog)
                }
            }
        }
        return true
    }

    fun deleteSelectedNotes() =
        launchCatchingTask {
            activityViewModel.deleteSelectedNotes().ifNotZero { noteCount ->
                val deletedMessage = resources.getQuantityString(CommonPlurals.card_browser_cards_deleted, noteCount, noteCount)
                showUndoSnackbar(deletedMessage)
            }
        }

    fun onResetProgress() {
        launchCatchingTask {
            val allCardIds = activityViewModel.queryAllSelectedCardIds()
            Timber.i(
                "Reset Progress: mode=%s, selected rows=%d, cards=%d",
                activityViewModel.cardsOrNotes,
                activityViewModel.selectedRows.size,
                allCardIds.size,
            )
        }
        showDialogFragment(ForgetCardsDialog())
    }

    fun exportSelected() {
        val (type, selectedIds) = activityViewModel.querySelectionExportData() ?: return
        ExportDialogFragment
            .newInstance(
                IdsFile.getDirectory(),
                type,
                selectedIds,
            ).show(parentFragmentManager, "exportDialog")
    }

    fun showOptionsDialog() {
        val dialog =
            BrowserOptionsDialog.newInstance(
                activityViewModel.cardsOrNotes,
                activityViewModel.isTruncated,
                useBrowserControls = true,
            )
        dialog.show(parentFragmentManager, "browserOptionsDialog")
    }

    fun showFilteredDeckScreen() {
        // an invalid query is the user's input, so show the error without a crash report
        launchCatchingTask(skipCrashReport = { it is InvalidSearchException }) {
            withProgress {
                // the selected deck sits outside the search box, so build the same search the
                // browser uses, which combines the query and the filters via the backend
                val searchRequest = activityViewModel.searchRequestFlow.value
                val search = withCol { searchRequest.toSearchString().getOrThrow() }
                val intent = FilteredDeckOptionsFragment.getIntent(requireContext(), search = search.value)
                startActivity(intent)
            }
        }
    }

    private fun updateFlag(keyCode: Int) {
        val flag =
            when (keyCode) {
                KeyEvent.KEYCODE_1 -> Flag.RED
                KeyEvent.KEYCODE_2 -> Flag.ORANGE
                KeyEvent.KEYCODE_3 -> Flag.GREEN
                KeyEvent.KEYCODE_4 -> Flag.BLUE
                KeyEvent.KEYCODE_5 -> Flag.PINK
                KeyEvent.KEYCODE_6 -> Flag.TURQUOISE
                KeyEvent.KEYCODE_7 -> Flag.PURPLE
                else -> return
            }
        updateFlagForSelectedRows(flag)
    }

    fun updateFlagForSelectedRows(flag: Flag) =
        launchCatchingTask {
            prepareForUndoableOperation()
            activityViewModel.updateSelectedCardsFlag(flag)
        }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun filterByTag(vararg tags: String) {
        launchCatchingTask {
            val request = activityViewModel.searchRequestFlow.value.copyFilters { it.copy(tags = tags.toList()) }
            activityViewModel.launchSearchForCards(request, forceRefresh = false, feedbackMode = BrowserSearchFeedbackMode.SEARCH_RESULTS)
        }
    }

    fun showEditTagsDialog() {
        if (!activityViewModel.hasSelectedAnyRows()) {
            Timber.d("showEditTagsDialog: called with empty selection")
        }
        lifecycleScope.launch {
            val noteIds = activityViewModel.queryAllSelectedNoteIds()
            tagsDialogFactory.show(requireActivity(), noteIds = noteIds)
        }
    }

    override fun onSelectedTags(
        selectedTags: List<String>,
        indeterminateTags: List<String>,
        stateFilter: CardStateFilter,
    ) {
        launchCatchingTask {
            activityViewModel.editSelectedCardsTags(selectedTags, indeterminateTags)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showFindAndReplaceDialog() {
        lifecycleScope.launch {
            val noteIds = activityViewModel.loadFindAndReplaceNoteIds()
            val fragment = FindAndReplaceDialogFragment.newInstance(requireContext(), noteIds)
            fragment.show(parentFragmentManager, FindAndReplaceDialogFragment.TAG)
        }
    }

    /**
     * Change Deck
     * @param did Id of the deck
     */
    @VisibleForTesting
    internal fun moveSelectedCardsToDeck(did: DeckId): Job =
        launchCatchingTask {
            val changed = activityViewModel.moveSelectedCardsToDeck(did).await()
            showUndoSnackbar(TR.browsingCardsUpdated(changed.count))
        }

    @VisibleForTesting
    internal fun repositionCardsNoValidation(
        position: Int,
        step: Int,
        shuffle: Boolean,
        shift: Boolean,
    ) = launchCatchingTask {
        val count =
            activityViewModel.repositionSelectedRows(
                position = position,
                step = step,
                shuffle = shuffle,
                shift = shift,
            )
        showSnackbar(
            TR.browsingChangedNewPosition(count),
            Snackbar.LENGTH_SHORT,
        )
    }

    private fun showUndoSnackbar(message: CharSequence) {
        showSnackbar(message) {
            setAction(CommonString.undo) { launchCatchingTask { undoAndShowSnackbar() } }
            undoSnackbar = this
        }
    }

    @VisibleForTesting
    val addNoteDestination: NoteEditorDestination
        get() =
            NoteEditorDestination.AddNoteFromCardBrowser(
                searchTerms = activityViewModel.searchTerms,
                deckId = activityViewModel.lastDeckId,
            )

    internal fun addNote() {
        onAddNoteActivityResult.navigate(addNoteDestination)
    }

    fun prepareForUndoableOperation() {
        // dismiss undo-snackbar if shown to avoid race condition
        // (when another operation will be performed on the model, it will undo the latest operation)
        val snackbar = undoSnackbar ?: return
        if (snackbar.isShown) {
            snackbar.dismiss()
        }
    }

    val shortcuts get() =
        ShortcutGroup(
            listOf(
                shortcut("Ctrl+Shift+A", CommonString.menu_edit_tags),
                shortcut("Ctrl+A", CommonString.card_browser_select_all),
                shortcut("Ctrl+Shift+E", Translations::exportingExport),
                shortcut("Ctrl+E", CommonString.menu_add_note),
                shortcut("E", CommonString.cardeditor_title_edit_card),
                shortcut("Ctrl+D") { sentenceCase.changeDeck },
                shortcut("Ctrl+K") { sentenceCase.toggleMark },
                shortcut("Ctrl+Alt+R", Translations::browsingReschedule),
                shortcut("DEL", CommonString.delete_card_title),
                shortcut("Ctrl+Alt+N", CommonString.reset_card_dialog_title),
                shortcut("Ctrl+Alt+T") { sentenceCase.toggleCardsNotes },
                shortcut("Ctrl+T", CommonString.card_browser_search_by_tag),
                shortcut("Ctrl+Shift+S", Translations::actionsReposition),
                shortcut("Ctrl+Alt+S", CommonString.card_browser_list_my_searches),
                shortcut("Ctrl+S", CommonString.card_browser_list_my_searches_save),
                shortcut("Alt+S", CommonString.card_browser_show_suspended),
                shortcut("Ctrl+Shift+G") { sentenceCase.gradeNow },
                shortcut("Ctrl+Shift+J") { sentenceCase.toggleBury },
                shortcut("Ctrl+J") { sentenceCase.toggleSuspend },
                shortcut("Ctrl+Shift+I") { sentenceCase.cardInfo },
                shortcut("Ctrl+O", CommonString.show_order_dialog),
                shortcut("Ctrl+Shift+P", CommonString.card_editor_preview_card),
                shortcut("Ctrl+M", CommonString.card_browser_show_marked),
                shortcut("Esc", CommonString.card_browser_select_none),
                shortcut("Ctrl+1", CommonString.gesture_flag_red),
                shortcut("Ctrl+2", CommonString.gesture_flag_orange),
                shortcut("Ctrl+3", CommonString.gesture_flag_green),
                shortcut("Ctrl+4", CommonString.gesture_flag_blue),
                shortcut("Ctrl+5", CommonString.gesture_flag_pink),
                shortcut("Ctrl+6", CommonString.gesture_flag_turquoise),
                shortcut("Ctrl+7", CommonString.gesture_flag_purple),
            ),
            CommonString.card_browser_context_menu,
        )

    companion object {
        private const val FLAG_RENAME_TAG = "browserFlagRename"

        /**
         * Request key that identifies requests to select a deck for the browser note change decks
         * dialog.
         */
        const val REQUEST_DECK_SELECTION_CHANGE_DECK = "request_deck_selection_change_deck"
    }
}

class PreviewerDestination(
    val currentIndex: Int,
    val idsFile: IdsFile,
)

@CheckResult
fun PreviewerDestination.toIntent(context: Context) = PreviewerFragment.getIntent(context, idsFile, currentIndex)

/**
 * Updates the content of the [SearchView] to a provided fragment, retaining state.
 *
 * This method searches through child fragments by [tag]. [createFragment] is called if the fragment
 * is not found.
 */
suspend fun SearchRequest.toUserSpannable(context: Context): Spannable {
    val text = withCol { this@toUserSpannable.toSearchString() }.getOrDefault(SearchString.EMPTY)
    return this.toUserSpannable(context, text)
}

@CheckResult
fun SearchRequest.toUserSpannable(
    context: Context,
    searchString: SearchString,
): Spannable = buildUserSpannable(context, this.query, searchString)

@CheckResult
fun SearchHistory.SearchHistoryEntry.toUserSpannable(
    context: Context,
    searchString: SearchString,
) = buildUserSpannable(context, this.query, searchString)

fun buildUserSpannable(
    context: Context,
    query: String,
    searchString: SearchString,
): SpannableString {
    val spannable = SpannableString(searchString.value)

    // mute the additional filters, as they're handled by chips
    if (searchString.value.startsWith(query)) {
        spannable.setSpan(
            ForegroundColorSpan(getColorFromAttr(context, CommonR.attr.foundationOnBackgroundMuted)),
            query.length,
            searchString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    return spannable
}

@get:StringRes
private val CardBrowserProgress.messageRes: Int
    get() =
        when (this) {
            CardBrowserProgress.DELETING_NOTES -> CommonString.deleting_selected_notes
        }
