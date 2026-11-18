// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogActions
import com.ichi2.compose.ui.dialogs.DialogTitle
import com.ichi2.compose.ui.dialogs.createPaperDialog

internal class ReviewReminderTimeDialog : DialogFragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val time = requireArguments().requireParcelable<ReviewReminderTime>(ARG_TIME)
        return createPaperDialog(requireContext()) {
            val paper = MaterialTheme.foundation
            val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
            val preferInput = LocalDensity.current.fontScale >= 1.5f
            var keyboard by rememberSaveable { mutableStateOf(preferInput) }
            val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = is24Hour)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
                shape = paper.shapes.dialog,
                color = paper.colors.background,
                contentColor = paper.colors.onBackground,
                border = BorderStroke(paper.shapes.hairline, paper.colors.outline),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.fillMaxWidth()) { DialogTitle(stringResource(R.string.review_reminder_select_time)) }
                        if (keyboard) TimeInput(picker) else TimePicker(picker)
                        TextButton(onClick = { keyboard = !keyboard }) {
                            Text(
                                stringResource(
                                    if (keyboard) R.string.review_reminder_clock_input else R.string.review_reminder_keyboard_input,
                                ),
                            )
                        }
                    }
                    DialogActions(
                        listOf(
                            DialogAction(stringResource(CommonString.dialog_cancel), ::dismiss),
                            DialogAction(
                                stringResource(CommonString.dialog_ok),
                                {
                                    setFragmentResult(
                                        REQUEST_TIME,
                                        Bundle().apply { putParcelable(RESULT_TIME, ReviewReminderTime(picker.hour, picker.minute)) },
                                    )
                                    dismiss()
                                },
                                DialogActionStyle.Filled,
                            ),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_TIME = "arg_time"
        const val REQUEST_TIME = "request_reminder_time"
        const val RESULT_TIME = "result_reminder_time"

        fun newInstance(time: ReviewReminderTime) =
            ReviewReminderTimeDialog().apply { arguments = Bundle().apply { putParcelable(ARG_TIME, time) } }
    }
}
