// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Hari Srinivasan <harisrini21@gmail.com>

package com.ichi2.anki.dialogs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.CrashReportData.Companion.toCrashReportData
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.showError
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.sync.launchCatchingRequiringOneWaySync
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.InitStatus
import com.ichi2.anki.withProgress
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogActions
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogTitle
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.themes.Themes
import com.ichi2.utils.LanguageUtil
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

class ChangeNoteTypeDialog : AnalyticsDialogFragment() {
    private val viewModel: ChangeNoteTypeViewModel by viewModels { defaultViewModelProviderFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_AnkiDroid_AlertDialog_FullScreen)
        launchCatchingTask {
            viewModel.closeDialogFlow.filterNotNull().collect {
                parentFragmentManager.setFragmentResult(REQUEST_KEY_NOTE_TYPE_CHANGED, Bundle())
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val window = dialog?.window ?: return
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !Themes.isNightTheme
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { AnkiDroidTheme { ChangeNoteTypeContent() } }
    }

    @Composable
    private fun ChangeNoteTypeContent() {
        val status by viewModel.flowOfInitStatus.collectAsState()
        val foundation = MaterialTheme.foundation
        Surface(color = foundation.colors.background, contentColor = foundation.colors.onBackground) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = foundation.spacing.screenMargin),
            ) {
                DialogTitle(TR.sentenceCase.changeNoteType)
                if (status == InitStatus.Completed) {
                    MappingPages(Modifier.weight(1f))
                } else {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                if (status is InitStatus.Failed) {
                    LaunchedEffect(status) {
                        val error = (status as InitStatus.Failed).exception
                        requireContext().showError(error.toString(), error.toCrashReportData(requireContext()))
                        dismiss()
                    }
                }
                val actions = mutableListOf(DialogAction(getString(CommonString.dialog_cancel), ::dismiss))
                if (status == InitStatus.Completed) {
                    val changed by viewModel.hasChangesFlow.collectAsState()
                    actions.add(
                        DialogAction(getString(CommonString.save), {
                            requireActivity().changeNoteType(viewModel)
                        }, DialogActionStyle.Filled, enabled = changed),
                    )
                }
                DialogActions(actions, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    private fun MappingPages(modifier: Modifier) {
        val output by viewModel.outputNoteTypeFlow.collectAsState()
        val fieldMap by viewModel.fieldChangeMapFlow.collectAsState()
        val templateMap by viewModel.templateChangeMapFlow.collectAsState()
        val discardedFields by viewModel.discardedFieldsFlow.collectAsState()
        val discardedTemplates by viewModel.discardedTemplatesFlow.collectAsState()
        val input = viewModel.inputNoteType
        val foundation = MaterialTheme.foundation
        var pickingType by rememberSaveable { mutableStateOf(false) }
        val pager = rememberPagerState(viewModel.currentTab.position) { 2 }
        val scope = rememberCoroutineScope()
        LaunchedEffect(pager.currentPage) { viewModel.selectTabByPosition(pager.currentPage) }
        if (pickingType) {
            SingleChoiceDialog(
                title = getString(CommonString.change_note_type_to),
                choices = viewModel.availableNoteTypes.map { DialogChoice(it.name) },
                selectedIndex = viewModel.availableNoteTypes.indexOfFirst { it.id == output.id },
                onSelect = {
                    viewModel.setOutputNoteTypeId(viewModel.availableNoteTypes[it].id)
                    pickingType = false
                },
                onDismissRequest = { pickingType = false },
            )
        }
        HorizontalPager(pager, modifier = modifier, verticalAlignment = Alignment.Top) { page ->
            val fields = page == 0
            val inputs = if (fields) input.fieldsNames else input.templatesNames
            val outputs = if (fields) output.fieldsNames else output.templatesNames
            val mapping = if (fields) fieldMap else templateMap
            val discarded = if (fields) discardedFields else discardedTemplates
            var pickingRow by rememberSaveable(output.id, page) { mutableIntStateOf(-1) }
            if (pickingRow in outputs.indices) {
                val choices = inputs + TR.changeNotetypeNothing()
                SingleChoiceDialog(
                    title = outputs[pickingRow],
                    choices = choices.map { DialogChoice(it) },
                    selectedIndex = mapping[pickingRow] ?: inputs.size,
                    onSelect = { index ->
                        val selected = if (index == inputs.size) SelectedIndex.NOTHING else SelectedIndex.from(index)
                        if (fields) {
                            viewModel.updateFieldMapping(
                                pickingRow,
                                selected,
                            )
                        } else {
                            viewModel.updateTemplateMapping(pickingRow, selected)
                        }
                        pickingRow = -1
                    },
                    onDismissRequest = { pickingRow = -1 },
                )
            }
            LazyColumn(Modifier.fillMaxSize().testTag("mapping_page_$page"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        getString(CommonString.change_note_type_from),
                        style = foundation.type.caption,
                        color = foundation.colors.onBackgroundMuted,
                    )
                    Text(input.name, style = foundation.type.rowTitle)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                            ) { pickingType = true }
                            .testTag("target_type")
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            getString(CommonString.change_note_type_to),
                            style = foundation.type.caption,
                            color = foundation.colors.onBackgroundMuted,
                        )
                        Text(output.name, style = foundation.type.rowTitle)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf(TR.changeNotetypeFields(), TR.changeNotetypeTemplates()).forEachIndexed { index, title ->
                            Tab(selected = page == index, onClick = {
                                scope.launch { pager.scrollToPage(index) }
                            }, modifier = Modifier.weight(1f), text = { Text(title, style = foundation.type.button) })
                        }
                    }
                }
                if (!fields && !viewModel.canChangeTemplates) {
                    item {
                        val message =
                            if (input.isCloze &&
                                !output.isCloze
                            ) {
                                CommonString.extra_cloze_deletions_removed
                            } else {
                                CommonString.card_numbers_unchanged
                            }
                        Text(getString(message))
                    }
                } else {
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            Text(TR.changeNotetypeCurrent(), Modifier.weight(1f), style = foundation.type.caption)
                            Text(TR.changeNotetypeNew(), Modifier.weight(1f), style = foundation.type.caption)
                        }
                    }
                    items(outputs.size) { index ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = foundation.spacing.touchTarget)
                                .clickable(role = Role.Button) { pickingRow = index }
                                .testTag("mapping_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(mapping[index]?.let(inputs::getOrNull) ?: TR.changeNotetypeNothing(), Modifier.weight(1f))
                            Text(outputs[index], Modifier.weight(1f))
                        }
                        HorizontalDivider(color = foundation.colors.outline)
                    }
                    if (discarded.isNotEmpty()) {
                        item {
                            val label = if (fields) TR.changeNotetypeWillDiscardContent() else TR.changeNotetypeWillDiscardCards()
                            Text(label + " " + discarded.joinToString(LanguageUtil.getListSeparator(requireContext())))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_NOTE_IDS = "ARG_NOTE_IDS"

        /** Result key emitted via `setFragmentResult` when note type change completes successfully */
        const val REQUEST_KEY_NOTE_TYPE_CHANGED = "ChangeNoteTypeDialog::noteTypeChanged"

        @CheckResult
        fun newInstance(noteIds: List<NoteId>) =
            ChangeNoteTypeDialog().apply {
                val ids = noteIds.distinct()
                arguments =
                    Bundle().apply {
                        putLongArray(ARG_NOTE_IDS, ids.toLongArray())
                    }
                Timber.i("Showing 'change note type' dialog for %d notes", ids.size)
            }
    }
}

private fun FragmentActivity.changeNoteType(viewModel: ChangeNoteTypeViewModel) =
    this.launchCatchingRequiringOneWaySync {
        try {
            val notesUpdated =
                withProgress {
                    viewModel.executeChangeNoteTypeAsync().await()
                }
            val message = TR.browsingNotesUpdated(notesUpdated)
            showSnackbar(message, Snackbar.LENGTH_SHORT)
        } catch (ex: ChangeNoteTypeException) {
            showError(ex.kind.toString(this), crashReportData = null)
        }
    }

@VisibleForTesting
fun ChangeNoteTypeException.Kind.toString(context: Context): String =
    when (this) {
        ChangeNoteTypeException.Kind.NO_CHANGES -> context.getString(CommonString.error_no_changes_to_save)
    }
