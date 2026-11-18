// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Pankaj Gupta <pankajgupta0695@gmail.com>

package com.ichi2.preferences

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.EmptyApplicationCategory
import com.ichi2.testutils.AndroidTest
import com.ichi2.testutils.EmptyApplication
import com.ichi2.testutils.targetContext
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = EmptyApplication::class)
@Category(EmptyApplicationCategory::class)
class NumberRangePreferenceCompatTest : AndroidTest {
    private val preference = TestableNumberRangePreferenceCompat(targetContext)

    @Test
    fun getValidatedRangeFromIntWithinRangeReturnsInput() {
        preference.setMinValue(0)
        assertThat(preference.getValidatedRangeFromInt(50), equalTo(50))
    }

    @Test
    fun getValidatedRangeFromIntBelowMinReturnsMin() {
        preference.setMinValue(10)
        assertThat(preference.getValidatedRangeFromInt(5), equalTo(10))
    }

    @Test
    fun getValidatedRangeFromStringEmptyStringReturnsMin() {
        preference.setMinValue(5)
        assertThat(preference.getValidatedRangeFromString(""), equalTo(5))
    }

    @Test
    fun getValidatedRangeFromStringValidNumberReturnsValidatedNumber() {
        preference.setMinValue(0)
        assertThat(preference.getValidatedRangeFromString("50"), equalTo(50))
    }

    @Test
    fun getValidatedRangeFromStringInvalidNumberReturnsMin() {
        preference.setMinValue(5)
        assertThat(preference.getValidatedRangeFromString("abc"), equalTo(5))
    }

    // Test subclass to allow setting min value for testing
    private class TestableNumberRangePreferenceCompat(
        context: Context,
    ) : NumberRangePreferenceCompat(context) {
        fun setMinValue(value: Int) {
            min = value
        }
    }
}
