// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.android.view.locationInWindow
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.dispatchInsets
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Edge-to-edge inset handling for the [DeckPicker] FAB and deck list.
 *
 * See issue 21241: the FAB shifted when returning from Settings.
 */
@RunWith(AndroidJUnit4::class)
class DeckPickerInsetsTest : RobolectricTest() {
    @Test
    fun `destroying the deck picker removes its global layout listener`() {
        ensureCollectionLoadIsSynchronous()
        setIntroductionSlidesShown(true)
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        targetContext.sharedPrefs().edit { putBoolean("backupPromptDisabled", true) }
        addDeck("Test Deck")
        val controller = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
        val deckPicker = controller.get()
        advanceRobolectricLooperUntil(lazyMessage = { "the deck list should be shown" }) {
            deckPicker.viewModel.flowOfDeckListScreen.value != null
        }
        deckPicker.dispatchInsets(navBarBottom = 48.dp)
        deckPicker.layoutForTest()
        val list = deckPicker.deckPickerBinding.deckList
        val padding = deckPicker.deckListBottomPadding
        val observer = deckPicker.deckPickerBinding.root.viewTreeObserver
        check(list.height > 0) { "the deck list must be laid out, or the layout listener is a no-op" }
        val restingPadding = padding.intValue
        check(restingPadding > 0) { "the last Deck must scroll clear of the add button" }

        padding.intValue = 0
        observer.dispatchOnGlobalLayout()
        assertThat("the listener updates padding while the activity is alive", padding.intValue, equalTo(restingPadding))

        controller.pause().stop().destroy()
        padding.intValue = 0
        observer.dispatchOnGlobalLayout()
        assertThat("the window observer must no longer call the destroyed activity", padding.intValue, equalTo(0))
    }

    @Test
    fun `deck list padding follows a dialog keyboard opening and closing`() =
        withDeckPicker(deckCount = 2) { deckPicker ->
            deckPicker.dispatchInsets(navBarBottom = 48.dp)
            deckPicker.layoutForTest()
            val padding = deckPicker.deckListBottomPadding
            val restingPadding = padding.intValue

            repeat(2) {
                // A dialog opens the keyboard while the deck picker's FAB remains visible.
                deckPicker.dispatchInsets(navBarBottom = 48.dp, imeBottom = 300.dp)
                deckPicker.layoutForTest()
                assertThat(
                    "the deck list must gain clearance while the keyboard is open",
                    padding.intValue,
                    greaterThan(restingPadding),
                )

                deckPicker.dispatchInsets(navBarBottom = 48.dp)
                deckPicker.layoutForTest()
                assertThat("the keyboard must not leave extra space below the decks", padding.intValue, equalTo(restingPadding))
            }
        }

    @Test
    fun `FAB is above the navigation bar`() =
        withDeckPicker(deckCount = 2) { deckPicker ->
            val navBarBottom = 48.dp.toPx(targetContext)
            deckPicker.dispatchInsets(navBarBottom = 48.dp)
            deckPicker.layoutForTest()

            val fabColumnPadding = 12.dp.toPx(targetContext)
            val fabMargin = 16.dp.toPx(targetContext)
            assertThat(
                "the FAB clears the navigation bar and the bottom bar above it",
                deckPicker.fabDistanceToWindowBottom,
                equalTo(navBarBottom + bottomBarHeight + fabColumnPadding + fabMargin),
            )
        }

    // The phone's bottom bar above the navigation bar: 62dp, the Mockup's 78dp less its 16dp gesture area
    private val bottomBarHeight: Int
        get() = 62.dp.toPx(targetContext)

    /** The gap, in pixels, between the bottom of the FAB and the bottom of its container. */
    private val DeckPicker.fabDistanceToWindowBottom: Int
        get() {
            val fab = floatingActionButtonBinding.fabMain
            val container = floatingActionButtonBinding.root
            val fabBottom = fab.locationInWindow().y + fab.height
            val containerBottom = container.locationInWindow().y + container.height
            return containerBottom - fabBottom
        }

    /**
     * Forces measure/layout passes so view bounds can be asserted.
     *
     * Laid out at the window's current size so line wrapping is stable across passes.
     */
    private fun DeckPicker.layoutForTest() {
        advanceRobolectricLooper()
        performLayout()
    }

    /** [layoutForTest] without processing pending tasks: lays out the current state */
    private fun DeckPicker.performLayout() {
        val content = findViewById<View>(android.R.id.content)
        val width = if (content.width > 0) content.width else 1080
        val height = if (content.height > 0) content.height else 2400
        // Two passes: the deck list's bottom padding follows a layout pass
        repeat(2) {
            content.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
            content.layout(0, 0, width, height)
        }
    }
}
