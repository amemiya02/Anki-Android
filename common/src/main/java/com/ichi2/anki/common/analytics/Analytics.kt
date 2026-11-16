// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.analytics

/**
 * Where Upstream's code reports usage statistics.
 *
 * The Fork sends no usage statistics anywhere, so every call here does nothing. The calls
 * are kept so that code shared with Upstream does not have to change.
 *
 * Crashes and errors may call this before startup completes, so nothing here may throw.
 */
object Analytics {
    @Suppress("UNUSED_PARAMETER")
    fun send(event: AnalyticsEvent) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun sendAnalyticsScreenView(screen: Any) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun sendAnalyticsScreenView(screenName: String) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun sendAnalyticsException(
        t: Throwable,
        fatal: Boolean,
    ) = Unit
}
