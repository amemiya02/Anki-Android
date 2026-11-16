// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.common.crashreporting

import android.content.Context
import androidx.annotation.VisibleForTesting
import timber.log.Timber

/**
 * Receives reports of failures: exceptions the app caught, and problems it detected.
 *
 * The Fork sends no crash reports to anyone: [CrashReportService] writes each report to the
 * log. Tests may install their own [CrashReporter] to observe what is reported.
 */
interface CrashReporter {
    fun sendExceptionReport(
        message: String?,
        origin: String?,
    )

    /**
     * @param onlyIfSilent Kept from Upstream's call sites; it has no effect, since no report
     * is ever sent.
     */
    fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String? = null,
        onlyIfSilent: Boolean = false,
    )

    /**
     * @param onlyIfSilent Kept from Upstream's call sites; it has no effect, since no report
     * is ever sent.
     */
    fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String? = null,
        onlyIfSilent: Boolean = false,
        context: Context,
    )
}

/**
 * Global crash reporting service.
 *
 * Reports are only logged ([LoggingCrashReporter]): the Fork sends no crash reports to
 * Upstream's server or anywhere else. Reporting is called from error-handling paths, so it
 * must never throw.
 *
 * Usage:
 * ```
 * CrashReportService.sendExceptionReport(exception, "MyClass.myMethod")
 * ```
 */
object CrashReportService {
    var instance: CrashReporter = LoggingCrashReporter
        private set

    @VisibleForTesting
    fun setReporter(reporter: CrashReporter) {
        instance = reporter
    }

    @VisibleForTesting
    fun getReporter(): CrashReporter = instance

    /** Reset [instance] to the reporter which only logs exceptions */
    @VisibleForTesting
    fun resetForTesting() {
        instance = LoggingCrashReporter
    }

    /**
     * Reports a non-fatal issue without a [Throwable].
     * @param message Description of what went wrong.
     * @param origin The class or method name where the issue occurred.
     */
    fun sendExceptionReport(
        message: String?,
        origin: String?,
    ) = instance.sendExceptionReport(message, origin)

    /**
     * Reports a caught [Throwable].
     * @param e The exception or error to report.
     * @param origin The tag or location where it occurred.
     * @param additionalInfo Optional extra metadata to include in the report.
     * @param onlyIfSilent Has no effect: no report is ever sent.
     */
    fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String? = null,
        onlyIfSilent: Boolean = false,
    ) = instance.sendExceptionReport(e, origin, additionalInfo, onlyIfSilent)

    /**
     * Reports a caught [Throwable] with a specific [Context].
     * @param onlyIfSilent Has no effect: no report is ever sent.
     */
    fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String? = null,
        onlyIfSilent: Boolean = false,
        context: Context,
    ) = instance.sendExceptionReport(e, origin, additionalInfo, onlyIfSilent, context)
}

/** The [CrashReporter] of the Fork: it writes reports to the log and sends nothing. */
private object LoggingCrashReporter : CrashReporter {
    override fun sendExceptionReport(
        message: String?,
        origin: String?,
    ) {
        Timber.w("Not reported (%s): %s", origin, message)
    }

    override fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String?,
        onlyIfSilent: Boolean,
    ) {
        Timber.w(e, "Not reported (%s): %s", origin, additionalInfo)
    }

    override fun sendExceptionReport(
        e: Throwable,
        origin: String?,
        additionalInfo: String?,
        onlyIfSilent: Boolean,
        context: Context,
    ) {
        Timber.w(e, "Not reported (%s): %s", origin, additionalInfo)
    }
}

/**
 * Runs the provided block, catching [Exception], logging it and reporting it to [CrashReportService]
 *
 * **Example**
 * ```
 * runCatchingWithReport("callingMethod", onlyIfSilent = true) {
 *     doSomethingRisky()
 * }
 * ```
 *
 * **Note**: This differs from [runCatching] - `Error` is thrown
 *
 * @param origin Data logged to Timber, and provided as the 'origin' field in the error report
 * @param onlyIfSilent Has no effect: no report is ever sent.
 * @param block Code to execute
 *
 * @throws Error If raised, this will be reported and rethrown
 *
 * @return A Result containing either the successful result of [block] or the [Exception] thrown
 */
fun <T> runCatchingWithReport(
    origin: String?,
    onlyIfSilent: Boolean = false,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: Throwable) {
        Timber.w(e, origin)
        CrashReportService.sendExceptionReport(e, origin, onlyIfSilent = onlyIfSilent)
        if (e is Error) throw e
        Result.failure(e)
    }

/**
 * Runs the provided block, catching [Exception] and logging it.
 *
 * Unlike [runCatchingWithReport], this does **not** send a crash report.
 *
 * **Note**: This differs from [runCatching] - `Error` is thrown
 *
 * @param origin Data logged to Timber
 * @param block Code to execute
 *
 * @throws Error If raised, this will be rethrown
 *
 * @return A Result containing either the successful result of [block] or the [Exception] thrown
 */
fun <T> runCatchingWithLog(
    origin: String? = null,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: Throwable) {
        Timber.w(e, origin)
        if (e is Error) throw e
        Result.failure(e)
    }
