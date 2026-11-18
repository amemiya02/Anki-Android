// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.mediacheck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import anki.media.CheckMediaResponse
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.CrashReportData.Companion.toCrashReportData
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.ui.internationalization.trCase
import com.ichi2.anki.utils.observeMessages
import com.ichi2.anki.utils.showAndAwaitDismissal
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

/**
 * MediaCheckFragment for displaying a list of media files that are either unused or missing.
 * It allows users to tag missing media files or delete unused ones.
 **/
class MediaCheckFragment : Fragment() {
    private val viewModel: MediaCheckViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AnkiDroidTheme {
                val result by viewModel.mediaCheckResult.collectAsState()
                MediaCheckScreen(
                    result = result,
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onRestore = viewModel::restoreTrash,
                    onEmptyTrash = viewModel::deleteTrash,
                    onTagMissing = { viewModel.tagMissing(TR.mediaCheckMissingMediaTag()) },
                    onDeleteUnused = ::deleteConfirmationDialog,
                )
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeProgress(viewModel) { progress -> getString(progress.messageRes) }
        viewModel.checkMedia()
        observeMessages(viewModel.pendingMessages, viewModel::messageShown, ::showMessage)
    }

    private fun deleteConfirmationDialog() {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = TR.sentenceCase.checkMediaDeleteUnused,
                message = TR.mediaCheckDeleteUnusedConfirm(),
                confirm =
                    DialogAction(getString(CommonString.dialog_positive_delete), {
                        dialog.dismiss()
                        viewModel.deleteUnusedMedia()
                    }, DialogActionStyle.Destructive),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    private suspend fun showMessage(message: MediaCheckMessage) {
        when (message) {
            is MediaCheckMessage.TagsAdded ->
                showResultDialog(
                    CommonString.check_media_tags_added,
                    TR.browsingNotesUpdated(message.notesUpdated),
                )
            is MediaCheckMessage.MediaDeleted ->
                showResultDialog(
                    CommonString.delete_media_result_title,
                    resources.getQuantityString(CommonPlurals.delete_media_result_message, message.count, message.count),
                )
            MediaCheckMessage.TrashEmptied -> showResultDialog(message = TR.mediaCheckTrashEmptied())
            MediaCheckMessage.TrashRestored -> showResultDialog(message = TR.mediaCheckTrashRestored())
            is MediaCheckMessage.Failed -> showErrorDialog(message.exception)
        }
    }

    private suspend fun showResultDialog(
        @StringRes titleRes: Int? = null,
        message: String,
    ) {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = titleRes?.let(::getString).orEmpty(),
                message = message,
                confirm =
                    DialogAction(getString(CommonString.dialog_ok), {
                        finishScreen()
                        dialog.dismiss()
                    }),
                onDismissRequest = dialog::dismiss,
                dismissText = null,
            )
        }.apply { setCancelable(false) }.showAndAwaitDismissal()
    }

    private suspend fun showErrorDialog(exception: Exception) {
        createPaperDialog(requireContext()) { dialog ->
            ErrorDialog(
                title = getString(CommonString.vague_error),
                message = exception.localizedMessage ?: exception.toString(),
                onDismissRequest = dialog::dismiss,
                dismissText = null,
                primaryAction = DialogAction(getString(CommonString.dialog_ok), dialog::dismiss),
            )
        }.showAndAwaitDismissal()
        exception.toCrashReportData(requireActivity()).sendCrashReport()
    }

    companion object {
        fun getIntent(context: Context): Intent = SingleFragmentActivity.getIntent(context, MediaCheckFragment::class)
    }
}

@get:StringRes
private val MediaCheckProgress.messageRes: Int
    get() =
        when (this) {
            MediaCheckProgress.CHECKING_MEDIA -> CommonString.check_media_message
            MediaCheckProgress.ADDING_TAGS -> CommonString.check_media_adding_missing_tag
            MediaCheckProgress.DELETING_MEDIA -> CommonString.delete_media_message
        }

@Composable
private fun MediaCheckScreen(
    result: CheckMediaResponse?,
    onBack: () -> Unit,
    onRestore: () -> Unit,
    onEmptyTrash: () -> Unit,
    onTagMissing: () -> Unit,
    onDeleteUnused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = result?.report.orEmpty()
    val lines = remember(report) { report.lines() }
    val paper = MaterialTheme.foundation
    SettingsPage(trCase().checkMediaTitle(), onBack, modifier) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (result?.haveTrash == true) {
                item { TextButton(onClick = onRestore) { Text(trCase().restoreDeleted()) } }
                item { TextButton(onClick = onEmptyTrash) { Text(trCase().emptyTrash()) } }
            }
            if ((result?.missingCount ?: 0) > 0) {
                item { TextButton(onClick = onTagMissing) { Text(trCase().tagMissing()) } }
            }
            if ((result?.unusedCount ?: 0) > 0) {
                item { TextButton(onClick = onDeleteUnused) { Text(trCase().checkMediaDeleteUnused()) } }
            }
            items(lines.size) { index ->
                SelectionContainer {
                    Text(lines[index], color = paper.colors.onBackground, style = paper.type.rowTitle)
                }
            }
        }
    }
}
