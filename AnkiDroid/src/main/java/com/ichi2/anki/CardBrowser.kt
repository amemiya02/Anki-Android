// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import anki.collection.OpChanges
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.browser.BrowserRowCollection
import com.ichi2.anki.browser.BrowserSearchFeedbackMode
import com.ichi2.anki.browser.CardBrowserFragment
import com.ichi2.anki.browser.CardBrowserLaunchOptions
import com.ichi2.anki.browser.CardBrowserViewModel
import com.ichi2.anki.browser.CardBrowserViewModel.ChangeMultiSelectMode.SingleSelectCause
import com.ichi2.anki.browser.CardBrowserViewModel.NoteEditorCommand
import com.ichi2.anki.browser.CardBrowserViewModel.SearchState
import com.ichi2.anki.browser.CardOrNoteId
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.browser.SaveSearchResult
import com.ichi2.anki.browser.SharedPreferencesLastDeckIdRepository
import com.ichi2.anki.browser.registerFindReplaceHandler
import com.ichi2.anki.browser.search.SearchString
import com.ichi2.anki.browser.search.findCards
import com.ichi2.anki.browser.search.findNotes
import com.ichi2.anki.browser.toCardBrowserLaunchOptions
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.annotation.KotlinCleanup
import com.ichi2.anki.common.utils.ext.getLongExtra
import com.ichi2.anki.common.utils.ext.onPermanentDismissal
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.SavedBrowserSearchesDialogFragment
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.registerSaveSearchHandler
import com.ichi2.anki.dialogs.registerSavedSearchActionHandler
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.SortOrder
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.model.CardsOrNotes.CARDS
import com.ichi2.anki.model.CardsOrNotes.NOTES
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.scheduling.registerOnForgetHandler
import com.ichi2.anki.settings.uiConfigPrefs
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.ui.ResizablePaneLayout
import com.ichi2.anki.ui.ResizablePaneManager
import com.ichi2.anki.ui.ResizingDivider
import com.ichi2.anki.utils.ext.addPrepareMenuProvider
import com.ichi2.anki.utils.ext.collectInLifecycle
import com.ichi2.anki.utils.ext.onAllFragmentsLoaded
import com.ichi2.themes.Themes
import timber.log.Timber

