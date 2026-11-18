// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenameCardTypeDialogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val input get() = compose.onNode(hasSetTextAction())
    private val rename get() = compose.onNodeWithText(getResourceString(CommonString.rename))

    @Test
    fun `whitespace-only name is rejected`() {
        openRenameDialog()
        input.performTextReplacement("   ")
        rename.assertIsNotEnabled()
    }

    @Test
    fun `quotes-only name is rejected`() {
        openRenameDialog()
        input.performTextReplacement("\"\"")
        rename.assertIsNotEnabled()

        input.performTextReplacement(" \" \" ")
        rename.assertIsNotEnabled()
    }

    @Test
    fun `valid name is accepted`() {
        var renamedTo: CardTypeName? = null
        openRenameDialog { renamedTo = it }
        input.performTextReplacement("  Reverse  ")
        rename.assertIsEnabled().performClick()
        assertThat(renamedTo?.value, equalTo("Reverse"))
    }

    private fun openRenameDialog(onRename: (CardTypeName) -> Unit = {}) {
        val currentName = CardTypeName.fromString("Card 1")
        RenameCardTypeDialog.showInstance(
            startRegularActivity<AnkiActivity>(),
            prefill = currentName.value,
            currentName = currentName,
            existingNames = listOf(currentName),
            block = onRename,
        )
    }
}
