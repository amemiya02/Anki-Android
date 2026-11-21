// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.utils.formatSearchError
import com.ichi2.anki.utils.showSearchError
import com.ichi2.testutils.EmptyAnkiActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertContains
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ErrorMessageTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `inline code is rendered in monospace`() {
        val message = showMessage("Invalid search: an `and` was found. Try `\"and\"`.")
        assertEquals("Invalid search: an and was found. Try \"and\".", message.toString())
        assertEquals(listOf("and", "\"and\""), codeSpans(message))
    }

    @Test
    fun `preformatted errors preserve whitespace and literal code`() {
        val code = "regex parse error:\n    (?i)b[<>&`\n         ^\nerror: unclosed character class"
        val message = showMessage("<pre>$code</pre>")
        assertEquals(code, message.toString())
        assertEquals(listOf(code), codeSpans(message))
    }

    @Test
    fun `ordinary text and unmatched delimiters are preserved`() {
        val text = "Error: <tag> &amp; a_b *c*\n  `unfinished <pre>"
        assertEquals(text, showMessage(text).toString())
    }

    @Test
    fun `regex diagnostics preserve embedded closing tags`() {
        val code = "regex parse error:\n    (?i)</pre>[<b>&amp;`\n              ^\nerror: unclosed character class"
        val message = formatSearchError("<pre>$code</pre>")
        assertEquals(code, message.toString())
        assertEquals(listOf(code), codeSpans(message))
    }

    @Test
    fun `malformed or unsupported code delimiters remain literal`() {
        val messages =
            listOf(
                "an `unclosed",
                "`a` and `unclosed",
                "``a``",
                "`a``b`",
                "`a` then ` `",
                "`a\nb`",
                "`a\rb`",
                "\\`a\\`",
            )
        for (text in messages) {
            val message = formatSearchError(text)
            assertEquals(text, message.toString())
            assertEquals(emptyList(), codeSpans(message))
        }
    }

    @Test
    fun `inline code trims edge whitespace like Python Markdown`() {
        val message = formatSearchError("Use `  and  ` or ` <b>&amp; `.")
        assertEquals("Use and or <b>&amp;.", message.toString())
        assertEquals(listOf("and", "<b>&amp;"), codeSpans(message))
    }

    @Test
    fun `generic errors keep code delimiters literal`() {
        val text = "File `name` failed: <pre>  diagnostic</pre>"
        assertEquals(text, showMessage(text, searchError = false).toString())
    }

    @Test
    fun `browser formats actual backend search errors`() =
        runTest {
            throwOnShowError = false
            addBasicNote()
            val browser = startRegularActivity<CardBrowser>()
            browser.searchCardsSync("and")
            advanceRobolectricLooper()
            assertEquals(listOf("and", "\"and\""), codeSpans(latestMessage("and")))
            ShadowDialog.getLatestDialog().dismiss()

            for (regex in listOf("b[", "</pre>[")) {
                browser.searchCardsSync("re:$regex")
                advanceRobolectricLooper()
                val diagnostic = latestMessage("regex parse error")
                assertContains(diagnostic.toString(), "(?i)$regex")
                assertEquals(listOf(diagnostic.toString()), codeSpans(diagnostic))
                ShadowDialog.getLatestDialog().dismiss()
            }
        }

    private fun showMessage(
        message: String,
        searchError: Boolean = true,
    ): AnnotatedString {
        throwOnShowError = false
        val activity = startRegularActivity<EmptyAnkiActivity>()
        if (searchError) activity.showSearchError(message) else activity.showError(message, crashReportData = null)
        val displayed = if (searchError) formatSearchError(message).toString() else message
        return latestMessage(displayed)
    }

    private fun latestMessage(containing: String): AnnotatedString =
        compose
            .onNode(hasText(containing, substring = true) and hasAnyAncestor(isDialog()))
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()

    private fun codeSpans(message: AnnotatedString): List<String> =
        message.spanStyles.map { range ->
            assertEquals(FontFamily.Monospace, range.item.fontFamily)
            message.text.substring(range.start, range.end)
        }
}
