// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2013 Houssam Salem <houssam.salem.au@gmail.com>

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.use
import com.ichi2.anki.R

open class NumberRangePreferenceCompat(
    context: Context,
    attrs: AttributeSet? = null,
) : DialogPreference(context, attrs) {
    private val bounds =
        context.obtainStyledAttributes(attrs, R.styleable.NumberRangePreferenceCompat).use {
            it.getInt(R.styleable.NumberRangePreferenceCompat_min, 0)..it.getInt(
                R.styleable.NumberRangePreferenceCompat_max,
                Int.MAX_VALUE,
            )
        }
    var min = bounds.first
        protected set
    val max = bounds.last
    val maxDigits: Int get() = max.toString().length
    private var currentValue by mutableIntStateOf(min)
    val text: String get() = currentValue.toString()
    var canOpenEditor: () -> Boolean = { true }

    init {
        if (useSimpleSummary) setSummaryProvider { text }
    }

    override fun loadValue() {
        val default = initialValue?.toIntOrNull() ?: min
        currentValue = storage?.getInt(key, default) ?: default
        if (initialValue != null && storage?.contains(key) == false) saveInt(currentValue)
    }

    fun getValidatedRangeFromInt(input: Int): Int = input.coerceIn(min, max)

    fun getValidatedRangeFromString(input: String): Int = getValidatedRangeFromInt(input.toIntOrNull() ?: min)

    fun getValue(): Int = currentValue

    fun setValue(value: Int) {
        currentValue = getValidatedRangeFromInt(value)
        saveInt(currentValue)
    }

    fun setValue(value: String) = setValue(getValidatedRangeFromString(value))

    fun setOnPreferenceChangeListener(listener: (Int) -> Unit) {
        setOnPreferenceChangeListener { _, value ->
            if (value !is Int) return@setOnPreferenceChangeListener false
            listener(value)
            true
        }
    }

    override fun activate() {
        if (canOpenEditor()) super.activate()
    }
}
