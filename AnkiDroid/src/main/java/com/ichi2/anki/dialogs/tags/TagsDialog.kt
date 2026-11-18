// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.tags

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter
import android.text.Spanned
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.libanki.withCollapsedWhitespace
import com.ichi2.anki.model.CardStateFilter
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.DisplayUtils.setDialogKeyboardResize
import com.ichi2.utils.TagsUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import com.ichi2.anki.common.android.R as CommonR

class TagsDialog : AnalyticsDialogFragment {
    /**
     * Enum that define all possible types of TagsDialog
     */
    @Parcelize
    enum class DialogType : Parcelable {
        /**
         * Edit tags of note(s)
         */
        EDIT_TAGS,

        /**
         * Filter notes by tags
         */
        FILTER_BY_TAG,

        CUSTOM_STUDY,
    }

    private var type: DialogType? = null
    internal val isEditingTags: Boolean get() = type == DialogType.EDIT_TAGS
    private var picker by mutableStateOf<TagPickerState?>(null)
    private var query by mutableStateOf("")
    private var feedback by mutableStateOf<String?>(null)
    private val listener: TagsDialogListener?

    private var selectedOption by mutableStateOf(CardStateFilter.ALL_CARDS)

    @VisibleForTesting
    val viewModel: TagsDialogViewModel by viewModels {
        viewModelFactory {
            initializer {
                val idsFile = requireArguments().requireParcelable<IdsFile>(ARG_TAGS_FILE)
                val noteIds = idsFile.getIdsOrNull() ?: throw IOException("Tag selection is no longer available")
                val checkedTags =
                    requireNotNull(requireArguments().getStringArrayList(ARG_CHECKED_TAGS)) {
                        "$ARG_CHECKED_TAGS is required"
                    }
                val type = BundleCompat.getParcelable(requireArguments(), ARG_DIALOG_TYPE, DialogType::class.java)
                val isCustomStudying = type != null && type == DialogType.CUSTOM_STUDY
                TagsDialogViewModel(
                    noteIds = noteIds,
                    checkedTags = checkedTags,
                    isCustomStudying = isCustomStudying,
                )
            }
        }
    }

    /**
     * Constructs a new [TagsDialog] that will communicate the results using the provided listener.
     */
    constructor(listener: TagsDialogListener?) {
        this.listener = listener
    }

    /**
     * Constructs a new [TagsDialog] that will communicate the results using Fragment Result API.
     *
     * @see [Fragment Result API](https://developer.android.com/guide/fragments/communicate.fragment-result)
     */
    constructor() {
        listener = null
    }

    /**
     * Construct a tags dialog for a collection of notes
     *
     * @param type the type of dialog @see [DialogType]
     * @return Initialized instance of [TagsDialog]
     */
    fun withArguments(
        context: Context,
        type: DialogType,
        noteIds: List<NoteId> = emptyList(),
        checkedTags: ArrayList<String> = arrayListOf(),
    ): TagsDialog {
        // TODO: checkedTags is unbounded and could exceed the bundle size
        val file = IdsFile(IdsFile.getDirectory(), noteIds, IdsFile.Purpose.TAGS)
        arguments = this.arguments ?: Bundle().apply {
            putParcelable(ARG_TAGS_FILE, file)
            putParcelable(ARG_DIALOG_TYPE, type)
            putStringArrayList(ARG_CHECKED_TAGS, checkedTags)
        }
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = requireArguments().requireParcelable(ARG_DIALOG_TYPE)
        query = savedInstanceState?.getString("tagQuery").orEmpty()
        selectedOption = savedInstanceState?.getString("tagCardState")?.let(CardStateFilter::valueOf) ?: CardStateFilter.ALL_CARDS
        isCancelable = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tagQuery", query)
        outState.putString("tagCardState", selectedOption.name)
    }

