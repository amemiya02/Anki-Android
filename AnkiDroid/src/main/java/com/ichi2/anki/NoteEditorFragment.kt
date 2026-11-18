// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>
// SPDX-FileCopyrightText: Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.NoCopySpan
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.os.BundleCompat
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.draganddrop.DropHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import anki.notes.NoteFieldsCheckResponse
import anki.search.SearchNode
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.NoteEditorFragment.Companion.NoteEditorCaller.Companion.fromValue
import com.ichi2.anki.android.input.ShortcutGroup
import com.ichi2.anki.android.input.ShortcutGroupProvider
import com.ichi2.anki.android.input.shortcut
import com.ichi2.anki.browser.CardBrowserViewModel
import com.ichi2.anki.common.android.animationEnabled
import com.ichi2.anki.common.android.appContext
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.common.destinations.DeferredNavigation
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.destinations.toBundle
import com.ichi2.anki.common.json.NamedJSONComparator
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.ui.TransitionDirection
import com.ichi2.anki.common.utils.HashUtil
import com.ichi2.anki.common.utils.android.digit
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.annotation.KotlinCleanup
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.common.utils.ext.AddingDefaultsMode
import com.ichi2.anki.common.utils.ext.addingDefaultsMode
import com.ichi2.anki.common.utils.ext.getParcelableExtraCompat
import com.ichi2.anki.common.utils.ext.ifZero
import com.ichi2.anki.common.utils.ext.pasteImagesAsPng
import com.ichi2.anki.compat.CompatHelper.Companion.getSerializableCompat
import com.ichi2.anki.databinding.FragmentNoteEditorBinding
import com.ichi2.anki.dialogs.ChangeNoteTypeDialog
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.IntegerDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.dialogs.tags.TagsDialogFactory
import com.ichi2.anki.dialogs.tags.TagsDialogListener
import com.ichi2.anki.exception.toBytesShortString
import com.ichi2.anki.libanki.Card
import com.ichi2.anki.libanki.CardId
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.anki.libanki.Collection
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.Field
import com.ichi2.anki.libanki.Fields
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.Note.ClozeUtils
import com.ichi2.anki.libanki.NoteTypeId
import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.libanki.Notetypes
import com.ichi2.anki.libanki.Utils
import com.ichi2.anki.libanki.clozeNumbersInNote
import com.ichi2.anki.model.CardStateFilter
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.multimedia.MultimediaBottomSheet
import com.ichi2.anki.multimedia.MultimediaResult
import com.ichi2.anki.multimedia.MultimediaResultContract
import com.ichi2.anki.multimedia.MultimediaUtils.canTakePicture
import com.ichi2.anki.multimedia.MultimediaUtils.createImageFile
import com.ichi2.anki.multimedia.MultimediaViewModel
import com.ichi2.anki.multimediacard.impl.MultimediaEditableNote
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.noteeditor.CustomToolbarButton
import com.ichi2.anki.noteeditor.FieldState
import com.ichi2.anki.noteeditor.FieldState.FieldChangeType
import com.ichi2.anki.noteeditor.FieldState.Type
import com.ichi2.anki.noteeditor.ImageOcclusionActions
import com.ichi2.anki.noteeditor.NoteEditorAppBar
import com.ichi2.anki.noteeditor.NoteEditorDetails
import com.ichi2.anki.noteeditor.NoteEditorFragmentDelegate
import com.ichi2.anki.noteeditor.NoteEditorMenuAction
import com.ichi2.anki.noteeditor.NoteEditorMultimediaController
import com.ichi2.anki.noteeditor.NoteEditorSelectors
import com.ichi2.anki.noteeditor.Toolbar
import com.ichi2.anki.noteeditor.Toolbar.TextFormatListener
import com.ichi2.anki.noteeditor.Toolbar.TextWrapper
import com.ichi2.anki.noteeditor.ToolbarLayoutDialogHost
import com.ichi2.anki.noteeditor.data.ToolbarPreferences
import com.ichi2.anki.noteeditor.presentation.ToolbarLayoutMode
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.pages.ImageOcclusion
import com.ichi2.anki.pages.viewmodel.ImageOcclusionArgs
import com.ichi2.anki.previewer.TemplatePreviewerArguments
import com.ichi2.anki.previewer.TemplatePreviewerPage
import com.ichi2.anki.servicelayer.LanguageHintService.languageHint
import com.ichi2.anki.servicelayer.NoteService
import com.ichi2.anki.servicelayer.NoteService.convertToHtmlNewline
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.snackbar.BaseSnackbarBuilderProvider
import com.ichi2.anki.snackbar.SnackbarBuilder
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.HanUnificationDetector
import com.ichi2.anki.utils.RunOnlyOnce
import com.ichi2.anki.utils.bottomCornerSideClearance
import com.ichi2.anki.utils.doOnApplyWindowInsets
import com.ichi2.anki.utils.ext.getLongOrNull
import com.ichi2.anki.utils.ext.requireLong
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.utils.insetsOf
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.imagecropper.ImageCropper
import com.ichi2.imagecropper.ImageCropper.Companion.EXTRA_CROP_IMAGE_RESULT
import com.ichi2.imagecropper.ImageCropperLauncher
import com.ichi2.utils.ClipboardUtil
import com.ichi2.utils.ClipboardUtil.MEDIA_MIME_TYPES
import com.ichi2.utils.ClipboardUtil.hasMedia
import com.ichi2.utils.ClipboardUtil.items
import com.ichi2.utils.ImportUtils
import com.ichi2.utils.IntentUtil.resolveMimeType
import com.ichi2.utils.NoteFieldDecorator
import com.ichi2.utils.TextViewUtil
import com.ichi2.utils.configureView
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch
import net.ankiweb.rsdroid.Backend
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.util.LinkedList
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.ichi2.anki.common.android.R as CommonR

/**
 * Allows the user to edit a note, for instance if there is a typo. A card is a presentation of a note, and has two
 * sides: a question and an answer. Any number of fields can appear on each side. When you add a note to Anki, cards
 * which show that note are generated. Some models generate one card, others generate more than one.
 * Features:
 * - Implements [MenuHost] ([onCreateMenu]/[onPrepareMenu]) to handle toolbar menu item clicks.
 * - Implements [DispatchKeyEventListener] to handle key events.
 *
 * @see [the Anki Desktop manual](https://docs.ankiweb.net/getting-started.html.cards)
 */
