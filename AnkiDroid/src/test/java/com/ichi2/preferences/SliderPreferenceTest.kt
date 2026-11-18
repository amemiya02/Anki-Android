// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.preferences.AccessibilitySettingsFragment
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SliderPreferenceTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun adjustingCardZoomPersistsAnInteger() {
        val intent = PreferencesActivity.getIntent(targetContext, AccessibilitySettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose
                .onNodeWithContentDescription(targetContext.getString(CommonString.card_zoom))
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(125f) }
            compose.waitForIdle()
            assertEquals(125, Prefs.sharedPrefs.getInt(targetContext.getString(R.string.card_zoom_preference), -1))
        }
    }
}
