// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.DialogField
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

/**
 * Follows desktop implementation to show the same ui with all the options to reposition new
 * (currently selected) cards in [CardBrowser].
 * See https://github.com/ankitects/anki/blob/44e01ea063e6d1b812ace9c001f7ba4a8ccf4479/qt/aqt/forms/reposition.ui#L14
 * See https://github.com/ankitects/anki/blob/1fb1cbbf85c48a54c05cb4442b1b424a529cac60/qt/aqt/operations/scheduling.py#L107
 */
class RepositionCardFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            var start by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue("0", selection = TextRange(0, 1)))
            }
            var step by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("1")) }
            var random by rememberSaveable { mutableStateOf(requireArguments().getBoolean(ARG_RANDOM)) }
            var shift by rememberSaveable { mutableStateOf(requireArguments().getBoolean(ARG_SHIFT)) }
            val position = start.text.toIntOrNull()
            val stepSize = step.text.toIntOrNull()
            PaperDialog(
                title = TR.sentenceCase.repositionNewCards,
                onDismissRequest = ::dismiss,
                actions =
                    listOf(
                        DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                        DialogAction(TR.actionsReposition(), {
                            if (position != null && stepSize != null) {
                                setFragmentResult(
                                    REQUEST_REPOSITION_NEW_CARDS,
                                    Bundle().apply {
                                        putInt(RESULT_POSITION, position)
                                        putInt(RESULT_STEP, stepSize)
                                        putBoolean(RESULT_RANDOM, random)
                                        putBoolean(RESULT_SHIFT, shift)
                                    },
                                )
                                dismiss()
                            }
                        }, DialogActionStyle.Filled, enabled = position != null && stepSize != null),
                    ),
            ) {
                DialogDetail(TR.browsingQueueTop(requireArguments().getInt(ARG_QUEUE_TOP)))
                DialogDetail(TR.browsingQueueBottom(requireArguments().getInt(ARG_QUEUE_BOTTOM)))
                val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
                val focus = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    LaunchedEffect(focus) {
                        focus.requestFocus()
                        keyboard?.show()
                    }
                }
                // Desktop limits the start position to six digits and the step to four.
                DialogField(
                    TR.browsingStartPosition().removeSuffix(":"),
                    start,
                    { start = it.withDigitLimit(6) },
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = numeric,
                )
                DialogField(TR.browsingStep().removeSuffix(":"), step, { step = it.withDigitLimit(4) }, keyboardOptions = numeric)
                DialogChoiceRow(DialogChoice(TR.browsingRandomizeOrder()), random, { random = !random }, multiple = true)
                DialogChoiceRow(DialogChoice(TR.browsingShiftPositionOfExistingCards()), shift, { shift = !shift }, multiple = true)
            }
        }

    private fun TextFieldValue.withDigitLimit(limit: Int): TextFieldValue {
        val digits = text.filter(Char::isDigit).take(limit)
        return if (digits == text) this else copy(text = digits, composition = null)
    }

    companion object {
        const val REQUEST_REPOSITION_NEW_CARDS = "request_repositions_new_cards"
        private const val ARG_QUEUE_TOP = "arg_queue_top"
        private const val ARG_QUEUE_BOTTOM = "arg_queue_bottom"
        const val RESULT_POSITION = "arg_position"
        const val RESULT_STEP = "arg_step"
        const val RESULT_RANDOM = "arg_random"
        const val RESULT_SHIFT = "arg_shift"

        private const val ARG_RANDOM = "arg_random"
        private const val ARG_SHIFT = "arg_shift"

        fun newInstance(
            queueTop: Int,
            queueBottom: Int,
            random: Boolean,
            shift: Boolean,
        ) = RepositionCardFragment().apply {
            arguments =
                Bundle().apply {
                    putInt(ARG_QUEUE_TOP, queueTop)
                    putInt(ARG_QUEUE_BOTTOM, queueBottom)
                    putBoolean(ARG_RANDOM, random)
                    putBoolean(ARG_SHIFT, shift)
                }
        }
    }
}
