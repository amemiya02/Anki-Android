// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Shridhar Goel <shridhar.goel@gmail.com>

package com.ichi2.anki

import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.ichi2.anki.TestUtils.isTablet
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission.storagePermission
import com.ichi2.anki.testutil.disableIntroductionSlide
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.testutil.notificationPermission
import com.ichi2.anki.testutil.tapOnCountLayouts
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DeckPickerTest : InstrumentedTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val runtimePermissionRule = grantPermissions(storagePermission, notificationPermission)

    @Before
    fun before() {
        addNoteUsingBasicNoteType()
        disableIntroductionSlide()
    }

    @Test
    fun checkIfClickOnCountsLayoutOpensStudyOptionsOnMobile() {
        assumeTrue(isEmulator())

        withDeckPicker { scenario ->
            assumeTrue(!isTablet)

            tapOnCountLayouts("Default")
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithTag("study_deck_title").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("study_deck_title").assertIsDisplayed()
            scenario.onActivity { deckPicker ->
                check(deckPicker.currentScreenFragment is StudyOptionsFlowFragment)
                deckPicker.onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    @Test
    fun bottomBarIsShownWhereverTheLayoutHasNoDrawer() =
        withDeckPicker { scenario ->
            scenario.onActivity { deckPicker ->
                val hasBottomNavLayout = deckPicker.resources.getBoolean(R.bool.bottom_navigation_available)
                assertEquals(!deckPicker.fragmented, hasBottomNavLayout)
                assertEquals(hasBottomNavLayout, deckPicker.bottomNavigationEnabled)
                assertEquals(hasBottomNavLayout, deckPicker.findViewById<View>(R.id.bottom_navigation) != null)
            }
        }

    @Test
    fun checkIfStudyOptionsIsDisplayedOnTablet() {
        assumeTrue(isEmulator())

        withDeckPicker {
            assumeTrue(isTablet)

            onView(withId(R.id.studyoptions_fragment))
                .check(ViewAssertions.matches(isDisplayed()))
        }
    }

    private fun withDeckPicker(test: (ActivityScenario<DeckPicker>) -> Unit) {
        val preferences = testContext.sharedPrefs()
        val backupPromptKey = "backupPromptDisabled"
        val oldBackupPrompt = preferences.getBoolean(backupPromptKey, false).takeIf { preferences.contains(backupPromptKey) }
        preferences.edit { putBoolean(backupPromptKey, true) }
        try {
            ActivityScenario.launch(DeckPicker::class.java).use(test)
        } finally {
            preferences.edit {
                if (oldBackupPrompt == null) remove(backupPromptKey) else putBoolean(backupPromptKey, oldBackupPrompt)
            }
        }
    }
}
