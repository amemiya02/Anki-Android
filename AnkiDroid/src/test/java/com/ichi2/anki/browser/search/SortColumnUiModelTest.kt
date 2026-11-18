// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser.search

import com.ichi2.anki.browser.CardBrowserColumn
import com.ichi2.anki.browser.CardBrowserColumn.Companion.fromColumnKey
import com.ichi2.anki.browser.ColumnType
import com.ichi2.anki.browser.JvmCardBrowserViewModelTestBase
import com.ichi2.anki.browser.sortableColumns
import com.ichi2.anki.model.CardsOrNotes.CARDS
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SortColumnUiModelTest : JvmCardBrowserViewModelTestBase() {
    @Test
    fun activeSortableColumnsRetainTheirOrderAboveOtherChoices() =
        runViewModelTest {
            assertTrue(
                updateActiveColumns(
                    listOf(CardBrowserColumn.ORIGINAL_POSITION, CardBrowserColumn.QUESTION, CardBrowserColumn.LAPSES),
                    CARDS,
                ),
            )

            val actual = sortableColumns(CARDS).map { it.first.value }

            assertEquals(listOf("originalPosition", "cardLapses"), actual.take(2))
            assertTrue("cardEase" in actual)
            assertFalse("question" in actual)
            assertFalse("answer" in actual)
        }

    @Test
    fun easeRemainsAvailableWithTheSameLabelWhetherShownOrHidden() =
        runViewModelTest {
            fun easeLabel() = sortableColumns(CARDS).single { it.first.value == "cardEase" }.second

            assertEquals("Ease", easeLabel())
            assertTrue(updateActiveColumns(listOf(CardBrowserColumn.EASE, CardBrowserColumn.QUESTION), CARDS))
            assertEquals("Ease", easeLabel())
        }

    @Test
    fun columnTypeIsAssignedByEngineKey() {
        val types =
            mapOf(
                "noteFld" to ColumnType.TEXT,
                "deck" to ColumnType.TEXT,
                "cardEase" to ColumnType.NUMERIC,
                "originalPosition" to ColumnType.NUMERIC,
                "noteCrt" to ColumnType.DATE,
                "cardMod" to ColumnType.DATE,
                "cardDue" to ColumnType.UNSPECIFIED,
            )
        assertEquals(types, types.keys.associateWith { fromColumnKey(it).type(CARDS) })
    }
}
