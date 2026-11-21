// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.content.Intent
import android.os.Looper
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.SingleFragmentScreenshotTest
import com.ichi2.anki.libanki.Consts
import org.junit.Before
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Screenshot tests for [DeckOptions]
 *
 * `./gradlew :AnkiDroid:verifyRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.pages.DeckOptionsScreenshotTest"`
 */
class DeckOptionsScreenshotTest : SingleFragmentScreenshotTest() {
    @Before
    fun pauseLoadingAnimationClock() = pauseDeckOptionsAnimationClock()

    override fun buildIntent(): Intent = DeckOptions.getIntent(targetContext, Consts.DEFAULT_DECK_ID)

    override fun withScreen(block: (SingleFragmentActivity) -> Unit) {
        super.withScreen { activity ->
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            block(activity)
        }
    }
}
