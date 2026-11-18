// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Brian Da Silva <brianjose2010@gmail.com>

package com.ichi2.anki.ui.windows.managespace

import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.LocalizedUnambiguousBackupTimeFormatter
import com.ichi2.anki.R
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.analytics.AnalyticsEvent
import com.ichi2.anki.ioDispatcher
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.SettingsScrollPage
import com.ichi2.anki.preferences.settingsBackAction
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.progress.HasProgress
import com.ichi2.anki.progress.ProgressManager
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.utils.getUserFriendlyErrorText
import com.ichi2.anki.utils.showAndAwaitDismissal
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

sealed interface Size {
    data object Calculating : Size

    class Bytes(
        val totalSize: Long,
    ) : Size

    class FilesAndBytes(
        val files: Collection<File>,
        val totalSize: Long,
    ) : Size

    class Error(
        val exception: Exception,
        @StringRes val widgetTextId: Int = CommonString.pref__widget_text__error,
    ) : Size
}

/**************************************************************************************************
 ********************************************* Model **********************************************
 **************************************************************************************************/

class ManageSpaceViewModel(
    val app: Application,
) : AndroidViewModel(app),
    CollectionDirectoryProvider,
    HasProgress<ManageSpaceProgress> {
    override val collectionDirectory = CollectionManager.getCollectionDirectory()
    override val progressManager = ProgressManager<ManageSpaceProgress>()

    val flowOfDeleteUnusedMediaSize = MutableStateFlow<Size>(Size.Calculating)
    val flowOfDeleteBackupsSize = MutableStateFlow<Size>(Size.Calculating)
    val flowOfDeleteCollectionSize = MutableStateFlow<Size>(Size.Calculating)
    val flowOfDeleteEverythingSize = MutableStateFlow<Size>(Size.Calculating)

    init {
        launchCalculationOfBackupsSize()
        launchCalculationOfSizeOfEverything()
        launchCalculationOfCollectionSize()
        launchSearchForUnusedMedia()
    }

    /************************************* Unused media files *************************************/

    private fun launchSearchForUnusedMedia() =
        viewModelScope.launch {
            flowOfDeleteUnusedMediaSize.ifCollectionDirectoryExistsEmit {
                withCol {
                    val unusedFiles = media.findUnusedMediaFiles()
                    val unusedFilesSize = unusedFiles.sumOf(::calculateSize)
                    Size.FilesAndBytes(unusedFiles, unusedFilesSize)
                }
            }
        }

    suspend fun deleteMediaFiles(filesNamesToDelete: List<String>) {
        progressManager.withProgress(message = ManageSpaceProgress.DELETING_MEDIA) {
            try {
                withCol {
                    media.trashFiles(filesNamesToDelete)
                    media.emptyTrash()
                }
            } finally {
                launchCalculationOfSizeOfEverything()
                launchCalculationOfCollectionSize()
                launchSearchForUnusedMedia()
            }
        }
    }

    /***************************************** Backups ********************************************/

    private fun launchCalculationOfBackupsSize() =
        viewModelScope.launch {
            flowOfDeleteBackupsSize.ifCollectionDirectoryExistsEmit {
                withCol {
                    val backupFiles = BackupManager.getBackups(colDb).toList()
                    val backupFilesSize = backupFiles.sumOf(::calculateSize)
                    Size.FilesAndBytes(backupFiles, backupFilesSize)
                }
            }
        }

    suspend fun deleteBackups(backupsToDelete: List<File>) {
        progressManager.withProgress(message = ManageSpaceProgress.DELETING_BACKUPS) {
            try {
                withCol { BackupManager.deleteBackups(this@withCol, backupsToDelete) }
            } finally {
                launchCalculationOfBackupsSize()
                launchCalculationOfCollectionSize()
                launchCalculationOfSizeOfEverything()
            }
        }
    }

    /*************************************** Collection *******************************************/

    private fun launchCalculationOfCollectionSize() =
        viewModelScope.launch {
            flowOfDeleteCollectionSize.ifCollectionDirectoryExistsEmit {
                withContext(ioDispatcher) {
                    Size.Bytes(calculateSize(collectionDirectory))
                }
            }
        }

    suspend fun deleteCollection() {
        progressManager.withProgress(message = ManageSpaceProgress.DELETING_COLLECTION) {
            try {
                CollectionManager.deleteCollectionDirectory() // Executed in withQueue
            } finally {
                launchCalculationOfBackupsSize()
                launchCalculationOfSizeOfEverything()
                launchCalculationOfCollectionSize()
                launchSearchForUnusedMedia()
            }
        }
    }

    /*************************************** Everything *******************************************/

    private fun launchCalculationOfSizeOfEverything() =
        viewModelScope.launch {
            flowOfDeleteEverythingSize.emit(Size.Calculating)
            flowOfDeleteEverythingSize.emit(
                withContext(ioDispatcher) {
                    try {
                        Size.Bytes(app.getUserDataAndCacheSize())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Size.Error(e)
                    }
                },
            )
        }

    // This kills the process afterwards, so no need to recalculate sizes
    fun deleteEverything() {
        getSystemService(app, ActivityManager::class.java)?.clearApplicationUserData()
    }

    /**********************************************************************************************/

    private suspend fun MutableStateFlow<Size>.ifCollectionDirectoryExistsEmit(block: suspend () -> Size) {
        try {
            ensureCanWriteToOrCreateCollectionDirectory()
            if (!collectionDirectoryExists()) {
                emit(Size.FilesAndBytes(emptyList(), 0L))
            } else {
                emit(Size.Calculating)
                emit(block())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Size.Error(e))
        }
    }
}

