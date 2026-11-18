// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.widget

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.descendants
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.NoteEditorActivity
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.CollectionHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AddNoteWidgetTest : RobolectricTest() {
    @Test
    fun `add note button opens the note editor before storage is decided`() =
        runTest {
            targetContext.sharedPrefs().edit { remove(CollectionHelper.PREF_COLLECTION_PATH) }
            val widget = targetContext.renderWidget(AddNoteWidget(), width = 76, height = 76)
            val label =
                (widget as ViewGroup).descendants.filterIsInstance<TextView>().firstOrNull {
                    it.text.toString() == targetContext.getString(CommonString.menu_add_note)
                }
            assertNotNull(label, "the widget displays its action as text")
            val button = generateSequence<View>(label) { it.parent as? View }.first { it.hasOnClickListeners() }
            assertTrue(button.performClick())

            val opened = assertNotNull(shadowOf(targetContext as Application).nextStartedActivity, "button opened nothing")
            assertEquals(NoteEditorActivity::class.java.name, opened.component?.className)
        }
}
