// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2018 Mike Hardy <mike@mikehardy.net>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.ichi2.anki.CommonString
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import java.util.function.Consumer

// TODO: Pass optional validation condition i.e. Positive button not enabled if condition is true
open class IntegerDialog : AnalyticsDialogFragment() {
    private var consumer: Consumer<Int>? = null

    fun setCallbackRunnable(consumer: Consumer<Int>?) {
        this.consumer = consumer
    }

    /** use named arguments with this method for clarity */
    fun setArgs(
        title: String,
        prompt: String?,
        digits: Int,
        content: String? = null,
        defaultValue: String? = null,
    ) {
        val args = Bundle()
        args.putString("title", title)
        args.putString("prompt", prompt)
        args.putInt("digits", digits)
        args.putString("content", content)
        args.putString("defaultValue", defaultValue)
        arguments = args
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        return createPaperDialog(requireContext()) {
            var input by rememberSaveable { mutableStateOf(args.getString("defaultValue").orEmpty()) }
            val keyboard = LocalSoftwareKeyboardController.current
            val focus = remember { FocusRequester() }
            val value = input.toIntOrNull()
            val applyValue: () -> Unit = {
                if (value != null) {
                    consumer?.accept(value)
                    dismiss()
                }
            }
            PaperDialog(
                title = args.getString("title").orEmpty(),
                onDismissRequest = ::dismiss,
                actions =
                    listOf(
                        DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                        DialogAction(getString(CommonString.dialog_ok), applyValue, DialogActionStyle.Filled, enabled = value != null),
                    ),
            ) {
                args.getString("content")?.let { DialogBody(it) }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit).take(args.getInt("digits")) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    label = args.getString("prompt")?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { applyValue() }),
                )
                LaunchedEffect(focus) {
                    focus.requestFocus()
                    keyboard?.show()
                }
            }
        }
    }
}
