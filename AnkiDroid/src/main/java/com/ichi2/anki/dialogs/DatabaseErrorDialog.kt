// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.ConflictResolution
import com.ichi2.anki.DatabaseRestorationListener
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.FatalInitializationError.StorageError
import com.ichi2.anki.InitialActivity.StartupFailure.InitializationError
import com.ichi2.anki.LocalizedUnambiguousBackupTimeFormatter
import com.ichi2.anki.R
import com.ichi2.anki.ankiActivity
import com.ichi2.anki.backend.DatabaseCorruption
import com.ichi2.anki.backend.getDatabaseVersion
import com.ichi2.anki.common.destinations.PreferencesDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.dialogs.DatabaseErrorDialog.CustomExceptionData
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_CONFIRM_DATABASE_CHECK
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_CONFIRM_RESTORE_BACKUP
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_DB_ERROR
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_DB_LOCKED
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_DISK_FULL
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_ERROR_HANDLING
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_LOAD_FAILED
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_NEW_COLLECTION
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_ONE_WAY_SYNC_FROM_SERVER
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_REPAIR_COLLECTION
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_RESTORE_BACKUP
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType.INCOMPATIBLE_DB_VERSION
import com.ichi2.anki.dialogs.DatabaseErrorDialog.ShowDatabaseErrorDialog
import com.ichi2.anki.dialogs.DatabaseErrorDialog.UninstallListItem.Companion.createList
import com.ichi2.anki.dialogs.ImportFileSelectionFragment.ImportOptions
import com.ichi2.anki.exception.SystemStorageException
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.requireAnkiActivity
import com.ichi2.anki.servicelayer.DebugInfoService
import com.ichi2.anki.showImportDialog
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import com.ichi2.anki.startup.resetAnkiDroidDirectory
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.ErrorDetails
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.TruncatedString
import com.ichi2.utils.copyToClipboard
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File
import java.io.IOException

