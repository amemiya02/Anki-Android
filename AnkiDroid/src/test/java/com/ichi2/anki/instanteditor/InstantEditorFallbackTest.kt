// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.instanteditor

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.NoteEditorActivity
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.utils.ext.getParcelableExtraCompat
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
import com.ichi2.testutils.withCollectionAccessOn
import kotlinx.coroutines.android.asCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
            val editor =
                withCollectionAccessOn(Handler(Looper.getMainLooper()).asCoroutineDispatcher()) {
                    startRegularActivity<InstantNoteEditorActivity>(intent)
                }
            compose.onNodeWithText(getResourceString(CommonString.cloze_note_required)).assertIsDisplayed()
            compose.onNodeWithText(getResourceString(CommonString.open)).performClick()
            val next = assertNotNull(shadowOf(editor).nextStartedActivity)
            assertEquals(DeckPicker::class.java.name, next.component?.className)
            val destination = assertNotNull(next.getParcelableExtraCompat<Intent>("appNavigationEntryScreen"))
            assertEquals(NoteEditorActivity::class.java.name, destination.component?.className)
            assertEquals(text, destination.getStringExtra(Intent.EXTRA_TEXT))
        }
}
