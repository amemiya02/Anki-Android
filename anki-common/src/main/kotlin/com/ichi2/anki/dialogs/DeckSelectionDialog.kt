// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import anki.collection.opChanges
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.common.ALL_DECKS_ID
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.deckpicker.DeckFilters
import com.ichi2.anki.dialogs.DeckSelectionDialog.Companion.REQUEST_SELECT_DECK
import com.ichi2.anki.dialogs.DeckSelectionDialog.Companion.RESULT_SELECTED_DECK
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.sched.DeckNode
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.getParcelableCompat
import com.ichi2.anki.utils.ext.requireLongArray
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.anki.withProgress
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.Job
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

/**
 * A fragment which allows the user to select one or multiple decks from a list of decks. It also
 * provides options to filter the list of decks and add top level or sub decks.
 *
 * @see SelectableDeck
 */
@NeedsTest("simulate 'don't keep activities'")
@NeedsTest("Test the ordering of the dialog")
@NeedsTest("test the ordering of decks in search page in the dialog")
@NeedsTest("test syncing the status of collapsing deck with teh deckPicker")
class DeckSelectionDialog : AnalyticsDialogFragment() {
    private var paperTree: DeckNode? = null
    private var removedDeckIds by mutableStateOf(longArrayOf())

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(STATE_REMOVED_DECK_IDS, removedDeckIds)
    }

    private val title: String
        get() =
            requireArguments().getString(ARG_TITLE, null)
                ?: getString(CommonString.select_deck_title)
    private val templateEditorMessage: String?
        get() = requireArguments().getString(ARG_TEMPLATE_EDITOR_MESSAGE, null)
    private val allowMultipleSelection: Boolean
        get() = requireArguments().getBoolean(ARG_ALLOW_MULTIPLE_SELECTION, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = true
        removedDeckIds = savedInstanceState?.getLongArray(STATE_REMOVED_DECK_IDS) ?: longArrayOf()
        return createPaperDialog(requireContext()) { DeckSelectionContent() }
    }

    @Composable
    private fun DeckSelectionContent() {
        val root by produceState<DeckNode?>(null) {
            value = withCol { sched.deckDueTree() }
            paperTree = value
        }
        var query by rememberSaveable { mutableStateOf("") }
        var toggled by rememberSaveable { mutableStateOf(longArrayOf()) }
        var searchCollapsed by rememberSaveable(query) { mutableStateOf(longArrayOf()) }
        val filters = DeckFilters.create(query)
        val allowed = getDeckIds().toSet() - removedDeckIds.toSet()
        PaperDialog(
            title = title,
            onDismissRequest = ::dismiss,
            actions =
                buildList {
                    add(DialogAction(getString(CommonString.dialog_cancel), ::dismiss))
                    if (templateEditorMessage != null) {
                        add(
                            DialogAction(getString(CommonString.restore_default), {
                                onDeckSelected(null)
                                dismiss()
                            }),
                        )
                    }
                    add(DialogAction(TR.sentenceCase.createDeck, ::showDeckDialog))
                },
        ) {
            templateEditorMessage?.let { DialogBody(it) }
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(getString(CommonString.deck_picker_dialog_filter_decks)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                if (ALL_DECKS_ID in allowed && filters.accept(TR.sentenceCase.allDecks)) {
                    item {
                        PaperDeckRow(SelectableDeck.AllDecks, name = TR.sentenceCase.allDecks, depth = 0)
                    }
                }
                val visible =
                    root
                        ?.let { tree ->
                            allowed.mapNotNull(tree::find).filter { node ->
                                filters.accept(node.fullDeckName) &&
                                    generateSequence(node.parent?.get()) { it.parent?.get() }.none { parent ->
                                        if (filters.isActive()) {
                                            parent.did in searchCollapsed
                                        } else {
                                            parent.collapsed xor
                                                (parent.did in toggled)
                                        }
                                    }
                            }
                        }.orEmpty()
                items(visible, key = { it.did }) { node ->
                    val deck = SelectableDeck.Deck(node.did, node.fullDeckName)
                    val collapsed =
                        if (filters.isActive()) node.did in searchCollapsed else node.collapsed xor (node.did in toggled)
                    PaperDeckRow(
                        deck,
                        name = if (filters.isActive()) node.fullDeckName else deck.getDisplayName(requireContext()),
                        depth = if (filters.isActive()) 0 else node.depth,
                        collapsed = collapsed.takeIf { node.children.isNotEmpty() },
                        onToggle = {
                            if (filters.isActive()) {
                                searchCollapsed = searchCollapsed.toggle(node.did)
                            } else {
                                toggled = toggled.toggle(node.did)
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun PaperDeckRow(
        deck: SelectableDeck,
        name: String,
        depth: Int,
        collapsed: Boolean? = null,
        onToggle: () -> Unit = {},
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = (depth.coerceIn(0, 4) * 12).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (collapsed != null) {
                IconButton(onClick = onToggle) {
                    Icon(
                        painterResource(if (collapsed) CommonR.drawable.symbol_chevron_right else CommonR.drawable.symbol_expand_more),
                        getString(if (collapsed) CommonString.expand else CommonString.collapse),
                    )
                }
            } else {
                Box(Modifier.width(48.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .combinedClickable(
                        role = Role.RadioButton,
                        onClick = { selectDeckAndClose(deck) },
                        onLongClick = if (deck is SelectableDeck.Deck) ({ showSubDeckDialog(deck) }) else null,
                        onLongClickLabel = getString(CommonString.create_subdeck),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(name)
            }
        }
    }

    private fun LongArray.toggle(id: Long): LongArray = if (id in this) filterNot { it == id }.toLongArray() else this + id

    override fun onResume() {
        super.onResume()
        // needed otherwise the keyboard can focus the Search field
        dialog?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        )
    }

    private fun getDeckIds(): LongArray = requireArguments().requireLongArray(ARG_DECK_IDS)

    /**
     * Displays a dialog to create a subdeck under the specified parent deck.
     *
     * @param parentDeck The parent deck under which the subdeck will be created
     */
    private fun showSubDeckDialog(parentDeck: SelectableDeck.Deck) {
        val createDeckDialog =
            CreateDeckDialog(
                context = requireActivity(),
                title = getString(CommonString.create_subdeck),
                deckDialogType = CreateDeckDialog.DeckDialogType.SUB_DECK,
                parentId = parentDeck.deckId,
            )
        createDeckDialog.onNewDeckCreated = { did: DeckId -> onNewDeckCreated(did) }
        createDeckDialog.showComposeDialog()
    }

    private fun showDeckDialog() {
        val createDeckDialog =
            CreateDeckDialog(
                context = requireActivity(),
                title = TR.sentenceCase.createDeck,
                deckDialogType = CreateDeckDialog.DeckDialogType.DECK,
                parentId = null,
            )
        createDeckDialog.onNewDeckCreated = { did: DeckId -> onNewDeckCreated(did) }
        createDeckDialog.showComposeDialog()
    }

    /** Updates the list and simulates a click on the newly created deck */
    private fun onNewDeckCreated(id: DeckId) {
        // a deck/subdeck was created
        ChangeManager.notifySubscribers(
            opChanges {
                deck = true
                studyQueues = true
            },
            initiator = this,
        )
        launchCatchingTask {
            val name = withCol { decks.name(id) }
            val deck = SelectableDeck.Deck(id, name)
            selectDeckAndClose(deck)
        }
    }

    private fun onDeckSelected(deck: SelectableDeck?) {
        val requestKey = requireArguments().getString(ARG_REQUEST_KEY) ?: REQUEST_SELECT_DECK
        parentFragmentManager.setFragmentResult(
            requestKey,
            Bundle().apply {
                putParcelable(RESULT_SELECTED_DECK, deck)
            },
        )
    }

    /**
     * Same action as pressing on the deck in the list. I.e. send the deck to listener and close the
     * dialog. When multiple selection is enabled, the dialog stays open and removes the selected
     * deck from the list.
     */
    private fun selectDeckAndClose(deck: SelectableDeck) {
        Timber.d("selected deck '%s'", deck)
        onDeckSelected(deck)
        if (!allowMultipleSelection) {
            dismiss()
            return
        }
        removedDeckIds +=
            when (deck) {
                is SelectableDeck.Deck ->
                    paperTree?.find(deck.deckId)?.map { it.did }?.toLongArray() ?: longArrayOf(deck.deckId)
                SelectableDeck.AllDecks -> longArrayOf(ALL_DECKS_ID)
            }
        if (getDeckIds().all { it in removedDeckIds }) dismiss()
    }

    companion object {
        const val TAG = "DeckSelectionDialog"
        const val REQUEST_SELECT_DECK = "request_select_deck"
        const val RESULT_SELECTED_DECK = "arg_selected_deck"
        const val ARG_REQUEST_KEY = "arg_request_key"
        const val ARG_ALLOW_ALL = "arg_allow_all"
        const val ARG_ALLOW_FILTERED = "arg_allow_filtered"
        const val ARG_SKIP_EMPTY_DEFAULT = "arg_skip_empty_default"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_TEMPLATE_EDITOR_MESSAGE = "arg_template_editor_message"
        private const val ARG_DECK_IDS = "arg_deck_ids"
        private const val ARG_ALLOW_MULTIPLE_SELECTION = "arg_allow_multiple_selection"
        private const val STATE_REMOVED_DECK_IDS = "removed_deck_ids"

        /** Creates a new instance of [DeckSelectionDialog]. */
        fun newInstance(
            title: String? = null,
            templateEditorMessage: String? = null,
            decks: List<SelectableDeck>,
            requestKey: String = REQUEST_SELECT_DECK,
            allowMultipleSelection: Boolean = false,
            allowAll: Boolean = true,
            allowFiltered: Boolean = true,
            skipEmptyDefault: Boolean = false,
        ): DeckSelectionDialog =
            DeckSelectionDialog().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_TEMPLATE_EDITOR_MESSAGE, templateEditorMessage)
                        putString(ARG_TITLE, title)
                        putLongArray(ARG_DECK_IDS, decks.map { (it as? SelectableDeck.Deck)?.deckId ?: ALL_DECKS_ID }.toLongArray())
                        putString(ARG_REQUEST_KEY, requestKey)
                        putBoolean(ARG_ALLOW_MULTIPLE_SELECTION, allowMultipleSelection)
                        putBoolean(ARG_ALLOW_ALL, allowAll)
                        putBoolean(ARG_ALLOW_FILTERED, allowFiltered)
                        putBoolean(ARG_SKIP_EMPTY_DEFAULT, skipEmptyDefault)
                    }
            }
    }
}

/**
 * Register a fragment result listener to listen for a deck selection.
 * @param requestKey usually [REQUEST_SELECT_DECK], but can be changed to handle situations when
 * there are multiple listeners for this event that do different things in response
 * @param action a lambda that provides the user selected deck
 */
fun FragmentActivity.registerDeckSelectedHandler(
    requestKey: String = REQUEST_SELECT_DECK,
    action: (deck: SelectableDeck?) -> Unit,
) {
    setFragmentResultListener(requestKey) { _, bundle ->
        val selectedDeck = bundle.getParcelableCompat<SelectableDeck?>(RESULT_SELECTED_DECK)
        action(selectedDeck)
    }
}

/**
 * Register a fragment result listener to listen for a deck selection.
 * Note: the fragment result listener is set on [Fragment.getParentFragmentManager]
 * @param requestKey usually [REQUEST_SELECT_DECK], but can be changed to handle situations when
 * there are multiple listeners for this event that do different things in response
 * @param action a lambda that provides the user selected deck
 */
fun Fragment.registerDeckSelectedHandler(
    requestKey: String = REQUEST_SELECT_DECK,
    action: (deck: SelectableDeck?) -> Unit,
) {
    setFragmentResultListener(requestKey) { _, bundle ->
        val selectedDeck = bundle.getParcelableCompat<SelectableDeck?>(RESULT_SELECTED_DECK)
        action(selectedDeck)
    }
}

fun Fragment.registerChildDeckSelectedHandler(
    requestKey: String = REQUEST_SELECT_DECK,
    action: (deck: SelectableDeck?) -> Unit,
) {
    childFragmentManager.setFragmentResultListener(requestKey, this) { _, bundle ->
        val selectedDeck = bundle.getParcelableCompat<SelectableDeck?>(RESULT_SELECTED_DECK)
        action(selectedDeck)
    }
}

/**
 * Displays a [DeckSelectionDialog] for the user to select a deck. Using the default parameters will
 * result in all possible decks('All Decks', normal, filtered and the 'Default' deck) being shown.
 *
 * @param title the title for the selection dialog, uses [CommonString.select_decks_title] if not provided
 * @param asChild if true [DeckSelectionDialog] will start as a child of [Fragment]([Fragment.getChildFragmentManager]
 * will be used to start it). If false, the [FragmentActivity.getSupportFragmentManager] will be used
 * instead to start [DeckSelectionDialog]
 * @param templateEditorMessage an optional message shown only when used by `CardTemplateEditor`
 * @param requestKey specifies the request key to be used for this request
 * @param allowMultipleSelection if selection of multiple decks is permitted
 * @param allowAll shows 'All decks' option if true
 * @param allowFiltered shows filtered decks if true
 * @param skipEmptyDefault if true, hides the 'Default' deck if it has no cards or subdecks
 */
fun Fragment.startDeckSelection(
    title: String? = null,
    asChild: Boolean = false,
    templateEditorMessage: String? = null,
    requestKey: String = REQUEST_SELECT_DECK,
    allowMultipleSelection: Boolean = false,
    allowAll: Boolean = true,
    allowFiltered: Boolean = true,
    skipEmptyDefault: Boolean = false,
): Job =
    launchCatchingTask {
        val decks =
            withProgress {
                val decks: MutableList<SelectableDeck> =
                    SelectableDeck
                        .fromCollection(
                            includeFiltered = allowFiltered,
                            skipEmptyDefault = skipEmptyDefault,
                        ).toMutableList()
                if (allowAll) {
                    decks.add(0, SelectableDeck.AllDecks)
                }
                decks
            }
        val dialog =
            DeckSelectionDialog.newInstance(
                decks = decks,
                title = title,
                templateEditorMessage = templateEditorMessage,
                requestKey = requestKey,
                allowMultipleSelection = allowMultipleSelection,
                allowAll = allowAll,
                allowFiltered = allowFiltered,
                skipEmptyDefault = skipEmptyDefault,
            )
        val targetManager = if (asChild) childFragmentManager else parentFragmentManager
        if (!targetManager.isStateSaved) {
            dialog.show(targetManager, DeckSelectionDialog.TAG)
        }
    }

/**
 * Displays a [DeckSelectionDialog] for the user to select a deck. Using the default parameters will
 * result in all possible decks('All Decks', normal, filtered and the 'Default' deck) being shown.
 *
 * @param title the title for the selection dialog, uses [CommonString.select_decks_title] if not provided
 * @param templateEditorMessage an optional message shown only when used by `CardTemplateEditor`
 * @param requestKey specifies the request key to be used for this request
 * @param allowMultipleSelection if selection of multiple decks is permitted
 * @param allowAll shows 'All decks' option if true
 * @param allowFiltered shows filtered decks if true
 * @param skipEmptyDefault if true, hides the 'Default' deck if it has no cards or subdecks
 */
fun FragmentActivity.startDeckSelection(
    title: String? = null,
    templateEditorMessage: String? = null,
    requestKey: String = REQUEST_SELECT_DECK,
    allowMultipleSelection: Boolean = false,
    allowAll: Boolean = true,
    allowFiltered: Boolean = true,
    skipEmptyDefault: Boolean = false,
) {
    launchCatchingTask {
        val decks =
            withProgress {
                val decks: MutableList<SelectableDeck> =
                    SelectableDeck
                        .fromCollection(
                            includeFiltered = allowFiltered,
                            skipEmptyDefault = skipEmptyDefault,
                        ).toMutableList()
                if (allowAll) {
                    decks.add(0, SelectableDeck.AllDecks)
                }
                decks
            }
        val dialog =
            DeckSelectionDialog.newInstance(
                decks = decks,
                title = title,
                templateEditorMessage = templateEditorMessage,
                requestKey = requestKey,
                allowMultipleSelection = allowMultipleSelection,
                allowAll = allowAll,
                allowFiltered = allowFiltered,
                skipEmptyDefault = skipEmptyDefault,
            )
        if (!supportFragmentManager.isStateSaved) {
            dialog.show(supportFragmentManager, DeckSelectionDialog.TAG)
        }
    }
}
