// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget

import com.ichi2.anki.ScreenshotTest
import com.ichi2.widget.AddNoteWidget
import com.ichi2.widget.renderWidget
import org.junit.Test

class AddNoteWidgetScreenshotTest : ScreenshotTest() {
    @Test
    fun defaultSize() =
        runTest {
            captureView("default_size", targetContext.renderWidget(AddNoteWidget(), width = 76, height = 76))
        }
}
