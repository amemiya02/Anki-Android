// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.android.view.locationInWindow
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.dispatchInsets
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ImporterInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `package import toolbar clears the status bar and top cutout`() =
        withImporter(AnkiPackageImporterFragment.getIntent(targetContext, "/unused.apkg")) { host ->
            val importer = assertIs<AnkiPackageImporterFragment>(host.currentScreenFragment)
            host.dispatchInsets(cutoutTop = 36.dp)
            advanceRobolectricLooper()
            compose.waitForIdle()

            val root = importer.requireView() as ViewGroup
            val toolbarTop = root.getChildAt(0).locationInWindow().y
            val cutout = 36.dp.toPx(host)
            assertTrue(
                toolbarTop >= cutout,
                "Toolbar starts at $toolbarTop and the cutout ends at $cutout (root top padding ${root.paddingTop})",
            )
        }

    @Test
    fun `CSV import toolbar and page clear edge cutouts and the navigation bar`() =
        withImporter(CsvImporter.getIntent(targetContext, "/unused.csv")) { host ->
            val importer = assertIs<CsvImporter>(host.currentScreenFragment)
            host.dispatchInsets(navBarRight = 48.dp, cutoutLeft = 32.dp)
            advanceRobolectricLooper()
            compose.waitForIdle()

            val root = importer.requireView() as ViewGroup
            val toolbar = root.getChildAt(0)
            val webView = importer.webViewLayout
            val rootPosition = root.locationInWindow()
            val toolbarPosition = toolbar.locationInWindow()
            val webViewPosition = webView.locationInWindow()
            val cutout = rootPosition.x + 32.dp.toPx(host)
            assertTrue(
                toolbarPosition.x >= cutout,
                "Toolbar starts at ${toolbarPosition.x} and the cutout ends at $cutout (root left padding ${root.paddingLeft})",
            )
            assertTrue(toolbarPosition.y >= 24.dp.toPx(host), "Import toolbar overlaps the status bar")
            assertTrue(webViewPosition.x >= cutout, "Import page overlaps the side cutout")
            assertTrue(webViewPosition.x + webView.width <= rootPosition.x + root.width - 48.dp.toPx(host))

            host.dispatchInsets(navBarBottom = 24.dp)
            advanceRobolectricLooper()
            compose.waitForIdle()

            val contentBottom = webView.locationInWindow().y + webView.height
            assertTrue(contentBottom <= root.locationInWindow().y + root.height - 24.dp.toPx(host))
        }

    @Test
    fun `the standalone import host insets the toolbar once`() {
        val intent = AnkiPackageImporterFragment.getIntent(targetContext, "/unused.apkg")
        Robolectric.buildActivity(SingleFragmentActivity::class.java, intent).use { controller ->
            val host = controller.setup().visible().get()
            host.dispatchInsets(cutoutTop = 36.dp)
            advanceRobolectricLooper()
            compose.waitForIdle()

            val importer = assertIs<AnkiPackageImporterFragment>(host.fragment)
            val toolbar = (importer.requireView() as ViewGroup).getChildAt(0)
            assertEquals(36.dp.toPx(host), toolbar.locationInWindow().y)
        }
    }

    private fun withImporter(
        intent: Intent,
        block: (DeckPicker) -> Unit,
    ) = withDeckPicker(deckCount = 1) { host ->
        host.startActivity(intent)
        compose.onNodeWithText(TR.actionsImport()).assertIsDisplayed()
        block(host)
    }
}
