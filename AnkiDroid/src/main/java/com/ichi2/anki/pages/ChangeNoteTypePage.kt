// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import anki.collection.OpChanges
import anki.notetypes.ChangeNotetypeRequest
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.libanki.NoteTypeId
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.showAndAwaitDismissal
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The bundled Anki Page maps fields and templates. Android owns the selected notes and applies the result. */
class ChangeNoteTypePage : PageFragment() {
    private val noteIds by lazy { requireNotNull(requireArguments().getLongArray(NOTE_IDS)).toList() }
    private val sourceType by lazy { requireArguments().getLong(NOTE_TYPE_ID) }
    private val saveLock = Mutex()
    private var saved = false

    override val pagePath get() = "change-notetype/$sourceType"

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        pageTitle = with(requireContext()) { TR.sentenceCase.changeNoteType }
    }

    override suspend fun handlePostRequest(
        uri: PostRequestUri,
        bytes: ByteArray,
    ): ByteArray {
        if (uri.backendMethodName != "changeNotetype") return super.handlePostRequest(uri, bytes)
        return saveLock.withLock {
            if (saved || !isAdded || view == null) return@withLock byteArrayOf()
            val request = ChangeNotetypeRequest.parseFrom(bytes)
            require(request.oldNotetypeId == sourceType)
            if (!confirmSchemaChange() || view == null) return@withLock byteArrayOf()

            val input = request.toBuilder().clearNoteIds().addAllNoteIds(noteIds).build()
            val changes: OpChanges = undoableOp { notetypes.changeNotetypeOfNotes(input) }
            saved = true
            view?.post {
                if (isAdded && view != null) {
                    showThemedToast(requireContext(), TR.browsingNotesUpdated(noteIds.size), shortLength = true)
                    finishScreen(Activity.RESULT_OK)
                }
            }
            changes.toByteArray()
        }
    }

    private suspend fun confirmSchemaChange(): Boolean {
        if (withCol { schemaChanged() }) return true
        val context = context ?: return false
        if (view == null) return false
        var confirmed = false
        createPaperDialog(context) { dialog ->
            ConfirmationDialog(
                title = "",
                message = getString(CommonString.full_sync_confirmation),
                confirm =
                    DialogAction(getString(CommonString.dialog_ok), {
                        confirmed = true
                        dialog.dismiss()
                    }),
                onDismissRequest = dialog::dismiss,
            )
        }.showAndAwaitDismissal()
        return confirmed
    }

    companion object {
        private const val NOTE_IDS = "noteIds"
        private const val NOTE_TYPE_ID = "sourceNoteType"

        fun getIntent(
            context: Context,
            noteIds: List<NoteId>,
            sourceType: NoteTypeId,
        ) = SingleFragmentActivity.getIntent(
            context,
            ChangeNoteTypePage::class,
            Bundle().apply {
                putLongArray(NOTE_IDS, noteIds.distinct().toLongArray())
                putLong(NOTE_TYPE_ID, sourceType)
            },
        )
    }
}
