// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.mediacheck

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.SingleFragmentScreenshotTest
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Screenshot tests for [MediaCheckFragment]
 *
 * `./gradlew :AnkiDroid:verifyRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.mediacheck.MediaCheckScreenshotTest"`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class MediaCheckScreenshotTest : SingleFragmentScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun unusedFile() {
        File(col.media.dir, "unused.png").writeText("fixture")
        withScreen {
            compose.onNodeWithText("unused.png", substring = true).assertIsDisplayed()
            captureScreen("unused_file")
        }
    }

    // Backend media checks require an on-disk collection and media database.
    override fun getCollectionStorageMode() = CollectionStorageMode.ON_DISK

    override fun buildIntent(): Intent = MediaCheckFragment.getIntent(targetContext)
}
