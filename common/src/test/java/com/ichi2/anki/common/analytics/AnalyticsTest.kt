// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.analytics

import org.junit.Test

/**
 * Analytics is called from the crash path, which can run before startup completes.
 * Nothing here may throw, or it would mask the crash being handled.
 */
class AnalyticsTest {
    @Test
    fun `sending an event does nothing`() {
        Analytics.send(AnalyticsEvent.SettingTapped("key"))
    }

    @Test
    fun `sending a screen view does nothing`() {
        Analytics.sendAnalyticsScreenView("Screen")
        Analytics.sendAnalyticsScreenView(this)
    }

    @Test
    fun `reporting an exception does nothing`() {
        Analytics.sendAnalyticsException(RuntimeException("boom"), fatal = true)
    }
}