class DatabaseErrorDialog : AsyncDialogFragment() {
    private fun requireDeckPicker() = activity as DeckPicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val type = requireDialogType()
        isCancelable =
            type !in
            setOf(
                DIALOG_LOAD_FAILED,
                DIALOG_DB_ERROR,
                DIALOG_DB_LOCKED,
                INCOMPATIBLE_DB_VERSION,
                DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL,
            )
        val canRepair = type == DIALOG_ERROR_HANDLING && isSqliteInstalled()
        val backupFiles =
            if (type == DIALOG_RESTORE_BACKUP) {
                BackupManager.getBackups(CollectionHelper.getCollectionPath(requireContext())).reversed()
            } else {
                emptyList()
            }
        return createPaperDialog(requireContext()) { DatabaseDialogContent(type, canRepair, backupFiles) }
            .apply { setCanceledOnTouchOutside(isCancelable) }
    }

    @Composable
    private fun DatabaseDialogContent(
        type: DatabaseErrorDialogType,
        canRepair: Boolean,
        backupFiles: List<File>,
    ) {
        val cancel = getString(CommonString.dialog_cancel)
        val close = getString(CommonString.close)
        val body = message.orEmpty()
        val errorMark = DialogMark(R.drawable.symbol_warning, warning = true)

        fun textAction(
            label: Int,
            run: () -> Unit,
        ) = DialogAction(getString(label), run)

        fun primary(
            label: Int,
            run: () -> Unit,
        ) = DialogAction(getString(label), run, DialogActionStyle.Filled)

        fun destructive(
            label: Int,
            run: () -> Unit,
        ) = DialogAction(getString(label), run, DialogActionStyle.Destructive)
        when (type) {
            DIALOG_LOAD_FAILED, DIALOG_DB_ERROR -> {
                val report = type == DIALOG_DB_ERROR
                PaperDialog(
                    title,
                    onDismissRequest = {},
                    mark = errorMark,
                    actions =
                        listOfNotNull(
                            DialogAction(close, ::closeCollectionAndFinish),
                            if (report) {
                                DialogAction(
                                    getString(CommonString.answering_error_report),
                                    {
                                        requireDeckPicker().sendErrorReport()
                                        requireActivity().dismissAllDialogFragments()
                                    },
                                    enabled = requireDeckPicker().hasErrorFiles(),
                                )
                            } else {
                                null
                            },
                            primary(CommonString.error_handling_options) { showDatabaseErrorDialog(DIALOG_ERROR_HANDLING) },
                        ),
                ) {
                    DialogBody(body)
                    exceptionData?.let { ErrorDetails(it.toHumanReadableString()) }
                }
            }

            DIALOG_ERROR_HANDLING -> {
                val options = errorHandlingOptions(canRepair)
                SingleChoiceDialog(
                    title = title,
                    choices = options.map { DialogChoice(it.first) },
                    selectedIndex = null,
                    onSelect = { selectErrorHandlingOption(options[it].second) },
                    onDismissRequest = ::dismiss,
                )
            }

            DIALOG_REPAIR_COLLECTION, DIALOG_CONFIRM_DATABASE_CHECK ->
                ConfirmationDialog(
                    title,
                    body,
                    confirm =
                        if (type == DIALOG_REPAIR_COLLECTION) {
                            primary(CommonString.dialog_positive_repair) {
                                requireDeckPicker().repairCollection()
                                requireActivity().dismissAllDialogFragments()
                            }
                        } else {
                            primary(CommonString.dialog_ok) {
                                requireDeckPicker().integrityCheck()
                                requireActivity().dismissAllDialogFragments()
                            }
                        },
                    onDismissRequest = ::dismiss,
                    markIcon = R.drawable.symbol_settings_backup_restore,
                )

            DIALOG_RESTORE_BACKUP -> {
                if (backupFiles.isEmpty()) {
                    ErrorDialog(
                        title,
                        body,
                        onDismissRequest = ::dismiss,
                        dismissText = null,
                        primaryAction = primary(CommonString.dialog_ok) { showDatabaseErrorDialog(DIALOG_ERROR_HANDLING) },
                    )
                } else {
                    val formatter = LocalizedUnambiguousBackupTimeFormatter()
                    PaperDialog(
                        getString(CommonString.backup_restore_select_title),
                        onDismissRequest = ::dismiss,
                        actions =
                            listOf(
                                DialogAction(cancel, ::dismiss),
                                textAction(CommonString.restore_backup_choose_another) {
                                    ankiActivity?.let {
                                        ImportFileSelectionFragment.openImportFilePicker(
                                            it,
                                            ImportFileSelectionFragment.ImportFileType.APKG,
                                        )
                                    }
                                    dismiss()
                                },
                            ),
                    ) {
                        backupFiles.forEach { file ->
                            DialogChoiceRow(
                                DialogChoice(formatter.getTimeOfBackupAsText(file)),
                                checked = false,
                                onClick = { selectBackup(file) },
                                multiple = false,
                            )
                        }
                    }
                }
            }

            DIALOG_NEW_COLLECTION, DIALOG_CONFIRM_RESTORE_BACKUP, DIALOG_ONE_WAY_SYNC_FROM_SERVER ->
                ConfirmationDialog(
                    title,
                    body,
                    confirm =
                        destructive(
                            if (type ==
                                DIALOG_NEW_COLLECTION
                            ) {
                                CommonString.create_new_collection
                            } else {
                                CommonString.paper_dialog_replace_collection
                            },
                        ) {
                            when (type) {
                                DIALOG_NEW_COLLECTION -> {
                                    val time = TimeManager.time
                                    Timber.i("closeCollection: DatabaseErrorDialog: Before Create New Collection")
                                    CollectionManager.closeCollectionBlocking()
                                    val path = CollectionHelper.getCollectionPath(requireActivity())
                                    if (BackupManager.moveDatabaseToBrokenDirectory(path, false, time)) {
                                        ActivityCompat.recreate(requireDeckPicker())
                                    } else {
                                        showDatabaseErrorDialog(DIALOG_LOAD_FAILED)
                                    }
                                }
                                DIALOG_CONFIRM_RESTORE_BACKUP -> {
                                    dismiss()
                                    showDatabaseErrorDialog(DIALOG_RESTORE_BACKUP)
                                }
                                else -> {
                                    requireDeckPicker().sync(ConflictResolution.FULL_DOWNLOAD)
                                    requireActivity().dismissAllDialogFragments()
                                }
                            }
                        },
                    onDismissRequest = ::dismiss,
                )

            DIALOG_DB_LOCKED, DIALOG_DISK_FULL ->
                PaperDialog(
                    title,
                    onDismissRequest = ::dismiss,
                    mark = errorMark,
                    actions =
                        listOfNotNull(
                            if (type == DIALOG_DB_LOCKED) {
                                textAction(CommonString.settings) {
                                    navigate(PreferencesDestination.Advanced)
                                    dismiss()
                                }
                            } else {
                                null
                            },
                            DialogAction(close, ::closeCollectionAndFinish, DialogActionStyle.Filled),
                        ),
                ) { DialogBody(body) }

            INCOMPATIBLE_DB_VERSION ->
                PaperDialog(
                    title,
                    onDismissRequest = {},
                    mark = errorMark,
                    actions = listOf(DialogAction(close, ::closeCollectionAndFinish, DialogActionStyle.Filled)),
                ) {
                    DialogBody(body)
                    DialogChoiceRow(
                        DialogChoice(getString(CommonString.backup_restore)),
                        checked = false,
                        onClick = { showDatabaseErrorDialog(DIALOG_RESTORE_BACKUP) },
                        multiple = false,
                    )
                    if (isLoggedIn()) {
                        DialogChoiceRow(
                            DialogChoice(getString(CommonString.backup_one_way_sync_from_server)),
                            checked = false,
                            onClick = { showDatabaseErrorDialog(DIALOG_ONE_WAY_SYNC_FROM_SERVER) },
                            multiple = false,
                        )
                    }
                }

            DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL ->
                PaperDialog(title, onDismissRequest = {}, mark = errorMark) {
                    DialogBody(body)
                    UninstallListItem.createList().forEach { item ->
                        DialogChoiceRow(
                            DialogChoice(getString(item.stringRes)),
                            checked = false,
                            onClick = {
                                item.onClick(requireAnkiActivity())
                                if (item.dismissesDialog) dismiss()
                            },
                            multiple = false,
                        )
                    }
                }
        }
    }

    private fun isSqliteInstalled(): Boolean =
        try {
            Runtime.getRuntime().exec("sqlite3 --version").waitFor() == 0
        } catch (e: IOException) {
            Timber.i("sqlite3 not installed: %s", e.message)
            false
        } catch (e: InterruptedException) {
            Timber.i("test for sqlite3 failed: %s", e.message)
            false
        }

    private fun errorHandlingOptions(canRepair: Boolean): List<Pair<String, ErrorHandlingEntries>> {
        val activity = requireActivity()
        val canResetDirectory =
            try {
                CollectionHelper.getCurrentAnkiDroidDirectory(activity).absolutePath != getDefaultAnkiDroidDirectory(activity).absolutePath
            } catch (e: Throwable) {
                Timber.w(e, "Failed to determine whether to offer reset-to-default directory option")
                false
            }
        return buildList {
            if (!CollectionManager.isOpenUnsafe()) add(getString(CommonString.backup_retry_opening) to ErrorHandlingEntries.RETRY)
            if (canRepair) add(getString(CommonString.backup_error_menu_repair) to ErrorHandlingEntries.REPAIR)
            add(getString(CommonString.backup_restore) to ErrorHandlingEntries.RESTORE)
            if (isLoggedIn()) add(getString(CommonString.backup_one_way_sync_from_server) to ErrorHandlingEntries.ONE_WAY)
            if (canResetDirectory) add(getString(CommonString.backup_use_default_folder) to ErrorHandlingEntries.RESET_TO_DEFAULT_DIRECTORY)
            add(getString(CommonString.backup_del_collection) to ErrorHandlingEntries.NEW)
            add(TR.sentenceCase.copyDebugInfo to ErrorHandlingEntries.DEBUG_INFO)
        }
    }

    private fun selectErrorHandlingOption(option: ErrorHandlingEntries) {
        val activity = requireActivity()
        val next =
            when (option) {
                ErrorHandlingEntries.REPAIR -> DIALOG_REPAIR_COLLECTION
                ErrorHandlingEntries.RESTORE -> DIALOG_RESTORE_BACKUP
                ErrorHandlingEntries.ONE_WAY -> DIALOG_ONE_WAY_SYNC_FROM_SERVER
                ErrorHandlingEntries.NEW -> DIALOG_NEW_COLLECTION
                else -> null
            }
        if (next != null) {
            showDatabaseErrorDialog(next)
            return
        }
        when (option) {
            ErrorHandlingEntries.RETRY -> ActivityCompat.recreate(activity)
            ErrorHandlingEntries.RESET_TO_DEFAULT_DIRECTORY -> {
                try {
                    val defaultDir = getDefaultAnkiDroidDirectory(activity)
                    CollectionManager.closeCollectionBlocking()
                    resetAnkiDroidDirectory(activity, defaultDir)
                    closeCollectionAndFinish()
                } catch (e: Throwable) {
                    Timber.w(e, "Failed to reset AnkiDroid directory to default")
                    showDatabaseErrorDialog(DIALOG_LOAD_FAILED)
                }
            }
            ErrorHandlingEntries.DEBUG_INFO ->
                lifecycleScope.launch {
                    withContext(NonCancellable) { copyStackTraceAndDebugInfo() }
                }
            else -> Unit
        }
        dismiss()
    }

    private fun selectBackup(file: File) {
        if (file.length() > 0) {
            requireDeckPicker().restoreFromBackup(file.path)
            requireActivity().dismissAllDialogFragments()
            return
        }
        createPaperDialog(requireContext()) { dialog ->
            ErrorDialog(
                getString(CommonString.vague_error),
                getString(CommonString.backup_invalid_file_error),
                onDismissRequest = dialog::dismiss,
                dismissText = getString(CommonString.dialog_ok),
            )
        }.show()
    }

    /** @see DeckPicker.showDatabaseErrorDialog */
    private fun showDatabaseErrorDialog(errorDialogType: DatabaseErrorDialogType) {
        requireAnkiActivity().showDatabaseErrorDialogInDeckPicker(errorDialogType, exceptionData)
    }

    /**
     * Collects the current exception's stack trace and debug information,
     * and copies the combines information to the Android clipboard.
     */
    private suspend fun copyStackTraceAndDebugInfo() {
        val context = getSafeContext()
        val combinedInfo =
            listOfNotNull(
                exceptionData?.toHumanReadableString(),
                DebugInfoService.getDebugInfo(context),
            ).joinToString(separator = "\n")

        context.copyToClipboard(
            TruncatedString.from(combinedInfo),
            failureMessageId = CommonString.about_ankidroid_error_copy_debug_info,
        )
    }

    /** List items for [DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL] */
    enum class UninstallListItem(
        @StringRes val stringRes: Int,
        val dismissesDialog: Boolean,
        val onClick: (AnkiActivity) -> Unit,
    ) {
        RESTORE_FROM_ANKIWEB(
            CommonString.restore_data_from_ankiweb,
            dismissesDialog = true,
            {
                this.displayCreateNewCollectionDialog(it)
            },
        ),
        INSTALL_NON_PLAY_APP_RECOMMENDED(
            CommonString.install_non_play_store_ankidroid_recommended,
            dismissesDialog = false,
            {
                it.openUrl(R.string.link_install_non_play_store_install)
            },
        ),
        INSTALL_NON_PLAY_APP_NORMAL(
            CommonString.install_non_play_store_ankidroid,
            dismissesDialog = false,
            {
                it.openUrl(R.string.link_install_non_play_store_install)
            },
        ),
        RESTORE_FROM_BACKUP(
            CommonString.restore_data_from_backup,
            dismissesDialog = false,
            { activity ->
                Timber.i("Restoring from colpkg")
                val newAnkiDroidDirectory = getDefaultAnkiDroidDirectory(activity)
                activity.importColpkgListener = DatabaseRestorationListener(activity, newAnkiDroidDirectory)

                activity.launchCatchingTask {
                    CollectionHelper.ankiDroidDirectoryOverride = newAnkiDroidDirectory

                    CollectionManager.withCol {
                        activity.showImportDialog(
                            ImportOptions(
                                importTextFile = false,
                                importColpkg = true,
                                importApkg = false,
                            ),
                        )
                    }
                }
            },
        ),
        GET_HELP(
            CommonString.help_title_get_help,
            dismissesDialog = false,
            {
                it.openUrl(R.string.link_forum)
            },
        ),
        RECREATE_COLLECTION(
            CommonString.create_new_collection,
            dismissesDialog = false,
            {
                this.displayCreateNewCollectionDialog(it)
            },
        ),
        ;

        companion object {
            /** A dialog which creates a new collection in an unsafe location */
            fun displayCreateNewCollectionDialog(context: AnkiActivity) {
                val directory =
                    try {
                        getDefaultAnkiDroidDirectory(context)
                    } catch (e: SystemStorageException) {
                        Timber.w(e, "failed to show 'Create new collection' dialog")
                        FatalErrorDialog.build(context, InitializationError(StorageError(e))).show()
                        return
                    }
                createPaperDialog(context) { dialog ->
                    ConfirmationDialog(
                        title = context.getString(CommonString.backup_new_collection),
                        message = context.getString(CommonString.new_unsafe_collection),
                        confirm =
                            DialogAction(context.getString(CommonString.create_new_collection), {
                                Timber.w("Creating new collection")
                                Timber.i("closeCollection: DatabaseErrorDialog: Before Create New Collection")
                                CollectionManager.closeCollectionBlocking()
                                resetAnkiDroidDirectory(context, directory)
                                context.closeCollectionAndFinish()
                                dialog.dismiss()
                            }, DialogActionStyle.Destructive),
                        onDismissRequest = dialog::dismiss,
                    )
                }.apply {
                    setCanceledOnTouchOutside(false)
                    setCancelable(false)
                    show()
                }
            }

            /**
             * List of options to present to the user when the collection is broken.
             */
            fun createList() =
                if (isLoggedIn()) {
                    listOf(RESTORE_FROM_ANKIWEB, INSTALL_NON_PLAY_APP_NORMAL, RESTORE_FROM_BACKUP, GET_HELP, RECREATE_COLLECTION)
                } else {
                    listOf(INSTALL_NON_PLAY_APP_RECOMMENDED, RESTORE_FROM_BACKUP, GET_HELP, RECREATE_COLLECTION)
                }

            /**
             * List of options to present to the users when the storage is not usable.
             * Copied from [createList], without [RESTORE_FROM_ANKIWEB]. Indeed, if we're unable to
             * access the Deck Picker to sync, we have no folder to restore to.
             */
            fun createNoStorageList() = createList().filter { it != RESTORE_FROM_ANKIWEB }
        }
    }

    private fun closeCollectionAndFinish() {
        requireAnkiActivity().closeCollectionAndFinish()
    } // Generic message shown when a libanki task failed

    // The sqlite database has been corrupted (DatabaseErrorHandler.onCorrupt() was called)
    // Show a specific message appropriate for the situation
    private val message: String?
        get() =
            when (requireDialogType()) {
                DIALOG_LOAD_FAILED ->
                    if (DatabaseCorruption.isDetected) {
                        // The sqlite database has been corrupted (DatabaseErrorHandler.onCorrupt() was called)
                        // Show a specific message appropriate for the situation
                        res().getString(CommonString.corrupt_db_message, res().getString(R.string.repair_deck))
                    } else {
                        // Generic message shown when a libanki task failed
                        res().getString(CommonString.access_collection_failed_message, res().getString(R.string.link_help))
                    }
                DIALOG_DB_ERROR -> res().getString(CommonString.answering_error_message)
                DIALOG_DISK_FULL -> res().getString(CommonString.storage_full_message)
                DIALOG_REPAIR_COLLECTION -> res().getString(CommonString.repair_deck_dialog, BackupManager.BROKEN_COLLECTIONS_SUFFIX)
                DIALOG_RESTORE_BACKUP -> res().getString(CommonString.backup_restore_no_backups)
                DIALOG_NEW_COLLECTION -> res().getString(CommonString.backup_del_collection_question)
                DIALOG_CONFIRM_DATABASE_CHECK -> res().getString(CommonString.check_db_warning)
                DIALOG_CONFIRM_RESTORE_BACKUP -> res().getString(CommonString.restore_backup)
                DIALOG_ONE_WAY_SYNC_FROM_SERVER -> res().getString(CommonString.backup_full_sync_from_server_question)
                DIALOG_DB_LOCKED -> res().getString(CommonString.database_locked_summary_new, res().getString(CommonString.col_path))
                INCOMPATIBLE_DB_VERSION -> {
                    var databaseVersion = -1
                    try {
                        databaseVersion = getDatabaseVersion(requireContext(), CollectionHelper.getCollectionPath(requireContext()))
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to get database version, using -1")
                    }
                    val schemaVersion = Consts.BACKEND_SCHEMA_VERSION
                    res().getString(
                        CommonString.incompatible_database_version_summary,
                        schemaVersion,
                        databaseVersion,
                    )
                }
                DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL -> {
                    val directory =
                        context?.let { CollectionHelper.getCurrentAnkiDroidDirectory(it) }
                            ?: res().getString(CommonString.card_browser_unknown_deck_name)
                    res().getString(CommonString.directory_inaccessible_after_uninstall_summary, directory)
                }
                DIALOG_ERROR_HANDLING -> requireArguments().getString("dialogMessage")
            }
    private val title: String
        get() =
            when (requireDialogType()) {
                DIALOG_LOAD_FAILED -> res().getString(CommonString.open_collection_failed_title)
                DIALOG_ERROR_HANDLING -> res().getString(CommonString.error_handling_title)
                DIALOG_REPAIR_COLLECTION -> res().getString(CommonString.dialog_positive_repair)
                DIALOG_RESTORE_BACKUP -> res().getString(CommonString.backup_restore)
                DIALOG_NEW_COLLECTION -> res().getString(CommonString.backup_new_collection)
                DIALOG_CONFIRM_DATABASE_CHECK -> TR.sentenceCase.checkDatabase
                DIALOG_CONFIRM_RESTORE_BACKUP -> res().getString(CommonString.restore_backup_title)
                DIALOG_ONE_WAY_SYNC_FROM_SERVER -> res().getString(CommonString.backup_one_way_sync_from_server)
                DIALOG_DB_LOCKED -> res().getString(CommonString.database_locked_title)
                INCOMPATIBLE_DB_VERSION -> res().getString(CommonString.incompatible_database_version_title)
                DIALOG_DB_ERROR -> res().getString(CommonString.answering_error_title)
                DIALOG_DISK_FULL -> res().getString(CommonString.storage_full_title)
                DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL -> res().getString(CommonString.directory_inaccessible_after_uninstall)
            }

    override val notificationMessage: String? get() = message
    override val notificationTitle: String get() = res().getString(CommonString.answering_error_title)

    override val dialogHandlerMessage
        get() = ShowDatabaseErrorDialog(requireDialogType())

    private fun requireDialogType() = BundleCompat.getParcelable(requireArguments(), ARG_DIALOG, DatabaseErrorDialogType::class.java)!!

    private val exceptionData: CustomExceptionData? by lazy {
        BundleCompat.getParcelable(requireArguments(), ARG_EXCEPTION, CustomExceptionData::class.java)
    }

    @Parcelize
    enum class DatabaseErrorDialogType : Parcelable {
        DIALOG_LOAD_FAILED,
        DIALOG_DB_ERROR,
        DIALOG_ERROR_HANDLING,
        DIALOG_REPAIR_COLLECTION,
        DIALOG_RESTORE_BACKUP,
        DIALOG_NEW_COLLECTION,
        DIALOG_CONFIRM_DATABASE_CHECK,
        DIALOG_CONFIRM_RESTORE_BACKUP,
        DIALOG_ONE_WAY_SYNC_FROM_SERVER,

        /** If the database is locked, all we can do is reset the app  */
        DIALOG_DB_LOCKED,

        /** If the database is at a version higher than what we can currently handle  */
        INCOMPATIBLE_DB_VERSION,

        /** If the disk space is full **/
        DIALOG_DISK_FULL,

        /** If [android.R.attr.preserveLegacyExternalStorage] is no longer active */
        DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL,
    }

    companion object {
        /**
         * Key for passing a CustomExceptionData object in a Bundle,
         * contains error message and stack trace.
         *
         * @see CustomExceptionData
         */
        private const val ARG_EXCEPTION = "exception"

        // Key for the dialog type in the Bundle, indicating which dialog to show
        private const val ARG_DIALOG = "dialog"

        /**
         * A set of dialogs which deal with problems with the database when it can't load
         *
         * @param dialogType the sub-dialog to show
         */
        @CheckResult
        fun newInstance(
            dialogType: DatabaseErrorDialogType,
            exceptionData: CustomExceptionData? = null,
        ): DatabaseErrorDialog {
            val f = DatabaseErrorDialog()
            val args = Bundle()
            args.putParcelable(ARG_DIALOG, dialogType)
            exceptionData?.let { args.putParcelable(ARG_EXCEPTION, it) }
            f.arguments = args
            return f
        }
    }

    /** Database error dialog */
    class ShowDatabaseErrorDialog(
        val dialogType: DatabaseErrorDialogType,
        val exceptionData: CustomExceptionData? = null,
    ) : DialogHandlerMessage(
            which = WhichDialogHandler.MSG_SHOW_DATABASE_ERROR_DIALOG,
            analyticName = "DatabaseErrorDialog",
        ) {
        override fun handleAsyncMessage(activity: AnkiActivity) {
            activity.showDatabaseErrorDialog(dialogType, exceptionData)
        }

        override fun toMessage(): Message =
            Message.obtain().apply {
                what = this@ShowDatabaseErrorDialog.what
                data =
                    Bundle().apply {
                        putParcelable(ARG_DIALOG, dialogType)
                        exceptionData?.let { putParcelable(ARG_EXCEPTION, it) }
                    }
            }

        companion object {
            fun fromMessage(message: Message): ShowDatabaseErrorDialog {
                val dialogType = BundleCompat.getParcelable(message.data, ARG_DIALOG, DatabaseErrorDialogType::class.java)!!
                val exceptionData = BundleCompat.getParcelable(message.data, ARG_EXCEPTION, CustomExceptionData::class.java)
                return ShowDatabaseErrorDialog(dialogType, exceptionData)
            }
        }
    }

    @Parcelize
    class CustomExceptionData(
        val stackTrace: String,
    ) : Parcelable {
        fun toHumanReadableString() = stackTrace

        companion object {
            private const val MAX_STACKTRACE_LINES = 2000

            @CheckResult
            fun fromException(exception: Exception): CustomExceptionData {
                val stackTraceLines = exception.stackTraceToString().lines()
                val truncatedStackTrace =
                    if (stackTraceLines.size > MAX_STACKTRACE_LINES) {
                        stackTraceLines.take(MAX_STACKTRACE_LINES).joinToString("\n") + "\n...<stack trace truncated>...\n"
                    } else {
                        stackTraceLines.joinToString("\n")
                    }

                return CustomExceptionData(truncatedStackTrace)
            }
        }
    }
}

fun Activity.showDatabaseErrorDialogInDeckPicker(
    errorDialogType: DatabaseErrorDialogType,
    exceptionData: CustomExceptionData? = null,
) {
    if (this is DeckPicker) {
        showDatabaseErrorDialog(errorDialogType, exceptionData)
        return
    }
    Timber.i("Opening the Deck Picker to show %s", errorDialogType)
    DialogHandler.storeMessage(ShowDatabaseErrorDialog(errorDialogType, exceptionData).toMessage())
    startActivity(DeckPicker.getIntent(this).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
}

private enum class ErrorHandlingEntries {
    RETRY,
    REPAIR,
    RESTORE,
    ONE_WAY,
    RESET_TO_DEFAULT_DIRECTORY,
    NEW,
    DEBUG_INFO,
}
