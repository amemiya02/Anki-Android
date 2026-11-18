// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.scheduling

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.asyncCatching
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.browser.removeSafely
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.CardId
import com.ichi2.anki.libanki.sched.Scheduler
import com.ichi2.anki.scheduling.SetDueDateViewModel.Tab
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.doOnImeHidden
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.utils.openUrl
import com.ichi2.anki.withProgress
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.DialogField
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.Deferred
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Dialog for [Scheduler.setDueDate], containing two tabs: [Tab.SINGLE_DAY] and [Tab.DATE_RANGE]
 *
 * @see SetDueDateViewModel
 */
class SetDueDateDialog : AnalyticsDialogFragment() {
    // We explicitly do not use calendar controls in this class
    // User feedback:
    // (1) Don't have to think about what today is in order to use it,
    // (2) looking at a calendar makes the future date too concrete
    //  (... easier to consider as a nebulous range than a deadline)
    // (3) If the interval is changed, it will be set to a number of days, not a date.
    // (4) Inconsistent with Anki Desktop

    val viewModel: SetDueDateViewModel by activityViewModels<SetDueDateViewModel>()

    val cardIds: List<Long>?
        get() = requireArguments().requireParcelable<IdsFile>(ARG_IDS_FILE).getIdsOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cardIds =
            try {
                this.cardIds
            } catch (e: IOException) {
                Timber.w(e, "Failed to read cardIds")
                null
            }
        if (cardIds == null) {
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            dismiss()
            return
        }

        viewModel.init(cardIds)
        Timber.d("Set due date dialog: %d card(s)", cardIds.size)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // DialogFragment also calls this from onDestroyView, so without this check a rotation
        // would delete the file which the recreated dialog still needs
        if (activity?.isChangingConfigurations == true) {
            Timber.d("not removing IdsFile: dialog is being recreated")
            return
        }

        if (arguments?.containsKey(ARG_IDS_FILE) == true) {
            requireArguments()
                .requireParcelable<IdsFile>(ARG_IDS_FILE)
                .removeSafely("SetDueDateDialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) { DueDateContent() }.apply {
            window?.clearFlags(FLAG_NOT_FOCUSABLE or FLAG_ALT_FOCUSABLE_IM)
        }

    @Composable
    private fun DueDateContent() {
        if (!viewModel.hasCards) return
        val valid by viewModel.isValidFlow.collectAsState()
        val fsrsEnabled by viewModel.fsrsEnabled.collectAsState()
        val interval by viewModel.currentInterval.collectAsState()
        var tab by rememberSaveable { mutableStateOf(Tab.SINGLE_DAY) }
        var single by rememberDays(viewModel.nextSingleDayDueDate)
        var start by rememberDays(viewModel.dateRange.start)
        var end by rememberDays(viewModel.dateRange.end)
        var matchInterval by rememberSaveable { mutableStateOf(viewModel.updateIntervalToMatchDueDate) }
        val firstInput = remember(tab) { FocusRequester() }
        val lastInput = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            viewModel.currentTab = tab
            viewModel.nextSingleDayDueDate = single.text.toIntOrNull()
            viewModel.setNextDateRangeStart(start.text.toIntOrNull())
            viewModel.setNextDateRangeEnd(end.text.toIntOrNull())
        }
        LaunchedEffect(fsrsEnabled) {
            if (fsrsEnabled == false) viewModel.updateIntervalToMatchDueDate = matchInterval
        }
        LaunchedEffect(tab) {
            withFrameNanos { }
            firstInput.requestFocus()
            keyboard?.show()
        }

        fun submitFromKeyboard() {
            if (!viewModel.isValidFlow.value) return
            requireActivity().launchCatchingTask {
                if (requireActivity().updateDueDate(viewModel, showError = false).await() != null) dismiss()
            }
        }

        PaperDialog(
            title = TR.sentenceCase.setDueDate,
            onDismissRequest = ::dismiss,
            actions =
                listOf(
                    DialogAction(getString(CommonString.help), { openUrl(R.string.link_set_due_date_help) }),
                    DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                    DialogAction(getString(CommonString.dialog_ok), {
                        requireActivity().updateDueDate(viewModel, showError = true)
                        dismiss()
                    }, DialogActionStyle.Filled, enabled = valid),
                ),
        ) {
            DueDateTabRow(tab) { selected ->
                viewModel.currentTab = selected
                tab = selected
            }
            when (tab) {
                Tab.SINGLE_DAY -> {
                    DueDateField(
                        label = pluralStringResource(CommonPlurals.set_due_date_single_day_label, viewModel.cardCount),
                        value = single,
                        onValueChange = {
                            single = it
                            viewModel.nextSingleDayDueDate = it.text.toIntOrNull()
                        },
                        focus = firstInput,
                        onEnter = ::submitFromKeyboard,
                        supportingText =
                            stringResource(
                                CommonString.set_due_date_hintText,
                                pluralStringResource(CommonPlurals.set_due_date_label_suffix, 0),
                                pluralStringResource(CommonPlurals.set_due_date_label_suffix, 1),
                            ),
                    )
                }
                Tab.DATE_RANGE -> {
                    DialogBody(pluralStringResource(CommonPlurals.set_due_date_range_label, viewModel.cardCount))
                    DueDateField(
                        label = stringResource(CommonString.set_due_date_range_start),
                        value = start,
                        onValueChange = {
                            start = it
                            viewModel.setNextDateRangeStart(it.text.toIntOrNull())
                        },
                        focus = firstInput,
                        onEnter = { lastInput.requestFocus() },
                        imeAction = ImeAction.Next,
                    )
                    DueDateField(
                        label = stringResource(CommonString.set_due_date_range_end),
                        value = end,
                        onValueChange = {
                            end = it
                            viewModel.setNextDateRangeEnd(it.text.toIntOrNull())
                        },
                        focus = lastInput,
                        onEnter = ::submitFromKeyboard,
                    )
                }
            }
            interval?.takeIf { viewModel.cardCount == 1 }?.let {
                DialogDetail(pluralStringResource(CommonPlurals.set_due_date_current_interval, it, it))
            }
            if (fsrsEnabled == false) {
                DialogChoiceRow(
                    choice = DialogChoice(stringResource(CommonString.set_due_date_match_interval)),
                    checked = matchInterval,
                    onClick = {
                        matchInterval = !matchInterval
                        viewModel.updateIntervalToMatchDueDate = matchInterval
                    },
                    multiple = true,
                )
            }
        }
    }

