// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class ChangeNoteTypePageTest : RobolectricTest() {
    @Test
    fun `save applies the mapping to the selected notes only`() =
        runTest {
            val selected = addBasicNote("discarded prompt", "answer becomes the prompt")
            val other = addBasicNote("untouched prompt", "untouched answer")
            val target = checkNotNull(col.notetypes.byName("Basic (and reversed card)"))
            val intent = ChangeNoteTypePage.getIntent(targetContext, listOf(selected.id), selected.noteTypeId)
            val activity = startRegularActivity<SingleFragmentActivity>(intent)
            val page = assertIs<ChangeNoteTypePage>(activity.fragment)
            col.modSchema(check = false)
            val request =
                col.notetypes
                    .changeNotetypeInfo(selected.noteTypeId, target.id)
                    .input
                    .toBuilder()
                    .addNoteIds(other.id)
                    .clearNewFields()
                    .addAllNewFields(listOf(1, -1))
                    .build()

            page.handlePostRequest(PostRequestUri("/_anki/changeNotetype"), request.toByteArray())

            val converted = col.getNote(selected.id)
            assertEquals(target.id, converted.noteTypeId)
            assertEquals(listOf("answer becomes the prompt", ""), converted.fields)
            assertEquals(selected.noteTypeId, col.getNote(other.id).noteTypeId)
            assertEquals(listOf("untouched prompt", "untouched answer"), col.getNote(other.id).fields)
        }
}
