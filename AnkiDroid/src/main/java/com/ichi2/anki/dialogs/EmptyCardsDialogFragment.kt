// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.fragment.app.viewModels
import anki.card_rendering.EmptyCardsReport
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.dialogs.EmptyCardsUiState.EmptyCardsSearchFailure
import com.ichi2.anki.dialogs.EmptyCardsUiState.EmptyCardsSearchResult
import com.ichi2.anki.dialogs.EmptyCardsUiState.SearchingForEmptyCards
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.emptyCids
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.ProgressDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

/**
 * A dialog that searches for empty cards and presents the user with the option to delete them.
 * A user may 'keep notes', which retains the first card of each note, even if the note is empty.
 */
class EmptyCardsDialogFragment : AnalyticsDialogFragment() {
    private val viewModel by viewModels<EmptyCardsViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.searchForEmptyCards()
        return createPaperDialog(requireContext()) { EmptyCardsContent() }
    }

    @Composable
    private fun EmptyCardsContent() {
        val state by viewModel.uiState.collectAsState()
        val title = TR.sentenceCase.emptyCardsTitle
        when (val result = state) {
            is SearchingForEmptyCards ->
                ProgressDialog(title = title, message = getString(CommonString.emtpy_cards_finding), onCancel = ::dismiss)
            is EmptyCardsSearchFailure ->
                ErrorDialog(
                    title = getString(CommonString.vague_error),
                    message = title,
                    details = result.throwable.toString(),
                    onDismissRequest = ::dismiss,
                    primaryAction = DialogAction(getString(CommonString.dialog_ok), ::dismiss),
                    dismissText = null,
                )
            is EmptyCardsSearchResult -> {
                val report = result.emptyCardsReport
                val hasEmptyCards = remember(report) { report.emptyCids().isNotEmpty() }
                if (!hasEmptyCards) {
                    ConfirmationDialog(
                        title = title,
                        message = TR.emptyCardsNotFound(),
                        confirm = DialogAction(getString(CommonString.dialog_ok), ::dismiss),
                        onDismissRequest = ::dismiss,
                        dismissText = null,
                    )
                } else {
                    var preserveNotes by rememberSaveable { mutableStateOf(true) }
                    ConfirmationDialog(
                        title = title,
                        message = "",
                        confirm =
                            DialogAction(getString(CommonString.dialog_positive_delete), {
                                (requireActivity() as DeckPicker).startDeletingEmptyCards(report, preserveNotes)
                                dismiss()
                            }, DialogActionStyle.Destructive),
                        onDismissRequest = ::dismiss,
                    ) {
                        EmptyCardsReportLinks(report)
                        DialogChoiceRow(
                            choice = DialogChoice(TR.emptyCardsPreserveNotesCheckbox()),
                            checked = preserveNotes,
                            onClick = { preserveNotes = !preserveNotes },
                            multiple = true,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyCardsReportLinks(report: EmptyCardsReport) {
        val emphasis = MaterialTheme.foundation.colors.emphasis
        val text =
            remember(report, emphasis) {
                val html = AnnotatedString.fromHtml(report.report)
                val links = TextLinkStyles(style = SpanStyle(color = emphasis, textDecoration = TextDecoration.Underline))
                buildAnnotatedString {
                    var start = 0
                    noteIdPattern.findAll(html).forEach { match ->
                        append(html.subSequence(start, match.range.first))
                        val id = match.groupValues[1]
                        withLink(
                            LinkAnnotation.Clickable(tag = id, styles = links) {
                                navigate(BrowserDestination.Search(query = "nid:$id", allDecks = true))
                            },
                        ) { append(id) }
                        start = match.range.last + 1
                    }
                    append(html.subSequence(start, html.length))
                }
            }
        Text(text)
    }

    companion object {
        const val TAG = "EmptyCardsDialog"
        private val noteIdPattern = Regex("""\[anki:nid:(\d+)\]""")
    }
}

// TODO the fragment should just send a fragment result to the DeckPicker with the report and keep
//  notes flag(currently the report can exceed the transport Bundle capacity so we call it directly)
fun DeckPicker.startDeletingEmptyCards(
    report: EmptyCardsReport,
    keepNotes: Boolean,
) {
    Timber.i(
        "Starting to delete found %d empty cards, keepNotes: %b",
        report.emptyCids().size,
        keepNotes,
    )
    launchCatchingTask {
        viewModel.deleteEmptyCards(report, keepNotes).join()
    }
}
