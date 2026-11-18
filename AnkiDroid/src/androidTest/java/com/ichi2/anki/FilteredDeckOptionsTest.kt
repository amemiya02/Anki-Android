// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.disableIntroductionSlide
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.testutil.useResumedActivity
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FilteredDeckOptionsTest : InstrumentedTest() {
    @get:Rule
    val runtimePermissionRule = grantPermissions(GrantStoragePermission.storagePermission)

    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun before() {
        disableIntroductionSlide()
    }

    @Test
    fun canOpenDeckOptions() {
        ActivityScenario.launch(DeckPicker::class.java).use { scenario ->
            scenario.onActivity { deckPicker ->
                val deckId = col.decks.newFiltered("Filtered Testing")
                deckPicker.viewModel.openDeckOptions(deckId)
            }
            useResumedActivity<ConfigAwareSingleFragmentActivity> {
                val name = hasSetTextAction() and hasText("Filtered Testing")
                compose.waitUntil(10_000) { compose.onAllNodes(name).fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(name).assertIsDisplayed()
            }
        }
    }
}