    private val tagsDialogListener: TagsDialogListener
        get() =
            listener
                ?: TagsDialogListener.createFragmentResultSender(parentFragmentManager)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Resolve the selection before creating any controls that could submit an operation.
        try {
            viewModel
        } catch (e: IOException) {
            Timber.w(e, "Failed to read tag dialog IDs")
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            dismiss()
            return super.onCreateDialog(savedInstanceState)
        }
        return createPaperDialog(requireContext()) {
            var searching by rememberSaveable { mutableStateOf(false) }
            var showProgress by remember { mutableStateOf(false) }
            LaunchedEffect(viewModel) { picker = TagPickerState(viewModel.tags.await()).also { if (query.isNotEmpty()) it.filter(query) } }
            LaunchedEffect(picker) {
                if (picker == null) {
                    delay(600.milliseconds)
                    showProgress = true
                }
            }
            val state = picker
            PaperDialog(
                title = getString(if (isEditingTags) CommonString.card_details_tags else CommonString.studyoptions_limit_select_tags),
                onDismissRequest = ::dismiss,
                scrollContent = false,
                actions =
                    listOf(
                        DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                        DialogAction(confirmLabel(), {
                            if (state != null) {
                                tagsDialogListener.onSelectedTags(
                                    state.tags.copyOfCheckedTagList(),
                                    state.tags.copyOfPartiallySelectedTagList(),
                                    selectedOption,
                                )
                                dismiss()
                            }
                        }, DialogActionStyle.Filled, enabled = state != null),
                    ),
            ) {
                if (state == null) {
                    if (showProgress) {
                        LinearProgressIndicator()
                        val progress by viewModel.initProgress.collectAsState()
                        val fetching = progress as? TagsDialogViewModel.InitProgress.FetchingNoteTags
                        Text(fetching?.let { "${it.noteNumber}/${it.noteCount}" } ?: getString(CommonString.dialog_processing))
                    }
                    return@PaperDialog
                }
                if (searching) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = applyTagFilter(it)
                            state.filter(query)
                        },
                        label = { Text(getString(if (isEditingTags) CommonString.add_new_filter_tags else CommonString.filter_tags)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row {
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) {
                            query = ""
                            state.filter(query)
                        }
                    }) { Icon(painterResource(CommonR.drawable.symbol_search), getString(CommonString.filter_tags)) }
                    if (isEditingTags) {
                        IconButton(onClick = {
                            if (searching && query.isNotEmpty()) {
                                val tag = query
                                query = ""
                                addTag(tag)
                            } else {
                                createAddTagDialog(null)
                            }
                        }) { Icon(painterResource(CommonR.drawable.symbol_add), getString(CommonString.add_tag)) }
                    }
                    IconButton(onClick = {
                        state.toggleAll()
                        showMaxTagSelectedNotice(state.tags)
                    }) { Icon(painterResource(R.drawable.symbol_select_all), getString(CommonString.check_all_tags)) }
                }
                TagPickerList(
                    state,
                    onChecked = {
                        showMaxTagSelectedNotice(state.tags)
                    },
                    onAddChild = if (isEditingTags) ::createAddTagDialog else null,
                    header = {
                        if (type == DialogType.FILTER_BY_TAG) {
                            listOf(
                                CardStateFilter.ALL_CARDS to CommonString.tags_dialog_option_all_cards,
                                CardStateFilter.NEW to CommonString.tags_dialog_option_new_cards,
                                CardStateFilter.DUE to CommonString.tags_dialog_option_due_cards,
                            ).forEach { (option, label) ->
                                DialogChoiceRow(DialogChoice(getString(label)), checked = selectedOption == option, onClick = {
                                    selectedOption =
                                        option
                                }, multiple = false)
                            }
                        }
                        if (state.tags.isEmpty) Text(getString(CommonString.no_tags))
                    },
                    modifier =
                        Modifier
                            .weight(
                                1f,
                                fill = false,
                            ).heightIn(max = 360.dp),
                )
                feedback?.let { message ->
                    Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.foundation.type.caption)
                    LaunchedEffect(message) {
                        delay(4_000)
                        feedback = null
                    }
                }
            }
        }.also { it.updateSoftInputMode(resources.configuration) }
    }

    private fun showMaxTagSelectedNotice(tags: TagsList) {
        if (type == DialogType.CUSTOM_STUDY && tags.copyOfCheckedTagList().size > 100) {
            // the backend text is long and is more like an explanation and doesn't fit into
            // a snackbar so cut just for the first sentence:
            //  "A maximum of 100 tags can be selected."
            val backendText = withCollapsedWhitespace(TR.errors100TagsMax())
            val firstPointIndex = backendText.indexOf(".")
            val userFacingText =
                if (firstPointIndex < 0) {
                    // backend text was changed so just return the full text
                    backendText
                } else {
                    backendText.substring(0..firstPointIndex)
                }
            feedback = userFacingText
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.updateSoftInputMode(newConfig)
    }

    private fun Dialog.updateSoftInputMode(configuration: Configuration) {
        setDialogKeyboardResize(this, resize = configuration.screenHeightDp >= COMPACT_HEIGHT_DP)
    }

    /**
     * Create an add tag dialog.
     *
     * @param prefixTag: The tag to be prefilled into the EditText section. A trailing '::' will be appended.
     */
    private fun createAddTagDialog(prefixTag: String?) {
        createPaperDialog(requireContext()) { dialog ->
            val tags by produceState<TagsList?>(null) { value = viewModel.tags.await() }
            TextInputDialog(
                title = getString(CommonString.add_tag),
                label = TR.actionsName().dropLastWhile { it == ':' },
                confirmText = getString(CommonString.menu_add),
                onDismissRequest = dialog::dismiss,
                initialText = prefixTag?.let { applyTagFilter("$it ") }.orEmpty(),
                capitalization = KeyboardCapitalization.None,
                enabled = tags != null,
                transformInput = { input ->
                    if (' ' !in input.text) {
                        input
                    } else {
                        TextFieldValue(
                            applyTagFilter(input.text),
                            selection =
                                TextRange(
                                    applyTagFilter(input.text.take(input.selection.start)).length,
                                    applyTagFilter(input.text.take(input.selection.end)).length,
                                ),
                        )
                    }
                },
                errorMessageFor = { tag ->
                    when {
                        tag.isBlank() -> ""
                        tags?.contains(TagsUtil.getUniformedTag(tag.trim())) == true -> getString(CommonString.tag_already_exists)
                        else -> null
                    }
                },
                onConfirm = {
                    addTag(it)
                    dialog.dismiss()
                },
            )
        }.show()
    }

    private fun applyTagFilter(text: String): String = addTagFilter.filter(text, 0, text.length, null, 0, 0)?.toString() ?: text

    private fun confirmLabel(): String = getString(if (isEditingTags) CommonString.dialog_confirm else CommonString.select)

    @VisibleForTesting
    fun addTag(rawTag: String?) {
        if (rawTag.isNullOrEmpty()) return
        lifecycleScope.launch {
            val tags = viewModel.tags.await()
            val tag = TagsUtil.getUniformedTag(rawTag)
            val feedbackText: String
            if (tags.add(tag)) {
                feedbackText = getString(CommonString.tag_editor_add_feedback, tag, confirmLabel())
            } else {
                feedbackText = getString(CommonString.tag_editor_add_feedback_existing, tag)
            }
            tags.check(tag)
            tags.sort()
            picker?.filter(query, expandTarget = tag)
            feedback = feedbackText
        }
    }

    companion object {
        const val ARG_TAGS_FILE = "tagsFile"
        private const val ARG_DIALOG_TYPE = "dialogType"
        private const val ARG_CHECKED_TAGS = "checkedTags"
        private const val COMPACT_HEIGHT_DP = 480

        /**
         * The filter that constrains the inputted tag.
         * Space is not allowed in a tag. For UX of hierarchical tag, inputting a space will instead
         * insert "::" at the cursor. If there are already some colons in front of the cursor,
         * complete to 2 colons. For example:
         *   "tag"   -- input a space --> "tag::"
         *   "tag:"  -- input a space --> "tag::"
         *   "tag::" -- input a space --> "tag::"
         */
        private val addTagFilter =
            InputFilter { source: CharSequence, start: Int, end: Int, dest: Spanned?, destStart: Int, _: Int ->
                if (!source.subSequence(start, end).contains(' ')) {
                    return@InputFilter null
                }
                var previousColonsCnt = 0
                if (dest != null) {
                    val previousPart = dest.take(destStart)
                    if (previousPart.endsWith("::")) {
                        previousColonsCnt = 2
                    } else if (previousPart.endsWith(":")) {
                        previousColonsCnt = 1
                    }
                }
                val sb = StringBuilder()
                for (char in source.subSequence(start, end)) {
                    if (char == ' ') {
                        if (previousColonsCnt == 0) {
                            sb.append("::")
                        } else if (previousColonsCnt == 1) {
                            sb.append(":")
                        }
                    } else {
                        sb.append(char)
                    }
                    previousColonsCnt =
                        if (char == ':') {
                            previousColonsCnt + 1
                        } else {
                            0
                        }
                }
                sb
            }
    }
}
