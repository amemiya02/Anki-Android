// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Test

/**
 * The deck list in English, Simplified Chinese, Japanese and Arabic: proof that interface text is
 * drawn in the bundled faces, with Japanese or Chinese forms of shared Han characters by language.
 *
 * ```sh
 * ./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot --tests 'com.ichi2.anki.InterfaceTypefaceScreenshotTest'
 * ```
 */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
class InterfaceTypefaceScreenshotTest : ScreenshotTest() {
    @Test
    fun deck_list() {
        // The same Decks in every language: 骨, 直 and 角 differ between Japanese and Chinese
        for (name in listOf("Kanji 骨 直 角", "漢字の読み方", "汉语词汇 HSK 4", "Vocabulary", "مفردات عربية")) {
            addDeck(name)
        }
        withDeckPicker(deckCount = 0) {
            captureScreen("deck_list")
        }
    }
}
