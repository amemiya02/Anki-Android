// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.compat.CompatHelper.Companion.getPackageInfoCompat
import com.ichi2.anki.compat.PackageInfoFlagsCompat
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.millisecondsSinceLastSyncAttempt
import com.ichi2.anki.servicelayer.ScopedStorageService.collectionWillBeMadeInaccessibleAfterUninstall
import com.ichi2.anki.servicelayer.ScopedStorageService.userIsPromptedToDeleteCollectionOnUninstall
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.ImportUtils
import com.ichi2.utils.Permissions
import timber.log.Timber
import java.io.File

/**
 * Prompts a user to backup via either sync or 'export collection'
 *
 * If dismissed, it will not appear for a period of time (~2 weeks): [calculateNextTimeToShowDialog]
 * After 2 dismissals, the user may hide the dialog permanently.
 *
 * This exists to inform the user their data is at risk when in a scoped folder.
 *
 * See [shouldShowDialog] for the criteria to display the dialog
 */
class BackupPromptDialog private constructor(
    private val windowContext: Context,
) {
    /**
     * After 2 dismissals, allow ignoring
     * Note: this is 0-based - the dialog has not been dismissed on the first viewing
     */
    private val allowUserToPermanentlyDismissDialog: Boolean
        get() = timesDialogDismissed > 1

    private var timesDialogDismissed: Int
        get() = windowContext.sharedPrefs().getInt("backupPromptDismissedCount", 0)
        set(value) =
            windowContext
                .sharedPrefs()
                .edit { putInt("backupPromptDismissedCount", value) }

    private var dialogPermanentlyDismissed: Boolean
        get() = windowContext.sharedPrefs().getBoolean("backupPromptDisabled", false)
        set(disablePermanently) {
            windowContext.sharedPrefs().edit {
                putBoolean("backupPromptDisabled", disablePermanently)
                if (disablePermanently) {
                    remove("backupPromptDismissedCount")
                    remove("timeToShowBackupDialog")
                }
            }
        }

    private var nextTimeToShowDialog: Long
        get() = windowContext.sharedPrefs().getLong("timeToShowBackupDialog", 0)
        set(value) {
            windowContext
                .sharedPrefs()
                .edit { putLong("timeToShowBackupDialog", value) }
        }

    private fun onDismiss(dontShowAgain: Boolean) {
        Timber.i("BackupPromptDialog dismissed")
        if (dontShowAgain) {
            showPermanentlyDismissDialog(
                this.windowContext,
                onCancel = { onDismiss(dontShowAgain = false) },
                onDisableReminder = { dialogPermanentlyDismissed = true },
            )
        } else {
            timesDialogDismissed += 1
            nextTimeToShowDialog = calculateNextTimeToShowDialog()
        }
    }

    private fun onBackup() {
        nextTimeToShowDialog = calculateNextTimeToShowDialog()
    }

    private fun calculateNextTimeToShowDialog(): Long {
        val now = TimeManager.time.intTimeMS()
        val fixedDayCount = 12
        val oneToFourDays = (1..4).random() // 13-16 days
        return now + (fixedDayCount + oneToFourDays) * ONE_DAY_IN_MS
    }

    private fun show(
        isLoggedIn: Boolean,
        performBackup: () -> Unit,
    ) {
        createPaperDialog(windowContext) { dialog ->
            var dontShowAgain by rememberSaveable { mutableStateOf(false) }
            ConfirmationDialog(
                title = windowContext.getString(CommonString.backup_your_collection),
                message = windowContext.getString(CommonString.backup_collection_message),
                confirm =
                    DialogAction(
                        windowContext.getString(if (isLoggedIn) CommonString.button_sync else CommonString.button_backup),
                        {
                            Timber.i("User selected 'backup'")
                            dialog.dismiss()
                            onBackup()
                            performBackup()
                        },
                        enabled = !dontShowAgain,
                    ),
                onDismissRequest = {
                    dialog.dismiss()
                    onDismiss(dontShowAgain)
                },
                dismissText = windowContext.getString(CommonString.button_backup_later),
                markIcon = if (isLoggedIn) R.drawable.symbol_backup else R.drawable.symbol_settings_backup_restore,
            ) {
                if (allowUserToPermanentlyDismissDialog) {
                    DialogChoiceRow(
                        DialogChoice(windowContext.getString(CommonString.button_do_not_show_again)),
                        checked = dontShowAgain,
                        onClick = { dontShowAgain = !dontShowAgain },
                        multiple = true,
                    )
                }
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            show()
        }
    }

    companion object {
        private const val ONE_DAY_IN_MS = 1000 * 60 * 60 * 24

        /** @return Whether the dialog was shown */
        suspend fun showIfAvailable(deckPicker: DeckPicker): Boolean {
            val backupPrompt = BackupPromptDialog(deckPicker)

            if (!backupPrompt.shouldShowDialog()) {
                return false
            }
            val isLoggedIn = isLoggedIn()
            backupPrompt.show(isLoggedIn) {
                if (isLoggedIn) {
                    deckPicker.sync(conflict = null)
                } else {
                    deckPicker.exportCollection()
                }
            }
            return true
        }

        fun onFileExported(
            context: Context,
            file: File,
        ) {
            if (!ImportUtils.isCollectionPackage(file.name)) return
            Timber.i("Collection exported, postponing the backup reminder")
            BackupPromptDialog(context).onBackup()
        }

        /**
         *
         * @return A confirmation message to show the user before 'don't show this again' takes place
         * `null` if a confirmation dialog is not required and the dialog will be dismissed permanently without confirmation
         */
        @StringRes
        fun getPermanentlyDismissDialogMessageOrImmediatelyDismiss(context: Context): Int? {
            if (userIsPromptedToDeleteCollectionOnUninstall(context)) {
                Timber.d("User's collection may be deleted on uninstall")
                return CommonString.dismiss_backup_warning_new_user // message stating collection will be deleted (new user/migrated)
            }

            // Full build users/users on old Androids will see this dialog, but only if they are syncing
            // It's much safer to ignore this. We'd like users to sync, but we shouldn't nag  if they don't consent
            if (Permissions.canManageExternalStorage(context)) {
                Timber.d("User is on a 'full' build. Disabling backup reminder without confirmation")
                return null
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                // It is an assumption that Q will always handle android.R.attr.preserveLegacyExternalStorage
                // If we are incorrect, a user doesn't see a confirmation dialog
                // AND the user would be able to regain access with a 'full' build.
                Timber.d("User can regain access to their collection. Disabling backup reminder without confirmation")
                return null
            }

            // Given the assumptions above, this conditional should return true
            return if (collectionWillBeMadeInaccessibleAfterUninstall(context)) {
                Timber.d("User will lose access to their collection")
                // message stating collection will be made inaccessible
                // (existing user, not migrated)
                CommonString.dismiss_backup_warning_upgrade
            } else {
                // A user is on a Play Store Build. They are on a version of Android with storage restrictions
                // Their collection is in a 'legacy' location but they are not going to lose access to their collection when they uninstall
                // The user is very likely syncing
                Timber.w("getPermanentlyDismissDialogMessage: unexpected state")
                CrashReportService.sendExceptionReport(IllegalStateException("unexpected state"), "getPermanentlyDismissDialogMessage")
                // assume this is a mistake and show a scary confirmation prompt
                CommonString.dismiss_backup_warning_new_user // message stating collection will be deleted
            }
        }

        /** Explains to the user they should sync/backup as they risk to have data deleted or inaccessible (depending on whether legacy storage permission is kept) */
        fun showPermanentlyDismissDialog(
            context: Context,
            onCancel: () -> Unit,
            onDisableReminder: () -> Unit,
        ) {
            val message = getPermanentlyDismissDialogMessageOrImmediatelyDismiss(context)
            if (message == null) {
                Timber.i("permanently disabling 'Backup Prompt' reminder - no confirmation")
                onDisableReminder()
                return
            }

            createPaperDialog(context) { dialog ->
                ConfirmationDialog(
                    title = context.getString(CommonString.dismiss_backup_warning_title),
                    message = context.getString(message),
                    confirm =
                        DialogAction(context.getString(CommonString.button_disable_reminder), {
                            dialog.dismiss()
                            onDisableReminder()
                        }, DialogActionStyle.Destructive),
                    onDismissRequest = {
                        dialog.dismiss()
                        onCancel()
                    },
                )
            }.show()
        }
    }

    private suspend fun shouldShowDialog(): Boolean = !userIsNewToAnkiDroid() && canProvideBackupOption() && timeToShowDialogAgain()

    /**
     * Whether:
     * * The user can sync, and we want to encourage them to sync regularly
     * * The user will lose/have their data deleted if
     *
     * @return `true` if user syncs; `true` for Play Store builds >= Android 11 (lose storage access on uninstall)
     * `false` if the user is not syncing and will not lose storage access on uninstall
     */
    private fun canProvideBackupOption(): Boolean {
        // If we are on a 'full' build, the user can always restore access to their collection.
        // But we want them to sync regularly as a backup
        if (isLoggedIn()) {
            // Show dialog to sync if no sync attempt has been recorded in a while
            return millisecondsSinceLastSyncAttempt() >= ONE_DAY_IN_MS * 7
        }

        // Android proposes the user deletes non-legacy locations on uninstall
        if (userIsPromptedToDeleteCollectionOnUninstall(windowContext)) {
            Timber.v("Collection may be removed on uninstall")
            return true
        }

        // The user may have upgraded, in which it's unsafe to uninstall as Android
        // will permanently revoke access to the legacy folder
        // The collection won't be lost, but it will be inaccessible to a Play Store build
        return collectionWillBeMadeInaccessibleAfterUninstall(windowContext)
    }

    private fun timeToShowDialogAgain(): Boolean = !dialogPermanentlyDismissed && nextTimeToShowDialog <= TimeManager.time.intTimeMS()

    private suspend fun userIsNewToAnkiDroid(): Boolean {
        // A user is new if the app was installed > 7 days ago  OR if they have no cards
        val firstInstallTime = getFirstInstallTime() ?: 0
        if (TimeManager.time.intTimeMS() - firstInstallTime >= ONE_DAY_IN_MS * 7) {
            return false
        }

        // if for some reason the user has no cards after 7 days, don't bother
        return withCol {
            cardCount() == 0
        }
    }

    /** The time at which the app was first installed. Units are as per [System.currentTimeMillis()]. */
    private fun getFirstInstallTime(): Long? {
        return try {
            return windowContext.packageManager
                .getPackageInfoCompat(
                    windowContext.packageName,
                    PackageInfoFlagsCompat.of(0),
                )?.firstInstallTime
        } catch (exception: Exception) {
            Timber.w("failed to get first install time")
            null
        }
    }
}
