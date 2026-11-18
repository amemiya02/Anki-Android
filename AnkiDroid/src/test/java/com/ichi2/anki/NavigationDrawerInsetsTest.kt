// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.os.Build
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.GravityCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import com.ichi2.anki.common.android.R as CommonR

/**
 * Edge-to-edge inset handling for the navigation drawer of [NavigationDrawerActivity],
 * hosted by [DeckPicker]
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(AndroidJUnit4::class)
class NavigationDrawerInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `Hayaki's mark in the drawer header sits below the status bar`() {
        // Only large screens have the side drawer
        RuntimeEnvironment.setQualifiers("w1280dp-h800dp-xlarge")
        withDeckPicker(deckCount = 1) { deckPicker ->
            deckPicker.drawerLayout.openDrawer(GravityCompat.START, false)
            advanceRobolectricLooper()
            // dispatchInsets always adds a 24dp status bar
            deckPicker.dispatchInsets()
            advanceRobolectricLooper()

            val markTop =
                compose
                    .onNodeWithTag("drawer-mark")
                    .fetchSemanticsNode()
                    .boundsInWindow.top
                    .toInt()
            assertThat(
                "the mark clears the status bar and keeps the header's own space above it",
                markTop,
                greaterThanOrEqualTo(
                    24.dp.toPx(targetContext) + deckPicker.resources.getDimensionPixelSize(CommonR.dimen.foundation_space_24),
                ),
            )
        }
    }
}
