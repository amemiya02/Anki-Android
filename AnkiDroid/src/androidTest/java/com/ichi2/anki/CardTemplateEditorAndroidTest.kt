// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ichi2.anki.notetype.ManageNoteTypesDestination
import com.ichi2.anki.notetype.ManageNotetypes
import com.ichi2.anki.tests.InstrumentedTest
import org.junit.Rule
import org.junit.Test

class CardTemplateEditorAndroidTest : InstrumentedTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun fieldsAppearInNoteTypeDetailsAndAreEditableInTheTemplate() {
        ActivityScenario.launch<ManageNotetypes>(ManageNoteTypesDestination().toIntent(testContext)).use {
            awaitText("Basic")
            val noteType = checkNotNull(col.notetypes.byName("Basic"))
            val template = noteType.templates[0]
            compose.onNodeWithText(noteType.name).performClick()
            awaitText("Front › Back")
            compose.onNodeWithText("Front › Back").assertIsDisplayed()
            compose.onNodeWithText(template.name).assertIsDisplayed().performClick()
            awaitText(templateSectionLabel(testContext, EditorViewType.FRONT))

            val code = onView(withId(R.id.edit_text))
            code.check(matches(withText(template.qfmt)))
            val editedSource = "<div>${template.qfmt}</div>"
            code.perform(replaceText(editedSource))
            code.check(matches(withText(editedSource)))

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            check(device.pressKeyCode(KeyEvent.KEYCODE_2, KeyEvent.META_CTRL_ON))
            compose.onNodeWithText(templateSectionLabel(testContext, EditorViewType.BACK)).assertIsSelected()
            code.check(matches(withText(template.afmt)))
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}
