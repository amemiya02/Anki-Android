// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.ChangeNoteTypeDialog.Companion.ARG_NOTE_IDS
import com.ichi2.anki.libanki.NoteId
import org.hamcrest.Matchers.equalTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class ChangeNoteTypeDialogTest : RobolectricTest() {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun `no changes failure has a translated message`() {
        assertThat(ChangeNoteTypeException.Kind.NO_CHANGES.toString(targetContext), equalTo("No changes to save"))
    }

    /**
     * #20944: with an accessibility service enabled, a Spinner reports its initial selection.
     * A template selection is meaningless for cloze, and used to be fatal
     */
    @Test
    fun `templates tab for a cloze note does not crash with an accessibility service`() {
        enableAccessibilityService()

        withChangeNoteTypeDialog(addClozeNote("{{c1::Hi}}").id) {
            selectTemplatesTab()
        }
        // a crash is reported by FailOnUnhandledExceptionRule
    }

    /** #20944: spinners which can't be used still deliver selections, which used to be fatal */
    @Test
    fun `no template spinners are built for a cloze note type`() =
        withChangeNoteTypeDialog(addClozeNote("{{c1::Hi}}").id) {
            selectTemplatesTab()
            mappings().assertCountEquals(0)
        }

    @Test
    fun `template spinners are built for a regular note type`() =
        withChangeNoteTypeDialog(addBasicNote().id) {
            selectTemplatesTab()
            mappings().assertCountEquals(1)
        }

    @Test
    fun `template spinners are rebuilt after visiting cloze`() =
        withChangeNoteTypeDialog(addBasicNote().id) {
            selectTemplatesTab()

            selectNoteType("Cloze")
            selectNoteType("Basic (and reversed card)")

            mappings().assertCountEquals(2)
        }

    private fun mappings() =
        compose.onAllNodes(
            SemanticsMatcher("mapping row") {
                it.config.getOrNull(SemanticsProperties.TestTag)?.matches(Regex("mapping_[0-9]+")) ==
                    true
            },
        )

    private fun selectTemplatesTab() {
        compose.onNodeWithText(TR.changeNotetypeTemplates()).performClick()
        compose.onNodeWithTag("mapping_page_1").assertIsDisplayed()
    }

    private fun selectNoteType(name: String) {
        compose.onNodeWithTag("target_type").performClick()
        compose.onNodeWithText(name).performClick()
    }

    /** Makes [AccessibilityManager.isEnabled] return true, as it would with TalkBack running */
    private fun enableAccessibilityService() {
        val manager = targetContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabled(true)
    }

    private fun withChangeNoteTypeDialog(
        noteId: NoteId,
        action: ChangeNoteTypeDialog.() -> Unit,
    ) {
        launchFragment<ChangeNoteTypeDialog>(
            fragmentArgs = Bundle().apply { putLongArray(ARG_NOTE_IDS, longArrayOf(noteId)) },
            themeResId = CommonR.style.Theme_Light,
        ).use { scenario -> scenario.onFragment { action(it) } }
    }
}
