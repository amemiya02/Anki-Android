// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.preferences.reviewer.WhiteboardAction
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.CardSide
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.reviewer.ReviewerBinding
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.HamcrestUtils
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlsSettingsFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val testBinding = Binding.KeyCode(KeyEvent.KEYCODE_F11)

    @Test
    fun `XML keys match the Enum keys`() {
        for (screen in ControlPreferenceScreen.entries) {
            val xmlKeys = PreferenceTestUtils.getKeysFromXml(targetContext, screen.xmlRes, excludeCategories = true)
            val actions =
                if (screen == ControlPreferenceScreen.REVIEWER) {
                    val menuOnlyActions =
                        setOf(
                            ViewerAction.BURY_MENU,
                            ViewerAction.SUSPEND_MENU,
                            ViewerAction.FLAG_MENU,
                            ViewerAction.DECK_OPTIONS,
                            ViewerAction.RESET_PROGRESS,
                            ViewerAction.FLAG_RED,
                            ViewerAction.FLAG_ORANGE,
                            ViewerAction.FLAG_GREEN,
                            ViewerAction.FLAG_BLUE,
                            ViewerAction.FLAG_PINK,
                            ViewerAction.FLAG_TURQUOISE,
                            ViewerAction.FLAG_PURPLE,
                        )
                    ViewerAction.entries - menuOnlyActions + WhiteboardAction.entries
                } else {
                    screen.getActions()
                }
            val enumKeys = actions.map { it.preferenceKey }

            assertThat(xmlKeys, HamcrestUtils.containsInAnyOrder(enumKeys))
        }
    }

    @Test
    fun `skip button binding only to answer`() {
        withAnswerKeyPicker {
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_skip)).performClick()
            assertThat(bindings(ViewerAction.SHOW_ANSWER), not(hasItem(equalTo(testBinding to CardSide.QUESTION))))
            assertThat(bindings(ViewerAction.ANSWER_GOOD), hasItem(equalTo(testBinding to CardSide.ANSWER)))
        }
    }

    @Test
    fun `assign buttons binding to both answer and show answer`() {
        withAnswerKeyPicker {
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_assign)).performClick()
            assertThat(bindings(ViewerAction.SHOW_ANSWER), hasItem(equalTo(testBinding to CardSide.QUESTION)))
            assertThat(bindings(ViewerAction.ANSWER_GOOD), hasItem(equalTo(testBinding to CardSide.ANSWER)))
        }
    }

    @Test
    fun `no dialog if binding is already assigned to show answer`() {
        Prefs.sharedPrefs.edit {
            putString(
                ViewerAction.SHOW_ANSWER.preferenceKey,
                listOf(ReviewerBinding(testBinding, CardSide.QUESTION)).toPreferenceString(),
            )
        }
        withAnswerKeyPicker {
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_assign)).assertDoesNotExist()
            assertThat(bindings(ViewerAction.SHOW_ANSWER), hasItem(equalTo(testBinding to CardSide.QUESTION)))
            assertThat(bindings(ViewerAction.ANSWER_GOOD), hasItem(equalTo(testBinding to CardSide.ANSWER)))
        }
    }

    @Test
    fun aReviewerBindingCanBeLimitedToTheQuestionSide() {
        val intent = PreferencesActivity.getIntent(targetContext, ControlsSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onAllNodesWithText(targetContext.getString(CommonString.undo))[1].performScrollTo().performClick()
            inputKey()
            compose.onNodeWithText(targetContext.getString(CommonString.card_side_question)).performClick()
            assertThat(bindings(ViewerAction.UNDO), hasItem(equalTo(testBinding to CardSide.QUESTION)))
        }
    }

    private fun bindings(action: ViewerAction): List<Pair<Binding, CardSide>> =
        action.getBindings(Prefs.sharedPrefs).map { it.binding to it.side }

    private fun withAnswerKeyPicker(block: () -> Unit) {
        val intent = PreferencesActivity.getIntent(targetContext, ControlsSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            lateinit var answerTitle: String
            scenario.onActivity { activity ->
                val host = activity.fragment as PreferencesFragment
                val controls = host.childFragmentManager.findFragmentById(R.id.settings_container)!!
                answerTitle = controls.controlPreferenceTitle(ViewerAction.ANSWER_GOOD.preferenceKey)!!
            }
            compose.onNodeWithText(answerTitle).performScrollTo().performClick()
            inputKey()
            block()
        }
    }

    private fun inputKey() {
        compose.onNodeWithText(targetContext.getString(CommonString.binding_add_key)).performClick()
        compose.onNodeWithText(targetContext.getString(CommonString.key_picker_default_press_key)).performKeyInput { pressKey(Key.F11) }
        compose.onNodeWithText(targetContext.getString(CommonString.dialog_ok)).performClick()
        advanceRobolectricLooper()
    }
}
