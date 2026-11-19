// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class DeckPickerViewConstructorTest : RobolectricTest() {
    @Test
    fun `tools can construct the primary add button with a context`() {
        val button = DeckPickerAddButton::class.java.getConstructor(Context::class.java).newInstance(targetContext)
        assertEquals(CommonR.drawable.symbol_add, button.icon)
        assertFalse(button.isExtended)
    }

    @Test
    fun `tools can construct the tablet drawer before a navigation listener is attached`() {
        val drawer = TabletDrawer::class.java.getConstructor(Context::class.java).newInstance(targetContext)
        assertEquals(R.id.navdrawer_items_container, drawer.id)
    }
}
