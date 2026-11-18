// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.progress

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.testutils.EmptyAnkiActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@RunWith(AndroidJUnit4::class)
class ProgressDialogMessageTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `the message is resolved into the dialog text`() {
        val controller = Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup()
        saveControllerForCleanup(controller)
        val source =
            object : HasProgress<String> {
                override val progressManager = ProgressManager<String>()
            }
        controller.get().observeProgress(source, delayMillis = 600.milliseconds) { message ->
            getString(CommonString.progress_amount_bytes, message, "2 MB")
        }
        val gate = CompletableDeferred<Unit>()
        val op =
            CoroutineScope(Dispatchers.Unconfined).launch {
                source.progressManager.withProgress(message = "1 MB") { gate.await() }
            }
        try {
            shadowOf(Looper.getMainLooper()).idleFor(1200.milliseconds.toJavaDuration())
            compose.onNodeWithText("1 MB/2 MB").assertIsDisplayed()
        } finally {
            gate.complete(Unit)
            op.cancel()
        }
    }
}
