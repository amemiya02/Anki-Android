// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.compose.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.ichi2.anki.common.android.R
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.preview.AnkiDroidPreview
import com.ichi2.compose.ui.preview.DialogPreviews

/**
 * Dialog asking the user for a single line of text. Compose counterpart of
 * the View based `AlertDialog.input(...)` helper.
 *
 * @param onConfirm called with the text, but only while it is valid
 * @param initialText initial value shown in the field
 * @param initialSelection initial cursor position or selected range; defaults to the end of [initialText]
 * @param errorMessageFor error to show for the current input, null when valid
 * @param helperText shown when no error is displayed; an empty string reserves feedback space
 * @param description optional context below the title
 * @param transformInput applied to text edits, not cursor-only changes
 * @param validateOnInput when false, validate on confirmation instead of while editing
 * @param enabled whether confirmation is allowed from the button or keyboard
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
    dismissText: String = stringResource(R.string.dialog_cancel),
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
    maxLengthCounter: Int? = null,
    lengthOf: (String) -> Int = { it.length },
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    errorMessageFor: (String) -> String? = { null },
    helperText: String? = null,
    description: String? = null,
    transformInput: (TextFieldValue) -> TextFieldValue = { it },
    validateOnInput: Boolean = true,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    onTextChanged: (String) -> Unit = {},
) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialText, selection = initialSelection))
    }
    var hasUserEdited by rememberSaveable { mutableStateOf(false) }
    var submissionAttempted by rememberSaveable { mutableStateOf(false) }
    val errorMessage = if (validateOnInput || submissionAttempted) errorMessageFor(input.text) else null
    val displayedError = errorMessage?.takeIf { hasUserEdited || submissionAttempted }
    val supportingMessage = displayedError ?: helperText
    val hasSupportingText = maxLengthCounter != null || supportingMessage != null || !validateOnInput
    val focusRequester = remember { FocusRequester() }

    fun confirmIfValid() {
        if (!enabled) return
        submissionAttempted = true
        if (errorMessageFor(input.text) == null) onConfirm(input.text)
    }
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    PaperDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        actions =
            listOf(
                DialogAction(dismissText, onDismissRequest),
                DialogAction(
                    confirmText,
                    ::confirmIfValid,
                    DialogActionStyle.Filled,
                    enabled = enabled && (!validateOnInput || errorMessage == null),
                ),
            ),
    ) {
        if (description != null) DialogBody(description)
        Text(label, Modifier.testTag("text_input_label"), style = foundation.type.caption, color = colors.onBackgroundMuted)
        OutlinedTextField(
            value = input,
            onValueChange = { proposed ->
                if (input.text != proposed.text) {
                    input = transformInput(proposed)
                    hasUserEdited = true
                    submissionAttempted = false
                    onTextChanged(input.text)
                } else {
                    input = proposed
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = label },
            textStyle = foundation.type.deckNameNested,
            shape = foundation.shapes.dialog,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.onBackground,
                    unfocusedBorderColor = colors.onBackgroundMuted,
                    errorBorderColor = colors.emphasis,
                    errorSupportingTextColor = colors.emphasis,
                    unfocusedSupportingTextColor = colors.onBackgroundMuted,
                    focusedSupportingTextColor = colors.onBackgroundMuted,
                ),
            isError = displayedError != null,
            supportingText =
                if (!hasSupportingText) {
                    null
                } else {
                    {
                        Row(Modifier.fillMaxWidth()) {
                            if (supportingMessage != null || !validateOnInput) {
                                Text(supportingMessage.orEmpty(), Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            if (maxLengthCounter != null) Text("${lengthOf(input.text)}/$maxLengthCounter")
                        }
                    }
                },
            singleLine = singleLine,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = capitalization,
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                ),
            keyboardActions = KeyboardActions(onDone = { confirmIfValid() }),
        )
        LaunchedEffect(focusRequester) {
            onTextChanged(input.text)
            focusRequester.requestFocus()
        }
    }
}

/** @see TextInputDialog */
@DialogPreviews
@Composable
private fun TextInputDialogPreview() {
    AnkiDroidPreview {
        TextInputDialog("Rename deck", "Deck name", "Rename", {}, {}, maxLengthCounter = 50)
    }
}
