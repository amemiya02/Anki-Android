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
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.syncServerDisplayName
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
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
                    primaryAction = action(TR.syncSignIn(), listener::loginToSyncServer),
                    dismissText = cancel,
                    markIcon = icon,
                )

            DIALOG_CONNECTION_ERROR ->
                ErrorDialog(
                    title,
                    errorBody,
                    details = requireArguments().getString(ARG_DIALOG_MESSAGE),
                    onDismissRequest = { activity?.dismissAllDialogFragments() },
                    primaryAction = DialogAction(getString(CommonString.paper_sync_retry), { syncAndDismissAllDialogFragments() }),
                    dismissText = getString(CommonString.paper_sync_not_now),
                    markIcon = R.drawable.symbol_cloud_off,
                )

            DIALOG_SYNC_CONFLICT_RESOLUTION, DIALOG_SYNC_SANITY_ERROR -> {
                val conflict = dialogType == DIALOG_SYNC_CONFLICT_RESOLUTION
                PaperDialog(
                    title,
                    onDismissRequest = ::dismiss,
                    mark = DialogMark(R.drawable.symbol_call_split, warning = true),
                    actions = listOf(DialogAction(cancel, ::dismiss)),
                ) {
                    DialogBody(errorBody)
                    SettingsRow(
                        title = getString(CommonString.paper_sync_keep_phone),
                        summary = getString(CommonString.paper_sync_keep_phone_hint),
                        icon = R.drawable.symbol_smartphone,
                        onClick = {
                            dismiss()
                            listener.showSyncErrorDialog(
                                if (conflict) DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL else DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL,
                            )
                        },
                        trailing = { SettingsChevron() },
                    )
                    SettingsRow(
                        title = getString(CommonString.paper_sync_keep_server),
                        summary = getString(CommonString.paper_sync_keep_server_hint),
                        icon = R.drawable.symbol_dns,
                        onClick = {
                            dismiss()
                            listener.showSyncErrorDialog(
                                if (conflict) DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE else DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE,
                            )
                        },
                        trailing = { SettingsChevron() },
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
                    details = requireArguments().getString(ARG_DIALOG_MESSAGE),
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
                    errorBody,
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
                    details = DateFormat.getDateTimeInstance().format(TimeManager.time.currentDate),
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
            res().getString(
                when (dialogType) {
                    DIALOG_USER_NOT_LOGGED_IN_SYNC -> CommonString.sync_server_not_logged_in_title
                    DIALOG_CONNECTION_ERROR -> CommonString.paper_sync_unreachable_title
                    DIALOG_SYNC_CONFLICT_RESOLUTION, DIALOG_SYNC_SANITY_ERROR -> CommonString.paper_sync_conflict_title
                    DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE,
                    DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE,
                    -> CommonString.sync_conflict_replace_title
                    else -> CommonString.sync_error
                },
            )

    override val notificationTitle: String
        get() = title

    private val message: String
        get() {
            val messageResource =
                when (dialogType) {
                    DIALOG_USER_NOT_LOGGED_IN_SYNC -> CommonString.paper_sync_not_signed_in
                    DIALOG_CONNECTION_ERROR -> CommonString.paper_sync_unreachable_body
                    DIALOG_SYNC_CONFLICT_RESOLUTION -> CommonString.paper_sync_conflict_body
                    DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL ->
                        CommonString.paper_sync_keep_phone_hint
                    DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE, DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE ->
                        CommonString.paper_sync_keep_server_hint
                    DIALOG_SYNC_SANITY_ERROR -> CommonString.paper_sync_sanity
                    DIALOG_MEDIA_SYNC_ERROR -> CommonString.paper_sync_media_error
                    DIALOG_SYNC_BASIC_CHECK_ERROR -> CommonString.paper_sync_db_error
                    DIALOG_SYNC_CORRUPT_COLLECTION -> CommonString.paper_sync_corrupt_error
                    DIALOG_SYNC_CLOCK_OFF -> CommonString.paper_sync_clock_error
                    DIALOG_COLLECTION_TOO_LARGE -> CommonString.paper_sync_too_large
                }
            val resources = res()
            return when (dialogType) {
                DIALOG_USER_NOT_LOGGED_IN_SYNC, DIALOG_CONNECTION_ERROR ->
                    resources.getString(
                        messageResource,
                        syncServerDisplayName() ?: resources.getString(CommonString.sync_server_title),
                    )
                else -> resources.getString(messageResource)
            }
        }

    override val notificationMessage: String
        get() = message

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
