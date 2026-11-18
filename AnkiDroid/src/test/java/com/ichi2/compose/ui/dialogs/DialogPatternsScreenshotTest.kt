// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.ComposeScreenshotTest
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class DialogPatternsScreenshotTest : ComposeScreenshotTest() {
    @Test
    fun confirmation() =
        capture("confirmation", node = isDialog()) {
            ConfirmationDialog(
                title = stringResource(CommonString.backup_your_collection),
                message = stringResource(CommonString.backup_collection_message),
                confirm = DialogAction(stringResource(CommonString.button_backup), {}),
                onDismissRequest = {},
                dismissText = stringResource(CommonString.button_backup_later),
                markIcon = R.drawable.symbol_settings_backup_restore,
            )
        }

    @Test
    fun destructive_confirmation_focuses_cancel() =
        capture(
            "destructive",
            node = isDialog(),
            beforeCapture = {
                val cancel = RuntimeEnvironment.getApplication().getString(CommonString.dialog_cancel)
                composeRule.onNodeWithText(cancel).assertIsDisplayed().assertIsFocused()
            },
        ) {
            ConfirmationDialog(
                title = stringResource(CommonString.restore_backup_title),
                message = stringResource(CommonString.restore_backup),
                confirm = DialogAction(stringResource(CommonString.paper_dialog_replace_collection), {}, DialogActionStyle.Destructive),
                onDismissRequest = {},
            )
        }

    @Test
    fun input() =
        capture(
            "input",
            node = isDialog(),
            beforeCapture = { composeRule.onNode(hasSetTextAction()).performTextReplacement("日本語 · N3 語彙") },
        ) {
            TextInputDialog(
                title = stringResource(CommonString.paper_dialog_create_deck),
                label = stringResource(CommonString.paper_dialog_deck_name),
                confirmText = stringResource(CommonString.dialog_positive_create),
                onConfirm = {},
                onDismissRequest = {},
            )
        }

    @Test
    fun single_choice_applies_on_tap() {
        var selection = -1
        capture(
            "single_choice",
            node = isDialog(),
            beforeCapture = {
                val option = RuntimeEnvironment.getApplication().getString(CommonString.dialog_no)
                composeRule.onNodeWithText(option).performClick()
                assertEquals(1, selection)
            },
        ) {
            var selected by remember { mutableStateOf(0) }
            SingleChoiceDialog(
                title = stringResource(CommonString.export_deck),
                choices =
                    listOf(
                        DialogChoice(stringResource(CommonString.dialog_yes)),
                        DialogChoice(stringResource(CommonString.dialog_no)),
                    ),
                selectedIndex = selected,
                onSelect = {
                    selection = it
                    selected = it
                },
                onDismissRequest = {},
            )
        }
    }

    @Test
    fun multiple_choice_waits_for_confirmation() {
        var selection: Set<Int>? = null
        capture(
            "multiple_choice",
            node = isDialog(),
            beforeCapture = {
                val strings = RuntimeEnvironment.getApplication()
                composeRule.onNodeWithText(strings.getString(CommonString.dialog_yes)).performClick().assertIsOff()
                assertEquals(null, selection)
            },
        ) {
            var selected by remember { mutableStateOf(setOf(0, 1)) }
            MultipleChoiceDialog(
                title = stringResource(CommonString.export_deck),
                choices =
                    listOf(
                        DialogChoice(stringResource(CommonString.dialog_yes)),
                        DialogChoice(stringResource(CommonString.dialog_no)),
                    ),
                selected = selected,
                onToggle = { selected = if (it in selected) selected - it else selected + it },
                confirm = DialogAction(stringResource(CommonString.dialog_ok), { selection = selected }),
                onDismissRequest = {},
            )
        }
        composeRule.onNodeWithText(RuntimeEnvironment.getApplication().getString(CommonString.dialog_ok)).performClick()
        assertEquals(setOf(1), selection)
    }

    @Test
    fun non_cancelable_determinate_progress() =
        capture("progress", node = isDialog()) {
            ProgressDialog(
                title = stringResource(CommonString.dialog_processing),
                message = "日本語 N3.apkg",
                progress = 0.62f,
                status = "62%",
            )
        }

    @Test
    fun error_details_are_revealed_on_request() {
        capture(
            "error",
            node = isDialog(),
            beforeCapture = { composeRule.onNodeWithText("Host unreachable").assertDoesNotExist() },
        ) {
            ErrorDialog(
                title = stringResource(CommonString.paper_dialog_sync_unreachable),
                message = stringResource(CommonString.connection_error_message),
                details = "Host unreachable",
                markIcon = R.drawable.symbol_cloud_off,
                onDismissRequest = {},
                primaryAction = DialogAction(stringResource(CommonString.retry), {}),
            )
        }
        val showDetails = RuntimeEnvironment.getApplication().getString(CommonString.paper_dialog_show_details)
        composeRule.onNodeWithText(showDetails).performClick()
        composeRule.onNodeWithText("Host unreachable").assertIsDisplayed()
    }
}
