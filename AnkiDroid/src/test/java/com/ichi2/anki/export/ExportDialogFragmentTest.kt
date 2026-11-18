// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.export

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.parcelSize
import com.ichi2.testutils.parcelledCopy
import com.ichi2.testutils.saveState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class ExportDialogFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `collection export keeps its options and replacement warning`() =
        onExportDialog {
            option(TR.exportingIncludeMedia()).assertIsOn()
            option(TR.exportingSupportOlderAnkiVersions()).assertIsOff()
            option(str(CommonString.export_collection_replaces)).assertIsDisplayed()
            option(str(CommonString.export_deck_merges)).assertDoesNotExist()
        }

    @Test
    fun `deck export keeps its options and merge warning`() =
        onDeckExportDialog {
            option(TR.exportingIncludeMedia()).assertIsOn()
            option(TR.exportingIncludeDeckConfigs()).assertIsOff()
            option(TR.exportingIncludeSchedulingInformation()).assertIsOn()
            option(TR.exportingSupportOlderAnkiVersions()).assertIsOff()
            option(str(CommonString.export_deck_merges)).performScrollTo().assertIsDisplayed()
            option(str(CommonString.export_collection_replaces)).assertDoesNotExist()
        }

    @Test
    fun `legacy export is available for both package formats and absent for text formats`() =
        onExportDialog {
            val legacy = option(TR.exportingSupportOlderAnkiVersions())
            legacy.assertIsOff()
            chooseFormat(
                "Anki collection package",
                "Anki deck package",
            )
            legacy.assertIsOff()
            chooseFormat(
                "Anki deck package",
                "Notes in plain text",
            )
            legacy.assertDoesNotExist()
            chooseFormat(
                "Notes in plain text",
                "Cards in plain text",
            )
            legacy.assertDoesNotExist()
        }

    @Test
    fun `deck selected in the deck selection dialog is kept on recreation`() {
        addDeck("Chosen")
        onDeckExportDialog { scenario ->
            openDeckSelection()
            option("Chosen").performClick()
            advanceRobolectricLooper()
            option("Chosen").assertIsDisplayed().assertHasClickAction()

            scenario.recreate()
            advanceRobolectricLooper()
            option("Chosen").assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun `filtered decks and all decks are available as export sources`() {
        addDynamicDeck("Filtered export source")
        onDeckExportDialog {
            openDeckSelection()
            option(with(targetContext) { TR.sentenceCase.allDecks }).assertIsDisplayed()
            option("Filtered export source").performClick()
            advanceRobolectricLooper()
            option("Filtered export source").assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun `deck created through export is visible after cancelling export`() =
        onExportDialogOverDeckPicker { deckPicker, _ ->
            openDeckSelection()
            option(with(targetContext) { TR.sentenceCase.createDeck }).performClick()
            val name = "Created from export"
            compose.onNodeWithContentDescription(str(CommonString.paper_dialog_deck_name)).performTextInput(name)
            option(str(CommonString.dialog_positive_create)).performClick()
            advanceRobolectricLooperUntil { col.decks.idForName(name) != null }
            val createdId = assertNotNull(col.decks.idForName(name))

            option(str(CommonString.dialog_cancel)).performClick()
            advanceRobolectricLooperUntil(
                lazyMessage = { "Deck created through export was saved but never appeared in the deck list" },
            ) {
                deckPicker.displayedDecks.any { it.did == createdId }
            }
        }

    @Test
    fun `cancelling export while the deck selector loads does not show an error`() =
        onExportDialogOverDeckPicker { _, export ->
            holdingCollectionAccess { scheduler ->
                deckSelector().performClick()
                assertNull(export.childFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG))
                option(str(CommonString.dialog_cancel)).performClick()
                advanceRobolectricLooper()
                assertFalse(export.isAdded)

                scheduler.advanceUntilIdle()
                advanceRobolectricLooper()
            }
        }

    @Test
    fun `double tap while the decks load opens one deck dialog`() =
        onExportDialogOverDeckPicker { _, export ->
            holdingCollectionAccess { scheduler ->
                deckSelector().performClick().performClick()
                scheduler.advanceUntilIdle()
                advanceRobolectricLooper()
            }
            assertEquals(1, export.childFragmentManager.fragments.count { it is DeckSelectionDialog })
        }

    @Test
    fun `double tap with no collection backlog opens one deck dialog`() =
        onExportDialogOverDeckPicker { _, export ->
            val action = deckSelector().fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action
            compose.runOnIdle {
                val click = checkNotNull(action)
                click()
                click()
            }
            advanceRobolectricLooper()
            assertEquals(1, export.childFragmentManager.fragments.count { it is DeckSelectionDialog })
        }

    @Test
    fun `opening the deck picker keeps saved state below the Binder limit`() {
        ensureCollectionLoadIsSynchronous()
        val parent = "Medical Curriculum::Clinical Medicine and Surgery::Examination Preparation::Detailed Subject Review"
        repeat(3500) { addDeck("$parent::Topic ${it.toString().padStart(4, '0')}") }

        Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
            controller.get().setTheme(CommonR.style.Theme_Light)
            val activity = controller.setup().get()
            val export = ExportDialogFragment.forDeckList(DEFAULT_DECK_ID)
            export.show(activity.supportFragmentManager, "export")
            advanceRobolectricLooper()
            openDeckSelection()
            assertNotNull(export.childFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG))

            // Fragment arguments are included in the activity state sent over Binder.
            // The process shares a 1 MiB transaction buffer, so this is an upper bound.
            val bytes = controller.saveState().parcelSize()
            assertTrue(bytes < 1024 * 1024, "Activity saved state is $bytes bytes")
        }
    }

    @Test
    fun `restoring export reloads the selected deck name`() {
        ensureCollectionLoadIsSynchronous()
        val deckId = addDeck("Original name")
        val savedState =
            Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
                controller.get().setTheme(CommonR.style.Theme_Light)
                val activity = controller.setup().get()
                ExportDialogFragment.forDeckList(deckId).show(activity.supportFragmentManager, "export")
                advanceRobolectricLooper()
                controller.saveState().parcelledCopy(FragmentActivity::class.java.classLoader)
            }

        // Simulate a collection change while the activity is stopped.
        col.decks.rename(deckId, "Renamed deck")

        Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
            controller.get().setTheme(CommonR.style.Theme_Light)
            controller.setup(savedState)
            advanceRobolectricLooper()
            option("Renamed deck").assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun `ids file is not removed on a configuration change`() {
        val fragment =
            ExportDialogFragment.newInstance(
                idsFileDirectory = targetContext.cacheDir,
                type = ExportDialogFragment.ExportType.Notes,
                ids = listOf(1L, 2L, 3L),
            )
        val idsFile = fragment.requireArguments().requireParcelable<IdsFile>(ARG_IDS_FILE)
        assertTrue(idsFile.exists())

        launchFragment<ExportDialogFragment>(
            fragmentArgs = fragment.arguments,
            themeResId = CommonR.style.Theme_Light,
        ).use { scenario ->
            scenario.recreate()
            assertTrue(idsFile.exists(), "ids file must survive a configuration change")
        }
    }

    /** Launches [ExportDialogFragment] and executes [action] on the fragment. */
    private fun onExportDialog(action: () -> Unit) =
        launchFragment<ExportDialogFragment>(
            themeResId = CommonR.style.Theme_Light,
        ).use { action() }

    private fun onDeckExportDialog(action: (FragmentScenario<ExportDialogFragment>) -> Unit) {
        ensureCollectionLoadIsSynchronous()
        launchFragment<ExportDialogFragment>(
            fragmentArgs = ExportDialogFragment.forDeckList(DEFAULT_DECK_ID).arguments,
            themeResId = CommonR.style.Theme_Light,
        ).use { scenario ->
            advanceRobolectricLooper()
            action(scenario)
        }
    }

    private fun onExportDialogOverDeckPicker(action: (DeckPicker, ExportDialogFragment) -> Unit) =
        withDeckPicker(1, withCards = true) { deckPicker ->
            val export = ExportDialogFragment.forDeckList(DEFAULT_DECK_ID)
            export.show(deckPicker.supportFragmentManager, "export")
            deckPicker.supportFragmentManager.executePendingTransactions()
            advanceRobolectricLooper()
            action(deckPicker, export)
        }

    private fun option(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    private fun str(resource: Int) = targetContext.getString(resource)

    private fun deckSelector() =
        compose.onNode(
            hasText(TR.exportingInclude().replace(Regex("<[^>]+>"), "")) and hasClickAction() and hasAnyAncestor(isDialog()),
        )

    private fun openDeckSelection() {
        deckSelector().performClick()
        advanceRobolectricLooper()
    }

    private fun chooseFormat(
        current: String,
        desired: String,
    ) {
        compose
            .onNode(
                hasText(current, substring = true, ignoreCase = true) and hasClickAction() and hasAnyAncestor(isDialog()),
            ).performClick()
        compose
            .onNode(
                hasText(desired, substring = true, ignoreCase = true) and hasAnyAncestor(isDialog()),
            ).performScrollTo()
            .performClick()
    }

    private fun holdingCollectionAccess(block: (TestCoroutineScheduler) -> Unit) {
        val queue = StandardTestDispatcher()
        val previousQueue = CollectionManager.setTestDispatcher(queue, useReentrantLock = false)
        try {
            block(queue.scheduler)
        } finally {
            CollectionManager.setTestDispatcher(previousQueue)
        }
    }

    companion object {
        private const val ARG_IDS_FILE = "arg_ids_file"
    }
}
