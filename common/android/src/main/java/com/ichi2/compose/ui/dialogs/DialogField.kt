// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.foundation

/** An outlined, labelled field for dialogs that combine several inputs or choices. */
@Composable
fun DialogField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    suffix: String? = null,
    supportingText: String? = null,
) {
    val foundation = MaterialTheme.foundation
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        textStyle = foundation.type.deckNameNested,
        shape = foundation.shapes.dialog,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = foundation.colors.onBackground,
                unfocusedBorderColor = foundation.colors.onBackgroundMuted,
            ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = suffix?.let { { Text(it, Modifier.padding(end = 12.dp), style = foundation.type.caption) } },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
    )
}
