// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.instantnoteeditor

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.IntegerDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity.ClozeMode
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity.DialogType
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity.EditMode
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.noteeditor.toIntent
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.servicelayer.NoteService
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/** Edits text sent from Android's share sheet. */
class InstantNoteEditorFragment : Fragment() {
    private val viewModel: InstantEditorViewModel by viewModels()

    private var deckName by mutableStateOf("")
    private var warning by mutableStateOf<String?>(null)
    private val extraValues = mutableStateListOf<String>()
    private var initialExtraValues: List<String>? = null
    private var fieldNames = emptyList<String>()
    private var clozeFieldIndex = -1
    private var instantAlertDialog: Dialog? = null

    private var closing = false

    private val sharedText: String?
        get() = requireArguments().getCharSequence(Intent.EXTRA_TEXT)?.toString()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = View(inflater.context)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        if (viewModel.actualClozeFieldText.value == null) {
            viewModel.setClozeFieldText(savedInstanceState?.getString(STATE_UPDATED_TEXT) ?: sharedText)
        }
        initialExtraValues = savedInstanceState?.getStringArrayList(STATE_EXTRA_VALUES) ?: extraValues.toList()
        val restoredDeck = savedInstanceState?.getLong(STATE_SELECTED_DECK)?.takeIf { it != 0L }
        setupErrorListeners()
        observeProgress(viewModel) { progress -> getString(progress.messageRes) }
        registerDeckSelectedHandler(action = ::onDeckSelected)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dialogType.collect { type ->
                when (type) {
                    DialogType.NO_CLOZE_NOTE_TYPES_DIALOG -> noClozeNoteTypesFoundDialog()
                    DialogType.SHOW_EDITOR_DIALOG -> {
                        if (restoredDeck != null) viewModel.deckId = restoredDeck
                        showEditorDialog()
                    }
                    null -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        instantAlertDialog?.dismiss()
        instantAlertDialog = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_UPDATED_TEXT, viewModel.actualClozeFieldText.value)
        outState.putStringArrayList(STATE_EXTRA_VALUES, ArrayList(extraValues))
        viewModel.deckId?.let { outState.putLong(STATE_SELECTED_DECK, it) }
    }

