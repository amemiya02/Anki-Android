// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.cardviewer.Gesture
import com.ichi2.anki.dialogs.KeySelectionDialogUtils
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.CardSide
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.preferences.ControlPreference
import com.ichi2.preferences.ReviewerControlPreference
import com.ichi2.preferences.WhiteboardControlPreference

private enum class BindingEditorStep { BINDINGS, GESTURE, KEY, AXIS, REPLACE_AXIS, SELECT_SIDE, ASSIGN_SHOW_ANSWER }

@Composable
internal fun ControlBindingEditor(
    action: ControlPreference,
    showAnswer: ReviewerControlPreference?,
    onDismiss: () -> Unit,
    onChange: () -> Unit,
) {
    val context = LocalContext.current
    val title = action.title.toString()
    val cancel = DialogAction(stringResource(CommonString.dialog_cancel), onDismiss)
    var step by rememberSaveable { mutableStateOf(BindingEditorStep.BINDINGS) }
    var pendingBinding by rememberSaveable { mutableStateOf<String?>(null) }
    val binding = pendingBinding?.let(Binding::fromString)

    fun done() {
        onChange()
        onDismiss()
    }

    fun assign(input: Binding) {
        if (action !is ReviewerControlPreference) {
            action.addBinding(input)
            done()
            return
        }
        when (val side = action.side) {
            null -> step = BindingEditorStep.SELECT_SIDE
            CardSide.ANSWER -> {
                val hasQuestionBinding =
                    showAnswer?.getMappableBindings()?.any { it.binding == input && it.side != CardSide.ANSWER } == true
                if (hasQuestionBinding || showAnswer == null) {
                    action.addBinding(input, side)
                    done()
                } else {
                    step = BindingEditorStep.ASSIGN_SHOW_ANSWER
                }
            }
            else -> {
                action.addBinding(input, side)
                done()
            }
        }
    }

    fun select(input: Binding) {
        pendingBinding = input.toString()
        if (step == BindingEditorStep.AXIS && action.otherActionTitleForBinding(input) != null) {
            step = BindingEditorStep.REPLACE_AXIS
        } else {
            assign(input)
        }
    }

    when (step) {
        BindingEditorStep.BINDINGS ->
            PaperDialog(title, onDismiss, actions = listOf(cancel)) {
                if (action.areGesturesEnabled) {
                    SettingsRow(
                        stringResource(CommonString.binding_add_gesture),
                        icon = R.drawable.symbol_touch_app,
                        onClick = { step = BindingEditorStep.GESTURE },
                    )
                }
                SettingsRow(
                    stringResource(CommonString.binding_add_key),
                    icon = R.drawable.symbol_keyboard,
                    onClick = { step = BindingEditorStep.KEY },
                )
                SettingsRow(
                    stringResource(CommonString.binding_add_axis),
                    icon = R.drawable.symbol_videogame_asset,
                    onClick = { step = BindingEditorStep.AXIS },
                )
                action.getMappableBindings().forEachIndexed { index, mapped ->
                    SettingsRow(
                        stringResource(CommonString.binding_remove_binding, action.displayBinding(mapped)),
                        onClick = {
                            action.removeBindingAt(index)
                            done()
                        },
                    )
                }
            }

        BindingEditorStep.GESTURE ->
            GestureBindingPicker(title, action, cancel, onDismiss, ::select)

        BindingEditorStep.KEY ->
            KeyBindingPicker(title, action, cancel, onDismiss, ::select)

        BindingEditorStep.AXIS ->
            PaperDialog(title, onDismiss, actions = listOf(cancel)) {
                AxisBindingPicker(::select)
            }

        BindingEditorStep.REPLACE_AXIS ->
            if (binding != null) {
                PaperDialog(
                    binding.toDisplayString(context),
                    onDismiss,
                    actions =
                        listOf(
                            cancel,
                            DialogAction(
                                stringResource(CommonString.dialog_positive_replace),
                                { assign(binding) },
                                DialogActionStyle.Filled,
                            ),
                        ),
                ) {
                    DialogBody(
                        stringResource(
                            CommonString.bindings_already_bound,
                            action.otherActionTitleForBinding(binding)?.toString().orEmpty(),
                        ),
                    )
                }
            }

        BindingEditorStep.SELECT_SIDE ->
            if (binding != null && action is ReviewerControlPreference) {
                val sides = listOf(CardSide.BOTH, CardSide.QUESTION, CardSide.ANSWER)
                SingleChoiceDialog(
                    title = stringResource(CommonString.card_side_selection_title),
                    choices =
                        listOf(
                            DialogChoice(stringResource(CommonString.card_side_both)),
                            DialogChoice(stringResource(CommonString.card_side_question)),
                            DialogChoice(stringResource(CommonString.card_side_answer)),
                        ),
                    selectedIndex = null,
                    onSelect = { index ->
                        action.addBinding(binding, sides[index])
                        done()
                    },
                    onDismissRequest = onDismiss,
                )
            }

        BindingEditorStep.ASSIGN_SHOW_ANSWER ->
            if (binding != null && action is ReviewerControlPreference) {
                PaperDialog(
                    title,
                    onDismiss,
                    actions =
                        listOf(
                            DialogAction(stringResource(CommonString.dialog_skip), {
                                action.addBinding(binding, CardSide.ANSWER)
                                done()
                            }),
                            DialogAction(stringResource(CommonString.dialog_assign), {
                                action.addBinding(binding, CardSide.ANSWER)
                                showAnswer?.addBinding(binding, CardSide.QUESTION)
                                done()
                            }, DialogActionStyle.Filled),
                        ),
                ) {
                    DialogBody(stringResource(CommonString.also_assign_binding_to_show_answer, stringResource(CommonString.show_answer)))
                }
            }
    }
}

