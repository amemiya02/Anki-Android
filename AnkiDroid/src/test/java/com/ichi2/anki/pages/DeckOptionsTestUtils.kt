// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.libanki.Consts
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

internal fun pauseDeckOptionsAnimationClock() {
    // The WebView shadow never signals readiness, so the loading indicator keeps requesting animation frames.
    ShadowChoreographer.setPaused(true)
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
}

internal fun RobolectricTest.startDeckOptionsController(): ActivityController<SingleFragmentActivity> {
    pauseDeckOptionsAnimationClock()
    return startActivityControllerNormallyOpenCollectionWithIntent(
        SingleFragmentActivity::class.java,
        DeckOptions.getIntent(targetContext, Consts.DEFAULT_DECK_ID),
    )
}
