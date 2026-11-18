// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import anki.notetypes.StockNotetype
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CardTemplateEditor
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.awaitCatchingTask
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.NoteTypeId
import com.ichi2.anki.progress.ViewModelProgress
import com.ichi2.anki.progress.observeProgress
import com.ichi2.anki.startup.ensureStorageIsReady
import com.ichi2.anki.utils.Destination
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.themes.Themes

class ManageNotetypes : AnkiActivity() {
    val viewModel by viewModels<ManageNoteTypesViewModel>()

    private var templateRevision by mutableIntStateOf(0)
    private val templateEditor =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.refreshNoteTypes()
            templateRevision++
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        if (!ensureStorageIsReady()) return

        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme })
        setContent {
            AnkiDroidTheme {
                NoteTypesRoute(
                    viewModel = viewModel,
                    templateRevision = templateRevision,
                    onClose = ::finish,
                    onTemplate = { id, row -> templateEditor.launch(CardTemplateEditor.getIntent(this, id, ord = row.ord)) },
                    runTask = { block -> launchCatchingTask { block() } },
                    onAdd = { item, name ->
                        val added =
                            if (item.isStandard) {
                                viewModel.addStockNoteType(name, StockNotetype.Kind.forNumber(item.id.toInt()))
                            } else {
                                viewModel.cloneNoteType(name, item.id)
                            }
                        added.awaitCatchingTask()
                    },
                )
            }
        }
        observeProgress(viewModel) { it }
        viewModel.pendingAdd?.awaitCatchingTask()
        viewModel.pendingFieldEdit?.awaitCatchingTask()
    }
}

