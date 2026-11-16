// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.cardviewer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.previewer.stdHtml
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Card Content keeps the font its Card Template sets: the interface typefaces apply to the Card
 * Frame and the rest of the interface only. Checked in Japanese, where the interface face differs
 * most from a Card's.
 */
@RunWith(AndroidJUnit4::class)
class CardContentTypefaceTest : RobolectricTest() {
    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Before
    fun interfaceInJapanese() = RuntimeEnvironment.setQualifiers("ja")

    @Test
    fun `a Card is drawn in the font its Card Template sets`() {
        val noteType = col.notetypes.byName("Basic")!!
        noteType.css = TEMPLATE_CSS
        col.notetypes.save(noteType)
        val card = addBasicNote("骨", "直").firstCard()

        val renderer = AndroidCardRenderContext.createInstance(targetContext, col, TypeAnswer.createInstance(targetContext.sharedPrefs()))
        for (side in SingleCardSide.entries) {
            val html = renderer.renderCard(col, card, side).html
            assertContains(html, TEMPLATE_CSS, message = "$side")
            assertNoInterfaceFace(html)
        }
    }

    @Test
    fun `the study screen's page around a Card names no interface typeface`() {
        assertNoInterfaceFace(stdHtml(targetContext))
    }

    private fun assertNoInterfaceFace(html: String) {
        for (face in interfaceFaces) {
            assertTrue(face !in html.lowercase(), "Card Content must not be given the interface face '$face'")
        }
    }

    companion object {
        const val TEMPLATE_CSS = ".card { font-family: \"Template Face\", serif; }"

        /** How the interface faces are named in fonts, files and resources */
        val interfaceFaces = listOf("newsreader", "wenkai", "klee", "foundation_interface")
    }
}
