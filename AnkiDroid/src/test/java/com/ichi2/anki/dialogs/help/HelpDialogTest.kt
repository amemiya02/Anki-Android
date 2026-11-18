// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.help

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.dialogs.help.HelpItem.Action.Rate
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class HelpDialogTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var mockActionDispatcher: HelpItemActionsDispatcher

    @Before
    fun setUp() {
        mockActionDispatcher = mockk(relaxed = true)
    }

    @Test
    fun `Support menu handles Rate item availability correctly`() {
        // if the system doesn't have an app to handle rate intents, don't show rate menu item
        val itemsWithoutRate = HelpDialog.newSupportInstance(false).requireArgsHelpEntries()
        assertFalse(
            itemsWithoutRate.any { it.action == Rate },
            "Found help menu item for Rate but system can't handle it",
        )
        // if the system has an app to handle rate intents, show rate menu item
        val itemsWithRate = HelpDialog.newSupportInstance(true).requireArgsHelpEntries()
        assertTrue(
            itemsWithRate.any { it.action == Rate },
            "Missing help menu item for Rate when system can handle it",
        )
    }

    @Test
    fun `Help contains the expected items at start`() {
        // checking the support menu
        val expectedSupportItems =
            listOfNotNull(
                CommonString.help_item_support_opencollective_donate.takeIf { BuildConfig.SHOW_DONATE_LINKS },
                CommonString.multimedia_editor_trans_translate,
                CommonString.help_item_support_develop_ankidroid,
                CommonString.help_item_support_rate_ankidroid,
                CommonString.help_item_support_other_ankidroid,
                CommonString.send_feedback,
            )
        val actualSupportItems =
            HelpDialog.newSupportInstance(true).requireArgsHelpEntries().map { it.titleResId }
        assertEquals(
            expectedSupportItems,
            actualSupportItems,
            "Unexpected support menu item at start",
        )
        // checking the help menu
        val expectedHelpItems =
            listOf(
                CommonString.help_title_using_ankidroid,
                CommonString.help_title_get_help,
                CommonString.help_title_community,
                CommonString.help_title_privacy,
            )
        val actualHelpItems =
            HelpDialog.newHelpInstance().requireArgsHelpEntries().map { it.titleResId }
        assertEquals(
            expectedHelpItems,
            actualHelpItems,
            "Unexpected help menu item at start",
        )
    }

    @Test
    fun `Get help offers no way to send a crash report`() {
        val getHelpParentId = mainHelpMenuItems.single { it.titleResId == CommonString.help_title_get_help }.id
        val getHelpItems = childHelpMenuItems.filter { it.parentId == getHelpParentId }.map { it.titleResId }

        assertEquals(
            listOf(CommonString.help_item_mailing_list, CommonString.help_item_report_bug),
            getHelpItems,
        )
    }

    @Test
    fun `Menu items IDs are consistent`() {
        // support menu items have unique ids
        assertEquals(
            supportMenuItems.size,
            supportMenuItems.map { it.id }.toSet().size,
            "Support menu has items with the same id",
        )
        // main help menu items have unique ids
        assertEquals(
            mainHelpMenuItems.size,
            mainHelpMenuItems.map { it.id }.toSet().size,
            "Main help menu has items with the same id",
        )
        // help menu child items have a non-null valid parent id
        val allFoundParentIds = childHelpMenuItems.map { it.parentId }
        assertFalse(
            allFoundParentIds
                .any { it == null || !mainHelpMenuItems.map { entry -> entry.id }.contains(it) },
            "Help item has an invalid parentId",
        )
    }

    @Test
    fun `Help menu handles submenus correctly`() {
        withHelpDialog {
            node(CommonString.help_title_community).performClick()
            // check that the expected six children are shown
            node(CommonString.help_item_discord).assertIsDisplayed()
            node(CommonString.help_item_reddit).assertIsDisplayed()
            node(CommonString.help_item_facebook).assertIsDisplayed()
            node(CommonString.help_item_mailing_list).assertIsDisplayed()
            node(CommonString.help_item_twitter).performScrollTo().assertIsDisplayed()
            node(CommonString.help_item_anki_forums).performScrollTo().assertIsDisplayed()
            // press back
            pressBackUnconditionally()
            // check that the expected initial four menu items are shown
            node(CommonString.help_title_community).assertIsDisplayed()
            node(CommonString.help_title_get_help).assertIsDisplayed()
            node(CommonString.help_title_privacy).assertIsDisplayed()
            node(CommonString.help_title_using_ankidroid).assertIsDisplayed()
        }
    }

    @Test
    fun `Help menu survives recreating its view`() {
        withHelpDialog { scenario ->
            node(CommonString.help_title_community).assertIsDisplayed()

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            node(CommonString.help_title_community).assertIsDisplayed()
        }
    }

    @Test
    fun `Help submenu and back navigation survive recreating the view`() {
        withHelpDialog { scenario ->
            node(CommonString.help_title_community).performClick()
            node(CommonString.help_item_discord).assertIsDisplayed()

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            node(CommonString.help_item_discord).assertIsDisplayed()
            pressBackUnconditionally()
            node(CommonString.help_title_community).assertIsDisplayed()
        }
    }

    @Test
    fun `Help menu item executes expected action on menu item selection`() {
        withHelpDialog { scenario ->
            scenario.onFragment { it.actionsDispatcher = mockActionDispatcher }
            // start the first submenu
            node(CommonString.help_title_using_ankidroid).performClick()
            // the manual url is being shown
            node(CommonString.help_item_ankidroid_manual).performClick()
            verify(exactly = 1) { mockActionDispatcher.onOpenUrl(AnkiDroidApp.manualUrl) }
            // an url resource is being shown
            node(CommonString.help_item_anki_manual).performClick()
            verify(exactly = 1) { mockActionDispatcher.onOpenUrlResource(R.string.link_anki_manual) }
            pressBackUnconditionally()
            // start the second submenu
            node(CommonString.help_title_get_help).performClick()
            // the feedback url is being shown
            node(CommonString.help_item_report_bug).performClick()
            verify(exactly = 1) { mockActionDispatcher.onOpenUrl(AnkiDroidApp.feedbackUrl) }
        }
    }

    private fun node(id: Int) = composeRule.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(id))

    private fun withHelpDialog(block: (FragmentScenario<HelpDialog>) -> Unit) {
        launchFragment<HelpDialog>(
            fragmentArgs = HelpDialog.newHelpInstance().arguments,
            themeResId = CommonR.style.Theme_Light,
        ).use(block)
    }
}
