// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteTypesScreenTest : RobolectricTest() {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `opening a note type shows its fields and card templates on the same page`() {
        ensureCollectionLoadIsSynchronous()
        val activity = startRegularActivity<ManageNotetypes>()
        val name = activity.viewModel.state.value.noteTypes.first().name

        compose.onNodeWithText(name).performClick()

        compose.onNodeWithText("Fields").assertIsDisplayed()
        compose.onNodeWithText("Card templates").assertIsDisplayed()
    }
}
