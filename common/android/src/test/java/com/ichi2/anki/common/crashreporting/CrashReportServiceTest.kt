// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.crashreporting

import android.util.Log
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CrashReportServiceTest {
    private data class LogEntry(
        val priority: Int,
        val message: String,
        val throwable: Throwable?,
    )

    private val logs = mutableListOf<LogEntry>()

    private val recordingTree =
        object : Timber.Tree() {
            override fun log(
                priority: Int,
                tag: String?,
                message: String,
                t: Throwable?,
            ) {
                logs.add(LogEntry(priority, message, t))
            }
        }

    @BeforeEach
    fun setUp() {
        CrashReportService.resetForTesting()
        Timber.plant(recordingTree)
    }

    @AfterEach
    fun tearDown() {
        Timber.uproot(recordingTree)
    }

    /** Crash reporting is called from error-handling paths, so it must never throw. */
    @Test
    fun `a report is logged, not sent or thrown`() {
        val exception = Exception("a caught failure")

        CrashReportService.sendExceptionReport(exception, origin = "CrashReportServiceTest")

        val warning = logs.single { it.priority == Log.WARN }
        assertSame(exception, warning.throwable, "the log should hold the exception for local debugging")
        assertTrue(warning.message.contains("CrashReportServiceTest"), "the log should name the origin")
    }
}
