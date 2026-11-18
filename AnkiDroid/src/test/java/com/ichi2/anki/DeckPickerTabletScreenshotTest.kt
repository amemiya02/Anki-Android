// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import org.junit.Before
import org.junit.Test

/**
 * The deck list on a tablet keeps its two-pane layout and the drawer, with the Mockup's Decks.
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests "com.ichi2.anki.DeckPickerTabletScreenshotTest"`
 */
class DeckPickerTabletScreenshotTest : DeckListScreenshotTest() {
    @Before
    override fun setUp() {
        super.setUp()
        setTabletQualifiers()
    }

    @Test
    fun decks() {
        seedMockupDecks()
        showDeckList("decks_tablet")
    }
}