@Composable
private fun NoteTypesRoute(
    viewModel: ManageNoteTypesViewModel,
    templateRevision: Int,
    onClose: () -> Unit,
    onTemplate: (NoteTypeId, NoteTemplateRow) -> Unit,
    runTask: (suspend () -> Unit) -> Unit,
    onAdd: (AddNotetypeUiModel, String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progressManager.progress.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val fieldResult by viewModel.fieldResult.collectAsStateWithLifecycle()
    var openedId by rememberSaveable { mutableStateOf<NoteTypeId?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<NoteTypeDialog?>(null) }
    var field by rememberSaveable { mutableStateOf<NoteFieldRef?>(null) }
    var pendingChange by rememberSaveable { mutableStateOf<FieldChange?>(null) }
    var fieldMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val opened = state.noteTypes.firstOrNull { it.id == openedId }
    val openedDetail = detail?.takeIf { it.id == openedId }
    val closeDialog = { dialog = null }
    val busy = progress is ViewModelProgress.Active

    fun goBack() {
        if (busy) return
        when {
            openedId != null -> openedId = null
            state.isInMultiSelectMode -> viewModel.clearSelection()
            searching -> {
                searching = false
                viewModel.filter("")
            }
            else -> onClose()
        }
    }

    fun edit(
        id: NoteTypeId,
        change: FieldChange,
        confirmed: Boolean = false,
    ) {
        dialog = null
        runTask { viewModel.editField(id, change, confirmed).await() }
    }

    BackHandler { goBack() }
    LaunchedEffect(state.destination) {
        state.destination?.let {
            openedId = it
            viewModel.clearDestination()
        }
    }
    LaunchedEffect(openedId, templateRevision) {
        openedId?.let { id -> runTask { viewModel.refreshDetail(id) } }
    }
    LaunchedEffect(opened, state.noteTypes) {
        if (openedId != null && opened == null && state.noteTypes.isNotEmpty()) {
            openedId = null
            dialog = null
        }
    }
    LaunchedEffect(fieldResult) {
        val outcome = fieldResult ?: return@LaunchedEffect
        viewModel.consumeFieldEditResult()
        if (outcome.id != openedId) return@LaunchedEffect
        when (outcome.result) {
            FieldChangeResult.NeedsFullSync -> {
                pendingChange = outcome.change
                dialog = NoteTypeDialog.SyncField
            }
            FieldChangeResult.DuplicateName -> fieldMessage = CommonString.toast_duplicate_field
            FieldChangeResult.LastField -> fieldMessage = CommonString.toast_last_field
            else -> Unit
        }
    }

    if (opened == null) {
        NoteTypesScreen(
            state = state,
            searching = searching,
            onBack = ::goBack,
            onSearch = { searching = true },
            onQuery = viewModel::filter,
            onAdd = { dialog = NoteTypeDialog.AddType },
            onNoteType = viewModel::onItemClick,
            onSelect = viewModel::onItemLongClick,
            onDeleteSelected = { dialog = NoteTypeDialog.DeleteTypes },
        )
    } else {
        key(opened.id) {
            NoteTypeDetailPage(
                item = opened,
                detail = openedDetail,
                onBack = ::goBack,
                onRename = { dialog = NoteTypeDialog.RenameType },
                onDelete = { dialog = NoteTypeDialog.DeleteTypes },
                onAddField = {
                    field = null
                    dialog = NoteTypeDialog.FieldName
                },
                onField = {
                    field = it
                    dialog = NoteTypeDialog.FieldActions
                },
                onMove = { ref, position -> edit(opened.id, FieldChange(FieldAction.Move, ref, position = position)) },
                onTemplate = { onTemplate(opened.id, it) },
            )
        }
    }

    val context = LocalContext.current
    when (dialog) {
        NoteTypeDialog.AddType -> {
            var options by remember { mutableStateOf<Pair<List<AddNotetypeUiModel>, List<String>>?>(null) }
            LaunchedEffect(Unit) { runTask { options = viewModel.loadAddNoteTypeOptions() } }
            options?.let { (choices, names) ->
                AddNoteTypeSheet(choices, names, onAdd = { item, name ->
                    closeDialog()
                    onAdd(item, name)
                }, onDismiss = closeDialog)
            }
        }
        NoteTypeDialog.RenameType ->
            if (opened != null) {
                TextInputDialog(
                    title = stringResource(CommonString.rename_model),
                    label = TR.deckConfigNamePrompt(),
                    confirmText = stringResource(CommonString.rename),
                    initialText = opened.name,
                    onConfirm = {
                        closeDialog()
                        viewModel.rename(opened.id, it.trim())
                    },
                    onDismissRequest = closeDialog,
                    errorMessageFor = { input -> renameError(context, opened, state.noteTypes, input.trim()) },
                )
            }
        NoteTypeDialog.DeleteTypes -> {
            val selected = if (opened == null) viewModel.selectedNoteTypes else listOf(opened)
            DeleteNoteTypesDialog(
                selected = selected,
                onDismiss = closeDialog,
                onConfirm = {
                    closeDialog()
                    if (state.noteTypes.size == 1) {
                        viewModel.delete(state.noteTypes.single().id)
                    } else {
                        runTask {
                            if (withCol { schemaChanged() }) {
                                if (opened != null) viewModel.delete(opened.id) else viewModel.deleteSelectedNoteTypes()
                            } else {
                                dialog = NoteTypeDialog.SyncTypes
                            }
                        }
                    }
                },
            )
        }
        NoteTypeDialog.SyncTypes ->
            NoteTypeFullSyncDialog(onDismiss = closeDialog, onConfirm = {
                closeDialog()
                if (opened !=
                    null
                ) {
                    viewModel.delete(opened.id, confirmSchema = true)
                } else {
                    viewModel.deleteSelectedNoteTypes(confirmSchema = true)
                }
            })
        NoteTypeDialog.SyncField -> {
            val change = pendingChange
            val id = openedId
            if (change != null && id != null) {
                NoteTypeFullSyncDialog(
                    onDismiss = {
                        pendingChange = null
                        closeDialog()
                    },
                    onConfirm = {
                        pendingChange = null
                        edit(id, change, confirmed = true)
                    },
                    deletingField = change.action == FieldAction.Delete,
                )
            }
        }
        null -> Unit
        else ->
            if (openedDetail != null) {
                FieldDialogs(
                    dialog = dialog,
                    fields = openedDetail.fields,
                    field = field,
                    onDialog = { dialog = it },
                    onEdit = { edit(openedDetail.id, it) },
                    onDelete = { selected ->
                        val change = FieldChange(FieldAction.Delete, selected)
                        if (openedDetail.fields.size == 1) {
                            edit(openedDetail.id, change)
                        } else {
                            runTask {
                                if (withCol { schemaChanged() }) {
                                    dialog = NoteTypeDialog.DeleteField
                                } else {
                                    pendingChange = change
                                    dialog = NoteTypeDialog.SyncField
                                }
                            }
                        }
                    },
                )
            }
    }

    val error = state.error
    if (error != null) {
        LaunchedEffect(error) {
            if (error.isReportable) CrashReportService.sendExceptionReport(error.source, ManageNotetypes::class.java.simpleName)
        }
        ErrorDialog(
            title = "",
            message = error.source.localizedMessage.orEmpty(),
            onDismissRequest = {
                viewModel.clearError()
                viewModel.refreshNoteTypes()
            },
        )
    }
    if (fieldMessage != null || state.message != null) {
        ErrorDialog(
            title = "",
            message = stringResource(fieldMessage ?: CommonString.toast_last_model),
            onDismissRequest = {
                fieldMessage = null
                viewModel.clearMessage()
            },
        )
    }
}

private fun renameError(
    context: Context,
    opened: NoteTypeItemState,
    types: List<NoteTypeItemState>,
    input: String,
): String? =
    when {
        input.isBlank() || input == opened.name -> ""
        types.any { it.id != opened.id && it.name.equals(input, ignoreCase = true) } -> context.getString(CommonString.error_name_exists)
        else -> null
    }

@Composable
private fun DeleteNoteTypesDialog(
    selected: List<NoteTypeItemState>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(CommonString.model_browser_delete),
        message =
            if (selected.size == 1) {
                stringResource(CommonString.model_delete_warning)
            } else {
                stringResource(CommonString.model_delete_multiple_warning, selected.joinToString { it.name })
            },
        confirm = DialogAction(stringResource(CommonString.dialog_positive_delete), onConfirm, DialogActionStyle.Destructive),
        onDismissRequest = onDismiss,
    )
}