enum class ManageSpaceProgress {
    DELETING_MEDIA,
    DELETING_BACKUPS,
    DELETING_COLLECTION,
}

/**************************************************************************************************
 ******************************************** Fragment ********************************************
 **************************************************************************************************/

class ManageSpaceFragment : Fragment() {
    private val viewModel: ManageSpaceViewModel by viewModels()

    private val collectionRemovedWithApp: Boolean
        get() = viewModel.collectionDirectory.isInsideDirectoriesRemovedWithTheApp(requireContext())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.sendAnalyticsScreenView("manageSpace")
        observeProgress(viewModel) { progress -> getString(progress.messageRes) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        settingsComposeView(R.id.settings_manage_space) {
            SettingsScrollPage(stringResource(CommonString.pref__manage_space__screen_title), settingsBackAction) {
                DeletionRow(
                    R.string.pref_delete_media_key,
                    CommonString.pref__delete_unused_media_files__title,
                    CommonString.pref__delete_unused_media_files__summary,
                    viewModel.flowOfDeleteUnusedMediaSize,
                    ::onDeleteUnusedMediaClick,
                )
                DeletionRow(
                    R.string.pref_delete_backups_key,
                    CommonString.pref__delete_backups__title,
                    null,
                    viewModel.flowOfDeleteBackupsSize,
                    ::onDeleteBackupsClick,
                )
                DeletionRow(
                    R.string.pref_delete_collection_key,
                    CommonString.pref__delete_collection__title,
                    CommonString.pref__delete_collection__summary,
                    viewModel.flowOfDeleteCollectionSize,
                    ::onDeleteCollectionClick,
                )
                val deleteTitle: Int
                val deleteSummary: Int
                if (collectionRemovedWithApp) {
                    deleteTitle = CommonString.pref__delete_everything__title
                    deleteSummary = CommonString.pref__delete_everything__summary
                } else {
                    deleteTitle = CommonString.pref__delete_app_data__title
                    deleteSummary = CommonString.pref__delete_app_data__summary
                }
                DeletionRow(
                    R.string.pref_delete_everything_key,
                    deleteTitle,
                    deleteSummary,
                    viewModel.flowOfDeleteEverythingSize,
                    ::onDeleteEverythingClick,
                )
            }
        }

    @Composable
    private fun DeletionRow(
        @StringRes key: Int,
        @StringRes title: Int,
        @StringRes summary: Int?,
        sizes: StateFlow<Size>,
        action: suspend () -> Unit,
    ) {
        val size by sizes.collectAsStateWithLifecycle()
        val current = size
        val empty =
            when (current) {
                is Size.Bytes -> current.totalSize == 0L
                is Size.FilesAndBytes -> current.files.isEmpty()
                else -> false
            }
        SettingsRow(
            title = stringResource(title),
            summary = summary?.let { stringResource(it) },
            value = current.displayText(),
            enabled = !empty,
            onClick = {
                Analytics.send(AnalyticsEvent.SettingTapped(getString(key)))
                launchCatchingTask { action() }
            },
        )
    }

    /************************************ Delete unused media *************************************/

    private suspend fun onDeleteUnusedMediaClick() {
        val size = viewModel.flowOfDeleteUnusedMediaSize.value
        if (size is Size.FilesAndBytes) {
            val unusedFiles = size.files
            val unusedFileNames = unusedFiles.map { it.name }

            val selected =
                chooseFilesForDeletion(
                    title = getString(CommonString.dialog__delete_unused_media_files__title),
                    names = unusedFileNames,
                    initiallySelected = unusedFileNames.indices.toSet(),
                )
            if (selected != null) {
                viewModel.deleteMediaFiles(unusedFileNames.filterIndexed { index, _ -> index in selected })
            }
        } else {
            showSnackbarIfCalculatingOrError(size)
        }
    }

    /*************************************** Delete backups ***************************************/

    private suspend fun onDeleteBackupsClick() {
        val size = viewModel.flowOfDeleteBackupsSize.value
        if (size is Size.FilesAndBytes) {
            val formatter = LocalizedUnambiguousBackupTimeFormatter()
            val backupFiles = size.files
            val backupNames = backupFiles.map { formatter.getTimeOfBackupAsText(it) }

            val selected =
                chooseFilesForDeletion(
                    title = getString(CommonString.dialog__delete_backups__title),
                    names = backupNames,
                    initiallySelected = emptySet(),
                )
            if (selected != null) {
                viewModel.deleteBackups(backupFiles.filterIndexed { index, _ -> index in selected })
            }
        } else {
            showSnackbarIfCalculatingOrError(size)
        }
    }

    // ************************************ Delete collection *************************************

    /** While we are in this activity, we may have other activities opened.
     * These activities might be visible to user, e.g. when using Split screen mode.
     * When the collection is deleted, whatever these activities are showing may become invalid.
     * To fix that, ideally we should refresh or finish these activities,
     * however, as this task is not trivial, we opt for killing the process instead and exit the application.
     * One major downside here is that killing the process makes this activity,
     * and possibly even the settings app, vanish without animation. **/
    private suspend fun onDeleteCollectionClick() {
        val size = viewModel.flowOfDeleteCollectionSize.value
        if (size is Size.Bytes) {
            if (confirmDeletion(CommonString.dialog__delete_collection__title, CommonString.dialog__delete_collection__message)) {
                try {
                    viewModel.deleteCollection()
                } finally {
                    exitProcess(0)
                }
            }
        } else {
            showSnackbarIfCalculatingOrError(size)
        }
    }

    /************************************* Delete everything **************************************/

    private suspend fun onDeleteEverythingClick() {
        val (title, message) =
            if (collectionRemovedWithApp) {
                CommonString.dialog__delete_everything__title to CommonString.dialog__delete_everything__message
            } else {
                CommonString.dialog__delete_app_data__title to CommonString.dialog__delete_app_data__message
            }
        if (confirmDeletion(title, message)) viewModel.deleteEverything()
    }

    private suspend fun confirmDeletion(
        @StringRes title: Int,
        @StringRes message: Int,
    ): Boolean {
        var confirmed = false
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = getString(title),
                message = getString(message),
                confirm =
                    DialogAction(getString(CommonString.dialog_positive_delete), {
                        confirmed = true
                        dialog.dismiss()
                    }, DialogActionStyle.Destructive),
                onDismissRequest = dialog::dismiss,
            )
        }.showAndAwaitDismissal()
        return confirmed
    }

    private suspend fun chooseFilesForDeletion(
        title: String,
        names: List<String>,
        initiallySelected: Set<Int>,
    ): Set<Int>? {
        var result: Set<Int>? = null
        createPaperDialog(requireContext()) { dialog ->
            var selected by remember { mutableStateOf(initiallySelected) }
            PaperDialog(
                title = title,
                onDismissRequest = dialog::dismiss,
                mark = DialogMark(R.drawable.symbol_warning, warning = true),
                actions =
                    listOf(
                        DialogAction(getString(CommonString.dialog_cancel), dialog::dismiss),
                        DialogAction(getString(CommonString.dialog_positive_delete), {
                            result = selected
                            dialog.dismiss()
                        }, DialogActionStyle.Destructive),
                    ),
            ) {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    itemsIndexed(names) { index, name ->
                        DialogChoiceRow(
                            DialogChoice(name),
                            checked = index in selected,
                            onClick = {
                                selected = if (index in selected) selected - index else selected + index
                            },
                            multiple = true,
                        )
                    }
                }
            }
        }.showAndAwaitDismissal()
        return result
    }

    /**********************************************************************************************
     ************************************* Misplaced methods **************************************
     **********************************************************************************************/

    private fun Size.displayText(): String =
        when (this) {
            Size.Calculating -> getString(CommonString.pref__widget_text__calculating)
            is Size.Error -> getString(widgetTextId)
            is Size.Bytes -> Formatter.formatShortFileSize(requireContext(), totalSize)
            is Size.FilesAndBytes ->
                resources.getQuantityString(
                    CommonPlurals.pref__widget_text__n_files_n_bytes,
                    files.size,
                    files.size,
                    Formatter.formatShortFileSize(requireContext(), totalSize),
                )
        }

    private fun showSnackbarIfCalculatingOrError(size: Size) {
        when (size) {
            is Size.Calculating -> showSnackbar(CommonString.pref__etc__snackbar__calculating)
            is Size.Error -> showSnackbar(requireContext().getUserFriendlyErrorText(size.exception))
            else -> {}
        }
    }
}

@get:StringRes
private val ManageSpaceProgress.messageRes: Int
    get() =
        when (this) {
            ManageSpaceProgress.DELETING_MEDIA -> CommonString.delete_media_message
            ManageSpaceProgress.DELETING_BACKUPS -> CommonString.progress__deleting_backups
            ManageSpaceProgress.DELETING_COLLECTION -> CommonString.progress__deleting_collection
        }
