// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.os.Build
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.android.material.navigation.NavigationView
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import com.ichi2.anki.common.android.R as CommonR

/**
 * Edge-to-edge inset handling for the navigation drawer of [NavigationDrawerActivity],
 * hosted by [DeckPicker]
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(AndroidJUnit4::class)
class NavigationDrawerInsetsTest : RobolectricTest() {
    @Test
    fun `Hayaki's mark in the drawer header sits below the status bar`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            deckPicker.drawerLayout.openDrawer(GravityCompat.START, false)
            advanceRobolectricLooper()
            // dispatchInsets always adds a 24dp status bar
            deckPicker.dispatchInsets()
            advanceRobolectricLooper()

            val header = deckPicker.findViewById<NavigationView>(R.id.navdrawer_items_container).getHeaderView(0)
            val mark = (header as ViewGroup).getChildAt(0) as ImageView
            val markTop = IntArray(2).also { mark.getLocationInWindow(it) }[1]
            assertThat(
                "the mark clears the status bar and keeps the header's own space above it",
                markTop,
                greaterThanOrEqualTo(24.dp.toPx(targetContext) + header.resources.getDimensionPixelSize(CommonR.dimen.foundation_space_24)),
            )
        }
}
