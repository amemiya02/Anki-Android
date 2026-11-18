// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.ui

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.EmptyApplicationCategory
import com.ichi2.anki.dialogs.KeySelectionDialogUtils.disallowModifierKeyCodes
import com.ichi2.anki.dialogs.KeySelectionDialogUtils.keyBinding
import com.ichi2.anki.reviewer.Binding
import com.ichi2.testutils.AndroidTest
import com.ichi2.testutils.EmptyApplication
import com.ichi2.testutils.KeyEventUtils
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(application = EmptyApplication::class)
@Category(EmptyApplicationCategory::class)
class KeyPickerTest : AndroidTest {
    @Test
    fun aValidKeyProducesABinding() {
        assertEquals(Binding.KeyCode(KeyEvent.KEYCODE_V), keyBinding(KeyEventUtils.getVKey()) { true })
    }

    @Test
    fun anInvalidKeyDoesNotReplaceTheCurrentBinding() {
        var selected = keyBinding(KeyEventUtils.getInvalid()) { true }
        assertNull(selected)
        selected = keyBinding(KeyEventUtils.getVKey()) { true }
        assertNotNull(selected)
        selected = keyBinding(KeyEventUtils.getInvalid()) { true } ?: selected
        assertEquals(Binding.KeyCode(KeyEvent.KEYCODE_V), selected)
    }

    @Test
    fun modifiersAreAcceptedOnlyWhenThePickerAllowsThem() {
        val shift = KeyEventUtils.leftShift()
        assertNull(keyBinding(shift, disallowModifierKeyCodes()))
        assertNotNull(keyBinding(shift) { true })
    }
}