@Composable
private fun KeyBindingPicker(
    title: String,
    action: ControlPreference,
    cancel: DialogAction,
    onDismiss: () -> Unit,
    onSelect: (Binding) -> Unit,
) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf<String?>(null) }
    val binding = input?.let { Binding.fromString(it) }
    val acceptsKey = remember { KeySelectionDialogUtils.disallowModifierKeyCodes() }
    PaperDialog(
        title,
        onDismiss,
        actions =
            listOf(
                cancel,
                DialogAction(
                    stringResource(CommonString.dialog_ok),
                    { binding?.let(onSelect) },
                    DialogActionStyle.Filled,
                    enabled = binding != null,
                ),
            ),
    ) {
        val focus = remember { FocusRequester() }
        Text(
            text = binding?.toDisplayString(context) ?: stringResource(CommonString.key_picker_default_press_key),
            style = MaterialTheme.foundation.type.rowTitle,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        KeySelectionDialogUtils.keyBinding(event.nativeKeyEvent, acceptsKey)?.let { input = it.toString() }
                        true
                    }.focusable()
                    .padding(vertical = 16.dp),
        )
        binding?.let(action::otherActionTitleForBinding)?.let {
            DialogDetail(stringResource(CommonString.bindings_already_bound, it))
        }
        LaunchedEffect(focus) { focus.requestFocus() }
    }
}

@Composable
private fun GestureBindingPicker(
    title: String,
    action: ControlPreference,
    cancel: DialogAction,
    onDismiss: () -> Unit,
    onSelect: (Binding) -> Unit,
) {
    val context = LocalContext.current
    val mapper = rememberControlGestureMapper()
    val whiteboard = action is WhiteboardControlPreference
    val options = listOf(null) + if (whiteboard) whiteboardGestures else mapper.tapGestureMode.availableGestures()
    var gesture by rememberSaveable { mutableStateOf<Gesture?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val selected = gesture?.let { Binding.GestureInput(it) }
    val noGesture = stringResource(CommonString.gestures_none)
    val pickerLabel = stringResource(CommonString.binding_add_gesture)

    if (choosing) {
        SingleChoiceDialog(
            title = pickerLabel,
            choices = options.map { DialogChoice(it?.toDisplayString(context) ?: noGesture) },
            selectedIndex = options.indexOf(gesture).takeIf { it >= 0 },
            onSelect = {
                gesture = options[it]
                choosing = false
            },
            onDismissRequest = { choosing = false },
        )
    } else {
        DetectShake { gesture = Gesture.SHAKE }
        PaperDialog(
            title,
            onDismiss,
            actions =
                listOf(
                    cancel,
                    DialogAction(
                        stringResource(CommonString.dialog_ok),
                        { selected?.let(onSelect) },
                        DialogActionStyle.Filled,
                        enabled = selected != null,
                    ),
                ),
        ) {
            if (!whiteboard) GesturePad(mapper, gesture) { gesture = it }
            SettingsRow(
                title = pickerLabel,
                value = gesture?.toDisplayString(context) ?: noGesture,
                onClick = { choosing = true },
                trailing = { SettingsChevron() },
            )
            selected?.let(action::otherActionTitleForBinding)?.let {
                DialogDetail(stringResource(CommonString.bindings_already_bound, it))
            }
        }
    }
}
