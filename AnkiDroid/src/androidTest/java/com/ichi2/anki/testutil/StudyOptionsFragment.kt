// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Arthur Milchior <arthur@milchior.fr>

package com.ichi2.anki.testutil

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

/**
 * If the study options is displayed, tap on the "study" button. Otherwise do nothing.
 */
fun clickOnStudyButtonIfExists() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).findObject(By.res("studyoptions_start"))?.click()
}