    companion object {
        const val ARG_IDS_FILE = "ARGS_IDS_FILE"

        /** Keeps the existing dialog and its input when another request arrives. */
        @MainThread
        fun show(
            activity: FragmentActivity,
            cardIds: List<CardId>,
        ) {
            if (activity.supportFragmentManager.fragments.any { it is SetDueDateDialog }) {
                Timber.d("Ignoring 'set due date' request: dialog is already open")
                return
            }
            val dialog = newInstance(IdsFile.getDirectory(), cardIds)
            activity.showDialogFragment(dialog)
        }

        @VisibleForTesting
        @CheckResult
        fun newInstance(
            idsFileDirectory: File,
            cardIds: List<CardId>,
        ): SetDueDateDialog =
            SetDueDateDialog().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(ARG_IDS_FILE, IdsFile(idsFileDirectory, cardIds, IdsFile.Purpose.SET_DUE_DATE))
                    }
                Timber.i("Showing 'set due date' dialog for %d cards", cardIds.size)
            }
    }
}

@Composable
private fun rememberDays(days: Int?) =
    rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = days?.toString().orEmpty()
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }

@Composable
private fun DueDateTabRow(
    selected: Tab,
    onSelect: (Tab) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tab in Tab.entries) {
            val active = tab == selected
            val color = if (active) foundation.colors.emphasis else foundation.colors.onBackgroundMuted
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = foundation.spacing.touchTarget)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(tab.icon), contentDescription = null, tint = color)
                Text(stringResource(tab.text), style = foundation.type.button, color = color)
            }
        }
    }
}

@Composable
private fun DueDateField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focus: FocusRequester,
    onEnter: () -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    supportingText: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    DialogField(
        label = label,
        value = value,
        onValueChange = { proposed ->
            val text = proposed.text.filter(Char::isDigit).take(5)
            onValueChange(if (text == proposed.text) proposed else TextFieldValue(text, TextRange(text.length)))
        },
        modifier =
            Modifier
                .focusRequester(focus)
                .onFocusChanged {
                    if (it.isFocused && !focused) onValueChange(value.copy(selection = TextRange(0, value.text.length)))
                    focused = it.isFocused
                }.onPreviewKeyEvent {
                    if (it.key != Key.Enter && it.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                    if (it.type == KeyEventType.KeyUp) onEnter()
                    true
                },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onEnter() }, onNext = { onEnter() }),
        suffix = pluralStringResource(CommonPlurals.set_due_date_label_suffix, value.text.toIntOrNull() ?: 0),
        supportingText = supportingText,
    )
}

// this can outlive the lifetime of the fragment
private fun FragmentActivity.updateDueDate(
    viewModel: SetDueDateViewModel,
    showError: Boolean,
): Deferred<Int?> =
    this.asyncCatching {
        // NICE_TO_HAVE: Display a snackbar if the activity is recreated while this executes
        val cardsUpdated =
            withProgress {
                // this is async as it should be run on the viewModel
                viewModel.updateDueDateAsync().await()
            }

        if (cardsUpdated == null) {
            Timber.w("unable to update due date")
            if (showError) {
                showThemedToast(this@updateDueDate, CommonString.something_wrong, true)
            }
            return@asyncCatching null
        }
        Timber.d("updated %d cards", cardsUpdated)
        // Ensure the snackbar doesn't appear in the middle of the screen
        doOnImeHidden {
            showSnackbar(TR.schedulingSetDueDateDone(cardsUpdated), Snackbar.LENGTH_SHORT)
        }
        return@asyncCatching cardsUpdated
    }
