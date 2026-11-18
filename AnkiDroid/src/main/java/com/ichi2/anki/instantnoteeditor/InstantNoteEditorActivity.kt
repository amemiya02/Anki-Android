// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.instantnoteeditor

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.IntegerDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.servicelayer.NoteService
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.withProgress
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.themes.setTransparentBackground
import com.ichi2.utils.AssetHelper.TEXT_PLAIN
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/** Adds a Note from shared text while keeping the sending app visible behind the editor. */
class InstantNoteEditorActivity : AnkiActivity() {
    private val viewModel: InstantEditorViewModel by viewModels()

    private var deckName by mutableStateOf("")
    private var warning by mutableStateOf<String?>(null)
    private val extraValues = mutableStateListOf<String>()
    private var initialExtraValues: List<String>? = null
    private var fieldNames = emptyList<String>()
    private var clozeFieldIndex = -1
    private var instantAlertDialog: Dialog? = null

    private val dialogBackCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!isSaveIgnored()) showDiscardChangesDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        if (!ensureStorageIsReady()) return
        setTransparentBackground()
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, dialogBackCallback)

        if (Intent.ACTION_SEND != intent.action && intent.type == null && TEXT_PLAIN != intent.type) {
            Timber.i("Intent type is not supported")
            return
        }
        viewModel.setClozeFieldText(savedInstanceState?.getString(UPDATED_TEXT) ?: getSharedIntentText(intent))
        initialExtraValues = savedInstanceState?.getStringArrayList(EXTRA_VALUES)
        val restoredDeck = savedInstanceState?.getLong(SELECTED_DECK)?.takeIf { it != 0L }
        setupErrorListeners()
        observeProgress(viewModel) { progress -> getString(progress.messageRes) }
        registerDeckSelectedHandler(action = ::onDeckSelected)
        lifecycleScope.launch {
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

    override fun onDestroy() {
        instantAlertDialog?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(UPDATED_TEXT, viewModel.actualClozeFieldText.value)
        outState.putStringArrayList(EXTRA_VALUES, ArrayList(extraValues))
        viewModel.deckId?.let { outState.putLong(SELECTED_DECK, it) }
    }

    private fun showEditorDialog() {
        fieldNames =
            viewModel.editorNote.notetype.fields
                .map { it.name }
        val clozeFieldNames = viewModel.getClozeFields()
        clozeFieldIndex = fieldNames.indexOfFirst { it in clozeFieldNames }.takeIf { it >= 0 } ?: 0
        extraValues.addAll(fieldNames.mapIndexed { index, _ -> initialExtraValues?.getOrNull(index).orEmpty() })
        initialExtraValues = null
        updateBackCallback()
        updateSelectedDeckName()

        instantAlertDialog =
            createPaperDialog(this, onOutsideClick = onBackPressedDispatcher::onBackPressed) {
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
                    onBack = onBackPressedDispatcher::onBackPressed,
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
                        updateBackCallback()
                    },
                    onWord = { index -> editWord(index, viewModel::buildClozeText) },
                    onWordLongPress = ::showChangeClozeNumberDialog,
                    onSave = ::checkAndSave,
                )
            }.apply {
                setCancelable(false)
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode != KeyEvent.KEYCODE_BACK || event.action != KeyEvent.ACTION_UP) {
                        return@setOnKeyListener false
                    }
                    this@InstantNoteEditorActivity.onBackPressedDispatcher.onBackPressed()
                    true
                }
                show()
            }
    }

    private fun setClozeText(text: String) {
        if (viewModel.actualClozeFieldText.value == text) return
        viewModel.setClozeFieldText(text)
        warning = null
        updateBackCallback()
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
        navigate(NoteEditorDestination.AddInstantNote(viewModel.actualClozeFieldText.value.orEmpty()))
        finish()
    }

    private fun updateBackCallback() {
        dialogBackCallback.isEnabled =
            viewModel.actualClozeFieldText.value != getSharedIntentText(intent) || extraValues.any(String::isNotEmpty)
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

    /** Gets the shared text received through an Intent. **/
    private fun getSharedIntentText(receivedIntent: Intent): String? = receivedIntent.getStringExtra(Intent.EXTRA_TEXT)

    private fun isSaveIgnored(): Boolean {
        if (isFinishing) {
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
                showThemedToast(this@InstantNoteEditorActivity, TR.addingAdded(), true)
                finish()
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
        createPaperDialog(this) { dialog ->
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
                    finish()
                },
            )
        }.show()
    }

    private fun setupErrorListeners() {
        viewModel.onError
            .flowWithLifecycle(lifecycle)
            .onEach { errorMessage ->
                createPaperDialog(this) { dialog ->
                    ErrorDialog(getString(CommonString.vague_error), errorMessage, onDismissRequest = dialog::dismiss)
                }.show()
            }.launchIn(lifecycleScope)

        viewModel.instantEditorError
            .onEach { errorMessage ->
                if (errorMessage == TR.addingYouHaveAClozeDeletionNote()) {
                    noClozeDialog(errorMessage)
                } else {
                    warning = errorMessage
                }
            }.launchIn(lifecycleScope)
    }

    /** In case saving the note fails we, want to allow user to cancel and try again, or exist the activity **/
    private fun savingErrorDialog(message: String) {
        createPaperDialog(this) { dialog ->
            ErrorDialog(
                title = getString(CommonString.vague_error),
                message = message,
                onDismissRequest = {
                    dialog.dismiss()
                    instantAlertDialog?.dismiss()
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
        createPaperDialog(this) { dialog ->
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
        lifecycleScope.launch {
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
        launchCatchingTask {
            val name = withProgress { withCol { decks.name(did) } }
            if (viewModel.deckId == did) deckName = name
        }
    }

    companion object {
        private const val UPDATED_TEXT = "updatedText"
        private const val EXTRA_VALUES = "instant_extra_values"
        private const val SELECTED_DECK = "instant_selected_deck"
    }

    private fun showDiscardChangesDialog() {
        DiscardChangesDialog.showDialog(this, message = TR.addingDiscardCurrentInput()) {
            Timber.i("InstantNoteEditorActivity:: OK button pressed to confirm discard changes")
            finish()
        }
    }

    /**
     * Enum representing the modes available for cloze functionality.
     */
    enum class ClozeMode {
        INCREMENT,
        NO_INCREMENT,
    }

    /**
     * This enum class represents the different edit modes available for the user interface element.
     */
    enum class EditMode {
        /**
         * In this mode, a single tap on the text will turn it to cloze
         */
        SINGLE_TAP,

        /**
         * In this mode, user can edit the text as they want
         */
        ADVANCED,
    }

    /**
     * Enum class that represent the dialog that can be shown when the InstantEditor is initialized
     * **/
    enum class DialogType {
        /** Indicates that no cloze note types were found. **/
        NO_CLOZE_NOTE_TYPES_DIALOG,

        /** Indicates that the editor dialog should be shown. **/
        SHOW_EDITOR_DIALOG,
    }
}

@get:StringRes
private val InstantEditorProgress.messageRes: Int
    get() =
        when (this) {
            InstantEditorProgress.SAVING_NOTE -> CommonString.saving_facts
        }
