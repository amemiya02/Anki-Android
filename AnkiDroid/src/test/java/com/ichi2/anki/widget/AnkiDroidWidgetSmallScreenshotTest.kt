// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget

import com.ichi2.anki.ScreenshotTest
import com.ichi2.widget.AnkiDroidWidgetSmall
import com.ichi2.widget.SmallWidgetStatus
import com.ichi2.widget.renderWidget
import com.ichi2.widget.widgetRepository
import org.junit.Test

class AnkiDroidWidgetSmallScreenshotTest : ScreenshotTest() {
    @Test
    fun `due count and time estimate`() =
        runTest {
            widgetRepository.storeSmallWidgetStatus(SmallWidgetStatus(26, 14))
            captureView("due_count", targetContext.renderWidget(AnkiDroidWidgetSmall(), width = 76, height = 76))
        }
}
