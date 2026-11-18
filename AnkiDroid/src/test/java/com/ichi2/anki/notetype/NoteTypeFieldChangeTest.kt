// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import com.ichi2.anki.libanki.NotetypeJson
import com.ichi2.anki.servicelayer.LanguageHintService.languageHint
import com.ichi2.testutils.JvmTest
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NoteTypeFieldChangeTest : JvmTest() {
    @Test
    fun `a pending action still uses the selected field after another reorder`() =
        runTest {
            val type = basic()
            val originalBack = NoteFieldRef(type.getField(1))
            type.deepClone().let { col.notetypes.moveFieldLegacy(it, it.getField(1), 0) }
            markSynced()

            assertEquals(FieldChangeResult.NeedsFullSync, col.changeNoteField(type.id, FieldChange(FieldAction.Delete, originalBack)))
            assertFalse(col.schemaChanged())
            assertEquals(
                FieldChangeResult.Changed,
                col.changeNoteField(type.id, FieldChange(FieldAction.Delete, originalBack), confirmed = true),
            )
            assertEquals(listOf("Front"), basic().fieldsNames)
        }

    @Test
    fun `a pending action on a removed field leaves the collection alone`() =
        runTest {
            val type = basic()
            val originalBack = NoteFieldRef(type.getField(1))
            type.deepClone().let { col.notetypes.remFieldLegacy(it, it.getField(1)) }
            markSynced()

            assertEquals(
                FieldChangeResult.Unchanged,
                col.changeNoteField(type.id, FieldChange(FieldAction.Sort, originalBack), confirmed = true),
            )
            assertEquals(listOf("Front"), basic().fieldsNames)
            assertFalse(col.schemaChanged())
        }

    @Test
    fun `confirming add does not create a field whose name was used while the warning was open`() =
        runTest {
            val type = basic()
            val change = FieldChange(FieldAction.Add, value = "History")
            markSynced()
            assertEquals(FieldChangeResult.NeedsFullSync, col.changeNoteField(type.id, change))
            col.notetypes.addFieldLegacy(type.deepClone(), col.notetypes.newField("History"))
            markSynced()

            assertEquals(FieldChangeResult.DuplicateName, col.changeNoteField(type.id, change, confirmed = true))
            assertFalse(col.schemaChanged())
            assertEquals(listOf("Front", "Back", "History"), basic().fieldsNames)
        }

    @Test
    fun `the last field remains editable after refusing deletion`() =
        runTest {
            val type = basic()
            type.deepClone().let { col.notetypes.remFieldLegacy(it, it.getField(1)) }
            markSynced()
            val front = NoteFieldRef(basic().getField(0))

            assertEquals(FieldChangeResult.LastField, col.changeNoteField(type.id, FieldChange(FieldAction.Delete, front)))
            assertFalse(col.schemaChanged())
            assertEquals(
                FieldChangeResult.Changed,
                col.changeNoteField(type.id, FieldChange(FieldAction.Keyboard, front, value = "ja-JP")),
            )
            assertEquals(Locale.JAPAN, basic().getField(0).languageHint)
        }

    @Test
    fun `the same forbidden characters are stripped when adding and renaming fields`() {
        assertEquals("A B", cleanFieldName("# ^ / {A}:\n B\"\r"))
        assertEquals("", cleanFieldName("#^/{}\r\n"))
    }

    private fun basic(): NotetypeJson = checkNotNull(col.notetypes.byName("Basic"))

    private fun markSynced() {
        col.db.execute("update col set ls = scm")
    }
}