    private fun showEditorDialog() {
        fieldNames =
            viewModel.editorNote.notetype.fields
                .map { it.name }
        val clozeFieldNames = viewModel.getClozeFields()
        clozeFieldIndex = fieldNames.indexOfFirst { it in clozeFieldNames }.takeIf { it >= 0 } ?: 0
        extraValues.clear()
        extraValues.addAll(fieldNames.mapIndexed { index, _ -> initialExtraValues?.getOrNull(index).orEmpty() })
        initialExtraValues = null
        updateSelectedDeckName()

        instantAlertDialog =
            createPaperDialog(requireContext(), onOutsideClick = ::onBack) {
                val clozeText by viewModel.actualClozeFieldText.collectAsStateWithLifecycle()
                val mode by viewModel.editorMode.collectAsStateWithLifecycle()
                val clozeMode by viewModel.currentClozeMode.collectAsStateWithLifecycle()
                val clozeNumber by viewModel.currentClozeNumber.collectAsStateWithLifecycle()
                val progress by viewModel.progressManager.progress.collectAsStateWithLifecycle()
                InstantEditorScreen(
                    state =
                        InstantEditorScreenState(
                            deck = deckName,
                            fields = fieldNames,
                            extraValues = extraValues.toList(),
                            clozeFieldIndex = clozeFieldIndex,
                            clozeText = clozeText.orEmpty(),
                            nextClozeNumber = clozeNumber,
                            incrementsCloze = clozeMode == ClozeMode.INCREMENT,
                            editMode = mode,
                            words =
                                viewModel.getWordsFromFieldText().map { word ->
                                    InstantEditorWord(viewModel.getCleanClozeWords(word), viewModel.getWordClozeNumber(word))
                                },
                            warning = warning,
                            saving = progress is ViewModelProgress.Active,
                        ),
                    onBack = ::onBack,
                    onPickDeck = {
                        startDeckSelection(
                            allowAll = false,
                            allowFiltered = false,
                            skipEmptyDefault = true,
                        )
                    },
                    onSwitchEditMode = {
                        viewModel.setEditorMode(if (mode == EditMode.SINGLE_TAP) EditMode.ADVANCED else EditMode.SINGLE_TAP)
                    },
                    onSwitchClozeMode = viewModel::toggleClozeMode,
                    onOpenEditor = ::openNoteEditor,
                    onClozeText = ::setClozeText,
                    onExtraField = { index, value ->
                        extraValues[index] = value
                        warning = null
                    },
                    onWord = { index -> editWord(index, viewModel::buildClozeText) },
                    onWordLongPress = ::showChangeClozeNumberDialog,
                    onSave = ::checkAndSave,
                )
            }.apply {
                setCancelable(true)
                setCanceledOnTouchOutside(false)
                onBackPressedDispatcher.addCallback { onBack() }
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode != KeyEvent.KEYCODE_BACK || event.action != KeyEvent.ACTION_UP) {
                        return@setOnKeyListener false
                    }
                    onBack()
                    true
                }
                show()
            }
    }

    private fun setClozeText(text: String) {
        if (viewModel.actualClozeFieldText.value == text) return
        viewModel.setClozeFieldText(text)
        warning = null
    }

    private fun editWord(
        index: Int,
        edit: (String) -> String,
    ) {
        val words = viewModel.getWordsFromFieldText().toMutableList()
        val word = words.getOrNull(index) ?: return
        words[index] = edit(word)
        setClozeText(words.joinToString(" "))
    }

    private fun showChangeClozeNumberDialog(index: Int) {
        val word = viewModel.getWordsFromFieldText().getOrNull(index) ?: return
        val oldNumber = viewModel.getWordClozeNumber(word) ?: return
        showDialogFragment(
            IntegerDialog().apply {
                setArgs(
                    title = getString(CommonString.change_cloze_number),
                    prompt = getString(CommonString.cloze_number),
                    digits = 2,
                    defaultValue = oldNumber.toString(),
                )
                setCallbackRunnable { newNumber ->
                    if (newNumber != oldNumber) editWord(index) { viewModel.updateClozeNumber(it, newNumber) }
                }
            },
        )
    }

    private fun openNoteEditor() {
        val fullEditor =
            NoteEditorDestination
                .AddInstantNote(viewModel.actualClozeFieldText.value.orEmpty())
                .toIntent(requireContext())
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(fullEditor)
    }

    private fun onBack() {
        if (isSaveIgnored()) return
        if (viewModel.actualClozeFieldText.value != sharedText || extraValues.any(String::isNotEmpty)) {
            showDiscardChangesDialog()
        } else {
            closeEditor()
        }
    }

    private fun closeEditor() {
        if (closing) return
        closing = true
        instantAlertDialog?.dismiss()
        finishScreen()
    }

    private fun checkAndSave() {
        if (isSaveIgnored()) return
        val note = viewModel.editorNote
        fieldNames.indices.forEach { index ->
            val text = if (index == clozeFieldIndex) viewModel.actualClozeFieldText.value.orEmpty() else extraValues[index]
            note.values()[index] = NoteService.convertToHtmlNewline(text, false)
        }
        saveNoteWithProgress(skipClozeCheck = false)
    }

    private fun isSaveIgnored(): Boolean {
        if (closing || view == null) {
            Timber.i("Ignoring save, the editor is closing")
            return true
        }
        if (viewModel.progressManager.progress.value is ViewModelProgress.Active) {
            Timber.i("Ignoring save while a note is saving")
            return true
        }
        return false
    }

    private fun handleSaveNoteResult(result: SaveNoteResult) {
        when (result) {
            is SaveNoteResult.Failure -> {
                Timber.d("Failed to save note")
                savingErrorDialog(result.message ?: getString(CommonString.something_wrong))
            }

            SaveNoteResult.Success -> {
                // Don't show snackbar to avoid blocking parent app
                showThemedToast(requireContext(), TR.addingAdded(), true)
                closeEditor()
            }

            is SaveNoteResult.Warning -> {
                Timber.d("Showing warning to the user")
                viewModel.setWarningMessage(result.message ?: getString(CommonString.something_wrong))
            }
        }
    }

    /** Show a dialog when there is no cloze note type is found, allowing user either to cancel or to open
     * AnkiDroid Note Editor **/
    private fun noClozeNoteTypesFoundDialog() {
        instantAlertDialog =
            createPaperDialog(requireContext()) { dialog ->
                ConfirmationDialog(
                    title = getString(CommonString.cloze_note_required),
                    message = getString(CommonString.cloze_not_found_message),
                    confirm =
                        DialogAction(getString(CommonString.open), {
                            dialog.dismiss()
                            openNoteEditor()
                        }),
                    onDismissRequest = {
                        dialog.dismiss()
                        closeEditor()
                    },
                )
            }.apply {
                setOnCancelListener { closeEditor() }
                show()
            }
    }

    private fun setupErrorListeners() {
        viewModel.onError
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { errorMessage ->
                createPaperDialog(requireContext()) { dialog ->
                    ErrorDialog(getString(CommonString.vague_error), errorMessage, onDismissRequest = dialog::dismiss)
                }.show()
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.instantEditorError
            .onEach { errorMessage ->
                if (errorMessage == TR.addingYouHaveAClozeDeletionNote()) {
                    noClozeDialog(errorMessage)
                } else {
                    warning = errorMessage
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /** In case saving the note fails we, want to allow user to cancel and try again, or exist the activity **/
    private fun savingErrorDialog(message: String) {
        createPaperDialog(requireContext()) { dialog ->
            ErrorDialog(
                title = getString(CommonString.vague_error),
                message = message,
                onDismissRequest = {
                    dialog.dismiss()
                    closeEditor()
                },
                primaryAction =
                    DialogAction(getString(CommonString.try_again), {
                        dialog.dismiss()
                        checkAndSave()
                    }),
                dismissText = getString(CommonString.dialog_cancel),
            )
        }.show()
    }

    /** Warns the user for no cloze in the cloze field, and provide the choice to proceed or
     * to abort save and go back to the editor  **/
    private fun noClozeDialog(errorMessage: String) {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = "",
                message = errorMessage,
                confirm =
                    DialogAction(TR.actionsSave(), {
                        dialog.dismiss()
                        if (!isSaveIgnored()) saveNoteWithProgress(skipClozeCheck = true)
                    }),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    private fun saveNoteWithProgress(skipClozeCheck: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            handleSaveNoteResult(viewModel.checkAndSaveNote(skipClozeCheck = skipClozeCheck))
        }
    }

    private fun onDeckSelected(deck: SelectableDeck?) {
        if (deck == null) return
        require(deck is SelectableDeck.Deck)
        viewModel.deckId = deck.deckId
        updateSelectedDeckName()
    }

    private fun updateSelectedDeckName() {
        val did = viewModel.deckId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val name = withCol { decks.name(did) }
            if (viewModel.deckId == did) deckName = name
        }
    }

    companion object {
        private const val STATE_UPDATED_TEXT = "updatedText"
        private const val STATE_EXTRA_VALUES = "instant_extra_values"
        private const val STATE_SELECTED_DECK = "instant_selected_deck"
    }

    private fun showDiscardChangesDialog() {
        DiscardChangesDialog.showDialog(requireActivity(), message = TR.addingDiscardCurrentInput()) {
            Timber.i("InstantNoteEditorActivity:: OK button pressed to confirm discard changes")
            closeEditor()
        }
    }
}

@get:StringRes
private val InstantEditorProgress.messageRes: Int
    get() =
        when (this) {
            InstantEditorProgress.SAVING_NOTE -> CommonString.saving_facts
        }
