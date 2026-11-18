// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.preferences.reviewer.MenuDisplayType
import com.ichi2.anki.preferences.reviewer.ReviewerMenuRepository
import com.ichi2.anki.preferences.reviewer.ReviewerMenuSettingsFragment
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.previewer.PreviewerAction
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.MappableBinding
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.settings.Prefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StudyControlsRenderedTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun previewerTabShowsTheSavedKeyAndOpensItsBindingEditor() {
        val key = MappableBinding(Binding.KeyCode(KeyEvent.KEYCODE_F11))
        Prefs.sharedPrefs.edit { putString(PreviewerAction.NEXT.preferenceKey, listOf(key).toPreferenceString()) }
        val intent = PreferencesActivity.getIntent(targetContext, ControlsSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val tab = compose.onNodeWithText(targetContext.getString(R.string.settings_previewer))
            tab.performClick().assertIsSelected()
            val next = compose.onNodeWithText(targetContext.getString(CommonString.previewer_next))
            next.performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(key.toDisplayString(targetContext)).assertIsDisplayed()
            next.performClick()
            compose.onNodeWithText(targetContext.getString(CommonString.binding_add_key)).assertIsDisplayed()
        }
    }

    @Test
    fun toolbarActionCanBeMovedWithoutDragging() {
        val intent = PreferencesActivity.getIntent(targetContext, ReviewerMenuSettingsFragment::class)
        val action = ViewerAction.UNDO
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose
                .onNodeWithContentDescription(
                    targetContext.getString(R.string.settings_toolbar_move_action, action.title(targetContext)),
                ).performScrollTo()
                .performClick()
            compose.onNode(hasText(targetContext.getString(CommonString.disabled)) and hasClickAction()).performClick()
        }
        val updated = ReviewerMenuRepository(Prefs.sharedPrefs).getActionsByMenuDisplayTypes()
        assertTrue(action in updated.getValue(MenuDisplayType.DISABLED))
    }

    @Test
    fun toolbarActionDragMovesItToTheMenu() {
        val intent = PreferencesActivity.getIntent(targetContext, ReviewerMenuSettingsFragment::class)
        val action = ViewerAction.UNDO
        val handle = targetContext.getString(R.string.settings_toolbar_move_action, action.title(targetContext))
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val source = compose.onNodeWithContentDescription(handle).assertIsDisplayed()
            val destination = compose.onNodeWithText(targetContext.getString(CommonString.custom_buttons_setting_menu_only))
            val y =
                destination
                    .fetchSemanticsNode()
                    .boundsInRoot.center.y - source.fetchSemanticsNode().boundsInRoot.top
            source.performTouchInput {
                down(center)
                moveTo(center.copy(y = y), delayMillis = 350)
                up()
            }
        }
        val updated = ReviewerMenuRepository(Prefs.sharedPrefs).getActionsByMenuDisplayTypes()
        assertTrue(action in updated.getValue(MenuDisplayType.MENU_ONLY))
    }
}
