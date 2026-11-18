// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.libanki.Consts
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

internal fun RobolectricTest.startDeckOptionsController(): ActivityController<SingleFragmentActivity> {
    // The WebView shadow cannot signal readiness. Advance loading animation frames only when the test clock advances.
    ShadowChoreographer.setPaused(true)
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
    return startActivityControllerNormallyOpenCollectionWithIntent(
        SingleFragmentActivity::class.java,
        DeckOptions.getIntent(targetContext, Consts.DEFAULT_DECK_ID),
    )
}