private enum class NoteTypeDialog {
    AddType,
    RenameType,
    DeleteTypes,
    SyncTypes,
    SyncField,
    FieldActions,
    FieldName,
    MoveField,
    Keyboard,
    DeleteField,
}

@Composable
private fun FieldDialogs(
    dialog: NoteTypeDialog?,
    fields: List<NoteFieldRow>,
    field: NoteFieldRef?,
    onDialog: (NoteTypeDialog?) -> Unit,
    onEdit: (FieldChange) -> Unit,
    onDelete: (NoteFieldRef) -> Unit,
) {
    val row = fields.firstOrNull { it.field == field } ?: fields.singleOrNull { field?.id != null && it.field.id == field.id }
    val dismiss = { onDialog(null) }
    if (field != null && row == null) {
        LaunchedEffect(field, dialog) { dismiss() }
        return
    }
    when (dialog) {
        NoteTypeDialog.FieldActions ->
            if (row != null) {
                FieldActionsSheet(row, dismiss) { action ->
                    when (action) {
                        FieldAction.Rename -> onDialog(NoteTypeDialog.FieldName)
                        FieldAction.Move -> onDialog(NoteTypeDialog.MoveField)
                        FieldAction.Sort -> onEdit(FieldChange(action, row.field))
                        FieldAction.Keyboard -> onDialog(NoteTypeDialog.Keyboard)
                        FieldAction.Delete -> onDelete(row.field)
                        FieldAction.Add -> Unit
                    }
                }
            }
        NoteTypeDialog.FieldName ->
            FieldNameDialog(row?.field, fields, dismiss) {
                onEdit(FieldChange(if (row == null) FieldAction.Add else FieldAction.Rename, row?.field, value = it))
            }
        NoteTypeDialog.MoveField ->
            if (row != null) {
                FieldPositionDialog(row, fields, dismiss) { onEdit(FieldChange(FieldAction.Move, row.field, position = it)) }
            }
        NoteTypeDialog.Keyboard ->
            if (row != null) {
                FieldKeyboardDialog(
                    row.keyboard,
                    dismiss,
                ) { onEdit(FieldChange(FieldAction.Keyboard, row.field, value = it?.toLanguageTag().orEmpty())) }
            }
        NoteTypeDialog.DeleteField ->
            if (row != null) {
                ConfirmationDialog(
                    title = stringResource(CommonString.model_field_editor_delete),
                    message = stringResource(R.string.note_type_field_delete_confirm, row.field.name),
                    confirm =
                        DialogAction(
                            stringResource(CommonString.dialog_positive_delete),
                            { onEdit(FieldChange(FieldAction.Delete, row.field)) },
                            DialogActionStyle.Destructive,
                        ),
                    onDismissRequest = dismiss,
                )
            }
        else -> Unit
    }
}

class ManageNoteTypesDestination : Destination {
    override fun toIntent(context: Context) = Intent(context, ManageNotetypes::class.java)
}
