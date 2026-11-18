// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.tags

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.RadioGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.RecyclerView
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.OnContextAndLongClickListener
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.databinding.DialogTagsBinding
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.libanki.withCollapsedWhitespace
import com.ichi2.anki.model.CardStateFilter
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.StableScrollbarLayoutManager
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.ui.AccessibleSearchView
import com.ichi2.utils.DisplayUtils.setDialogKeyboardResize
import com.ichi2.utils.TagsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @VisibleForTesting
    internal lateinit var binding: DialogTagsBinding
        private set

    private var type: DialogType? = null
    internal val isEditingTags: Boolean get() = type == DialogType.EDIT_TAGS
    private var tagsArrayAdapter: TagsArrayAdapter? = null
    private var toolbarSearchView: AccessibleSearchView? = null
    private var toolbarSearchItem: MenuItem? = null
    private val listener: TagsDialogListener?

    private lateinit var selectedOption: CardStateFilter

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
        isCancelable = true
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
        binding = DialogTagsBinding.inflate(layoutInflater)

        val tagsListLayout: RecyclerView.LayoutManager = StableScrollbarLayoutManager(requireContext())
        binding.tagsList.apply {
            requestFocus()
            layoutManager = tagsListLayout
        }
        binding.optionsGroup.apply {
            isVisible = type != DialogType.EDIT_TAGS && type != DialogType.CUSTOM_STUDY
            for (i in 0 until childCount) {
                getChildAt(i).id = i
            }
            check(0)
        }
        selectedOption = radioButtonIdToCardState(binding.optionsGroup.checkedRadioButtonId)
        binding.optionsGroup.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            selectedOption = radioButtonIdToCardState(checkedId)
        }

        adjustToolbar(binding.root)

        var loaded by mutableStateOf(false)
        val dialog =
            createPaperDialog(requireContext()) {
                PaperDialog(
                    title = getString(if (isEditingTags) CommonString.card_details_tags else CommonString.studyoptions_limit_select_tags),
                    onDismissRequest = ::dismiss,
                    scrollContent = false,
                    actions =
                        listOf(
                            DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                            DialogAction(confirmLabel(), {
                                onPositiveButton()
                                dismiss()
                            }, DialogActionStyle.Filled, enabled = loaded),
                        ),
                ) {
                    AndroidView(factory = { binding.root }, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp))
                }
            }

        lifecycleScope.launch {
            val showProgressJob =
                launch {
                    delay(600.milliseconds)
                    withContext(Dispatchers.Main) {
                        binding.loadingContainer.visibility = View.VISIBLE
                        viewModel.initProgress
                            .flowWithLifecycle(lifecycle)
                            .onEach { progress ->
                                binding.progressText.text =
                                    when (progress) {
                                        TagsDialogViewModel.InitProgress.Processing ->
                                            getString(CommonString.dialog_processing)
                                        is TagsDialogViewModel.InitProgress.FetchingNoteTags ->
                                            "${progress.noteNumber}/${progress.noteCount}"
                                        TagsDialogViewModel.InitProgress.Finished -> null
                                    }
                            }.launchIn(lifecycleScope)
                    }
                }
            val tags = viewModel.tags.await()

            tagsArrayAdapter = TagsArrayAdapter(tags) { binding.root.showMaxTagSelectedNotice(tags) }
            binding.tagsList.adapter = tagsArrayAdapter
            if (tags.isEmpty) {
                binding.noTagsTextView.visibility = View.VISIBLE
            }
            tagsArrayAdapter?.tagContextAndLongClickListener =
                if (type == DialogType.EDIT_TAGS) {
                    OnContextAndLongClickListener { v ->
                        createAddTagDialog(v.tag as String)
                        true
                    }
                } else {
                    OnContextAndLongClickListener { false }
                }
            showProgressJob.cancel()
            binding.loadingContainer.isVisible = false
            loaded = true
        }

        dialog.updateSoftInputMode(resources.configuration)

        return dialog
    }

    private fun View.showMaxTagSelectedNotice(tags: TagsList) {
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
            this.showSnackbar(userFacingText)
        }
    }

    private fun onPositiveButton() {
        lifecycleScope.launch {
            val tags = viewModel.tags.await()
            tagsDialogListener.onSelectedTags(
                tags.copyOfCheckedTagList(),
                tags.copyOfPartiallySelectedTagList(),
                selectedOption,
            )
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

    private fun radioButtonIdToCardState(id: Int) =
        when (id) {
            0 -> CardStateFilter.ALL_CARDS
            1 -> CardStateFilter.NEW
            2 -> CardStateFilter.DUE
            else -> {
                Timber.w("unexpected value: %d", id)
                CardStateFilter.ALL_CARDS
            }
        }

    private fun adjustToolbar(tagsDialogView: View) {
        val toolbar: Toolbar = binding.toolbar.root

        val toolbarAddItem = toolbar.menu.findItem(R.id.tags_dialog_action_add)
        val drawable = ContextCompat.getDrawable(requireContext(), CommonR.drawable.symbol_add)
        drawable?.setTint(getColorFromAttr(requireContext(), CommonR.attr.foundationOnBackground))
        toolbarAddItem.icon = drawable

        toolbarAddItem.setOnMenuItemClickListener {
            val query = toolbarSearchView!!.query.toString()
            if (toolbarSearchItem!!.isActionViewExpanded && query.isNotEmpty()) {
                addTag(query)
                toolbarSearchView!!.setQuery("", true)
            } else {
                createAddTagDialog(null)
            }
            true
        }
        toolbarSearchItem = toolbar.menu.findItem(R.id.tags_dialog_action_filter)
        val toolbarSearchItem: MenuItem? = toolbarSearchItem
        toolbarSearchView = toolbarSearchItem?.actionView as AccessibleSearchView
        val queryET = toolbarSearchView!!.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        queryET.filters = arrayOf(addTagFilter)
        toolbarSearchView!!.queryHint = getString(CommonString.filter_tags)
        toolbarSearchView!!.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    toolbarSearchView!!.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    tagsArrayAdapter?.filter?.filter(newText)
                    return true
                }
            },
        )
        val checkAllItem = toolbar.menu.findItem(R.id.tags_dialog_action_select_all)
        checkAllItem.setOnMenuItemClickListener {
            launchCatchingTask {
                val tags = viewModel.tags.await()
                val didChange = tags.toggleAllCheckedStatuses()
                if (didChange) {
                    tagsArrayAdapter?.notifyCheckedStatusesChanged()
                    view?.showMaxTagSelectedNotice(tags)
                }
            }
            true
        }
        if (type == DialogType.EDIT_TAGS) {
            toolbarSearchView!!.queryHint = getString(CommonString.add_new_filter_tags)
        } else {
            toolbarAddItem.isVisible = false
        }
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
                binding.noTagsTextView.isVisible = false
                tags.add(tag)
                feedbackText = getString(CommonString.tag_editor_add_feedback, tag, confirmLabel())
            } else {
                feedbackText = getString(CommonString.tag_editor_add_feedback_existing, tag)
            }
            tags.check(tag)
            tagsArrayAdapter?.sortData()
            tagsArrayAdapter?.notifyDataSetChanged()
            // Expand to reveal the newly added tag.
            tagsArrayAdapter?.filter?.apply {
                setExpandTarget(tag)
                refresh()
            }

            // Show a snackbar to let the user know the tag was added successfully
            binding.tagsDialogSnackbar.showSnackbar(feedbackText)
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
