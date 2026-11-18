// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.tags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TagPickerSelectionTest {
    @Test
    fun filteringPreservesPartiallySelectedSiblings() {
        val tags = TagsList(listOf("B::one", "B::two"), listOf("B::one", "B::two"), listOf("B::one", "B::two"))
        val picker = TagPickerState(tags)
        picker.filter("one")
        picker.toggle(picker[1])
        assertEquals(TagTree.State.INDETERMINATE, picker.state(picker[0]))
        picker.filter("")
        assertEquals(TagTree.State.INDETERMINATE, picker.state(picker[0]))
        assertEquals(TagTree.State.INDETERMINATE, picker.state(picker[2]))
    }

    @Test
    fun changingChildSelectionUpdatesTheParentAndPreservesUnrelatedRows() {
        val tags = TagsList(listOf("B::child", "Z"), listOf("B::child", "Z"))
        val picker = TagPickerState(tags)
        picker.toggle(picker[1])
        assertFalse(tags.isIndeterminate("B"))
        picker.filter("Z")
        assertEquals("Z", picker[0].tag)
        assertEquals(TagTree.State.CHECKED, picker.state(picker[0]))
        picker.filter("")
        val parent = (0 until picker.size).map { picker[it] }.single { it.tag == "B" }
        assertEquals(TagTree.State.UNCHECKED, picker.state(parent))
    }
}
