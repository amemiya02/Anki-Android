// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.ConflictResolution
import com.ichi2.anki.R
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_COLLECTION_TOO_LARGE
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_CONNECTION_ERROR
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_MEDIA_SYNC_ERROR
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_BASIC_CHECK_ERROR
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_CLOCK_OFF
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_CONFLICT_RESOLUTION
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_CORRUPT_COLLECTION
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_SANITY_ERROR
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE
import com.ichi2.anki.dialogs.SyncErrorDialog.Type.DIALOG_USER_NOT_LOGGED_IN_SYNC
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber
import java.text.DateFormat

class SyncErrorDialog : AsyncDialogFragment() {
    interface SyncErrorDialogListener {
        fun showSyncErrorDialog(dialogType: Type)

        fun showSyncErrorDialog(
            dialogType: Type,
            message: String?,
        )

        fun loginToSyncServer()

        fun sync(conflict: ConflictResolution? = null)

        fun mediaCheck()

        fun integrityCheck()
    }

    fun requireSyncErrorDialogListener() = activity as SyncErrorDialogListener

    /** The type of the sync error dialog*/
    private val dialogType: Type
        get() = Type.fromCode(requireArguments().getInt(ARG_DIALOG_TYPE))

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (dialogType == DIALOG_SYNC_CORRUPT_COLLECTION) isCancelable = false
        return createPaperDialog(requireContext()) { SyncDialogContent() }
    }

    @Composable
    private fun SyncDialogContent() {
        val listener = requireSyncErrorDialogListener()
        val listenerActivity = requireActivity()
        val cancel = getString(CommonString.dialog_cancel)

        fun action(
            label: String,
            run: () -> Unit,
        ) = DialogAction(label, {
            dismiss()
            run()
        })
        val errorBody = message.orEmpty()
        val icon = R.drawable.symbol_sync_problem
        when (dialogType) {
            DIALOG_USER_NOT_LOGGED_IN_SYNC ->
                ErrorDialog(
                    title,
                    errorBody,
                    onDismissRequest = ::dismiss,
                    primaryAction = action(TR.sentenceCase.logIn, listener::loginToSyncServer),
                    dismissText = cancel,
                    markIcon = icon,
                )

            DIALOG_CONNECTION_ERROR ->
                ErrorDialog(
                    title,
                    errorBody,
                    details = requireArguments().getString(ARG_DIALOG_MESSAGE),
                    onDismissRequest = { activity?.dismissAllDialogFragments() },
                    primaryAction = DialogAction(getString(CommonString.retry), { syncAndDismissAllDialogFragments() }),
                    dismissText = cancel,
                    markIcon = icon,
                )

            DIALOG_SYNC_CONFLICT_RESOLUTION, DIALOG_SYNC_SANITY_ERROR -> {
                val conflict = dialogType == DIALOG_SYNC_CONFLICT_RESOLUTION
                PaperDialog(
                    title,
                    onDismissRequest = ::dismiss,
                    mark = DialogMark(icon, warning = true),
                    actions =
                        listOfNotNull(
                            DialogAction(cancel, ::dismiss),
                            if (conflict) {
                                DialogAction(
                                    getString(CommonString.help),
                                    { requireContext().openUrl(R.string.link_sync_conflict_help) },
                                )
                            } else {
                                null
                            },
                        ),
                ) {
                    if (errorBody.isNotEmpty()) DialogBody(errorBody)
                    DialogChoiceRow(
                        DialogChoice(
                            getString(if (conflict) CommonString.sync_conflict_keep_local_new else CommonString.sync_sanity_local),
                        ),
                        checked = false,
                        onClick = {
                            dismiss()
                            listener.showSyncErrorDialog(
                                if (conflict) DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL else DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL,
                            )
                        },
                        multiple = false,
                    )
                    DialogChoiceRow(
                        DialogChoice(
                            getString(if (conflict) CommonString.sync_conflict_keep_remote_new else CommonString.sync_sanity_remote),
                        ),
                        checked = false,
                        onClick = {
                            dismiss()
                            listener.showSyncErrorDialog(
                                if (conflict) DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE else DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE,
                            )
                        },
                        multiple = false,
                    )
                }
            }

            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL,
            DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE,
            -> {
                val keepsLocal =
                    dialogType == DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL || dialogType == DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL
                ConfirmationDialog(
                    title,
                    errorBody,
                    confirm =
                        DialogAction(
                            getString(CommonString.paper_dialog_replace_collection),
                            {
                                syncAndDismissAllDialogFragments(
                                    if (keepsLocal) ConflictResolution.FULL_UPLOAD else ConflictResolution.FULL_DOWNLOAD,
                                )
                            },
                            DialogActionStyle.Destructive,
                        ),
                    onDismissRequest = ::dismiss,
                )
            }

            DIALOG_MEDIA_SYNC_ERROR, DIALOG_SYNC_BASIC_CHECK_ERROR, DIALOG_COLLECTION_TOO_LARGE -> {
                val media = dialogType == DIALOG_MEDIA_SYNC_ERROR
                ErrorDialog(
                    title,
                    errorBody,
                    onDismissRequest = ::dismiss,
                    primaryAction =
                        DialogAction(if (media) TR.sentenceCase.checkMediaAction else TR.sentenceCase.checkDatabase, {
                            if (media) listener.mediaCheck() else listener.integrityCheck()
                            activity?.dismissAllDialogFragments()
                        }),
                    dismissText = cancel,
                    markIcon = icon,
                )
            }

            DIALOG_SYNC_CORRUPT_COLLECTION ->
                ErrorDialog(
                    title,
                    getString(CommonString.sync_corrupt_database, getString(R.string.repair_deck)),
                    details = requireArguments().getString(ARG_DIALOG_MESSAGE),
                    onDismissRequest = {
                        listenerActivity.openUrl(R.string.repair_deck)
                        dismiss()
                    },
                    primaryAction = DialogAction(getString(CommonString.dialog_ok), ::dismiss),
                    dismissText = getString(CommonString.help),
                    markIcon = icon,
                )

            DIALOG_SYNC_CLOCK_OFF ->
                ErrorDialog(
                    title,
                    errorBody,
                    onDismissRequest = ::dismiss,
                    primaryAction =
                        DialogAction(getString(CommonString.open_settings), {
                            try {
                                startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                                dismiss()
                            } catch (e: ActivityNotFoundException) {
                                Timber.w(e, "No app can show date and time settings")
                            }
                        }),
                    dismissText = cancel,
                    markIcon = icon,
                )
        }
    }

    private val title: String
        get() =
            when (dialogType) {
                DIALOG_USER_NOT_LOGGED_IN_SYNC -> res().getString(CommonString.sync_server_not_logged_in_title)
                DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE ->
                    res().getString(
                        CommonString.sync_conflict_replace_title,
                    )
                DIALOG_SYNC_CONFLICT_RESOLUTION -> res().getString(CommonString.sync_conflict_title_new)
                DIALOG_CONNECTION_ERROR,
                DIALOG_SYNC_SANITY_ERROR,
                DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL,
                DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE,
                DIALOG_MEDIA_SYNC_ERROR,
                DIALOG_SYNC_CORRUPT_COLLECTION,
                DIALOG_SYNC_BASIC_CHECK_ERROR,
                DIALOG_COLLECTION_TOO_LARGE,
                -> res().getString(CommonString.sync_error)
                DIALOG_SYNC_CLOCK_OFF -> res().getString(CommonString.vague_error)
            }

    /**
     * Get the title which is shown in notification bar when dialog fragment can't be shown
     *
     * @return tile to be shown in notification in bar
     */
    override val notificationTitle: String
        get() {
            return if (dialogType == DIALOG_USER_NOT_LOGGED_IN_SYNC) {
                res().getString(CommonString.sync_error)
            } else {
                title
            }
        }

    private val message: String?
        get() =
            when (dialogType) {
                DIALOG_USER_NOT_LOGGED_IN_SYNC -> res().getString(CommonString.sync_server_login_message)
                DIALOG_CONNECTION_ERROR -> res().getString(CommonString.connection_error_message)
                DIALOG_SYNC_CONFLICT_RESOLUTION -> res().getString(CommonString.sync_conflict_message_new)
                DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL ->
                    res().getString(
                        CommonString.sync_conflict_local_confirm_new,
                    )
                DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE ->
                    res().getString(
                        CommonString.sync_conflict_remote_confirm_new,
                    )
                DIALOG_SYNC_CORRUPT_COLLECTION -> {
                    val syncMessage = requireArguments().getString(ARG_DIALOG_MESSAGE)
                    val repairUrl = res().getString(R.string.repair_deck)
                    val dialogMessage = res().getString(CommonString.sync_corrupt_database, repairUrl)
                    joinSyncMessages(dialogMessage, syncMessage)
                }
                DIALOG_SYNC_CLOCK_OFF ->
                    res().getString(
                        CommonString.sync_clock_off_with_current_time,
                        requireArguments().getString(ARG_DIALOG_MESSAGE).orEmpty(),
                        DateFormat.getDateTimeInstance().format(TimeManager.time.currentDate),
                    )
                else -> requireArguments().getString(ARG_DIALOG_MESSAGE)
            }

    private fun joinSyncMessages(
        dialogMessage: String?,
        syncMessage: String?,
    ): String? {
        // If both strings have text, separate them by a new line, otherwise return whichever has text
        return if (!dialogMessage.isNullOrEmpty() && !syncMessage.isNullOrEmpty()) {
            """
            $dialogMessage
            
            $syncMessage
            """.trimIndent()
        } else if (!dialogMessage.isNullOrEmpty()) {
            dialogMessage
        } else {
            syncMessage
        }
    }

    /**
     * Get the message which is shown in notification bar when dialog fragment can't be shown
     *
     * @return message to be shown in notification in bar
     */
    override val notificationMessage: String?
        get() {
            return if (dialogType == DIALOG_USER_NOT_LOGGED_IN_SYNC) {
                res().getString(CommonString.sync_server_not_logged_in_title)
            } else {
                message
            }
        }

    override val dialogHandlerMessage: SyncErrorDialogMessageHandler
        get() {
            val dialogMessage = requireArguments().getString(ARG_DIALOG_MESSAGE)
            return SyncErrorDialogMessageHandler(dialogType, dialogMessage)
        }

    /**
     * Syncs with [conflictResolution] then dismisses all dialog fragments.
     */
    fun syncAndDismissAllDialogFragments(conflictResolution: ConflictResolution? = null) {
        requireSyncErrorDialogListener().sync(conflictResolution)
        activity?.dismissAllDialogFragments()
    }

    enum class Type(
        val code: Int,
    ) {
        DIALOG_USER_NOT_LOGGED_IN_SYNC(0),
        DIALOG_CONNECTION_ERROR(1),
        DIALOG_SYNC_CONFLICT_RESOLUTION(2),
        DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL(3),
        DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE(4),
        DIALOG_SYNC_SANITY_ERROR(5),
        DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL(6),
        DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE(7),
        DIALOG_MEDIA_SYNC_ERROR(8),
        DIALOG_SYNC_CORRUPT_COLLECTION(9),
        DIALOG_SYNC_BASIC_CHECK_ERROR(10),
        DIALOG_SYNC_CLOCK_OFF(11),
        DIALOG_COLLECTION_TOO_LARGE(12),
        ;

        companion object {
            fun fromCode(code: Int) = Type.entries.first { code == it.code }
        }
    }

    companion object {
        /**
         * Key for the ordinal of the sync error in Type
         */
        const val ARG_DIALOG_TYPE = "dialogType"

        /**
         * Key for the message to display in the dialog
         */
        const val ARG_DIALOG_MESSAGE = "dialogMessage"

        /**
         * A set of dialogs belonging to AnkiActivity which deal with sync problems
         *
         * @param dialogType An integer which specifies which of the sub-dialogs to show
         * @param dialogMessage A string which can be optionally used to set the dialog message
         */
        @CheckResult
        fun newInstance(
            dialogType: Type,
            dialogMessage: String?,
        ) = SyncErrorDialog().apply {
            arguments =
                Bundle().apply {
                    putInt(ARG_DIALOG_TYPE, dialogType.code)
                    putString(ARG_DIALOG_MESSAGE, dialogMessage)
                }
        }
    }

    class SyncErrorDialogMessageHandler(
        val dialogType: Type,
        val dialogMessage: String?,
    ) : DialogHandlerMessage(WhichDialogHandler.MSG_SHOW_SYNC_ERROR_DIALOG, "SyncErrorDialog") {
        override fun handleAsyncMessage(activity: AnkiActivity) {
            // we may be called via any AnkiActivity but media check is a DeckPicker thing
            activity
                .requireDeckPickerOrShowError()
                ?.showSyncErrorDialog(dialogType, dialogMessage)
        }

        override fun toMessage(): Message =
            Message.obtain().apply {
                what = this@SyncErrorDialogMessageHandler.what
                data =
                    Bundle().apply {
                        putInt(ARG_DIALOG_TYPE, dialogType.code)
                        putString(ARG_DIALOG_MESSAGE, dialogMessage)
                    }
            }

        companion object {
            fun fromMessage(message: Message): SyncErrorDialogMessageHandler {
                val dialogType = Type.fromCode(message.data.getInt(ARG_DIALOG_TYPE))
                val dialogMessage = message.data.getString(ARG_DIALOG_MESSAGE)
                return SyncErrorDialogMessageHandler(dialogType, dialogMessage)
            }
        }
    }
}
