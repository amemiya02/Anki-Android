// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget.deckpicker

import com.ichi2.anki.ScreenshotTest
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.deckpicker.DeckPickerWidget
import com.ichi2.widget.deckpicker.DeckPickerWidgetPreferences
import com.ichi2.widget.renderWidget
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.widget.deckpicker.DeckPickerWidgetScreenshotTest"`
 *
 * Use the suite's default SDK until the backend supports multiple Robolectric classloaders.
 */
class DeckPickerWidgetScreenshotTest : ScreenshotTest() {
    @Test
    fun compact() =
        runTest {
            captureWidget("compact", heightDp = 102)
        }

    @Test
    fun fullHeight() =
        runTest {
            captureWidget("full_height", heightDp = 220)
        }

    @Test
    fun compactLargeFont() =
        runTest {
            captureWidget("compact_large_font", heightDp = 102, fontScale = 2f)
        }

    @Test
    fun fullHeightLargeFont() =
        runTest {
            captureWidget("full_height_large_font", heightDp = 220, fontScale = 2f)
        }

    @Test
    fun minimumHeight() =
        runTest {
            captureWidget("minimum_height", heightDp = 50)
        }

    @Test
    fun minimumHeightLargeFont() =
        runTest {
            captureWidget("minimum_height_large_font", heightDp = 50, fontScale = 2f)
        }

    private suspend fun TestScope.captureWidget(
        name: String,
        heightDp: Int,
        fontScale: Float = 1f,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val deckIds =
            listOf("✅ French", "✅ German", "✅ Spanish", "✅ Italian", "✅ Japanese")
                .mapIndexed { index, name -> addDeck(name).also { addNoteToDeck(it, count = index + 1) } }
                .toLongArray()
        val widgetId = AppWidgetId(1)
        DeckPickerWidgetPreferences(targetContext).saveSelectedDecks(widgetId, deckIds.map { it.toString() })
        captureView(name, targetContext.renderWidget(DeckPickerWidget(), width = 349, height = heightDp, id = widgetId))
    }
}
