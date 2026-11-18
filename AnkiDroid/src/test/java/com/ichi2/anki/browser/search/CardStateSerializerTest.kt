// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser.search

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/** Test for [CardStateSerializer] */
class CardStateSerializerTest {
    @Test
    fun `ensure mappings are unchanged`() {
        // CardState is serialized, so codes may not change
        val originalStates =
            listOf(
                CardState.New,
                CardState.Learning,
                CardState.Review,
                CardState.Buried,
                CardState.Suspended,
            )
        assertEquals(originalStates, Json.decodeFromString<List<CardState>>("[0,1,2,3,4]"))

        val allKnownStates = originalStates + listOf(CardState.Due, CardState.Marked)
        assertEquals(allKnownStates, CardState.entries)
        assertEquals("[0,1,2,3,4,5,6]", Json.encodeToString(allKnownStates))
    }
}