@Suppress("LeakingThis")
// The class is only 'open' due to testing
@KotlinCleanup("scan through this class and add attributes - in process")
open class CardBrowser :
    NavigationDrawerActivity(),
    ChangeManager.Subscriber,
    MenuHost {
    fun onDeckSelected(deck: SelectableDeck?) {
        deck?.let { deck -> viewModel.setSelectedDeck(deck) }
    }

    override var fragmented: Boolean
        get() = viewModel.isFragmented
        set(_) {
            throw UnsupportedOperationException()
        }

    lateinit var viewModel: CardBrowserViewModel

    private var binding: StandaloneBrowserLayout? = null

    lateinit var cardBrowserFragment: CardBrowserFragment

    init {
        ChangeManager.subscribe(this)
    }

    /**
     * Updates the browser's ui after a user search was saved based on the result of the operation.
     * @param saveSearchResult the result of the save search operation
     * @see [CardBrowser.registerSaveSearchHandler]
     */
    fun updateAfterUserSearchIsSaved(saveSearchResult: SaveSearchResult) {
        when (saveSearchResult) {
            SaveSearchResult.ALREADY_EXISTS ->
                showSnackbar(
                    CommonString.card_browser_list_my_searches_new_search_error_dup,
                    Snackbar.LENGTH_SHORT,
                )
            SaveSearchResult.SUCCESS -> {
                showSnackbar(
                    CommonString.card_browser_list_my_searches_successful_save,
                    Snackbar.LENGTH_SHORT,
                )
            }
        }
    }

    private val multiSelectOnBackPressedCallback =
        object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                Timber.i("back pressed - exiting multiselect")
                viewModel.endMultiSelectMode(SingleSelectCause.NavigateBack)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // the status bar sits over the app bar, which is the page colour
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme },
        )
        val contentBinding = StandaloneBrowserLayout(this).also { binding = it }
        if (!ensureStorageIsReady()) {
            return
        }

        // Set a simple RESULT_OK. The Reviewer or calling activity will now
        // rely on ChangeManager/OpChanges to detect if a refresh is needed.
        setResult(RESULT_OK)

        val launchOptions = intent?.toCardBrowserLaunchOptions() // must be called after super.onCreate()

        setDrawerContent(contentBinding.root)
        initNavigationDrawer(findViewById(android.R.id.content))
        applyToolbarInsets()

        /**
         * Check if noteEditorFrame is not null and if its visibility is set to VISIBLE.
         * If both conditions are true, assign true to the variable [fragmented], otherwise assign false.
         * [fragmented] will be true if the view size is large otherwise false
         */
        // TODO: Consider refactoring by storing noteEditorFrame and similar views in a sealed class (e.g., FragmentAccessor).
        val fragmented = binding?.noteEditorFrame?.visibility == View.VISIBLE
        Timber.i("Using split Browser: %b", fragmented)

        if (fragmented) {
            ResizablePaneManager(
                parentLayout = requireNotNull(binding?.cardBrowserXlView) { "cardBrowserXlView" },
                divider = requireNotNull(binding?.cardBrowserResizingDivider) { "cardBrowserResizingDivider" },
                startPane = requireNotNull(binding?.cardBrowserFrame) { "cardBrowserFrame" },
                endPane = requireNotNull(binding?.noteEditorFrame) { "noteEditorFrame" },
                sharedPrefs = uiConfigPrefs(),
                startPaneWeightKey = PREF_CARD_BROWSER_PANE_WEIGHT,
                endPaneWeightKey = PREF_NOTE_EDITOR_PANE_WEIGHT,
            )
        } else {
            binding?.noteEditorFrame?.isVisible = false
            binding?.cardBrowserResizingDivider?.isVisible = false
        }

        // must be called once we have an accessible collection
        viewModel = createViewModel(launchOptions, fragmented)
        onPermanentDismissal { viewModel.deleteSavedSelectionFile() }

        cardBrowserFragment = supportFragmentManager.findFragmentById(R.id.card_browser_frame) as? CardBrowserFragment
            ?: CardBrowserFragment().also { fragment ->
                supportFragmentManager.commit {
                    replace(R.id.card_browser_frame, fragment)
                }
            }

        startLoadingCollection()

        setupFlows()
        registerOnForgetHandler { viewModel.queryAllSelectedCardIds() }
        registerSaveSearchHandler()
        registerDeckSelectedHandler(action = ::onDeckSelected)
        registerFindReplaceHandler { result ->
            launchCatchingTask {
                val count = viewModel.findAndReplace(result).await()
                showSnackbar(TR.browsingNotesUpdated(count))
            }
        }
        registerSavedSearchActionHandler { type, searchName ->
            if (searchName == null) return@registerSavedSearchActionHandler
            when (type) {
                SavedBrowserSearchesDialogFragment.TYPE_SEARCH_SELECTED -> {
                    Timber.d("Selecting saved search selection named: %s", searchName)
                    launchCatchingTask {
                        val search = viewModel.savedSearches().find { it.name == searchName } ?: return@launchCatchingTask
                        Timber.d("OnSelection using search terms: %s", search.query)
                        cardBrowserFragment.searchViewModel.submitBrowserSearch(search.query)
                    }
                }
                SavedBrowserSearchesDialogFragment.TYPE_SEARCH_REMOVED -> {
                    Timber.d("Removing saved search named: %s", searchName)
                    launchCatchingTask {
                        viewModel.removeSavedSearch(searchName)
                    }
                }
                else -> error("Unexpected saved search action: $type")
            }
        }
        setupMenuProvider()
    }

    fun setupMenuProvider() {
        // the drawerToggle has priority over other menu items
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater,
                ) {
                    if (!viewModel.isInMultiSelectMode) {
                        restoreDrawerIcon()
                    } else {
                        showBackIcon()
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem) = drawerToggle.onOptionsItemSelected(menuItem)
            },
        )

        // Update the menu for a fragmented state
        // Added last so all changes take priority
        onAllFragmentsLoaded {
            addPrepareMenuProvider { menu ->
                if (!fragmented) return@addPrepareMenuProvider

                /** Return the menu item with a particular identifier or `null` */
                operator fun Menu.get(id: Int): MenuItem? = this.findItem(id)

                // NoteEditorFragment: Remove save/preview note options if there are no notes
                if (viewModel.rowCount == 0) {
                    menu[R.id.action_save]?.isVisible = false
                    menu[R.id.action_preview]?.isVisible = false
                }

                menu[R.id.action_edit_note]?.isVisible = false
            }
        }
    }

    override fun setupBackPressedCallbacks() {
        onBackPressedDispatcher.addCallback(this, multiSelectOnBackPressedCallback)
        super.setupBackPressedCallbacks()
    }

    override fun fitsSystemWindows(): Boolean = false

    private fun applyToolbarInsets() {
        val container = findViewById<View>(R.id.toolbar_container) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            // systemBars (not just statusBars) so a landscape 3-button navigation bar,
            // which is a side inset, is also cleared
            val bars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
                )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            insets
        }
    }

    private fun showSaveChangesDialog(destination: NoteEditorDestination) {
        DiscardChangesDialog.showDialog(
            context = this,
            positiveButtonText = this.getString(CommonString.save),
            negativeButtonText = this.getString(CommonString.discard),
            // The neutral button allows the user to back out of the action,
            // e.g., if they accidentally triggered a navigation or card selection.
            neutralButtonText = this.getString(CommonString.dialog_cancel),
            message = this.getString(CommonString.save_changes_message),
            positiveMethod = {
                launchCatchingTask {
                    fragment?.saveNote()
                    loadNoteEditorFragment(destination)
                }
            },
            negativeMethod = {
                loadNoteEditorFragment(destination)
            },
            neutralMethod = {},
        )
    }

    private fun loadNoteEditorFragment(destination: NoteEditorDestination) {
        supportFragmentManager.commit {
            replace(R.id.note_editor_frame, NoteEditorFragment.newInstance(destination))
        }
        // Invalidate options menu so that note editor menu will show
        invalidateOptionsMenu()
    }

    /**
     * Retrieves the `NoteEditor` fragment if it is present in the fragment container
     */
    val fragment: NoteEditorFragment?
        get() = supportFragmentManager.findFragmentById(R.id.note_editor_frame) as? NoteEditorFragment

    private fun setupFlows() {
        viewModel.flowOfMultiSelectModeChanged.collectInLifecycle { mode ->
            multiSelectOnBackPressedCallback.isEnabled = mode.resultedInMultiSelect
            invalidateOptionsMenu()
        }

        fun onNoteEditorCommand(command: NoteEditorCommand) {
            launchCatchingTask {
                when (command) {
                    is NoteEditorCommand.LoadInPane -> {
                        binding?.noteEditorFrame?.isVisible = true
                        val editor = fragment
                        when {
                            // Issue 19737: the pane already displays this selection. Refresh it in
                            // place: recreating the fragment is expensive enough to ANR when
                            // searches complete in quick succession. If there are unsaved
                            // changes, keep them: this is a redundant refresh, not a navigation
                            editor != null && editor.isEditingSameCards(command.destination) ->
                                if (!editor.hasUnsavedChanges()) {
                                    editor.reloadNoteFromCollection()
                                }
                            editor?.hasUnsavedChanges() == true -> showSaveChangesDialog(command.destination)
                            else -> loadNoteEditorFragment(command.destination)
                        }
                    }
                    is NoteEditorCommand.LaunchActivity -> {
                        navigate(command.destination)
                    }
                    NoteEditorCommand.HidePane -> {
                        binding?.noteEditorFrame?.isVisible = false
                        invalidateOptionsMenu()
                    }
                }
            }
        }

        viewModel.flowOfNoteEditorCommand.collectInLifecycle(::onNoteEditorCommand)
    }

    // Finish initializing the activity after the collection has been correctly loaded
    override fun onCollectionLoaded(col: Collection) {
        super.onCollectionLoaded(col)
        Timber.d("onCollectionLoaded()")
        registerReceiver()

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean =
        (::cardBrowserFragment.isInitialized && cardBrowserFragment.onKeyUp(keyCode, event)) ||
            super.onKeyUp(keyCode, event)

    override fun onPause() {
        super.onPause()
        // If the user entered something into the search, but didn't press "search", clear this.
        // It's confusing if the bar is shown with a query that does not relate to the data on the screen
        viewModel.removeUnsubmittedInput()
    }

    override fun onResume() {
        super.onResume()
        selectNavigationItem(R.id.nav_browser)
        viewModel.refreshColumnsFromPrefs()
    }

    override fun onNavigationPressed() {
        if (viewModel.isInMultiSelectMode) {
            viewModel.endMultiSelectMode(SingleSelectCause.NavigateBack)
        } else {
            super.onNavigationPressed()
        }
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModel.onReinit()
    }

    private fun refreshBrowserUI(feedbackMode: BrowserSearchFeedbackMode) {
        hideProgressBar()
        // reload whole view
        viewModel.launchSearchForCards(feedbackMode = feedbackMode)
        viewModel.endMultiSelectMode(SingleSelectCause.Other)
        invalidateOptionsMenu() // maybe the availability of undo changed
    }

    /**
     * Implementation of `by viewModels()` for use in [onCreate]
     *
     * @see showedActivityFailedScreen - we may not have appContext and therefore can't
     * create the ViewModel
     *
     * @param fragmented True if `noteEditorFrame` is non-null (x-large displays)
     */
    private fun createViewModel(
        launchOptions: CardBrowserLaunchOptions?,
        fragmented: Boolean,
    ) = ViewModelProvider(
        viewModelStore,
        CardBrowserViewModel.factory(
            lastDeckIdRepository = AnkiDroidApp.instance.sharedPrefsLastDeckIdRepository,
            idsFileDirectory = IdsFile.getDirectory(),
            options = launchOptions,
            isFragmented = fragmented,
            initialDeckId = intent?.getLongExtra(CardBrowserViewModel.EXTRA_DECK_ID),
        ),
        defaultViewModelCreationExtras,
    )[CardBrowserViewModel::class.java]

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        if (handler === this || handler === viewModel) {
            return
        }

        if (changes.browserSidebar ||
            changes.browserTable ||
            changes.noteText ||
            changes.card
        ) {
            // We refresh the Browser's own UI
            val feedbackMode =
                if (handler is NoteEditorFragment) BrowserSearchFeedbackMode.COUNT_ONLY else BrowserSearchFeedbackMode.NONE
            refreshBrowserUI(feedbackMode)
        }
    }

    override val shortcuts
        get() = cardBrowserFragment.shortcuts

    companion object {
        // Keys for saving pane weights in SharedPreferences
        private const val PREF_CARD_BROWSER_PANE_WEIGHT = "cardBrowserPaneWeight"
        private const val PREF_NOTE_EDITOR_PANE_WEIGHT = "cardBrowserNoteEditorPaneWeight"

        // Values related to persistent state data
        fun clearLastDeckId() = SharedPreferencesLastDeckIdRepository.clearLastDeckId()
    }
}

