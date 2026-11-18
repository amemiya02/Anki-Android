// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2018 Mike Hardy <mike@mikehardy.net>

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import anki.notetypes.StockNotetype
import anki.notetypes.StockNotetype.OriginalStockKind.ORIGINAL_STOCK_KIND_UNKNOWN_VALUE
import anki.notetypes.notetypeId
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.android.input.ShortcutGroup
import com.ichi2.anki.android.input.shortcut
import com.ichi2.anki.cardviewer.SingleCardSide
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.annotation.KotlinCleanup
import com.ichi2.anki.common.utils.ext.contentEquals
import com.ichi2.anki.compat.CompatHelper.Companion.getSerializableCompat
import com.ichi2.anki.dialogs.ConfirmationDialog
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.InsertFieldDialog
import com.ichi2.anki.dialogs.InsertFieldMetadata
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.anki.libanki.CardTemplates
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.libanki.NoteTypeId
import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.libanki.Notetypes
import com.ichi2.anki.libanki.Notetypes.Companion.NOT_FOUND_NOTE_TYPE
import com.ichi2.anki.libanki.backend.BackendUtils
import com.ichi2.anki.libanki.backend.model.toBackendNote
import com.ichi2.anki.libanki.exception.ConfirmModSchemaException
import com.ichi2.anki.libanki.getStockNotetype
import com.ichi2.anki.libanki.getStockNotetypeKinds
import com.ichi2.anki.libanki.utils.append
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.notetype.CardTypeName
import com.ichi2.anki.notetype.RenameCardTypeDialog
import com.ichi2.anki.notetype.RepositionCardTemplateDialog
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.previewer.TemplatePreviewerArguments
import com.ichi2.anki.previewer.TemplatePreviewerFragment
import com.ichi2.anki.previewer.TemplatePreviewerPage
import com.ichi2.anki.settings.uiConfigPrefs
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.themes.Themes
import com.ichi2.utils.TruncatedString
import com.ichi2.utils.copyToClipboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ankiweb.rsdroid.Translations
import net.ankiweb.rsdroid.exceptions.BackendTemplateException
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import com.ichi2.compose.ui.dialogs.ConfirmationDialog as PaperConfirmationDialog

private typealias BackendCardTemplate = com.ichi2.anki.libanki.CardTemplate

/** Edits a note type's templates before committing them to the collection. */
open class CardTemplateEditor : AnkiActivity() {
    @VisibleForTesting
    var tempNoteType: CardTemplateNotetype? = null
    private val tempModel get() = checkNotNull(tempNoteType)
    private var noteTypeId: NoteTypeId = 0
    private var noteId: NoteId = -1
    private var previewNoteId: NoteId = 0

    private var sections = hashMapOf<CardOrdinal, EditorViewType>()
    private var cursorPositions = hashMapOf<CardOrdinal, HashMap<EditorViewType, Int>>()
    private var ord by mutableIntStateOf(0)
    private var section by mutableStateOf(EditorViewType.FRONT)
    private var templateNames by mutableStateOf(emptyList<String>())
    private var noteTypeName by mutableStateOf("")
    private var fieldNames = emptyList<String>()
    private var deckOverrideEnabled by mutableStateOf(false)
    private var dynamicTemplates = false
    private var saving by mutableStateOf(false)
    private var codeEditor: EditText? = null
    private var replacingCode = false
    private var softwareKeyboardVisible = false
    private var previewContainer: FragmentContainerView? = null
    private var previewRefresh: Job? = null
    private var previewFile: NotetypeFile? = null
    private val snackbar = SnackbarHostState()

