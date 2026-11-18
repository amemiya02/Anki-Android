// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.REQUEST_FIND_AND_REPLACE
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_FIELD
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_MATCH_CASE
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_ONLY_SELECTED_NOTES
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_REGEX
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_REPLACEMENT
import com.ichi2.anki.browser.FindAndReplaceDialogFragment.Companion.RESULT_SEARCH
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.notetype.ManageNotetypes
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogField
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

/**
 * Dialog that shows the options for finding and replacing the text of notes in [CardBrowser].
 *
 * Note for completeness:
 *
 * Desktop also shows the fields of a note in the browser and the user can right-click on one of
 * them to start a find and replace only for that field. We display the fields only in
 * [ManageNotetypes] which doesn't feel like it should have this feature.
 * (see https://github.com/ankitects/anki/blob/64ca90934bc26ddf7125913abc9dd9de8cb30c2b/qt/aqt/browser/sidebar/tree.py#L1074)
 */
// TODO desktop offers history for inputs
class FindAndReplaceDialogFragment : AnalyticsDialogFragment() {
    private val idsFile: IdsFile by lazy {
        requireArguments().requireParcelable(ARG_IDS)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = createPaperDialog(requireContext()) { FindAndReplaceContent() }

    @Composable
    private fun FindAndReplaceContent() {
        val selection by produceState<Selection?>(null) {
            val noteIds = runCatching { idsFile.getIdsOrNull() }.getOrNull()
            if (noteIds == null) {
                requireActivity().showSnackbar(CommonString.something_wrong)
                dismiss()
            } else {
                val choices =
                    buildList {
                        add(FieldChoice(ALL_FIELDS_AS_FIELD, TR.sentenceCase.allFields))
                        add(FieldChoice(TAGS_AS_FIELD, TR.editingTags()))
                        addAll(withCol { fieldNamesForNoteIds(noteIds) }.map { FieldChoice(it, it) })
                    }
                value = Selection(noteIds.isNotEmpty(), choices)
            }
        }
        val loaded = selection
        var search by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
        var replacement by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
        var selectedField by rememberSaveable { mutableStateOf(ALL_FIELDS_AS_FIELD) }
        var selectedOnlyOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
        var ignoreCase by rememberSaveable { mutableStateOf(true) }
        var regex by rememberSaveable { mutableStateOf(false) }
        var choosingField by rememberSaveable { mutableStateOf(false) }
        val selectedOnly = loaded?.hasSelectedNotes == true && selectedOnlyOverride != false
        val field = loaded?.fields?.firstOrNull { it.key == selectedField }
        val fieldLabel = HtmlCompat.fromHtml(TR.browsingIn(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        BackHandler(choosingField) { choosingField = false }

        if (choosingField && loaded != null) {
            SingleChoiceDialog(
                title = fieldLabel,
                choices = loaded.fields.map { DialogChoice(it.label) },
                selectedIndex = loaded.fields.indexOfFirst { it.key == selectedField }.takeIf { it >= 0 },
                onSelect = {
                    selectedField = loaded.fields[it].key
                    choosingField = false
                },
                onDismissRequest = { choosingField = false },
            )
            return
        }

        PaperDialog(
            title = TR.sentenceCase.findAndReplace,
            onDismissRequest = ::dismiss,
            actions =
                listOf(
                    DialogAction(getString(CommonString.help), {
                        dismiss()
                        openUrl(R.string.link_manual_browser_find_replace)
                    }),
                    DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                    DialogAction(
                        getString(CommonString.dialog_positive_replace),
                        {
                            if (field != null && search.text.isNotEmpty()) {
                                submit(FindReplaceResult(search.text, replacement.text, field.key, selectedOnly, !ignoreCase, regex))
                                dismiss()
                            }
                        },
                        DialogActionStyle.Filled,
                        enabled = field != null && search.text.isNotEmpty(),
                    ),
                ),
        ) {
            if (loaded == null || field == null) {
                CircularProgressIndicator()
            } else {
                DialogField(
                    label = HtmlCompat.fromHtml(TR.browsingFind(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                    value = search,
                    onValueChange = { search = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                DialogField(
                    label = HtmlCompat.fromHtml(TR.browsingReplaceWith(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                    value = replacement,
                    onValueChange = { replacement = it },
                )
                SettingsRow(
                    title = fieldLabel,
                    value = field.label,
                    onClick = { choosingField = true },
                    trailing = { SettingsChevron() },
                )
                DialogChoiceRow(
                    DialogChoice(TR.browsingSelectedNotesOnly(), enabled = loaded.hasSelectedNotes),
                    selectedOnly,
                    { selectedOnlyOverride = !selectedOnly },
                    multiple = true,
                )
                DialogChoiceRow(DialogChoice(TR.browsingIgnoreCase()), ignoreCase, { ignoreCase = !ignoreCase }, multiple = true)
                DialogChoiceRow(
                    DialogChoice(TR.browsingTreatInputAsRegularExpression()),
                    regex,
                    { regex = !regex },
                    multiple = true,
                )
            }
        }
    }

    private data class FieldChoice(
        val key: String,
        val label: String,
    )

    private data class Selection(
        val hasSelectedNotes: Boolean,
        val fields: List<FieldChoice>,
    )

    private fun submit(result: FindReplaceResult) {
        Timber.i("Sending request to find and replace...")
        setFragmentResult(
            REQUEST_FIND_AND_REPLACE,
            Bundle().apply {
                putString(RESULT_SEARCH, result.search)
                putString(RESULT_REPLACEMENT, result.replacement)
                putString(RESULT_FIELD, result.field)
                putBoolean(RESULT_ONLY_SELECTED_NOTES, result.onlyOnSelectedNotes)
                putBoolean(RESULT_MATCH_CASE, result.matchCase)
                putBoolean(RESULT_REGEX, result.regex)
            },
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity?.isChangingConfigurations != true) idsFile.removeSafely(TAG)
    }

    companion object {
        const val TAG = "FindAndReplaceDialogFragment"
        const val REQUEST_FIND_AND_REPLACE = "request_find_and_replace"
        const val ARG_IDS = "arg_ids"
        const val RESULT_SEARCH = "arg_search"
        const val RESULT_REPLACEMENT = "arg_replacement"
        const val RESULT_FIELD = "arg_field"
        const val RESULT_ONLY_SELECTED_NOTES = "arg_only_selected_notes"
        const val RESULT_MATCH_CASE = "arg_match_case"
        const val RESULT_REGEX = "arg_regex"

        /**
         * Receiving this value in the result [Bundle] for the [RESULT_FIELD] entry means that
         * the user selected "All fields" as the field target for the find and replace action.
         */
        const val ALL_FIELDS_AS_FIELD = "find_and_replace_dialog_fragment_all_fields_as_field"

        /**
         * Receiving this value in the result [Bundle] for the [RESULT_FIELD] entry means that
         * the user selected "Tags" as the field target for the find and replace action.
         */
        const val TAGS_AS_FIELD = "find_and_replace_dialog_fragment_tags_as_field"

        fun newInstance(
            context: Context,
            noteIds: List<NoteId>,
        ): FindAndReplaceDialogFragment {
            val file = IdsFile(IdsFile.getDirectory(), noteIds, IdsFile.Purpose.FIND_AND_REPLACE)
            return FindAndReplaceDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(ARG_IDS, file)
                    }
            }
        }
    }
}

fun CardBrowser.registerFindReplaceHandler(action: (FindReplaceResult) -> Unit) {
    setFragmentResultListener(REQUEST_FIND_AND_REPLACE) { _, bundle -> action(bundle.toFindReplaceResult()) }
}

internal fun Fragment.registerBrowserFindReplaceHandler(action: (FindReplaceResult) -> Unit) {
    parentFragmentManager.setFragmentResultListener(REQUEST_FIND_AND_REPLACE, viewLifecycleOwner) { _, bundle ->
        action(bundle.toFindReplaceResult())
    }
}

private fun Bundle.toFindReplaceResult() =
    FindReplaceResult(
        search = getString(RESULT_SEARCH) ?: error("Missing required argument: search"),
        replacement = getString(RESULT_REPLACEMENT) ?: error("Missing required argument: replacement"),
        field = getString(RESULT_FIELD) ?: error("Missing required argument: field"),
        onlyOnSelectedNotes = getBoolean(RESULT_ONLY_SELECTED_NOTES, true),
        matchCase = getBoolean(RESULT_MATCH_CASE, false),
        regex = getBoolean(RESULT_REGEX, false),
    )

data class FindReplaceResult(
    val search: String,
    val replacement: String,
    val field: String,
    val onlyOnSelectedNotes: Boolean,
    val matchCase: Boolean,
    val regex: Boolean,
)
