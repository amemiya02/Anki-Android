// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

/** Compose content held to [CheckAccessibility] fails at capture on a contrast or touch-target problem */
@CheckAccessibility
class ComposeAccessibilityAtCaptureTest : ComposeScreenshotTest() {
    @Test
    fun `low-contrast text fails the capture`() {
        val failure = assertFailsWith<Throwable> { capture("low_contrast") { Screen(textColor = Color(0xFFBBBBBB)) } }
        assertThat(failure.message, containsString("TextContrastCheck"))
    }

    @Test
    fun `a small touch target fails the capture`() {
        val failure = assertFailsWith<Throwable> { capture("small_touch_target") { Screen(targetSize = 24) } }
        assertThat(failure.message, containsString("TouchTargetSizeCheck"))
    }

    @Test
    fun `readable text and large touch targets pass`() = capture("passes") { Screen() }

    @Composable
    private fun Screen(
        textColor: Color = Color.Black,
        targetSize: Int = 48,
    ) = Column(Modifier.background(Color.White)) {
        Text("Study now", color = textColor, fontSize = 18.sp)
        Box(
            Modifier
                .size(targetSize.dp)
                .background(Color.Black)
                .semantics { contentDescription = "Add" }
                .clickable { },
        )
    }
}
