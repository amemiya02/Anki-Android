// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instanteditor

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.NoteEditorPaneFragment
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class InstantEditorFallbackTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun sharedTextReachesTheFullEditorWhenClozeNoteTypesAreMissing() =
        runTest {
            col.notetypes
                .all()
                .filter { it.isCloze }
                .forEach { col.backend.removeNotetype(it.id) }
            val text = "A sentence from another app"
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            val editor = openSharedInstantNote(intent, compose)
            compose.onNodeWithText(getResourceString(CommonString.cloze_note_required)).assertIsDisplayed()
            compose.onNodeWithText(getResourceString(CommonString.open)).performClick()
            val destination = assertIs<NoteEditorPaneFragment>(editor.currentScreenFragment)
            assertEquals(text, destination.launchIntent.getStringExtra(Intent.EXTRA_TEXT))
            assertNull(shadowOf(editor).nextStartedActivity)
        }
}
