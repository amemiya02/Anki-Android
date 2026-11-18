// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.preferences.SettingsGroup
import com.ichi2.anki.preferences.StudyingSettingsFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RelatedSettingsPreferenceTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun studyingOpensRelatedControls() = openFromStudying(SettingsGroup.CONTROLS, R.string.settings_previewer)

    @Test
    fun studyingOpensRelatedAccessibility() = openFromStudying(SettingsGroup.ACCESSIBILITY, CommonString.card_zoom)

    private fun openFromStudying(
        destination: SettingsGroup,
        visibleSetting: Int,
    ) {
        val intent = PreferencesActivity.getIntent(targetContext, StudyingSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(getResourceString(destination.title)).performScrollTo().performClick()
            compose.onNodeWithText(getResourceString(visibleSetting)).assertIsDisplayed()
        }
    }
}
