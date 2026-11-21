// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.awaitDisplayed
import com.ichi2.anki.testutil.disableIntroductionSlide
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.testutil.withBackupPromptDisabled
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

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
    fun canOpenDeckOptions() =
        testContext.withBackupPromptDisabled {
            val deckId = col.decks.newFiltered("Filtered Testing")
            ActivityScenario.launch(DeckPicker::class.java).use { scenario ->
                scenario.onActivity { deckPicker ->
                    deckPicker.viewModel.openDeckOptions(deckId)
                }
                val name = hasSetTextAction() and hasText("Filtered Testing")
                compose.awaitDisplayed { onNode(name) }
                scenario.onActivity { assertIs<FilteredDeckOptionsFragment>(it.currentScreenFragment) }
            }
        }
}