private class StandaloneBrowserLayout(
    context: Context,
) {
    val root = CoordinatorLayout(context).apply { id = R.id.root_layout }
    val cardBrowserFrame = FragmentContainerView(context).apply { id = R.id.card_browser_frame }
    val noteEditorFrame: FragmentContainerView?
    val cardBrowserXlView: ResizablePaneLayout?
    val cardBrowserResizingDivider: ResizingDivider?

    init {
        if (context.resources.getBoolean(R.bool.bottom_navigation_available)) {
            root.addView(cardBrowserFrame, MATCH_PARENT, MATCH_PARENT)
            noteEditorFrame = null
            cardBrowserXlView = null
            cardBrowserResizingDivider = null
        } else {
            val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            column.addView(createAppToolbar(context, R.style.ActionBarStyle_Browser), MATCH_PARENT, WRAP_CONTENT)
            val panes =
                ResizablePaneLayout(context).apply {
                    id = R.id.card_browser_xl_view
                    orientation = LinearLayout.HORIZONTAL
                }
            val divider = ResizingDivider(context).apply { id = R.id.card_browser_resizing_divider }
            val editor = FragmentContainerView(context).apply { id = R.id.note_editor_frame }
            panes.addView(cardBrowserFrame, LinearLayout.LayoutParams(0, MATCH_PARENT, 3f))
            panes.addView(divider, context.layoutDp(8), MATCH_PARENT)
            panes.addView(editor, LinearLayout.LayoutParams(0, MATCH_PARENT, 2f))
            column.addView(panes, MATCH_PARENT, MATCH_PARENT)
            root.addView(column, MATCH_PARENT, MATCH_PARENT)
            noteEditorFrame = editor
            cardBrowserXlView = panes
            cardBrowserResizingDivider = divider
        }
    }
}

suspend fun searchForRows(
    search: SearchString,
    order: SortOrder,
    cardsOrNotes: CardsOrNotes,
): BrowserRowCollection =
    withCol {
        when (cardsOrNotes) {
            CARDS -> findCards(search, order)
            NOTES -> findNotes(search, order)
        }
    }.let { ids ->
        BrowserRowCollection(cardsOrNotes, ids.map { CardOrNoteId(it) }.toMutableList())
    }

/** Renders the row count for [this] event as a localized "X cards/notes" string. */
fun SearchState.Completed.formatCardCount(resources: android.content.res.Resources): String =
    resources.getQuantityString(
        if (cardsOrNotes == CARDS) CommonPlurals.card_browser_subtitle else CommonPlurals.card_browser_subtitle_notes_mode,
        rowCount,
        rowCount,
    )