@KotlinCleanup("Go through the class and select elements to fix")
@KotlinCleanup("see if we can lateinit")
@NeedsTest("19733")
class NoteEditorFragment :
    Fragment(R.layout.fragment_note_editor),
    TagsDialogListener,
    BaseSnackbarBuilderProvider,
    DispatchKeyEventListener,
    MenuProvider,
    ShortcutGroupProvider {
    @VisibleForTesting
    internal val binding by viewBinding(FragmentNoteEditorBinding::bind)

    private var bottomInsetPx = 0

    /** Whether any change are saved. E.g. multimedia, new card added, field changed and saved. */
    private var changed = false
    private var isTagsEdited = false
    private var isFieldEdited = false
    private var addNoteJob = RunOnlyOnce(scope = lifecycleScope)

    /**
     * The field values of a new note immediately after they were last (re)populated -
     * e.g. after a sticky field was carried over from the previous note. Used so
     * [hasUnsavedChanges] can tell sticky-carried-over content apart from an actual edit.
     */
    private var addNoteFieldBaseline: List<String> = emptyList()

    private val getColUnsafe: Collection
        get() = CollectionManager.getColUnsafe()

    /**
     * Flag which forces the calling activity to rebuild it's definition of current card from scratch
     */
    private var reloadRequired = false

    private var fieldsLayoutContainer: LinearLayout? = null
    private var tagsDialogFactory: TagsDialogFactory? = null

    @VisibleForTesting
    internal var selectedDeckName by mutableStateOf("")
        private set

    @VisibleForTesting
    internal var selectedNoteTypeName by mutableStateOf("")
        private set
    private var showCardDeckLabel by mutableStateOf(false)
    private var showSave by mutableStateOf(true)
    private var canCopyNote by mutableStateOf(false)
    private var capitalizeSentences by mutableStateOf(true)
    private var displayTags by mutableStateOf<List<String>>(emptyList())
    private var templateNames by mutableStateOf<List<String>>(emptyList())
    private var activeTemplateName by mutableStateOf<String?>(null)
    private var firstFieldLine: FieldEditLine? = null
    private var cardIdsAfterNoteTypeChange: List<CardId>? = null

    private var showImageOcclusionActions by mutableStateOf(false)

    // non-null after onCollectionLoaded
    @VisibleForTesting
    var editorNote: Note? = null
        private set

    internal val multimediaViewModel: MultimediaViewModel by activityViewModels()

    private var currentImageOccPath: String? = null

    // Null if adding a new card. Presently NonNull if editing an existing note - but this is subject to change
    var currentEditedCard: Card? = null
        private set
    var selectedTags: MutableList<String>? = null
        private set

    @get:VisibleForTesting
    var deckId: DeckId = 0
        private set

    /** Local selection while adding; [Collection.addNote] remembers it after a successful save. */
    private var selectedNoteTypeId: NoteTypeId? = null

    private val customViewIds = ArrayList<Int>()

    // indicates if a new note is added or a card is edited
    private var addNote = false
    private var aedictIntent = false

    // indicates which activity called Note Editor
    private var caller = NoteEditorCaller.NO_CALLER
    var editFields: LinkedList<FieldEditText>? = null
        private set
    private var sourceText: Array<String?>? = null
    private val fieldState = FieldState.fromEditor(this)
    private lateinit var toolbar: Toolbar
    private var lastFocusedField: FieldEditText? = null
    private val toolbarPreferences by lazy { ToolbarPreferences(sharedPrefs()) }
    private val toolbarLayoutDialog = ToolbarLayoutDialogHost()

    // save field index as key and text as value when toggle sticky clicked in Field Edit Text
    @VisibleForTesting
    var toggleStickyText: HashMap<Int, String?> = HashMap()
        private set

    var clipboard: ClipboardManager? = null

    private var delegate: NoteEditorFragmentDelegate? = null
    private val inNoteEditorActivity: Boolean
        get() = noteEditorHost != null

    private val noteEditorHost
        get() = parentFragment as? NoteEditorPaneFragment

    private val launchIntent: Intent
        get() = noteEditorHost?.launchIntent ?: requireActivity().intent

    // List of affected cards (siblings) when editing notes.
    private val cardIdsFromArguments: LongArray?
        get() = arguments?.getLongArray(EXTRA_CARD_IDS)

    /**
     * Whether this is displayed in a fragment view.
     * If true, this fragment is on the trailing side of the card browser.
     */
    private val inCardBrowserActivity
        get() = requireArguments().getBoolean(EXTRA_IN_CARD_BROWSER_ACTIVITY)

    private val requestAddLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            NoteEditorActivityResultCallback {
                if (it.resultCode != RESULT_CANCELED) {
                    changed = true
                }
            },
        )

    private val multimediaFragmentLauncher =
        registerForActivityResult(MultimediaResultContract()) { result ->
            when (result) {
                is MultimediaResult.Cancelled -> {
                    Timber.d("Multimedia result canceled")
                    multimediaController.handleActions(result.fieldIndex)
                }
                is MultimediaResult.Success -> {
                    Timber.d("Getting multimedia result")
                    multimediaController.handleResult(result)
                }
                null -> Timber.d("Multimedia launcher returned no result")
            }
        }

    @VisibleForTesting
    internal val multimediaController: NoteEditorMultimediaController = NoteEditorMultimediaController(this, multimediaFragmentLauncher)

    private val requestTemplateEditLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            NoteEditorActivityResultCallback { onTemplateEditorResult() },
        )

    /** Handles a return from the [CardTemplateEditor] */
    @VisibleForTesting
    internal fun onTemplateEditorResult() {
        // Note type can change regardless of exit type - update ourselves and CardBrowser
        reloadRequired = true
        editorNote!!.notetype = getColUnsafe.notetypes.get(editorNote!!.noteTypeId)!!
        if (currentEditedCard == null ||
            !editorNote!!
                .cardIds(getColUnsafe)
                .contains(currentEditedCard!!.id)
        ) {
            if (!addNote) {
            /* This can occur, for example, if the
             * card type was deleted or if the note
             * type was changed without moving this
             * card to another type. */
                Timber.d("onActivityResult() template edit return - current card is gone, close note editor")
                showSnackbar(getString(CommonString.template_for_current_card_deleted))
                closeNoteEditor()
            } else {
                Timber.d("onActivityResult() template edit return, in add mode, just re-display")
                // the fields may have changed, so rebuild them
                refreshNoteData(FieldChangeType.changeFieldCount(shouldReplaceNewlines()))
            }
        } else {
            Timber.d("onActivityResult() template edit return - current card exists")
            // reload current card - the template ordinals are possibly different post-edit
            currentEditedCard = getColUnsafe.getCard(currentEditedCard!!.id)
            @NeedsTest("#17282 returning from template editor saves further made changes")
            // make sure the card's note is available going forward
            currentEditedCard!!.note(getColUnsafe)
            editorNote = currentEditedCard!!.note // update the NoteEditor's working note reference
            // the fields may have changed, so rebuild them
            setNote(editorNote, FieldChangeType.changeFieldCount(shouldReplaceNewlines()))
        }
    }

    private val ioEditorLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                ImportUtils.getFileCachedCopy(requireContext(), uri)?.let { path ->
                    setupImageOcclusionEditor(path)
                }
            }
        }

    private val requestIOEditorCloser =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            NoteEditorActivityResultCallback { result ->
                if (result.resultCode != RESULT_CANCELED) {
                    changed = true
                    if (!addNote) {
                        reloadRequired = true
                        closeNoteEditor(RESULT_UPDATED_IO_NOTE, null)
                    }
                }
            },
        )

    /**
     * Listener for handling content received via drag and drop or copy and paste.
     * This listener processes URIs contained in the payload and attempts to paste the content into the target EditText view.
     */
    private val onReceiveContentListener =
        OnReceiveContentListener { view, payload ->
            val (uriContent, remaining) = payload.partition { item -> item.uri != null }

            if (uriContent == null) {
                return@OnReceiveContentListener remaining
            }

            val clip = uriContent.clip
            val description = clip.description

            if (!hasMedia(description)) {
                return@OnReceiveContentListener remaining
            }

            for (uri in clip.items().map { it.uri }) {
                lifecycleScope.launch {
                    try {
                        val pasteAsPng = shouldPasteAsPng()
                        multimediaController.onPaste(view as EditText, uri, description, pasteAsPng)
                    } catch (e: Exception) {
                        Timber.w(e)
                        CrashReportService.sendExceptionReport(e, "NoteEditor::onReceiveContent")
                    }
                }
            }

            return@OnReceiveContentListener remaining
        }

    private class NoteEditorActivityResultCallback(
        private val callback: (result: ActivityResult) -> Unit,
    ) : ActivityResultCallback<ActivityResult> {
        override fun onActivityResult(result: ActivityResult) {
            Timber.d("onActivityResult() with result: %s", result.resultCode)
            callback(result)
        }
    }

    fun onDeckSelected(deck: SelectableDeck?) {
        if (deck == null) {
            return
        }
        require(deck is SelectableDeck.Deck)
        deckId = deck.deckId
        selectedDeckName = deck.name
    }

    private enum class AddClozeType {
        SAME_NUMBER,
        INCREMENT_NUMBER,
    }

    @VisibleForTesting
    var addNoteErrorMessage: String? = null

    private fun displayErrorSavingNote() {
        val errorMessage = snackbarErrorText
        // Anki allows to proceed in case we try to add non cloze text in cloze field with warning,
        // this snackbar helps replicate similar behaviour
        if (errorMessage == TR.addingYouHaveAClozeDeletionNote()) {
            noClozeDialog(errorMessage)
        } else {
            showSnackbar(errorMessage)
        }
    }

    private fun noClozeDialog(errorMessage: String) {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = "",
                message = errorMessage,
                confirm =
                    DialogAction(TR.actionsSave(), {
                        dialog.dismiss()
                        lifecycleScope.launch { saveNoteWithProgress() }
                    }),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    // Sets the delegate to receive callbacks when note editor events occur (ready, saved, changed, etc.)
    // This allows the hosting activity to respond to fragment state changes
    fun setDelegate(delegate: NoteEditorFragmentDelegate) {
        this.delegate = delegate
    }

    @VisibleForTesting
    val snackbarErrorText: String
        get() =
            when {
                addNoteErrorMessage != null -> addNoteErrorMessage!!
                allFieldsHaveContent() -> resources.getString(CommonString.note_editor_no_cards_created_all_fields)
                else -> resources.getString(CommonString.note_editor_no_cards_created)
            }

    override val baseSnackbarBuilder: SnackbarBuilder = {
        val view = this@NoteEditorFragment.view?.findViewById<Toolbar?>(R.id.editor_toolbar)
        if (view?.isVisible == true) {
            anchorView = view
        }
    }

    private fun allFieldsHaveContent() = currentFieldStrings.none { it.isNullOrEmpty() }

    // ----------------------------------------------------------------------------
    // ANDROID METHODS
    // ----------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        tagsDialogFactory = TagsDialogFactory(this).attachToFragmentManager<TagsDialogFactory>(parentFragmentManager)
        super.onCreate(savedInstanceState)
        fieldState.setInstanceState(savedInstanceState)
        val intent = launchIntent
        if (savedInstanceState != null) {
            caller = fromValue(savedInstanceState.getInt(STATE_CALLER))
            addNote = savedInstanceState.getBoolean("addNote")
            isFieldEdited = savedInstanceState.getBoolean("isFieldEdited")
            cardIdsAfterNoteTypeChange = savedInstanceState.getLongArray("convertedCardIds")?.toList()
            deckId = savedInstanceState.getLong("did")
            selectedNoteTypeId =
                if (savedInstanceState.containsKey("noteTypeId")) savedInstanceState.getLong("noteTypeId") else null
            selectedTags = savedInstanceState.getStringArrayList("tags")
            reloadRequired = savedInstanceState.getBoolean(STATE_RELOAD_REQUIRED)
            multimediaController.onRestoreInstanceState(savedInstanceState)
            toggleStickyText =
                savedInstanceState.getSerializableCompat<HashMap<Int, String?>>("toggleSticky")!!
            changed = savedInstanceState.getBoolean(STATE_NOTE_CHANGED)
        } else {
            caller = fromValue(requireArguments().getInt(EXTRA_CALLER, NoteEditorCaller.NO_CALLER.value))
            if (caller == NoteEditorCaller.NO_CALLER) {
                val action = intent.action
                if (ACTION_CREATE_FLASHCARD == action || ACTION_CREATE_FLASHCARD_SEND == action || Intent.ACTION_PROCESS_TEXT == action) {
                    caller = NoteEditorCaller.NOTEEDITOR_INTENT_ADD
                }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Set up toolbar
        toolbar = binding.editorToolbar
        toolbar.apply {
            formatListener =
                TextFormatListener { formatter: Toolbar.TextFormatter ->
                    val field = fieldForFormatting() ?: return@TextFormatListener
                    modifyCurrentSelection(formatter, field)
                }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyBottomInset() }
        }
        toolbarLayoutDialog.bind(binding.toolbarLayoutDialog, toolbarPreferences, savedInstanceState, ::updateToolbar)
        capitalizeSentences = sharedPrefs().getBoolean(PREF_NOTE_EDITOR_CAPITALIZE, true)
        setupEdgeToEdge()
        registerDeckSelectedHandler(REQUEST_DECK_SELECTION_NOTE_EDITOR, ::onDeckSelected)
        try {
            setupEditor(getColUnsafe, savedInstanceState)
            if (addNote) {
                savedInstanceState?.getStringArrayList("addNoteFieldBaseline")?.let { addNoteFieldBaseline = it }
            }
        } catch (ex: RuntimeException) {
            Timber.w(ex, "setupEditor")
            requireAnkiActivity().onCollectionLoadError()
            return
        }
        // TODO this callback doesn't handle predictive back navigation!
        // see #14678, added to temporarily fix for a bug
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            Timber.i("NoteEditor:: onBackPressed()")
            closeCardEditorWithCheck()
        }

        if (!collectionHasLoaded()) return
        setupPresentation()
        // The dialog's arguments survive recreation. Reconnect it to the new editor view.
        (parentFragmentManager.findFragmentByTag(MultimediaBottomSheet.TAG) as? MultimediaBottomSheet)
            ?.let { multimediaController.handleActions(it.fieldIndex) }
        // The split Browser's controls live in its existing app bar.
        if (!inNoteEditorActivity) {
            (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }

    private fun chooseOcclusionImage() {
        try {
            ioEditorLauncher.launch("image/*")
        } catch (_: ActivityNotFoundException) {
            activity?.showSnackbar(CommonString.activity_start_failed)
        }
    }

    private fun pasteOcclusionImage() {
        if (ClipboardUtil.hasImage(clipboard)) {
            val uri = ClipboardUtil.getUri(clipboard)
            val i =
                Intent().apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(requireActivity().contentResolver, uri.toString(), uri)
                }
            ImportUtils.getFileCachedCopy(requireContext(), i)?.let { path ->
                setupImageOcclusionEditor(path)
            }
        } else {
            showSnackbar(TR.editingNoImageFoundOnClipboard())
        }
    }

    private fun setupPresentation() {
        binding.editorSelectors.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    NoteEditorSelectors(
                        noteType = selectedNoteTypeName,
                        deck = selectedDeckName,
                        adding = addNote,
                        isCardDeck = showCardDeckLabel,
                        onNoteTypeClick = ::chooseNoteType,
                        onDeckClick = ::chooseDeck,
                    )
                }
            }
        }
        binding.editorDetails.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    Column {
                        if (showImageOcclusionActions) {
                            ImageOcclusionActions(
                                adding = addNote,
                                canTakePicture = requireContext().canTakePicture(),
                                onGallery = ::chooseOcclusionImage,
                                onCamera = ::dispatchCameraEvent,
                                onPaste = ::pasteOcclusionImage,
                                onEdit = { setupImageOcclusionEditor() },
                            )
                        }
                        NoteEditorDetails(
                            tags = displayTags,
                            templates = templateNames,
                            activeTemplate = activeTemplateName,
                            onTagsClick = ::showTagsDialog,
                            onCardsClick = ::showCardTemplateEditor,
                        )
                    }
                }
            }
        }
        noteEditorHost?.let { activity ->
            activity.binding.editorAppBar.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
                setContent {
                    AnkiDroidTheme {
                        NoteEditorAppBar(
                            adding = addNote,
                            showSave = showSave,
                            showPreview = showSave && !activity.isPreviewerVisible,
                            menuActions = appBarMenuActions(),
                            onBack = ::closeCardEditorWithCheck,
                            onSave = { performEditorAction(R.id.action_save) },
                            onPreview = { performEditorAction(R.id.action_preview) },
                        )
                    }
                }
            }
        }
    }

    private fun appBarMenuActions() =
        buildList {
            if (!addNote) {
                add(appBarAction(CommonString.menu_add_note, R.id.action_add_note_from_note_editor))
                add(appBarAction(CommonString.note_editor_copy_note, R.id.action_copy_note, enabled = canCopyNote))
            }
            add(appBarAction(CommonString.menu_font_size, R.id.action_font_size))
            add(appBarAction(CommonString.note_editor_capitalize, R.id.action_capitalize, checked = capitalizeSentences))
            add(appBarAction(CommonString.note_editor_toolbar_layout, R.id.action_toolbar_layout))
            noteEditorHost?.takeIf { it.hasPreviewerPane }?.let { activity ->
                val label =
                    if (activity.isPreviewerVisible) CommonString.note_editor_hide_previewer else CommonString.note_editor_show_previewer
                add(appBarAction(label, R.id.action_toggle_previewer))
            }
        }

    private fun appBarAction(
        label: Int,
        actionId: Int,
        enabled: Boolean = true,
        checked: Boolean? = null,
    ) = NoteEditorMenuAction(getString(label), { performEditorAction(actionId) }, enabled, checked)

    private fun chooseDeck() {
        startDeckSelection(allowAll = false, allowFiltered = false, requestKey = REQUEST_DECK_SELECTION_NOTE_EDITOR)
    }

    private fun chooseNoteType() {
        if (!addNote) {
            launchChangeNoteTypeDialog()
            return
        }
        val noteTypes = getColUnsafe.notetypes.all().sortedWith(NamedJSONComparator.INSTANCE)
        createPaperDialog(requireContext()) { dialog ->
            SingleChoiceDialog(
                title = getString(CommonString.select_note_type),
                choices = noteTypes.map { DialogChoice(it.name) },
                selectedIndex = noteTypes.indexOfFirst { it.id == editorNote?.noteTypeId },
                onSelect = { position ->
                    dialog.dismiss()
                    changeNoteType(noteTypes[position].id)
                },
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        addInstanceStateToBundle(outState)
        super.onSaveInstanceState(outState)
        toolbarLayoutDialog.onSaveInstanceState(outState)
    }

    private fun addInstanceStateToBundle(savedInstanceState: Bundle) {
        Timber.i("Saving instance")
        savedInstanceState.putInt(STATE_CALLER, caller.value)
        savedInstanceState.putBoolean("addNote", addNote)
        if (addNote) {
            savedInstanceState.putBoolean("isFieldEdited", isFieldEdited)
            savedInstanceState.putStringArrayList("addNoteFieldBaseline", ArrayList(addNoteFieldBaseline))
        }
        savedInstanceState.putLong("did", deckId)
        cardIdsAfterNoteTypeChange?.let { savedInstanceState.putLongArray("convertedCardIds", it.toLongArray()) }
        selectedNoteTypeId?.let { savedInstanceState.putLong("noteTypeId", it) }
        savedInstanceState.putBoolean(STATE_NOTE_CHANGED, changed)
        savedInstanceState.putBoolean(STATE_RELOAD_REQUIRED, reloadRequired)
        savedInstanceState.putIntegerArrayList("customViewIds", customViewIds)
        fieldState.saveInstanceState(savedInstanceState)
        multimediaController.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putSerializable("toggleSticky", toggleStickyText)
        if (selectedTags == null) {
            selectedTags = ArrayList(0)
        }
        savedInstanceState.putStringArrayList("tags", selectedTags?.let { ArrayList(it) })
    }

    // Finish initializing the fragment after the collection has been correctly loaded
    private fun setupEditor(
        col: Collection,
        savedInstanceState: Bundle?,
    ) {
        val intent = launchIntent
        Timber.d("NoteEditor() onCollectionLoaded: caller: %s", caller)
        requireAnkiActivity().registerReceiver()
        fieldsLayoutContainer = requireView().findViewById(R.id.CardEditorEditFieldsLayout)
        try {
            clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        } catch (e: Exception) {
            Timber.w(e)
        }

        aedictIntent = false
        currentEditedCard = null
        when (caller) {
            NoteEditorCaller.NO_CALLER -> {
                Timber.e("no caller could be identified, closing")
                finishScreen()
                return
            }
            NoteEditorCaller.EDIT -> {
                val cardId = cardIdsAfterNoteTypeChange?.firstOrNull() ?: requireArguments().requireLong(EXTRA_CARD_ID)
                currentEditedCard = col.getCard(cardId)
                editorNote = currentEditedCard!!.note(col)
                addNote = false
            }
            NoteEditorCaller.PREVIEWER_EDIT -> {
                val id = cardIdsAfterNoteTypeChange?.firstOrNull() ?: requireArguments().getLong(EXTRA_EDIT_FROM_CARD_ID)
                currentEditedCard = col.getCard(id)
                editorNote = currentEditedCard!!.note(getColUnsafe)
            }
            NoteEditorCaller.STUDYOPTIONS,
            NoteEditorCaller.DECKPICKER,
            NoteEditorCaller.REVIEWER_ADD,
            NoteEditorCaller.CARDBROWSER_ADD,
            NoteEditorCaller.NOTEEDITOR,
            -> {
                addNote = true
            }
            NoteEditorCaller.NOTEEDITOR_INTENT_ADD,
            NoteEditorCaller.INSTANT_NOTE_EDITOR,
            -> {
                fetchIntentInformation(intent)
                if (sourceText == null) {
                    finishScreen()
                    return
                }
                if ("Aedict Notepad" == sourceText!![0] && addFromAedict(sourceText!![1])) {
                    finishScreen()
                    return
                }
                addNote = true
            }
            // image occlusion is handled at the end of this method, grep: CALLER_IMG_OCCLUSION
            // we need to have loaded the current note type
            NoteEditorCaller.IMG_OCCLUSION, NoteEditorCaller.ADD_IMAGE -> {
                addNote = true
            }
        }

        if (addNote) {
            // Initialize both selections from the same defaults
            // https://github.com/ankitects/anki/blob/754ce3a25f608010c0249e074e5d7fe95bda035f/qt/aqt/addcards_legacy.py#L91-L108
            val defaults = col.defaultsForAdding()
            if (selectedNoteTypeId?.let { col.notetypes.get(it) } == null) {
                selectedNoteTypeId = defaults.notetypeId
            }
            // A restored selection takes precedence over the original launch destination.
            deckId = deckId.ifZero { requireArguments().getLong(EXTRA_DID) }
            if (deckId == 0L && caller == NoteEditorCaller.NOTEEDITOR_INTENT_ADD &&
                col.config.addingDefaultsMode == AddingDefaultsMode.USE_CURRENT_DECK
            ) {
                // Sharing has its own destination; adding notes must not change the study deck.
                deckId = Prefs.lastSharedDeckId
            }
            val deck = col.decks.getLegacy(deckId)
            if (deck == null || deck.isFiltered) {
                deckId = defaults.deckId
            }
        } else {
            // When editing we always have a valid currentEditCard. Check to see if it's from a normal
            // deck or a filtered deck(in which case use it's original did or revert to 'Default')
            val cardDeckId = currentEditedCard?.did
            deckId =
                savedInstanceState?.getLongOrNull("did")
                    ?: if (cardDeckId == null || col.decks.isFiltered(cardDeckId)) {
                        currentEditedCard?.oDid ?: Consts.DEFAULT_DECK_ID
                    } else {
                        cardDeckId
                    }
        }
        val getTextFromSearchView = requireArguments().getString(EXTRA_TEXT_FROM_SEARCH_VIEW)
        setNote(
            editorNote,
            FieldChangeType.onActivityCreation(shouldReplaceNewlines()),
            noteTypeId = savedInstanceState?.getLongOrNull("noteTypeId") ?: requireArguments().getLongOrNull(EXTRA_NOTE_TYPE_ID),
        )
        if (addNote) {
            requireActivity().setTitle(CommonString.menu_add_note)
            // set information transferred by intent
            var contents: String? = null
            val tags = requireArguments().getStringArray(EXTRA_TAGS)

            try {
                // If content has been shared, we can't share to an image occlusion note type
                if (currentNotetypeIsImageOcclusion() && (sourceText != null || caller == NoteEditorCaller.ADD_IMAGE)) {
                    val noteType =
                        col.notetypes.all().first {
                            !it.isImageOcclusion
                        }
                    changeNoteType(noteType.id)
                }
            } catch (e: NoSuchElementException) {
                showSnackbar(CommonString.missing_note_type)
                // setting the text to null & caller to CALLER_NO_CALLER would skip adding text/image to edit field
                sourceText = null
                caller = NoteEditorCaller.NO_CALLER
                Timber.w(e)
            }

            if (sourceText != null) {
                if (aedictIntent && editFields!!.size == 3 && sourceText!![1]!!.contains("[")) {
                    contents =
                        sourceText!![1]!!
                            .replaceFirst("\\[".toRegex(), "\u001f" + sourceText!![0] + "\u001f")
                    contents = contents.dropLast(1)
                } else if (!editFields!!.isEmpty()) {
                    editFields!![0].setText(sourceText!![0])
                    if (editFields!!.size > 1) {
                        editFields!![1].setText(sourceText!![1])
                    }
                }
            } else {
                contents = requireArguments().getString(EXTRA_CONTENTS)
            }
            contents?.let { setEditFieldTexts(it) }
            tags?.let { setTags(it) }
            // If the activity was called to handle an image addition, launch a coroutine to process the image intent.
            if (caller == NoteEditorCaller.ADD_IMAGE) launchCatchingTask { multimediaController.handleImageIntent(intent) }
        } else {
            parentFragmentManager.setFragmentResultListener(
                ChangeNoteTypeDialog.REQUEST_KEY_NOTE_TYPE_CHANGED,
                viewLifecycleOwner,
            ) { _, _ ->
                reloadAfterNoteTypeChange()
            }
            requireActivity().setTitle(CommonString.cardeditor_title_edit_card)
        }
        if (!addNote && currentEditedCard != null) {
            Timber.i(
                "onCollectionLoaded() Edit note activity successfully started with card id %d",
                currentEditedCard!!.id,
            )
        }
        if (addNote) {
            Timber.i(
                "onCollectionLoaded() Edit note activity successfully started in add card mode with node id %d",
                editorNote!!.id,
            )
        }

        // don't open keyboard if not adding note
        if (!addNote) {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        // set focus to FieldEditText 'first' on startup like Anki desktop
        if (editFields != null && !editFields!!.isEmpty()) {
            // EXTRA_TEXT_FROM_SEARCH_VIEW takes priority over other intent inputs
            if (!getTextFromSearchView.isNullOrEmpty()) {
                editFields!!.first().setText(getTextFromSearchView)
            }
            editFields!!.first().requestFocus()
        }

        if (caller == NoteEditorCaller.IMG_OCCLUSION) {
            // val saveImageUri = ImageIntentManager.getImageUri()
            val saveImageUri = BundleCompat.getParcelable(requireArguments(), EXTRA_IMG_OCCLUSION, Uri::class.java)
            if (saveImageUri != null) {
                ImportUtils.getFileCachedCopy(requireContext(), saveImageUri)?.let { path ->
                    setupImageOcclusionEditor(path)
                }
            } else {
                Timber.w("Image uri is null")
            }
        }

        updateFieldPresentation()
        requireView().post {
            delegate?.onNoteEditorReady()
        }
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isPictureTaken ->
            if (isPictureTaken) {
                currentImageOccPath?.let { imagePath ->
                    val photoFile = File(imagePath)
                    val imageUri: Uri =
                        FileProvider.getUriForFile(
                            requireContext(),
                            requireActivity().packageName + ".apkgfileprovider",
                            photoFile,
                        )
                    startCrop(imageUri)
                }
            } else {
                Timber.d("Camera aborted or some interruption")
            }
        }

    /** Launches an activity to crop the image, using the [ImageCropper] */
    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {

                    result.data?.let {
                        val cropResultData =
                            it.getParcelableExtraCompat<ImageCropper.CropResultData>(EXTRA_CROP_IMAGE_RESULT)
                        Timber.d("Cropped image data: $cropResultData")
                        if (cropResultData?.uriPath == null) return@registerForActivityResult
                        setupImageOcclusionEditor(cropResultData.uriPath)
                    }
                }

                else -> {
                    Timber.v("Unable to crop the image")
                }
            }
        }

    private fun startCrop(imageUri: Uri) {
        Timber.i("launching crop")
        val intent = ImageCropperLauncher.ImageUri(imageUri).getIntent(requireContext())
        cropImageLauncher.launch(intent)
    }

    private fun dispatchCameraEvent() {
        val photoFile: File? =
            try {
                requireContext().createImageFile()
            } catch (e: Exception) {
                Timber.w(e, "Error creating the file")
                return
            }

        currentImageOccPath = photoFile?.absolutePath

        photoFile?.let {
            val photoURI: Uri =
                FileProvider.getUriForFile(
                    requireContext(),
                    requireActivity().packageName + ".apkgfileprovider",
                    it,
                )
            try {
                cameraLauncher.launch(photoURI)
            } catch (_: ActivityNotFoundException) {
                Timber.w("No app found to handle image capture")
                activity?.showSnackbar(CommonString.activity_start_failed)
            }
        }
    }

    private fun modifyCurrentSelection(
        formatter: Toolbar.TextFormatter,
        textBox: FieldEditText,
    ) {
        // get the current text and selection locations
        val selectionStart = textBox.selectionStart.coerceAtLeast(0)
        val selectionEnd = textBox.selectionEnd.coerceAtLeast(0)

        // #6762 values are reversed if using a keyboard and pressing Ctrl+Shift+LeftArrow
        val start = min(selectionStart, selectionEnd)
        val end = max(selectionStart, selectionEnd)
        val text = textBox.text?.toString() ?: ""

        // Split the text in the places where the formatting will take place
        val beforeText = text.take(start)
        val selectedText = text.substring(start, end)
        val afterText = text.substring(end)
        val (newText, newStart, newEnd) = formatter.format(selectedText)

        // Update text field with updated text and selection
        val length = beforeText.length + newText.length + afterText.length
        val newFieldContent =
            StringBuilder(length).append(beforeText).append(newText).append(afterText)
        textBox.setText(newFieldContent)
        textBox.setSelection(start + newStart, start + newEnd)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // We want to behave as onKeyUp and thus only react to ACTION_UP
        if (event.action != KeyEvent.ACTION_UP || !collectionHasLoaded()) return false
        val keyCode = event.keyCode
        if (toolbar.onKeyUp(keyCode, event)) {
            // Toolbar was able to handle this key event. No need to handle it in NoteEditor too.
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_ENTER ->
                if (event.isCtrlPressed) {
                    // disable it in case of image occlusion
                    if (allowSaveAction()) {
                        launchCatchingTask { saveNote() }
                        return true
                    }
                }
            KeyEvent.KEYCODE_D ->
                if (event.isCtrlPressed) {
                    chooseDeck()
                    return true
                }
            KeyEvent.KEYCODE_L ->
                if (event.isCtrlPressed) {
                    showCardTemplateEditor()
                    return true
                }
            KeyEvent.KEYCODE_N ->
                if (event.isCtrlPressed) {
                    chooseNoteType()
                    return true
                }
            KeyEvent.KEYCODE_T ->
                if (event.isCtrlPressed && event.isShiftPressed) {
                    showTagsDialog()
                    return true
                }
            KeyEvent.KEYCODE_C -> {
                if (event.isCtrlPressed && event.isShiftPressed) {
                    insertCloze(if (event.isAltPressed) AddClozeType.SAME_NUMBER else AddClozeType.INCREMENT_NUMBER)
                    // Anki Desktop warns, but still inserts the cloze
                    if (!isClozeType) {
                        showSnackbar(CommonString.note_editor_insert_cloze_no_cloze_note_type)
                    }
                    return true
                }
            }
            KeyEvent.KEYCODE_P -> {
                if (event.isCtrlPressed) {
                    Timber.i("Ctrl+P: Preview Pressed")
                    if (allowPreviewAction()) {
                        launchCatchingTask { performPreview() }
                        return true
                    }
                }
            }
        }

        // 7573: Ctrl+Shift+[Num] to select a field
        if (event.isCtrlPressed && event.isShiftPressed) {
            val digit = event.digit ?: return false
            // '0' is after '9' on the keyboard, so a user expects '10'
            val humanReadableDigit = if (digit == 0) 10 else digit
            // Subtract 1 to map to field index. '1' is the first field (index 0)
            selectFieldIndex(humanReadableDigit - 1)
            return true
        }
        return false
    }

    private fun selectFieldIndex(index: Int) {
        Timber.i("Selecting field index %d", index)
        if (editFields!!.size <= index || index < 0) {
            Timber.i("Index out of range: %d", index)
            return
        }
        val field: FieldEditText? =
            try {
                editFields!![index]
            } catch (e: IndexOutOfBoundsException) {
                Timber.w(e, "Error selecting index %d", index)
                return
            }
        field!!.requestFocus()
        Timber.d("Selected field")
    }

    private fun insertCloze(addClozeType: AddClozeType) {
        val field = fieldForFormatting() ?: return
        convertSelectedTextToCloze(field, addClozeType)
    }

    private fun fieldForFormatting(): FieldEditText? {
        // A toolbar control can take focus as part of a keyboard or accessibility action.
        val field = editFields.orEmpty().firstOrNull { it.hasFocus() }
        return field ?: lastFocusedField?.takeIf { it.isShown && it in editFields.orEmpty() }
    }

    private fun fetchIntentInformation(intent: Intent) {
        val extras = requireArguments()
        sourceText = arrayOfNulls(2)
        if (Intent.ACTION_PROCESS_TEXT == intent.action) {
            val stringExtra = extras.getString(Intent.EXTRA_PROCESS_TEXT)
            Timber.d("Obtained %s from intent: %s", stringExtra, Intent.EXTRA_PROCESS_TEXT)
            sourceText!![0] = stringExtra ?: ""
            sourceText!![1] = ""
        } else if (ACTION_CREATE_FLASHCARD == intent.action) {
            // mSourceLanguage = extras.getString(SOURCE_LANGUAGE);
            // mTargetLanguage = extras.getString(TARGET_LANGUAGE);
            sourceText!![0] = extras.getString(EXTRA_SOURCE_TEXT)
            sourceText!![1] = extras.getString(EXTRA_TARGET_TEXT)
        } else {
            var first: String?
            var second: String?
            first =
                if (extras.getString(Intent.EXTRA_SUBJECT) != null) {
                    extras.getString(Intent.EXTRA_SUBJECT)
                } else {
                    ""
                }
            second =
                if (extras.getString(Intent.EXTRA_TEXT) != null) {
                    extras.getString(Intent.EXTRA_TEXT)
                } else {
                    ""
                }
            // Some users add cards via SEND intent from clipboard. In this case SUBJECT is empty
            if ("" == first) {
                // Assume that if only one field was sent then it should be the front
                first = second
                second = ""
            }
            val messages = Pair(first, second)
            sourceText!![0] = messages.first
            sourceText!![1] = messages.second
        }
    }

    private fun addFromAedict(extraText: String?): Boolean {
        var category: String
        val notepadLines = extraText!!.split("\n".toRegex()).toTypedArray()
        for (i in notepadLines.indices) {
            if (notepadLines[i].startsWith("[") && notepadLines[i].endsWith("]")) {
                category = notepadLines[i].substring(1, notepadLines[i].length - 1)
                if ("default" == category) {
                    if (notepadLines.size > i + 1) {
                        val entryLines = notepadLines[i + 1].split(":".toRegex()).toTypedArray()
                        if (entryLines.size > 1) {
                            sourceText!![0] = entryLines[1]
                            sourceText!![1] = entryLines[0]
                            aedictIntent = true
                            return false
                        }
                    }
                    showSnackbar(resources.getString(CommonString.intent_aedict_empty))
                    return true
                }
            }
        }
        showSnackbar(resources.getString(CommonString.intent_aedict_category))
        return true
    }

    @VisibleForTesting
    fun hasUnsavedChanges(): Boolean {
        if (!collectionHasLoaded()) {
            return false
        }

        fun fieldsEdited(): Boolean {
            // Editing an existing note: Check to see if the fields are changed
            if (!addNote) {
                fun normalizeNewlines(s: String) =
                    convertToHtmlNewline(s, replaceNewlines = true)
                        .replace("<br(\\s*/*)>".toRegex(), "<br>")

                val currentStrings = editFields!!.map { it.text?.toString() ?: "" }
                val originalStrings = editorNote!!.fields.toList()
                return currentStrings.map(::normalizeNewlines) != originalStrings.map(::normalizeNewlines)
            }

            if (!isFieldEdited) return false
            return fieldState.currentFieldValues != addNoteFieldBaseline
        }

        // changed note type?
        if (!addNote && currentEditedCard != null) {
            val newNoteType = currentlySelectedNotetype
            val oldNoteType = currentEditedCard!!.noteType(getColUnsafe)
            if (newNoteType != oldNoteType) {
                return true
            }
        }
        // changed deck?
        if (!addNote && currentEditedCard != null && currentEditedCard!!.currentDeckId() != deckId) {
            return true
        }

        // changed fields?
        if (fieldsEdited()) {
            return true
        }

        // changed tags?
        return isTagsEdited
    }

    private fun collectionHasLoaded(): Boolean = editorNote != null

    /**
     * Whether this fragment is editing the cards which [destination] targets.
     */
    fun isEditingSameCards(destination: NoteEditorDestination): Boolean {
        if (destination !is NoteEditorDestination.EditSelection) return false
        if (arguments?.getInt(EXTRA_CALLER) != NoteEditorCaller.EDIT.value) return false
        return cardIdsFromArguments?.asList() == destination.cardIds
    }

    /**
     * Reloads the current note from the collection and rebuilds the fields, e.g. after the
     * note was updated by another component (find & replace in the Card Browser).
     *
     * Unsaved edits are discarded: check [hasUnsavedChanges] before calling.
     */
    fun reloadNoteFromCollection() {
        Timber.i("reloadNoteFromCollection()")
        val cardId = currentEditedCard?.id ?: return
        val card = getColUnsafe.getCard(cardId)
        currentEditedCard = card
        deckId = card.currentDeckId()
        // Reset so setNote() reloads the tags of the note.
        selectedTags = null
        isTagsEdited = false
        setNote(card.note(getColUnsafe), FieldChangeType.refresh(shouldReplaceNewlines()))
        isFieldEdited = false
        updateFieldPresentation()
    }

    // ----------------------------------------------------------------------------
    // SAVE NOTE METHODS
    // ----------------------------------------------------------------------------

    private fun onNoteAdded(count: Int) {
        changed = true
        sourceText = null
        if (caller == NoteEditorCaller.NOTEEDITOR || caller == NoteEditorCaller.NOTEEDITOR_INTENT_ADD || aedictIntent) {
            // Do not queue field or preview updates for an activity which is about to finish.
            val closeIntent = Intent()
            if (caller == NoteEditorCaller.NOTEEDITOR_INTENT_ADD) {
                closeIntent.putExtra(EXTRA_ID, requireArguments().getString(EXTRA_ID))
            }
            if (caller == NoteEditorCaller.NOTEEDITOR_INTENT_ADD || aedictIntent) {
                showThemedToast(requireContext(), CommonString.note_message, shortLength = true)
            }
            closeNoteEditor(closeIntent)
            return
        }

        fieldState.clearHiddenFields()
        refreshNoteData(FieldChangeType.onNoteAdded(shouldReplaceNewlines()))
        // backend text ends with dot
        val successMessage = TR.importingCardsAdded(count).replace(".", "")
        showSnackbar(successMessage, Snackbar.LENGTH_SHORT)

        if (!editFields!!.isEmpty()) {
            editFields!!.first().focusWithKeyboard()
        }

        // Reset check for changes to fields
        isFieldEdited = false
        isTagsEdited = false
        delegate?.onNoteSaved()
    }

    private suspend fun saveNoteWithProgress() {
        val destinationDeckId = deckId
        // adding current note to collection
        val changes =
            requireActivity().withProgress(resources.getString(CommonString.saving_facts)) {
                undoableOp {
                    addNote(editorNote!!, destinationDeckId)
                }
            }
        if (caller == NoteEditorCaller.NOTEEDITOR_INTENT_ADD) {
            Prefs.lastSharedDeckId = destinationDeckId
        }
        // update UI based on the result, noOfAddedCards
        onNoteAdded(changes.count)
    }

    @VisibleForTesting
    @NeedsTest("14664: 'first field must not be empty' no longer applies after saving the note")
    @KotlinCleanup("fix !! on oldNoteType/newNoteType")
    suspend fun saveNote() {
        if (selectedTags == null) {
            selectedTags = ArrayList(0)
        }
        saveToggleStickyMap()

        // treat add new note and edit existing note independently
        if (addNote) {
            // load all of the fields into the note
            for (f in editFields!!) {
                updateField(f)
            }
            // Check for Han Unification issues (Stage 1: detection only)
            checkForHanUnificationIssues()
            // Save tags to model
            editorNote!!.setTagsFromStr(getColUnsafe, tagsAsString(selectedTags!!))
            val tags = JSONArray()
            for (t in selectedTags!!) {
                tags.put(t)
            }

            reloadRequired = true

            // Use addNoteJob to ignore any requests made after the initial add note request, this prevents multiple clicks from crashing the app
            addNoteJob.launch {
                val noteFieldsCheck = checkNoteFieldsResponse(editorNote!!)
                if (noteFieldsCheck is NoteFieldsCheckResult.Failure) {
                    addNoteErrorMessage = noteFieldsCheck.localizedMessage ?: getString(CommonString.something_wrong)
                    displayErrorSavingNote()
                    return@launch
                }
                addNoteErrorMessage = null
                saveNoteWithProgress()
            }
        } else {
            // Regular changes in note content
            var modified = false
            // changed did? this has to be done first as remFromDyn() involves a direct write to the database
            if (currentEditedCard != null && currentEditedCard!!.currentDeckId() != deckId) {
                reloadRequired = true

                val cardIdsToMove = getAffectedCards()
                undoableOp(this@NoteEditorFragment) { setDeck(cardIdsToMove, deckId) }

                // refresh the card object to reflect the database changes from above
                currentEditedCard!!.load(getColUnsafe)
                // also reload the note object
                editorNote = currentEditedCard!!.note(getColUnsafe)
                // then set the card ID to the new deck
                currentEditedCard!!.did = deckId
                modified = true

                Timber.d("deck ID updated to '%d' for %d card(s) of note %d", deckId, cardIdsToMove.size, editorNote!!.id)
            }
            // now load any changes to the fields from the form
            for (f in editFields!!) {
                modified = modified or updateField(f)
            }
            // Check for Han Unification issues (Stage 1: detection only)
            checkForHanUnificationIssues()
            // added tag?
            for (t in selectedTags!!) {
                modified = modified || !editorNote!!.hasTag(getColUnsafe, tag = t)
            }
            // removed tag?
            modified = modified || editorNote!!.tags.size > selectedTags!!.size

            if (!modified) {
                closeNoteEditor()
                return
            }

            editorNote!!.setTagsFromStr(getColUnsafe, tagsAsString(selectedTags!!))
            changed = true

            // these activities are updated to handle `opChanges`
            // and no longer using the legacy ActivityResultCallback/onActivityResult to
            // accept & update the note in the activity
            if (caller == NoteEditorCaller.PREVIEWER_EDIT || caller == NoteEditorCaller.EDIT) {
                requireActivity().withProgress {
                    undoableOp(this@NoteEditorFragment) {
                        updateNote(currentEditedCard!!.note(this@undoableOp))
                    }
                }
            }

            closeNoteEditorAfterSave()
            return
        }
    }

    /**
     * Returns the list of Card IDs that should be affected by bulk operations.
     * For example deck changes.
     *
     * This list is determined by the caller (e.g., Card Browser) and passed
     * via arguments.
     *
     * When the list is not provided, we use the current card.
     */
    private fun getAffectedCards(): List<CardId> {
        val ids = cardIdsFromArguments
        return if (ids != null && ids.isNotEmpty()) {
            ids.toList()
        } else {
            listOf(currentEditedCard!!.id)
        }
    }

    private fun closeNoteEditorAfterSave() {
        isFieldEdited = false
        isTagsEdited = false
        closeNoteEditor()
    }

    /**
     * Checks note fields for potential Han Unification rendering issues.
     *
     * This detection runs during note save/edit to catch issues early, before the user encounters
     * them during review.
     *
     * @see HanUnificationDetector for detection logic
     */
    private fun checkForHanUnificationIssues() {
        if (editFields == null || editFields!!.isEmpty()) {
            return
        }

        val noteId = editorNote?.id ?: 0L
        val noteIdentifier = if (noteId == 0L) "New note" else "Note ID: $noteId"

        editFields!!.forEachIndexed { index, field ->
            val fieldText = field.fieldText ?: return@forEachIndexed
            if (fieldText.isBlank()) {
                return@forEachIndexed
            }

            val hasIssue = HanUnificationDetector.detectIssue(fieldText)
            if (!hasIssue) return@forEachIndexed

            val fieldName =
                runCatching { editorNote?.notetype?.getField(index) }
                    .getOrNull() ?: "Field $index"

            Timber.w(
                "Han Unification issue detected - $noteIdentifier, Field: '$fieldName' (index $index)",
            )
        }
    }

    /**
     * Launches the [ChangeNoteTypeDialog] for the current note being edited.
     * Used in edit mode when the user taps the note type spinner.
     */
    private fun launchChangeNoteTypeDialog() {
        val noteId = editorNote?.id ?: return
        val openDialog = { showDialogFragment(ChangeNoteTypeDialog.newInstance(listOf(noteId))) }
        if (hasUnsavedChanges()) {
            showDiscardChangesDialog {
                reloadNoteFromCollection()
                openDialog()
            }
        } else {
            openDialog()
        }
    }

    private fun reloadAfterNoteTypeChange() {
        val noteId = editorNote?.id ?: return
        val col = getColUnsafe
        val note = col.getNote(noteId)
        val convertedIds = note.cardIds(col)
        val activeId = currentEditedCard?.id?.takeIf { it in convertedIds } ?: convertedIds.firstOrNull()
        reloadRequired = true
        changed = true
        if (activeId == null) {
            closeNoteEditorAfterSave()
            return
        }
        val originalIds = getAffectedCards()
        cardIdsAfterNoteTypeChange = listOf(activeId) + convertedIds.filter { it != activeId && it in originalIds }
        currentEditedCard = col.getCard(activeId)
        reloadNoteFromCollection()
        delegate?.onNoteTypeChanged()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateToolbar()
    }

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.note_editor, menu)
        onPrepareMenu(menu)
    }

    /**
     * Configures the main toolbar with the appropriate menu items and their visibility based on the current state.
     */
    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.action_preview).isVisible = allowPreviewAction()
        if (addNote) {
            menu.findItem(R.id.action_copy_note).isVisible = false
            menu.findItem(R.id.action_save).isVisible = allowSaveAction()
            menu.findItem(R.id.action_preview).isVisible = allowPreviewAction()
        } else {
            // Hide add note item if fragment is in fragmented activity
            // because this item is already present in CardBrowser
            menu.findItem(R.id.action_add_note_from_note_editor).isVisible = !inCardBrowserActivity
        }
        menu.findItem(R.id.action_toggle_previewer).apply {
            isVisible = inNoteEditorActivity && noteEditorHost?.hasPreviewerPane == true
            isChecked = noteEditorHost?.isPreviewerVisible == true
            title =
                if (isChecked) {
                    getString(CommonString.note_editor_hide_previewer)
                } else {
                    getString(CommonString.note_editor_show_previewer)
                }
        }
        if (editFields != null) {
            for (i in editFields!!.indices) {
                val fieldText = editFields!![i].text
                if (!fieldText.isNullOrEmpty()) {
                    menu.findItem(R.id.action_copy_note).isEnabled = true
                    break
                } else if (i == editFields!!.size - 1) {
                    menu.findItem(R.id.action_copy_note).isEnabled = false
                }
            }
        }
        menu.findItem(R.id.action_capitalize).isChecked =
            sharedPrefs().getBoolean(PREF_NOTE_EDITOR_CAPITALIZE, true)
    }

    /**
     * When using the options such as image occlusion we don't need the menu's save/preview
     * option to save/preview the card as it has a built in option and the user is notified
     * when the card is saved successfully
     */
    private fun allowSaveAction(): Boolean =
        when {
            addNote && currentNotetypeIsImageOcclusion() -> false
            else -> true
        }

    /**
     * Determines whether the preview action should be available to the user.
     *
     * Preview action is disabled in the following scenarios:
     * - When running in a fragmented note editor (previewer is already visible in a side panel)
     * - When adding a new image occlusion note (uses built-in preview functionality)
     */
    // TODO: Move this logic to a [NoteEditorActivity.kt]
    private fun allowPreviewAction(): Boolean =
        when {
            inNoteEditorActivity && noteEditorHost?.isPreviewerVisible == true -> false
            addNote && currentNotetypeIsImageOcclusion() -> false
            else -> true
        }

    override fun onMenuItemSelected(item: MenuItem): Boolean = performEditorAction(item.itemId)

    private fun performEditorAction(actionId: Int): Boolean {
        when (actionId) {
            R.id.action_preview -> {
                Timber.i("NoteEditor:: Preview button pressed")
                if (allowPreviewAction()) {
                    launchCatchingTask { performPreview() }
                }
                return true
            }
            R.id.action_toggle_previewer -> {
                Timber.i("NoteEditor:: Toggle previewer pressed")
                val editorActivity = noteEditorHost
                if (editorActivity == null) {
                    Timber.w("NoteEditor:: Toggle previewer pressed outside of NoteEditorActivity")
                    return true
                }
                editorActivity.setPreviewerVisible(!editorActivity.isPreviewerVisible)
                requireActivity().invalidateMenu()
                return true
            }
            R.id.action_save -> {
                Timber.i("NoteEditor:: Save note button pressed")
                if (allowSaveAction()) {
                    launchCatchingTask { saveNote() }
                }
                return true
            }
            R.id.action_add_note_from_note_editor -> {
                Timber.i("NoteEditor:: Add Note button pressed")
                addNewNote()
                return true
            }
            R.id.action_copy_note -> {
                Timber.i("NoteEditor:: Copy Note button pressed")
                copyNote()
                return true
            }
            R.id.action_font_size -> {
                Timber.i("NoteEditor:: Font Size button pressed")
                val fontSizeDialog = IntegerDialog()
                fontSizeDialog.setArgs(getString(CommonString.menu_font_size), editTextFontSize, 2)
                fontSizeDialog.setCallbackRunnable { fontSizeSp: Int? -> setFontSize(fontSizeSp) }
                showDialogFragment(fontSizeDialog)
                return true
            }
            R.id.action_toolbar_layout -> {
                toolbarLayoutDialog.show()
                return true
            }
            R.id.action_capitalize -> {
                toggleCapitalize(!capitalizeSentences)
                requireActivity().invalidateMenu()
                return true
            }
        }
        return false
    }

    private fun toggleCapitalize(value: Boolean) {
        capitalizeSentences = value
        this.sharedPrefs().edit {
            putBoolean(PREF_NOTE_EDITOR_CAPITALIZE, value)
        }
        for (f in editFields!!) {
            f.setCapitalize(value)
        }
    }

    private fun setFontSize(fontSizeSp: Int?) {
        if (fontSizeSp == null || fontSizeSp <= 0) {
            return
        }
        Timber.i("Setting font size to %d", fontSizeSp)
        this.sharedPrefs().edit { putInt(PREF_NOTE_EDITOR_FONT_SIZE, fontSizeSp) }
        for (f in editFields!!) {
            f.textSize = fontSizeSp.toFloat()
        }
    }

    // Note: We're not being accurate here - the initial value isn't actually what's supplied in the layout.xml
    // So a value of 18sp in the XML won't be 18sp on the TextView, but it's close enough.
    // Values are setFontSize are whole when returned.
    private val editTextFontSize: String
        get() {
            // Note: We're not being accurate here - the initial value isn't actually what's supplied in the layout.xml
            // So a value of 18sp in the XML won't be 18sp on the TextView, but it's close enough.
            // Values are setFontSize are whole when returned.
            val sp = TextViewUtil.getTextSizeSp(editFields!!.first())
            return sp.roundToInt().toString()
        }

    private fun addNewNote() {
        requestAddLauncher.navigate(NoteEditorDestination.AddNote(deckId))
    }

    fun copyNote() {
        requestAddLauncher.navigate(
            NoteEditorDestination.CopyNote(
                deckId = deckId,
                fieldsText = fieldsText,
                noteTypeId = currentlySelectedNotetype?.id ?: editorNote!!.noteTypeId,
                tags = selectedTags.orEmpty(),
            ),
        )
    }

    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------
    @VisibleForTesting
    @NeedsTest("previewing newlines")
    @NeedsTest("cards with a cloze notetype but no cloze in fields are previewed as empty card")
    @NeedsTest("clozes that don't start at '1' are correctly displayed")
    suspend fun performPreview() {
        val fields = prepareNoteFields()
        val tags = selectedTags ?: mutableListOf()

        val ord = determineCardOrdinal(fields)

        val args =
            TemplatePreviewerArguments(
                notetypeFile = NotetypeFile(requireContext(), editorNote!!.notetype),
                fields = fields,
                tags = tags,
                id = editorNote!!.id,
                ord = ord,
                fillEmpty = false,
            )
        val intent = TemplatePreviewerPage.getIntent(requireContext(), args)
        startActivity(intent)
    }

    /**
     * Prepares the note fields for the previewer by converting them to the appropriate format.
     *
     * @return A list of field values properly formatted for display
     */
    fun prepareNoteFields(): MutableList<String> {
        val convertNewlines = shouldReplaceNewlines()

        fun String?.toFieldText(): String = convertToHtmlNewline(this.toString(), convertNewlines)

        return editFields?.mapTo(mutableListOf()) { it.fieldText.toFieldText() } ?: mutableListOf()
    }

    /**
     * Determines the appropriate card ordinal (template position) to display in the previewer
     *
     * For cloze notes, it identifies the first cloze number present in the fields.
     * For standard notes, it uses the currently edited card ordinal if available.
     *
     * @param fields The processed note fields
     * @return The ordinal (position) of the card template to display
     */
    suspend fun determineCardOrdinal(fields: MutableList<String>): CardOrdinal {
        val ord =
            if (editorNote!!.notetype.isCloze) {
                val tempNote = withCol { Note.fromNotetypeId(this@withCol, editorNote!!.notetype.id) }
                tempNote.fields = fields // makes possible to get the cloze numbers from the fields
                val clozeNumbers = withCol { clozeNumbersInNote(tempNote) }
                if (clozeNumbers.isNotEmpty()) {
                    clozeNumbers.first() - 1
                } else {
                    0
                }
            } else {
                currentEditedCard?.ord ?: 0
            }
        return ord
    }

    private fun setTags(tags: Array<String>) {
        selectedTags = tags.toCollection(ArrayList())
        updateTags()
    }

    private fun closeCardEditorWithCheck() {
        if (hasUnsavedChanges()) {
            showDiscardChangesDialog()
        } else {
            closeNoteEditor()
        }
    }

    private fun showDiscardChangesDialog(onDiscard: () -> Unit = { closeNoteEditor() }) {
        DiscardChangesDialog.showDialog(
            requireContext(),
            message = if (addNote) TR.addingDiscardCurrentInput() else TR.cardTemplatesDiscardChanges(),
        ) {
            Timber.i("NoteEditor:: OK button pressed to confirm discard changes")
            onDiscard()
        }
    }

    private fun closeNoteEditor(intent: Intent = Intent()) {
        val result: Int =
            if (changed) {
                Activity.RESULT_OK
            } else {
                RESULT_CANCELED
            }
        if (reloadRequired) {
            intent.putExtra(EXTRA_RELOAD_REQUIRED, true)
        }
        if (changed) {
            intent.putExtra(EXTRA_NOTE_CHANGED, true)
        }
        closeNoteEditor(result, intent)
    }

    private fun closeNoteEditor(
        result: Int,
        intent: Intent?,
    ) {
        // Preview files are only needed while the editor is open.
        CardTemplateNotetype.clearTempNoteTypeFiles()
        if (inCardBrowserActivity) return

        Timber.i("Closing note editor")
        val animation =
            BundleCompat.getParcelable(
                requireArguments(),
                AnkiActivity.EXTRA_FINISH_ANIMATION,
                TransitionDirection::class.java,
            )
        val host = requireAnkiActivity()
        if (animation != null && host !is DeckPicker) {
            host.setResult(result, intent)
            host.finishWithAnimation(animation)
        } else {
            finishScreen(result, intent)
        }
    }

    private fun showTagsDialog() {
        val selTags = selectedTags?.let { ArrayList(it) } ?: arrayListOf()
        tagsDialogFactory!!.show(requireActivity(), checkedTags = selTags)
    }

    override fun onSelectedTags(
        selectedTags: List<String>,
        indeterminateTags: List<String>,
        stateFilter: CardStateFilter,
    ) {
        if (this.selectedTags != selectedTags) {
            isTagsEdited = true
        }
        this.selectedTags = selectedTags as ArrayList<String>?
        updateTags()
    }

    private fun showCardTemplateEditor() {
        val intent =
            CardTemplateEditor.getIntent(
                requireContext(),
                noteTypeId = currentlySelectedNotetype!!.id,
                // Also pass the note id and ord if not adding new note
                noteId = if (addNote) null else currentEditedCard?.nid,
                ord = if (addNote) null else currentEditedCard?.ord,
            )
        requestTemplateEditLauncher.launch(intent)
    }

    /** Appends a string at the selection point, or appends to the end if not in focus  */
    @VisibleForTesting
    fun insertStringInField(
        fieldEditText: EditText?,
        formattedValue: String?,
    ) {
        if (fieldEditText!!.hasFocus()) {
            // Crashes if start > end, although this is fine for a selection via keyboard.
            val start = fieldEditText.selectionStart
            val end = fieldEditText.selectionEnd
            fieldEditText.text.replace(min(start, end), max(start, end), formattedValue)
        } else {
            fieldEditText.text.append(formattedValue)
        }
    }

    /** Sets EditText at index [fieldIndex]'s text to [newString] */
    @VisibleForTesting
    fun setField(
        fieldIndex: Int,
        newString: String,
    ) {
        clearField(fieldIndex)
        insertStringInField(getFieldForTest(fieldIndex), newString)
    }

    internal suspend fun getCurrentMultimediaEditableNote(): MultimediaEditableNote {
        val note = NoteService.createEmptyNote(editorNote!!.notetype)
        val fields = currentFieldStrings.requireNoNulls()
        withCol { NoteService.updateMultimediaNoteFromFields(this@withCol, fields, editorNote!!.noteTypeId, note) }

        return note
    }

    /** Returns the edit-field [FieldEditText] at [index] if present. */
    internal fun editFieldAt(index: Int): FieldEditText? = editFields?.getOrNull(index)

    /** Records that a multimedia capture has modified the note. */
    internal fun markMultimediaChanged() {
        changed = true
    }

    /** Determines whether pasted images should be handled as PNG format. **/
    private suspend fun shouldPasteAsPng() = withCol { config.pasteImagesAsPng }

    val currentFields: Fields
        get() = editorNote!!.notetype.fields

    @get:CheckResult
    val currentFieldStrings: Array<String?>
        get() {
            if (editFields == null) {
                return arrayOfNulls(0)
            }
            val ret = arrayOfNulls<String>(editFields!!.size)
            for (i in editFields!!.indices) {
                ret[i] = getCurrentFieldText(i)
            }
            return ret
        }

    private fun populateEditFields(type: FieldChangeType) {
        val editLines = fieldState.loadFieldEditLines(type)
        fieldsLayoutContainer!!.removeAllViews()
        customViewIds.clear()
        showImageOcclusionActions = currentNotetypeIsImageOcclusion()

        val indicesToHide = mutableListOf<Int>()
        if (currentNotetypeIsImageOcclusion()) {
            val occlusionTag = "0"
            val imageTag = "1"
            val fields = currentlySelectedNotetype!!.fields
            for ((i, field) in fields.withIndex()) {
                val tag = field.imageOcclusionTag
                if (tag == occlusionTag || tag == imageTag) {
                    indicesToHide.add(i)
                }
            }
        }

        editFields = LinkedList()

        firstFieldLine = editLines.firstOrNull()
        customViewIds.ensureCapacity(editLines.size)
        for (i in editLines.indices) {
            val editLineView = editLines[i]
            customViewIds.add(editLineView.id)
            val newEditText = editLineView.editText
            lifecycleScope.launch {
                val pasteAsPng = shouldPasteAsPng()
                newEditText.setPasteListener { editText: EditText?, uri: Uri?, description: ClipDescription? ->
                    multimediaController.onPaste(
                        editText!!,
                        uri!!,
                        description!!,
                        pasteAsPng,
                    )
                }
            }
            editLineView.configureView(
                requireActivity(),
                MEDIA_MIME_TYPES,
                DropHelper.Options
                    .Builder()
                    .setHighlightColor(getColorFromAttr(requireContext(), CommonR.attr.foundationEmphasis))
                    .setHighlightCornerRadiusPx(0)
                    .addInnerEditTexts(newEditText)
                    .build(),
                onReceiveContentListener,
            )
            editLineView.enableAnimation = requireActivity().animationEnabled()

            // Use custom implementation of ActionMode.Callback customize selection and insert menus
            editLineView.setActionModeCallbacks(getActionModeCallback(newEditText, View.generateViewId()))
            editLineView.setHintLocale(getHintLocaleForField(editLineView.name))
            initFieldEditText(editLineView, i)
            editFields!!.add(newEditText)
            val prefs = this.sharedPrefs()
            if (prefs.getInt(PREF_NOTE_EDITOR_FONT_SIZE, -1) > 0) {
                newEditText.textSize = prefs.getInt(PREF_NOTE_EDITOR_FONT_SIZE, -1).toFloat()
            }
            newEditText.setCapitalize(prefs.getBoolean(PREF_NOTE_EDITOR_CAPITALIZE, true))
            if (isClozeType) {
                // Preserve contrast for the cinnabar cloze mark when the text is selected.
                val background = getColorFromAttr(requireContext(), CommonR.attr.foundationBackground)
                newEditText.highlightColor = ColorUtils.blendARGB(background, newEditText.highlightColor, 0.6f)
            }
            if (addNote) {
                editLineView.sticky = currentFields[i].sticky
                if (currentFields[i].sticky) {
                    toggleStickyText.getOrPut(i) { "" }
                } else {
                    toggleStickyText.remove(i)
                }
            }
            editLineView.bindPresentation(
                onStickyToggle = { onToggleStickyText(editLineView, i) },
                onAttach = {
                    multimediaController.handleActions(i)
                    showMultimediaBottomSheet(i, editLineView.name.orEmpty())
                },
                onShowDuplicates = ::showDuplicates,
            )

            editLineView.isVisible = i !in indicesToHide
            val fieldLayout = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            if (i > 0 && (i - 1) !in indicesToHide) {
                fieldLayout.topMargin = 14.dp.toPx(requireContext())
            }
            fieldsLayoutContainer!!.addView(editLineView, fieldLayout)
        }
    }

    private fun getActionModeCallback(
        textBox: FieldEditText,
        clozeMenuId: Int,
    ): ActionMode.Callback =
        CustomActionModeCallback(
            isClozeType,
            getString(CommonString.multimedia_editor_popup_cloze),
            clozeMenuId,
            onActionItemSelected = { mode, item ->
                if (item.itemId == clozeMenuId) {
                    convertSelectedTextToCloze(textBox, AddClozeType.INCREMENT_NUMBER)
                    mode.finish()
                    true
                } else {
                    false
                }
            },
        )

    private fun showMultimediaBottomSheet(
        fieldIndex: Int,
        fieldName: String,
    ) {
        Timber.d("Showing MultimediaBottomSheet fragment")
        val multimediaBottomSheet = MultimediaBottomSheet.forField(fieldIndex, fieldName)
        multimediaBottomSheet.show(parentFragmentManager, MultimediaBottomSheet.TAG)
    }

    internal fun showLargeMediaFileWarning(
        fileName: String,
        fileSize: Long,
        onForceAdd: () -> Unit,
    ) {
        val context = requireContext()
        val fileSizeStr = fileSize.toBytesShortString(context)
        val limitStr = Backend.MAX_INDIVIDUAL_MEDIA_FILE_SIZE.toBytesShortString(context)

        createPaperDialog(context) { dialog ->
            ConfirmationDialog(
                title = getString(CommonString.media_file_size_warning_title),
                message = getString(CommonString.media_file_size_warning_message, fileName, fileSizeStr, limitStr),
                markIcon = R.drawable.symbol_warning,
                confirm =
                    DialogAction(getString(CommonString.media_file_size_add_anyway), {
                        dialog.dismiss()
                        onForceAdd()
                    }),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    private fun onToggleStickyText(
        line: FieldEditLine,
        index: Int,
    ) {
        val field = currentFields[index]
        field.sticky = !field.sticky
        line.sticky = field.sticky
        if (field.sticky) {
            toggleStickyText[index] = line.editText.fieldText
        } else {
            toggleStickyText.remove(index)
        }
        val noteType = editorNote!!.notetype
        launchCatchingTask { withCol { notetypes.save(noteType) } }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun saveToggleStickyMap() {
        for ((key) in toggleStickyText.toMap()) {
            // handle fields for different note type with different size
            if (key < editFields!!.size) {
                toggleStickyText[key] = editFields!![key].fieldText
            } else {
                toggleStickyText.remove(key)
            }
        }
    }

    private fun updateFieldsFromStickyText() {
        loadingStickyFields = true
        for ((key, value) in toggleStickyText) {
            // handle fields for different note type with different size
            if (key < editFields!!.size) {
                editFields!![key].setText(value)
            }
        }
        loadingStickyFields = false
    }

    @VisibleForTesting
    fun clearField(index: Int) {
        setFieldValueFromUi(index, "")
    }

    private fun initFieldEditText(
        fieldLine: FieldEditLine,
        index: Int,
    ) {
        fieldLine.editText.addTextChangedListener(EditFieldTextWatcher(index))
        fieldLine.editText.onFocusChangeListener =
            OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) lastFocusedField = view as FieldEditText
                fieldLine.focused = hasFocus
                if (index != 0 || hasFocus) return@OnFocusChangeListener
                try {
                    @SuppressLint("CheckResult")
                    val currentFieldStrings = currentFieldStrings
                    if (currentFieldStrings.size != 2 || currentFieldStrings[1]!!.isNotEmpty()) {
                        return@OnFocusChangeListener
                    }
                    val firstField = currentFieldStrings[0]
                    val decoratedText = NoteFieldDecorator.aplicaHuevo(firstField)
                    if (decoratedText != firstField) {
                        setFieldValueFromUi(1, decoratedText)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Unable to decorate text field")
                }
            }
    }

    @KotlinCleanup("make name non-null in FieldEditLine")
    private fun getHintLocaleForField(name: String?): Locale? = getFieldByName(name)?.languageHint

    private fun getFieldByName(name: String?): Field? {
        val pair: Pair<Int, Field>? =
            try {
                Notetypes.fieldMap(currentlySelectedNotetype!!)[name]
            } catch (_: Exception) {
                Timber.w("Failed to obtain field '%s'", name)
                return null
            }
        return pair?.second
    }

    private fun setEditFieldTexts(contents: String?) {
        var fields: List<String>? = null
        val len: Int
        if (contents == null) {
            len = 0
        } else {
            fields = Utils.splitFields(contents)
            len = fields.size
        }
        for (i in editFields!!.indices) {
            if (i < len) {
                editFields!![i].setText(fields!![i])
            } else {
                editFields!![i].setText("")
            }
        }
    }

    private fun updateDuplicateWarning() {
        val field = editFields?.firstOrNull() ?: return
        val line = firstFieldLine ?: return
        val note = editorNote ?: return
        // Use the Engine's existing first-field validation without changing the saved baseline.
        val firstValue = note.fields[0]
        updateField(field)
        try {
            line.duplicate = note.fieldsCheck(getColUnsafe) == NoteFieldsCheckResponse.State.DUPLICATE
        } finally {
            note.values()[0] = firstValue
        }
    }

    private fun showDuplicates() {
        val note = editorNote ?: return
        val content = convertToHtmlNewline(getCurrentFieldText(0), shouldReplaceNewlines())
        launchCatchingTask {
            val query =
                withCol {
                    val duplicates =
                        SearchNode.Dupe
                            .newBuilder()
                            .setNotetypeId(note.noteTypeId)
                            .setFirstField(content)
                    buildSearchString(listOf(SearchNode.newBuilder().setDupe(duplicates).build()))
                }
            startActivity(
                Intent(requireContext(), CardBrowser::class.java)
                    .putExtra(CardBrowserViewModel.EXTRA_SEARCH_QUERY, query)
                    .putExtra(CardBrowserViewModel.EXTRA_ALL_DECKS, true),
            )
        }
    }

    private fun updateFieldPresentation() {
        canCopyNote = editFields.orEmpty().any { !it.text.isNullOrEmpty() }
        updateDuplicateWarning()
    }

    @KotlinCleanup("remove 'requireNoNulls'")
    val fieldsText: String
        get() {
            val fields = arrayOfNulls<String>(editFields!!.size)
            for (i in editFields!!.indices) {
                fields[i] = getCurrentFieldText(i)
            }
            return Utils.joinFields(fields.requireNoNulls())
        }

    /** Returns the value of the field at the given index  */
    private fun getCurrentFieldText(index: Int): String {
        val fieldText = editFields!![index].text ?: return ""
        return fieldText.toString()
    }

    private fun updateDeckName() {
        selectedDeckName = getColUnsafe.decks.name(deckId)
    }

    /** Refreshes the UI using the currently selected note type as a template  */
    private fun refreshNoteData(changeType: FieldChangeType) {
        setNote(null, changeType)
    }

    /** Handles setting the current note (non-null afterwards) and rebuilding the UI based on this note  */
    private fun setNote(
        note: Note?,
        changeType: FieldChangeType,
        noteTypeId: NoteTypeId? = null,
    ) {
        // An explicit type takes precedence over the deck's remembered type.
        noteTypeId?.let { selectedNoteTypeId = it }
        editorNote = note ?: Note.fromNotetypeId(getColUnsafe, checkNotNull(selectedNoteTypeId))
        if (selectedTags == null) {
            selectedTags = editorNote!!.tags
        }
        selectedNoteTypeName = editorNote!!.notetype.name
        showCardDeckLabel = !addNote && editorNote!!.notetype.templates.length() > 1
        showSave = allowSaveAction()
        updateDeckName()
        updateTags()
        updateCards(editorNote!!.notetype)
        updateToolbar()

        if (changeType.type == Type.NOTE_ADDED) {
            // rebuilding the edit fields causes a flicker - avoid it
            // we update the fields after focus to stop a case where the field is cleared
            // causing two selection updates: handling the selected field being blanked
            // then focusing the first field
            editFields!!.first().focusWithKeyboard {
                editFields!!.forEach { it.setText("") }
                updateFieldsFromStickyText()
                if (addNote) {
                    addNoteFieldBaseline = editFields!!.map { it.text?.toString() ?: "" }
                }
            }
        } else {
            populateEditFields(changeType)
            if (changeType.type == Type.CHANGE_FIELD_COUNT) {
                // Keep the baseline for hidden fields as well as visible ones. Only visible
                // values pass through setContent(), so only normalize those baseline values.
                if (addNote) {
                    addNoteFieldBaseline =
                        List(maxOf(editFields!!.size, addNoteFieldBaseline.size)) { i ->
                            val old = addNoteFieldBaseline.getOrElse(i) { "" }
                            if (i < editFields!!.size) {
                                FieldEditText.convertBrToNewline(old, changeType.replaceNewlines)
                            } else {
                                old
                            }
                        }
                }
            } else {
                updateFieldsFromStickyText()
                if (addNote) {
                    addNoteFieldBaseline = editFields!!.map { it.text?.toString() ?: "" }
                }
            }
        }
    }

    private fun Insets.forEditorPane(): Insets {
        val isRtl = binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val start =
            if (inCardBrowserActivity) {
                0
            } else if (isRtl) {
                right
            } else {
                left
            }
        val end =
            if (noteEditorHost?.isPreviewerVisible == true) {
                0
            } else if (isRtl) {
                left
            } else {
                right
            }
        return insetsOf(
            left = if (isRtl) end else start,
            right = if (isRtl) start else end,
            bottom = bottom,
        )
    }

    private fun WindowInsetsCompat.paneInsets(): Insets = getInsets(systemBars() or displayCutout() or ime()).forEditorPane()

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val bars = insets.paneInsets()
            bottomInsetPx = bars.bottom
            toolbar.updatePadding(left = bars.left, right = bars.right)
            // move the toolbar as low as possible, without being impacted by rounded corners
            val corners = insets.bottomCornerSideClearance(bars.bottom).forEditorPane()
            toolbar.setSideClearance(
                left = (corners.left - bars.left).coerceAtLeast(0),
                right = (corners.right - bars.right).coerceAtLeast(0),
            )
            applyBottomInset()
            insets
        }
        binding.noteEditorLayout.doOnApplyWindowInsets { fields, insets, initial ->
            val bars = insets.paneInsets()
            fields.updatePadding(
                left = initial.padding.left + bars.left,
                right = initial.padding.right + bars.right,
            )
        }
        ViewCompat.requestApplyInsets(binding.rootLayout)
    }

    private fun applyBottomInset() {
        val toolbarHeight =
            when (toolbarPreferences.layout) {
                ToolbarLayoutMode.HIDDEN -> 0
                ToolbarLayoutMode.SCROLL -> resources.getDimensionPixelSize(R.dimen.note_editor_toolbar_height)
                ToolbarLayoutMode.STACK -> toolbar.stackedControlsHeight
            }
        toolbar.updatePadding(bottom = bottomInsetPx)
        val fields = binding.noteEditorLayout
        val bottomMargin = toolbarHeight + bottomInsetPx
        if ((fields.layoutParams as MarginLayoutParams).bottomMargin != bottomMargin) {
            fields.updateLayoutParams<MarginLayoutParams> { this.bottomMargin = bottomMargin }
        }
    }

    private fun updateToolbar() {
        val cloze =
            if (editorNote!!.notetype.isCloze) {
                listOf(
                    Toolbar.Action(label = TR.editingClozeDeletion(), text = "[c+]", emphasis = true) {
                        insertCloze(AddClozeType.INCREMENT_NUMBER)
                    },
                    Toolbar.Action(label = TR.editingClozeDeletionRepeat(), text = "[c]", emphasis = true) {
                        insertCloze(AddClozeType.SAME_NUMBER)
                    },
                )
            } else {
                emptyList()
            }
        val buttons = toolbarButtons
        val custom =
            buildList {
                buttons.forEach { button ->
                    val number = button.index + 1
                    val label = button.buttonText.ifEmpty { number.toString() }
                    add(
                        Toolbar.Action(
                            label = label,
                            text = label,
                            shortcut = (number % 10).digitToChar().takeIf { number <= 10 },
                            separated = button.index == 0,
                            onLongClick = { displayEditToolbarDialog(button) },
                        ) { toolbar.format(button.toFormatter()) },
                    )
                }
                add(
                    Toolbar.Action(
                        label = getString(CommonString.add_toolbar_item),
                        icon = CommonR.drawable.symbol_add,
                        separated = buttons.isEmpty(),
                        onClick = ::displayAddToolbarDialog,
                    ),
                )
            }
        toolbar.update(toolbarPreferences.layout, cloze, custom)
        applyBottomInset()
    }

    private val toolbarButtons: ArrayList<CustomToolbarButton>
        get() {
            val set =
                this
                    .sharedPrefs()
                    .getStringSet(PREF_NOTE_EDITOR_CUSTOM_BUTTONS, HashUtil.hashSetInit(0))
            return CustomToolbarButton.fromStringSet(set!!)
        }

    private fun saveToolbarButtons(buttons: ArrayList<CustomToolbarButton>) {
        this.sharedPrefs().edit {
            putStringSet(PREF_NOTE_EDITOR_CUSTOM_BUTTONS, CustomToolbarButton.toStringSet(buttons))
        }
    }

    private fun addToolbarButton(
        buttonText: String,
        prefix: String,
        suffix: String,
    ) {
        if (prefix.isEmpty() && suffix.isEmpty()) return
        val toolbarButtons = toolbarButtons
        toolbarButtons.add(CustomToolbarButton(toolbarButtons.size, buttonText, prefix, suffix))
        saveToolbarButtons(toolbarButtons)
        updateToolbar()
    }

    private fun editToolbarButton(
        buttonText: String,
        prefix: String,
        suffix: String,
        currentButton: CustomToolbarButton,
    ) {
        val toolbarButtons = toolbarButtons
        val currentButtonIndex = currentButton.index

        toolbarButtons[currentButtonIndex] =
            CustomToolbarButton(
                index = currentButtonIndex,
                buttonText = buttonText.ifEmpty { currentButton.buttonText },
                prefix = prefix,
                suffix = suffix,
            )

        saveToolbarButtons(toolbarButtons)
        updateToolbar()
    }

    private fun removeButton(button: CustomToolbarButton) {
        val toolbarButtons = toolbarButtons
        toolbarButtons.removeAt(button.index)
        saveToolbarButtons(toolbarButtons)
        updateToolbar()
    }

    private fun displayAddToolbarDialog() = showToolbarButtonDialog(null)

    private fun displayEditToolbarDialog(currentButton: CustomToolbarButton) = showToolbarButtonDialog(currentButton)

    private fun showToolbarButtonDialog(currentButton: CustomToolbarButton?) {
        createPaperDialog(requireContext()) { dialog ->
            var icon by rememberSaveable { mutableStateOf(currentButton?.buttonText.orEmpty()) }
            var prefix by rememberSaveable { mutableStateOf(currentButton?.prefix.orEmpty()) }
            var suffix by rememberSaveable { mutableStateOf(currentButton?.suffix.orEmpty()) }
            var edited by rememberSaveable { mutableStateOf(false) }
            val hasHtml = prefix.isNotBlank() || suffix.isNotBlank()
            val needsIcon = currentButton == null && icon.isBlank()

            PaperDialog(
                title = getString(if (currentButton == null) CommonString.add_toolbar_item else CommonString.edit_toolbar_item),
                onDismissRequest = dialog::dismiss,
                actions =
                    buildList {
                        add(
                            DialogAction(getString(CommonString.help), {
                                dialog.dismiss()
                                requireContext().openUrl(R.string.link_manual_note_format_toolbar)
                            }),
                        )
                        if (currentButton != null) {
                            add(
                                DialogAction(getString(CommonString.remove_toolbar_item), {
                                    confirmToolbarButtonRemoval(currentButton, dialog::dismiss)
                                }),
                            )
                        }
                        add(DialogAction(getString(CommonString.dialog_cancel), dialog::dismiss))
                        add(
                            DialogAction(
                                getString(if (currentButton == null) CommonString.dialog_positive_create else CommonString.save),
                                {
                                    dialog.dismiss()
                                    if (currentButton == null) {
                                        addToolbarButton(icon, prefix, suffix)
                                    } else {
                                        editToolbarButton(icon, prefix, suffix, currentButton)
                                    }
                                },
                                DialogActionStyle.Filled,
                                enabled = hasHtml && !needsIcon,
                            ),
                        )
                    },
            ) {
                DialogBody(getString(CommonString.toolbar_item_explain_edit_or_remove))
                OutlinedTextField(
                    value = icon,
                    onValueChange = {
                        icon = it
                        edited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(getString(CommonString.note_editor_toolbar_icon)) },
                    isError = edited && needsIcon,
                    supportingText =
                        if (edited && needsIcon) {
                            { Text(getString(CommonString.note_editor_toolbar_icon_requirement_error)) }
                        } else {
                            null
                        },
                )
                OutlinedTextField(
                    value = prefix,
                    onValueChange = {
                        prefix = it
                        edited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(getString(CommonString.before_text)) },
                    isError = edited && !hasHtml && currentButton == null,
                )
                OutlinedTextField(
                    value = suffix,
                    onValueChange = {
                        suffix = it
                        edited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(getString(CommonString.after_text)) },
                    isError = edited && !hasHtml && currentButton == null,
                    supportingText =
                        if (edited && !hasHtml && currentButton == null) {
                            { Text(getString(CommonString.before_after_text_requirement_error)) }
                        } else {
                            null
                        },
                )
            }
        }.show()
    }

    private fun confirmToolbarButtonRemoval(
        button: CustomToolbarButton,
        dismissEditor: () -> Unit,
    ) {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = getString(CommonString.remove_toolbar_item),
                message = "",
                confirm =
                    DialogAction(
                        getString(CommonString.dialog_positive_delete),
                        {
                            dialog.dismiss()
                            dismissEditor()
                            removeButton(button)
                        },
                        DialogActionStyle.Destructive,
                    ),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    override val shortcuts
        get() =
            ShortcutGroup(
                listOf(
                    shortcut("Ctrl+ENTER") { getString(CommonString.save) },
                    shortcut("Ctrl+D") { TR.sentenceCase.selectDeck },
                    shortcut("Ctrl+L") { getString(CommonString.card_template_editor_group) },
                    shortcut("Ctrl+N") { getString(CommonString.select_note_type) },
                    shortcut("Ctrl+Shift+T") { getString(CommonString.tag_editor) },
                    shortcut("Ctrl+Shift+C") { getString(CommonString.multimedia_editor_popup_cloze) },
                    shortcut("Ctrl+P") { getString(CommonString.card_editor_preview_card) },
                ),
                CommonString.note_editor_group,
            )

    private fun updateTags() {
        displayTags = selectedTags.orEmpty().distinct()
    }

    private fun updateCards(noteType: NotetypeJson) {
        val templates = noteType.templates
        templateNames = templates.map { it.name }
        activeTemplateName =
            currentEditedCard
                ?.takeIf { !addNote && templates.length() > 1 }
                ?.template(getColUnsafe)
                ?.name
    }

    private fun updateField(field: FieldEditText?): Boolean {
        val fieldContent = field!!.text?.toString() ?: ""
        val correctedFieldContent = convertToHtmlNewline(fieldContent, shouldReplaceNewlines())
        if (editorNote!!.values()[field.ord] != correctedFieldContent) {
            editorNote!!.values()[field.ord] = correctedFieldContent
            return true
        }
        return false
    }

    private fun tagsAsString(tags: List<String>): String = tags.joinToString(" ")

    private val currentlySelectedNotetype: NotetypeJson?
        get() = editorNote?.notetype

    val fieldsFromSelectedNote: Array<Array<String>>
        get() = editorNote!!.items()

    private fun currentNotetypeIsImageOcclusion() = currentlySelectedNotetype?.isImageOcclusion == true

    private fun setupImageOcclusionEditor(imagePath: String = "") {
        val args =
            if (addNote) {
                // if opened from an intent, the selected note type may not be suitable for IO
                val noteTypeId =
                    if (currentNotetypeIsImageOcclusion()) {
                        currentlySelectedNotetype!!.id
                    } else {
                        0
                    }
                ImageOcclusionArgs.Add(
                    noteTypeId = noteTypeId,
                    imagePath = imagePath,
                    originalDeckId = deckId,
                )
            } else {
                ImageOcclusionArgs.Edit(
                    noteId = editorNote!!.id,
                )
            }

        val intent = ImageOcclusion.getIntent(requireContext(), args)
        requestIOEditorCloser.launch(intent)
    }

    private fun changeNoteType(newId: NoteTypeId) {
        Timber.i("Changing note type to '%d", newId)

        if (editorNote!!.noteTypeId == newId) {
            return
        }

        if (getColUnsafe.notetypes.get(newId) == null) {
            Timber.w("New note type %s not found, not changing note type", newId)
            return
        }

        selectedNoteTypeId = newId
        // Preserves the editor's deck when the new note type has no remembered destination.
        getColUnsafe.defaultDeckForNoteType(newId)?.let { deckId = it }

        refreshNoteData(FieldChangeType.changeFieldCount(shouldReplaceNewlines()))
        updateFieldPresentation()
        requireActivity().invalidateMenu()
        delegate?.onNoteTypeChanged()
    }

    // ----------------------------------------------------------------------------
    // INNER CLASSES
    // ----------------------------------------------------------------------------
    private fun convertSelectedTextToCloze(
        textBox: FieldEditText,
        addClozeType: AddClozeType,
    ) {
        var nextClozeIndex = nextClozeIndex
        if (addClozeType == AddClozeType.SAME_NUMBER) {
            nextClozeIndex -= 1
        }
        val prefix = "{{c" + max(1, nextClozeIndex) + "::"
        val suffix = "}}"
        modifyCurrentSelection(TextWrapper(prefix, suffix), textBox)
    }

    // BUG: This assumes all fields are inserted as: {{cloze:Text}}
    private val nextClozeIndex: Int
        get() {
            // BUG: This assumes all fields are inserted as: {{cloze:Text}}
            val fieldValues: MutableList<String> =
                ArrayList(
                    editFields!!.size,
                )
            for (e in editFields!!) {
                val editable = e.text
                val fieldValue = editable?.toString() ?: ""
                fieldValues.add(fieldValue)
            }
            return ClozeUtils.getNextClozeIndex(fieldValues)
        }
    private val isClozeType: Boolean
        get() = currentlySelectedNotetype!!.isCloze

    private class ClozeColorSpan(
        color: Int,
    ) : ForegroundColorSpan(color),
        NoCopySpan

    private fun highlightClozes(text: Editable) {
        text.getSpans(0, text.length, ClozeColorSpan::class.java).forEach(text::removeSpan)
        if (editorNote?.notetype?.isCloze != true) return

        val emphasis = getColorFromAttr(requireContext(), CommonR.attr.foundationEmphasis)
        val openings = ArrayDeque<Int>()
        for (token in CLOZE_TOKEN.findAll(text)) {
            if (token.value == "}}") {
                val start = openings.removeLastOrNull() ?: continue
                val end = token.range.last + 1
                text.setSpan(ClozeColorSpan(emphasis), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                openings.addLast(token.range.first)
            }
        }
    }

    @VisibleForTesting
    fun setFieldValueFromUi(
        i: Int,
        newText: String?,
    ) {
        val editText = editFields!![i]
        editText.setText(newText)
        EditFieldTextWatcher(i).afterTextChanged(editText.text!!)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getFieldForTest(index: Int): FieldEditText = editFields!![index]

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setCurrentlySelectedNoteType(noteTypeId: NoteTypeId) {
        changeNoteType(noteTypeId)
    }

    /**
     * Whether sticky fields are currently being loaded. In this card, don't consider the text changed.
     */
    private var loadingStickyFields = false

    private inner class EditFieldTextWatcher(
        private val index: Int,
    ) : TextWatcher {
        override fun afterTextChanged(arg0: Editable) {
            highlightClozes(arg0)
            delegate?.onNoteTextChanged()

            if (!loadingStickyFields) {
                isFieldEdited = true
            }
            canCopyNote = editFields.orEmpty().any { !it.text.isNullOrEmpty() }
            if (index == 0) updateDuplicateWarning()
        }

        override fun beforeTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int,
        ) {
            // do nothing
        }

        override fun onTextChanged(
            arg0: CharSequence,
            arg1: Int,
            arg2: Int,
            arg3: Int,
        ) {
            // do nothing
        }
    }

    companion object {
        // DA 2020-04-13 - Refactoring Plans once tested:
        // * There is a difference in functionality depending on whether we are editing
        // * Extract mAddNote and mCurrentEditedCard into inner class. Gate mCurrentEditedCard on edit state.
        // * Possibly subclass
        // * Make this in memory and immutable for metadata, it's hard to reason about our state if we're modifying col
        // * Consider persistence strategy for temporary media. Saving after multimedia edit is probably too early, but
        // we don't want to risk the cache being cleared. Maybe add in functionality to remove from collection if added and
        // the action is cancelled?
        //    public static final String SOURCE_LANGUAGE = "SOURCE_LANGUAGE";
        //    public static final String TARGET_LANGUAGE = "TARGET_LANGUAGE";
        const val EXTRA_SOURCE_TEXT = "SOURCE_TEXT"
        const val EXTRA_TARGET_TEXT = "TARGET_TEXT"
        const val EXTRA_CALLER = "CALLER"
        const val EXTRA_CARD_ID = "CARD_ID"
        const val EXTRA_CONTENTS = "CONTENTS"
        private val CLOZE_TOKEN = Regex("""\{\{c\d+::|\}\}""")

        const val EXTRA_TAGS = "TAGS"
        const val EXTRA_ID = "ID"
        const val EXTRA_DID = "DECK_ID"
        const val EXTRA_NOTE_TYPE_ID = "NOTE_TYPE_ID"
        const val EXTRA_TEXT_FROM_SEARCH_VIEW = "SEARCH"
        const val EXTRA_EDIT_FROM_CARD_ID = "editCid"
        const val ACTION_CREATE_FLASHCARD = "org.openintents.action.CREATE_FLASHCARD"
        const val ACTION_CREATE_FLASHCARD_SEND = "android.intent.action.SEND"
        const val EXTRA_NOTE_CHANGED = "noteChanged"
        const val EXTRA_RELOAD_REQUIRED = "reloadRequired"
        private const val STATE_NOTE_CHANGED = "noteChanged"
        private const val STATE_RELOAD_REQUIRED = "reloadRequired"
        const val EXTRA_IMG_OCCLUSION = "image_uri"
        const val EXTRA_IN_CARD_BROWSER_ACTIVITY = "inCardBrowserActivity"
        const val EXTRA_CARD_IDS = "EXTRA_CARD_IDS"
        const val REQUEST_DECK_SELECTION_NOTE_EDITOR = "request_deck_selection_note_editor"
        private const val STATE_CALLER = "caller"

        // calling activity
        enum class NoteEditorCaller(
            val value: Int,
        ) {
            NO_CALLER(0),
            EDIT(1),
            STUDYOPTIONS(2),
            DECKPICKER(3),
            REVIEWER_ADD(11),
            CARDBROWSER_ADD(7),
            NOTEEDITOR(8),
            PREVIEWER_EDIT(9),
            NOTEEDITOR_INTENT_ADD(10),
            IMG_OCCLUSION(12),
            ADD_IMAGE(13),
            INSTANT_NOTE_EDITOR(14),
            ;

            companion object {
                fun fromValue(value: Int) = NoteEditorCaller.entries.first { it.value == value }
            }
        }

        const val RESULT_UPDATED_IO_NOTE = 11

        // preferences keys
        private const val PREF_NOTE_EDITOR_NEWLINE_REPLACE = "noteEditorNewlineReplace"
        private const val PREF_NOTE_EDITOR_CAPITALIZE = "note_editor_capitalize"
        private const val PREF_NOTE_EDITOR_FONT_SIZE = "note_editor_font_size"
        private const val PREF_NOTE_EDITOR_CUSTOM_BUTTONS = "note_editor_custom_buttons"

        fun newInstance(args: Bundle): NoteEditorFragment =
            NoteEditorFragment().apply {
                this.arguments = args
            }

        fun newInstance(destination: NoteEditorDestination): NoteEditorFragment =
            newInstance(with(DeferredNavigation) { destination.toBundle() })

        /** Default fragment arguments for "add a new note from the deck picker." */
        fun addNoteArgs(): Bundle = Bundle().apply { putInt(EXTRA_CALLER, NoteEditorCaller.DECKPICKER.value) }

        fun shouldReplaceNewlines(): Boolean =
            appContext
                .sharedPrefs()
                .getBoolean(PREF_NOTE_EDITOR_NEWLINE_REPLACE, true)

        @VisibleForTesting
        @CheckResult
        fun intentLaunchedWithImage(intent: Intent): Boolean {
            if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_VIEW) return false
            if (ImportUtils.isInvalidViewIntent(intent)) return false
            return intent.resolveMimeType()?.startsWith("image/") == true
        }
    }
}