    val displayDiscardChangesCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        }

    private val browserAppearanceResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val appearance = CardTemplateBrowserAppearanceEditor.Result.fromIntent(result.data) ?: return@registerForActivityResult
            val template = tempNoteType?.getTemplate(ord) ?: return@registerForActivityResult
            appearance.applyTo(template)
            updateDiscardChangesCallback()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        if (!ensureStorageIsReady()) return

        noteTypeId =
            savedInstanceState?.getLong(STATE_NOTE_TYPE_ID)
                ?: intent.getLongExtra(EXTRA_NOTE_TYPE_ID, NOT_FOUND_NOTE_TYPE)
        if (noteTypeId == NOT_FOUND_NOTE_TYPE) {
            finish()
            return
        }
        noteId = savedInstanceState?.getLong(STATE_NOTE_ID) ?: intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        ord = savedInstanceState?.getInt(STATE_STARTING_TEMPLATE_ORDINAL) ?: intent.getIntExtra(EXTRA_START_ORD_ID, 0)
        if (savedInstanceState != null) {
            tempNoteType = CardTemplateNotetype.fromBundle(savedInstanceState)
            sections = savedInstanceState.getSerializableCompat(STATE_TAB_TO_VIEW_ID) ?: hashMapOf()
            cursorPositions = savedInstanceState.getSerializableCompat(STATE_TAB_TO_CURSOR_POSITION) ?: hashMapOf()
        }

        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme })
        onBackPressedDispatcher.addCallback(this, displayDiscardChangesCallback)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val keyboardHeight = insets.getInsets(ime()).bottom
            softwareKeyboardVisible = keyboardHeight > (80 * resources.displayMetrics.density)
            updateDiscardChangesCallback()
            insets
        }
        supportFragmentManager.setFragmentResultListener(INSERT_FIELD_REQUEST_KEY, this) { _, result ->
            result.getString(InsertFieldDialog.RESULT_INSERTED_FIELD)?.let(::insertField)
        }
        registerDeckSelectedHandler(action = ::onDeckSelected)

        setContent {
            AnkiDroidTheme {
                if (templateNames.isNotEmpty()) {
                    CardTemplateEditorScreen(
                        noteTypeName = noteTypeName,
                        templateNames = templateNames,
                        selectedTemplate = ord,
                        section = section,
                        availableFields = fieldNames,
                        deckOverrideEnabled = deckOverrideEnabled,
                        dynamicTemplates = dynamicTemplates,
                        saving = saving,
                        splitPane = LocalConfiguration.current.smallestScreenWidthDp >= 600,
                        panePreferences = uiConfigPrefs(),
                        snackbar = snackbar,
                        onBack = { onActionBarBackPressed() },
                        onSelectTemplate = ::selectTemplate,
                        onSelectSection = ::selectSection,
                        onInsertField = { insertField("{{$it}}") },
                        onAction = ::runAction,
                        onEditorReady = ::onEditorReady,
                        onTextChanged = ::onCodeChanged,
                        onInsertFieldRequest = ::showInsertFieldDialog,
                        onPreviewReady = ::onPreviewReady,
                    )
                }
            }
        }
        startLoadingCollection()
    }

    override fun onCollectionLoaded(col: Collection) {
        super.onCollectionLoaded(col)
        // The editor may have been opened just after renaming a note type.
        col.notetypes.clearCache()
        val noteType = tempNoteType ?: CardTemplateNotetype(col.notetypes.get(noteTypeId)!!.deepClone())
        tempNoteType = noteType
        previewNoteId =
            noteId.takeIf { it > 0 } ?: col.db.queryLongScalar("select id from notes where mid = ? order by id limit 1", noteTypeId)
        fieldNames = noteType.notetype.fieldsNames
        noteTypeName = noteType.notetype.name
        dynamicTemplates = noteType.notetype.isCloze || noteType.notetype.isImageOcclusion
        ord = ord.coerceIn(0, noteType.templateCount - 1)
        section = sections[ord] ?: EditorViewType.FRONT
        refreshTemplateNames()
        updateDiscardChangesCallback()
    }

    override fun onResume() {
        super.onResume()
        requestPreviewRefresh()
    }

    override fun onDestroy() {
        if (isFinishing) previewFile?.delete()
        super.onDestroy()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        rememberCursor()
        tempNoteType?.let { outState.putAll(it.toBundle()) }
        outState.putLong(STATE_NOTE_TYPE_ID, noteTypeId)
        outState.putLong(STATE_NOTE_ID, noteId)
        outState.putInt(STATE_STARTING_TEMPLATE_ORDINAL, ord)
        outState.putSerializable(STATE_TAB_TO_VIEW_ID, sections)
        outState.putSerializable(STATE_TAB_TO_CURSOR_POSITION, cursorPositions)
        super.onSaveInstanceState(outState)
    }

    override fun onActionBarBackPressed(): Boolean {
        if (noteTypeHasChanged()) showDiscardChangesDialog() else finish()
        return true
    }

    private fun updateDiscardChangesCallback() {
        // Full IMEs take Back themselves. The accessory strip for a hardware keyboard does not.
        displayDiscardChangesCallback.isEnabled = !softwareKeyboardVisible && templateNames.isNotEmpty() && noteTypeHasChanged()
    }

    private fun refreshTemplateNames() {
        templateNames = tempModel.notetype.templatesNames
        refreshDeckOverride()
    }

    private fun refreshDeckOverride() {
        val template = tempModel.getTemplate(ord).jsonObject
        deckOverrideEnabled = template.has("did") && !template.isNull("did")
    }

    private fun selectTemplate(position: CardOrdinal) {
        if (position == ord) return
        rememberCursor()
        showTemplate(position)
        requestPreviewRefresh()
    }

    private fun showTemplate(position: CardOrdinal) {
        ord = position
        section = sections[position] ?: EditorViewType.FRONT
        refreshDeckOverride()
        showCode()
    }

    private fun selectSection(selected: EditorViewType) {
        if (selected == section) return
        rememberCursor()
        section = selected
        sections[ord] = selected
        showCode()
    }

    private fun rememberCursor() {
        val position = codeEditor?.selectionStart ?: return
        cursorPositions.getOrPut(ord, ::hashMapOf)[section] = position
    }

    private fun onEditorReady(editor: EditText) {
        rememberCursor()
        codeEditor = editor
        showCode()
        editor.doOnAttach {
            editor.post { if (codeEditor === editor) editor.requestFocus() }
        }
    }

    private fun showCode() {
        val editor = codeEditor ?: return
        val template = tempModel.getTemplate(ord)
        val code =
            when (section) {
                EditorViewType.FRONT -> template.qfmt
                EditorViewType.BACK -> template.afmt
                EditorViewType.STYLING -> tempModel.css
            }
        replacingCode = true
        try {
            editor.setText(code)
            editor.setSelection((cursorPositions[ord]?.get(section) ?: 0).coerceIn(0, code.length))
        } finally {
            replacingCode = false
        }
        editor.requestFocus()
    }

    private fun onCodeChanged(code: String) {
        if (replacingCode || saving) return
        val model = tempNoteType ?: return
        val template = model.getTemplate(ord)
        when (section) {
            EditorViewType.FRONT -> template.qfmt = code
            EditorViewType.BACK -> template.afmt = code
            EditorViewType.STYLING -> model.css = code
        }
        model.updateTemplate(ord, template)
        requestPreviewRefresh(REFRESH_PREVIEW_DELAY)
        updateDiscardChangesCallback()
    }

    private fun repositionTemplate(
        from: CardOrdinal,
        to: CardOrdinal,
    ) {
        if (from == to) return
        rememberCursor()
        val positions = (0 until tempModel.templateCount).toMutableList()
        positions.add(to, positions.removeAt(from))
        reorderEditorState(positions)
        tempModel.repositionTemplate(from, to)
        showTemplate(to)
        refreshTemplateNames()
    }

    private fun reorderEditorState(positions: List<Int>) {
        fun <T> reindex(state: Map<CardOrdinal, T>): HashMap<CardOrdinal, T> =
            positions.mapIndexedNotNull { index, old -> state[old]?.let { index to it } }.toMap(HashMap())
        sections = reindex(sections)
        cursorPositions = reindex(cursorPositions)
    }

    private fun onPreviewReady(container: FragmentContainerView) {
        previewContainer = container
        container.post { requestPreviewRefresh() }
    }

    private fun requestPreviewRefresh(wait: Duration = Duration.ZERO) {
        val container = previewContainer ?: return
        previewRefresh?.cancel()
        previewRefresh =
            launchCatchingTask {
                delay(wait)
                val model = tempNoteType ?: return@launchCatchingTask
                if (!container.isAttachedToWindow || supportFragmentManager.isStateSaved || isFinishing) return@launchCatchingTask
                val showingAnswer =
                    wait > Duration.ZERO &&
                        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? TemplatePreviewerFragment)
                            ?.viewModel
                            ?.showingAnswer
                            ?.value == true
                val args = createPreviewArguments(model.notetype, ord, showingAnswer)
                if (!container.isAttachedToWindow || supportFragmentManager.isStateSaved || isFinishing) {
                    args.sourceFile.delete()
                    return@launchCatchingTask
                }
                supportFragmentManager.commitNow {
                    replace(R.id.fragment_container, TemplatePreviewerFragment.newInstance(args, inTemplateEditor = true))
                }
                previewFile?.delete()
                previewFile = args.sourceFile
            }
    }

    private suspend fun createPreviewArguments(
        noteType: NotetypeJson,
        position: CardOrdinal,
        showingAnswer: Boolean = false,
    ): TemplatePreviewerArguments {
        val note = withCol { if (previewNoteId > 0) getNote(previewNoteId) else Note.fromNotetypeId(this, noteType.id) }
        return TemplatePreviewerArguments(
            notetypeFile = NotetypeFile(this, noteType),
            id = note.id,
            ord = position,
            fields = note.fields.toList(),
            tags = note.tags.toList(),
            fillEmpty = note.id == 0L,
            fromTemplateEditor = true,
            usingFirstNote = note.id != 0L && noteId <= 0,
            initiallyShowingAnswer = showingAnswer,
        )
    }

    private fun performPreview() {
        launchCatchingTask {
            val args = createPreviewArguments(tempModel.notetype, ord)
            startActivity(TemplatePreviewerPage.getIntent(this@CardTemplateEditor, args))
        }
    }

    private fun insertField(field: String) {
        if (section == EditorViewType.STYLING) return
        val editor = codeEditor ?: return
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        editor.text?.replace(minOf(start, end), maxOf(start, end), field)
        editor.requestFocus()
    }

    private fun showInsertFieldDialog() {
        if (section == EditorViewType.STYLING) return
        val template = tempModel.getTemplate(ord)
        val side = if (section == EditorViewType.BACK) SingleCardSide.BACK else SingleCardSide.FRONT
        val originalOrdinal = tempModel.getOriginalOrdinal(ord)
        launchCatchingTask {
            val dialog =
                InsertFieldDialog.newInstance(
                    fieldItems = fieldNames,
                    metadata =
                        InsertFieldMetadata.query(
                            side = side,
                            noteId = noteId.takeIf { it > 0 },
                            ord = originalOrdinal,
                            cardTemplateName = template.name,
                            noteTypeName = noteTypeName,
                        ),
                    requestKey = INSERT_FIELD_REQUEST_KEY,
                )
            showDialogFragment(dialog)
        }
    }

    private fun openBrowserAppearance() {
        val template = tempModel.getTemplate(ord)
        browserAppearanceResult.launch(CardTemplateBrowserAppearanceEditor.getIntentFromTemplate(this, template))
    }

    private fun displayDeckOverrideDialog() {
        if (dynamicTemplates) return
        startDeckSelection(
            title = getString(CommonString.card_template_editor_deck_override),
            templateEditorMessage = getString(CommonString.deck_override_explanation, tempModel.getTemplate(ord).name),
            allowAll = false,
            allowFiltered = false,
        )
    }

    private fun copyMarkdownTemplateToClipboard() {
        val template = tempModel.getTemplate(ord)
        val markdown = CardTemplate(template.qfmt, template.afmt, tempModel.css).toMarkdown(this)
        copyToClipboard(TruncatedString.from(markdown))
    }

    private fun saveNoteType() {
        val noteType = tempNoteType ?: return
        if (saving) return
        if (!noteTypeHasChanged()) {
            finish()
            return
        }
        saving = true
        launchCatchingTask(getString(CommonString.card_template_editor_save_error)) {
            try {
                withProgress(getString(CommonString.saving_model)) {
                    noteType.saveToDatabase()
                }
                onModelSaved()
            } finally {
                saving = false
            }
        }
    }

    private fun onModelSaved() {
        tempNoteType = null
        finish()
    }

    private fun deleteTemplate(
        template: BackendCardTemplate,
        noteType: NotetypeJson,
    ) {
        rememberCursor()
        val positions = (0 until tempModel.templateCount).filter { it != template.ord }
        val templates = CardTemplates(JSONArray())
        positions.forEach { templates.append(tempModel.getTemplate(it)) }
        tempModel.removeTemplate(template.ord)
        noteType.templates = templates
        Notetypes._updateTemplOrds(noteType)
        reorderEditorState(positions)
        showTemplate(templates.length() - 1)
        refreshTemplateNames()
    }

    private fun addNewTemplate(
        noteType: NotetypeJson,
        template: BackendCardTemplate,
    ) {
        rememberCursor()
        noteType.templates.append(template)
        tempModel.addNewTemplate()
        showTemplate(tempModel.templateCount - 1)
        refreshTemplateNames()
    }

    private fun runAction(action: TemplateEditorAction) {
        if (tempNoteType == null || saving) return
        when (action) {
            TemplateEditorAction.PREVIEW -> performPreview()
            TemplateEditorAction.SAVE -> saveNoteType()
            TemplateEditorAction.INSERT_FIELD -> showInsertFieldDialog()
            TemplateEditorAction.ADD -> addCardTemplate()
            TemplateEditorAction.RENAME -> showRenameDialog()
            TemplateEditorAction.REPOSITION -> showRepositionDialog()
            TemplateEditorAction.DELETE -> deleteCardTemplate()
            TemplateEditorAction.DECK_OVERRIDE -> displayDeckOverrideDialog()
            TemplateEditorAction.BROWSER_APPEARANCE -> openBrowserAppearance()
            TemplateEditorAction.COPY -> copyMarkdownTemplateToClipboard()
            TemplateEditorAction.RESTORE -> restoreToDefault()
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (!event.isCtrlPressed || tempNoteType == null) return super.onKeyUp(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_1 -> selectSection(EditorViewType.FRONT)
            KeyEvent.KEYCODE_2 -> selectSection(EditorViewType.BACK)
            KeyEvent.KEYCODE_3 -> selectSection(EditorViewType.STYLING)
            KeyEvent.KEYCODE_P -> runAction(TemplateEditorAction.PREVIEW)
            KeyEvent.KEYCODE_S -> runAction(TemplateEditorAction.SAVE)
            KeyEvent.KEYCODE_I -> runAction(TemplateEditorAction.INSERT_FIELD)
            KeyEvent.KEYCODE_N -> runAction(TemplateEditorAction.ADD)
            KeyEvent.KEYCODE_R -> runAction(TemplateEditorAction.RENAME)
            KeyEvent.KEYCODE_B -> runAction(TemplateEditorAction.BROWSER_APPEARANCE)
            KeyEvent.KEYCODE_D -> runAction(TemplateEditorAction.DELETE)
            KeyEvent.KEYCODE_O -> runAction(TemplateEditorAction.DECK_OVERRIDE)
            KeyEvent.KEYCODE_M -> runAction(TemplateEditorAction.COPY)
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }

    fun noteTypeHasChanged(): Boolean {
        // insets and 'Up' can both arrive before the note type is loaded: answer them without
        // opening the collection
        val tempNoteType = tempNoteType ?: return false
        val oldNoteType = getColUnsafe.notetypes.get(noteTypeId) ?: return true
        return !tempNoteType.notetype.jsonObject.contentEquals(oldNoteType.jsonObject)
    }

    private fun showDiscardChangesDialog() =
        DiscardChangesDialog.showDialog(this) {
            Timber.i("TemplateEditor:: OK button pressed to confirm discard changes")
            // Clear the edited note type from any cache files, and clear it from this objects memory to discard changes
            CardTemplateNotetype.clearTempNoteTypeFiles()
            tempNoteType = null
            finish()
        }

    /** When a deck is selected via Deck Override  */
    fun onDeckSelected(deck: SelectableDeck?) {
        require(deck is SelectableDeck.Deck?)
        val tempNoteType = tempNoteType ?: return
        if (tempNoteType.notetype.isCloze) {
            Timber.w("Attempted to set deck for cloze note type")
            showEditorMessage(getString(CommonString.multimedia_editor_something_wrong))
            return
        }

        val template = tempNoteType.getTemplate(ord)
        val templateName = template.name

        if (deck != null && getColUnsafe.decks.isFiltered(deck.deckId)) {
            Timber.w("Attempted to set default deck of %s to dynamic deck %s", templateName, deck.name)
            showEditorMessage(getString(CommonString.multimedia_editor_something_wrong))
            return
        }

        val message: String =
            if (deck == null) {
                Timber.i("Removing default template from template '%s'", templateName)
                template.jsonObject.put("did", JSONObject.NULL)
                getString(CommonString.model_manager_deck_override_removed_message, templateName)
            } else {
                Timber.i("Setting template '%s' to '%s'", templateName, deck.name)
                template.jsonObject.put("did", deck.deckId)
                getString(CommonString.model_manager_deck_override_added_message, templateName, deck.name)
            }

        showEditorMessage(message)

        // Deck Override can change from "on" <-> "off"
        refreshTemplateNames()
        updateDiscardChangesCallback()
    }

    private fun showEditorMessage(text: String) {
        lifecycleScope.launch { snackbar.showSnackbar(text) }
    }

    override val shortcuts
        get() =
            ShortcutGroup(
                listOf(
                    shortcut("Ctrl+P", CommonString.card_editor_preview_card),
                    shortcut("Ctrl+1", CommonString.edit_front_template),
                    shortcut("Ctrl+2", CommonString.edit_back_template),
                    shortcut("Ctrl+3", CommonString.edit_styling),
                    shortcut("Ctrl+S", CommonString.save),
                    shortcut("Ctrl+I", CommonString.card_template_editor_insert_field),
                    shortcut("Ctrl+N", Translations::cardTemplatesAddCardType),
                    shortcut("Ctrl+R", Translations::cardTemplatesRenameCardType),
                    shortcut("Ctrl+B", CommonString.edit_browser_appearance),
                    shortcut("Ctrl+D", Translations::cardTemplatesRemoveCardType),
                    shortcut("Ctrl+O", Translations::cardTemplatesDeckOverride),
                    shortcut("Ctrl+M", CommonString.copy_the_template),
                ),
                CommonString.card_template_editor_group,
            )

    @NeedsTest("Cancellation")
    @NeedsTest("Prefill is correct")
    @NeedsTest("Does not work for Cloze/Occlusion")
    @NeedsTest("UI is updated on success")
    private fun showRenameDialog() {
        if (dynamicTemplates) {
            Timber.w("attempted to rename a dynamic note type")
            return
        }
        val ordinal = ord
        val template = tempModel.getTemplate(ordinal)

        // obtain the current names (potentially unsaved)
        val existingNames = tempModel.notetype.templates.map { CardTypeName.fromString(it.name) }

        RenameCardTypeDialog.showInstance(
            this@CardTemplateEditor,
            prefill = template.name,
            currentName = CardTypeName.fromString(template.name),
            existingNames = existingNames,
        ) { newName ->
            template.name = newName.value
            updateDiscardChangesCallback()
            Timber.i("updated card template name")
            Timber.d("updated name of template %d to '%s'", ordinal, newName)

            refreshTemplateNames()
            requestPreviewRefresh()
        }
    }

    private fun showRepositionDialog() {
        if (dynamicTemplates || tempModel.templateCount < 2) return
        val currentPosition = ord
        RepositionCardTemplateDialog.showInstance(
            this@CardTemplateEditor,
            tempModel.templateCount,
        ) { newPosition ->
            val newOrdinal = newPosition - 1
            if (newOrdinal == currentPosition) return@showInstance
            Timber.i("moving card template %d to %d", currentPosition, newOrdinal)
            executeWithSyncCheck(Runnable { repositionTemplate(currentPosition, newOrdinal) })
        }
    }

    private fun deleteCardTemplate() {
        lifecycleScope.launch {
            val tempModel = tempNoteType ?: return@launch
            val ordinal = ord
            val template = tempModel.getTemplate(ordinal)
            // Don't do anything if only one template
            if (tempModel.templateCount < 2) {
                showSimpleMessageDialog(resources.getString(CommonString.card_template_editor_cant_delete))
                return@launch
            }

            if (deletionWouldOrphanNote(tempModel, ordinal)) {
                showOrphanNoteDialog()
                return@launch
            }

            // Show confirmation dialog
            val numAffectedCards =
                if (!CardTemplateNotetype.isOrdinalPendingAdd(tempModel, ordinal)) {
                    Timber.d("Ordinal is not a pending add, so we'll get the current card count for confirmation")
                    withCol { notetypes.tmplUseCount(tempModel.notetype, tempModel.getOriginalOrdinal(ordinal)!!) }
                } else {
                    0
                }
            confirmDeleteCards(template, tempModel.notetype, numAffectedCards)
        }
    }

    private fun showOrphanNoteDialog() {
        createPaperDialog(this@CardTemplateEditor) { dialog ->
            ErrorDialog(
                title = getString(CommonString.orphan_note_title),
                message = getString(CommonString.orphan_note_message),
                onDismissRequest = dialog::dismiss,
                dismissText = null,
                primaryAction = DialogAction(getString(android.R.string.ok), dialog::dismiss),
            )
        }.show()
    }

    private fun addCardTemplate() {
        if (dynamicTemplates) {
            Timber.w("addCardTemplate attempted on cloze note type")
            return
        }
        val noteType = tempNoteType!!.notetype
        val newTemplate = buildNewTemplate(noteType)
        launchCatchingTask {
            // Show confirmation dialog
            val numAffectedCards = withProgress { withCol { countCardsCreatedBy(noteType, newTemplate) } }
            confirmAddCards(noteType, newTemplate, numAffectedCards)
        }
    }

    @NeedsTest("Notetype is restored to stock kind")
    private suspend fun restoreNotetypeToStock(kind: StockNotetype.Kind? = null) {
        val nid = notetypeId { ntid = tempModel.noteTypeId }
        undoableOp { notetypes.restoreNotetypeToStock(nid, kind) }
        onModelSaved()
        showThemedToast(
            this@CardTemplateEditor,
            TR.cardTemplatesRestoredToDefault(),
            shortLength = false,
        )
    }

    private fun restoreToDefault() {
        fun askUser(kind: StockNotetype.Kind? = null) {
            createPaperDialog(this@CardTemplateEditor) { dialog ->
                PaperConfirmationDialog(
                    title = TR.sentenceCase.restoreToDefault,
                    message = TR.cardTemplatesRestoreToDefaultConfirmation(),
                    confirm =
                        DialogAction(getString(CommonString.restore), {
                            dialog.dismiss()
                            launchCatchingTask { restoreNotetypeToStock(kind) }
                        }, DialogActionStyle.Destructive),
                    onDismissRequest = dialog::dismiss,
                )
            }.show()
        }

        val originalStockKind = tempModel.notetype.originalStockKind
        if (originalStockKind != ORIGINAL_STOCK_KIND_UNKNOWN_VALUE) {
            Timber.d("Asking to restore to original stock kind %s", originalStockKind)
            askUser()
            return
        }

        launchCatchingTask {
            Timber.d("Unknown stock kind: asking which kind to restore to")
            val stockNotetypeKinds = getStockNotetypeKinds()
            val stockNotetypesNames =
                withCol {
                    stockNotetypeKinds.map { getStockNotetype(it).name }
                }
            createPaperDialog(this@CardTemplateEditor) { dialog ->
                SingleChoiceDialog(
                    title = TR.sentenceCase.restoreToDefault,
                    choices = stockNotetypesNames.map { DialogChoice(it) },
                    selectedIndex = null,
                    onSelect = { index ->
                        dialog.dismiss()
                        askUser(stockNotetypeKinds[index])
                    },
                    onDismissRequest = dialog::dismiss,
                )
            }.show()
        }
    }

    private suspend fun deletionWouldOrphanNote(
        tempModel: CardTemplateNotetype,
        position: Int,
    ): Boolean {
        // For existing templates, make sure we won't leave orphaned notes if we delete the template
        //
        // Note: we are in-memory, so the database is unaware of previous but unsaved deletes.
        // If we were deleting a template we just added, we don't care. If not, then for every
        // template delete queued up, we check the database to see if this delete in combo with any other
        // pending deletes could orphan cards
        if (!CardTemplateNotetype.isOrdinalPendingAdd(tempModel, position)) {
            val currentDeletes = tempModel.getDeleteDbOrds(position)
            val cardIds = withCol { notetypes.getCardIdsForNoteType(tempModel.noteTypeId, currentDeletes) }
            if (cardIds == null) {
                // It is possible but unlikely that a user has an in-memory template addition that would
                // generate cards making the deletion safe, but we don't handle that. All users who do
                // not already have cards generated making it safe will see this error message:
                return true
            }
        }
        return false
    }

    /**
     * Confirm if the user wants to delete all the cards associated with current template
     *
     * @param tmpl template to remove
     * @param notetype note type to remove template from, modified in place by reference
     * @param numAffectedCards number of cards which will be affected
     */
    private fun confirmDeleteCards(
        tmpl: BackendCardTemplate,
        notetype: NotetypeJson,
        numAffectedCards: Int,
    ) {
        val d = ConfirmationDialog()
        val msg =
            String.format(
                resources.getQuantityString(
                    CommonPlurals.card_template_editor_confirm_delete,
                    numAffectedCards,
                ),
                numAffectedCards,
                tmpl.jsonObject.optString("name"),
            )
        d.setArgs(
            title = getString(CommonString.delete_card_type),
            message = msg,
            positiveButtonText = getString(CommonString.dialog_positive_delete),
            destructive = true,
        )

        val deleteCard = Runnable { deleteTemplate(tmpl, notetype) }
        val confirm = Runnable { executeWithSyncCheck(deleteCard) }
        d.setConfirm(confirm)
        showDialogFragment(d)
    }

    /**
     * Confirm if the user wants to add new card template
     * @param notetype note type to add new template and modified in place by reference
     * @param numAffectedCards number of cards which will be affected
     */
    private fun confirmAddCards(
        notetype: NotetypeJson,
        newTemplate: BackendCardTemplate,
        numAffectedCards: Int,
    ) {
        val d = ConfirmationDialog()
        val msg =
            String.format(
                resources.getQuantityString(
                    CommonPlurals.card_template_editor_confirm_add,
                    numAffectedCards,
                ),
                numAffectedCards,
            )
        d.setArgs(
            title = getString(CommonString.add_card_type),
            message = msg,
            positiveButtonText = getString(CommonString.menu_add),
        )

        val addCard = Runnable { addNewTemplate(notetype, newTemplate) }
        val confirm = Runnable { executeWithSyncCheck(addCard) }
        d.setConfirm(confirm)
        showDialogFragment(d)
    }

    /**
     * Execute an action on the schema, asking the user to confirm that a full sync is ok
     * If [schemaChangingAction] is successfully executed, then the template is reloaded.
     *
     * This method is always useful because all calls to executeWithSyncCheck may need to refresh the previewer.
     * Due to conditional generation (e.g., {{#c5}}foo{{/c5}} which is non-empty only if it's the 5th card and is
     * empty otherwise), it's important to reload the template. This is particularly useful for cloze types,
     * where a card can move from the 5th to the 6th position due to adding an extra card type, causing content
     * to change or be deleted.
     *
     * @param schemaChangingAction The action to execute (adding / removing card)
     */
    private fun executeWithSyncCheck(schemaChangingAction: Runnable) {
        try {
            getColUnsafe.modSchema(check = true)
            schemaChangingAction.run()
            updateDiscardChangesCallback()
            requestPreviewRefresh()
        } catch (e: ConfirmModSchemaException) {
            e.log()
            val d = ConfirmationDialog()
            d.setArgs(resources.getString(CommonString.full_sync_confirmation))
            val confirm =
                Runnable {
                    getColUnsafe.modSchema(check = false)
                    schemaChangingAction.run()
                    updateDiscardChangesCallback()
                    requestPreviewRefresh()
                    dismissAllDialogFragments()
                }
            val cancel = Runnable { dismissAllDialogFragments() }
            d.setConfirm(confirm)
            d.setCancel(cancel)
            showDialogFragment(d)
        }
    }

    private fun buildNewTemplate(noteType: NotetypeJson): BackendCardTemplate {
        val oldCardIndex = ord
        val templates = noteType.templates
        val oldTemplate = templates[oldCardIndex]
        val newTemplate = Notetypes.newTemplate(newCardName(templates))
        // Set up question & answer formats
        newTemplate.qfmt = oldTemplate.qfmt
        newTemplate.afmt = oldTemplate.afmt
        // Reverse the front and back if only one template
        if (templates.length() == 1) {
            flipQA(newTemplate)
        }
        val lastExistingOrd = templates.last().ord
        Timber.d("buildNewTemplate() lastExistingOrd was %s", lastExistingOrd)
        newTemplate.setOrd(lastExistingOrd + 1)
        return newTemplate
    }

    /**
     * Flip the question and answer side of the template
     * @param template template to flip
     */
    @KotlinCleanup("Use Kotlin's Regex methods")
    private fun flipQA(template: BackendCardTemplate) {
        val qfmt = template.qfmt
        val afmt = template.afmt
        val m = Pattern.compile("(?s)(.+)<hr id=answer>(.+)").matcher(afmt)
        template.qfmt =
            if (!m.find()) {
                afmt.replace("{{FrontSide}}", "")
            } else {
                m.group(2)!!.trim()
            }
        template.afmt = "{{FrontSide}}\n\n<hr id=answer>\n\n$qfmt"
    }

    /**
     * Get name for new template
     * @param templates array of templates which is being added to
     * @return name for new template
     */
    private fun newCardName(templates: CardTemplates): String {
        // Start by trying to set the name to "Card n" where n is the new num of templates
        var n = templates.length() + 1
        // If the starting point for name already exists, iteratively increase n until we find a unique name
        while (true) {
            // Get new name
            val name = TR.cardTemplatesCard(n)
            // Cycle through all templates checking if new name exists
            if (templates.all { name != it.name }) {
                return name
            }
            n += 1
        }
    }

    data class CardTemplate(
        val front: String,
        val back: String,
        val style: String,
    ) {
        fun toMarkdown(context: Context) =
            // backticks are not supported by old reddit
            with(context) {
                buildString {
                    appendLine("**${TR.sentenceCase.frontTemplate}**\n")
                    appendLine("```html\n$front\n```\n")
                    appendLine("**${TR.sentenceCase.backTemplate}**\n")
                    appendLine("```html\n$back\n```\n")
                    appendLine("**${TR.cardTemplatesTemplateStyling()}**\n")
                    append("```css\n$style\n```")
                }
            }
    }

    companion object {
        private const val STATE_TAB_TO_CURSOR_POSITION = "templateCursorPositions"
        private const val STATE_TAB_TO_VIEW_ID = "templateSections"
        const val EXTRA_NOTE_TYPE_ID = "noteTypeId"
        private const val EXTRA_NOTE_ID = "noteId"
        private const val EXTRA_START_ORD_ID = "ordId"
        private const val STATE_NOTE_TYPE_ID = "noteTypeId"
        private const val STATE_NOTE_ID = "noteId"
        private const val STATE_STARTING_TEMPLATE_ORDINAL = "ordId"
        private const val INSERT_FIELD_REQUEST_KEY = "card_template_insert_field"
        private val REFRESH_PREVIEW_DELAY = 1.seconds

        @CheckResult
        fun getIntent(
            context: Context,
            noteTypeId: NoteTypeId,
            noteId: NoteId? = null,
            ord: CardOrdinal? = null,
        ) = Intent(context, CardTemplateEditor::class.java)
            .apply {
                putExtra(EXTRA_NOTE_TYPE_ID, noteTypeId)
                noteId?.let { putExtra(EXTRA_NOTE_ID, it) }
                ord?.let { putExtra(EXTRA_START_ORD_ID, it) }

                Timber.d(
                    "Built intent for CardTemplateEditor; ntid: %s; nid: %s; ord: %s",
                    noteTypeId,
                    noteId,
                    ord,
                )
            }
    }
}

@VisibleForTesting
internal fun Collection.countCardsCreatedBy(
    noteType: NotetypeJson,
    template: BackendCardTemplate,
    timeout: Duration = 1.seconds,
): Int {
    val deadline = TimeSource.Monotonic.markNow() + timeout
    val unreferencedOrd = generateSequence(0) { it + 1 }.first { "c${it + 1}" !in template.qfmt }
    val templateBytes = BackendUtils.toJsonBytes(template.deepClone().apply { setOrd(unreferencedOrd) })
    val noteIds = notetypes.nids(noteType)
    return try {
        noteIds.count { nid ->
            if (deadline.hasPassedNow()) {
                Timber.w("counting the cards of a new card type timed out")
                Analytics.sendAnalyticsException(TimeoutException("New card type count timed out with ${noteIds.size} notes"), false)
                return noteIds.size
            }
            !backend
                .renderUncommittedCardLegacy(getNote(nid).toBackendNote(), unreferencedOrd, templateBytes, false, true)
                .isEmpty
        }
    } catch (e: BackendTemplateException) {
        Timber.w(e, "new card type has a template error")
        0
    }
}
