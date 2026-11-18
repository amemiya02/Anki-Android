// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.use
import com.ichi2.anki.R
import com.google.android.material.R as MaterialR

class SliderPreference(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {
    private val range =
        context.obtainStyledAttributes(attrs, MaterialR.styleable.Slider).use {
            Triple(
                it.getInt(MaterialR.styleable.Slider_android_valueFrom, 0),
                it.getInt(MaterialR.styleable.Slider_android_valueTo, 0),
                it.getFloat(MaterialR.styleable.Slider_android_stepSize, 1F),
            )
        }
    val valueFrom = range.first
    val valueTo = range.second
    val stepSize = range.third
    private val presentation =
        context.obtainStyledAttributes(attrs, R.styleable.SliderPreference).use {
            it.getString(R.styleable.SliderPreference_displayFormat) to it.getBoolean(R.styleable.SliderPreference_displayValue, false)
        }

    private var currentValue by mutableIntStateOf(valueFrom)
    var value: Int
        get() = currentValue
        set(value) {
            require(value in valueFrom..valueTo)
            currentValue = value
            saveInt(value)
        }

    override fun loadValue() {
        val default = initialValue?.toIntOrNull() ?: valueFrom
        currentValue = (storage?.getInt(key, default) ?: default).coerceIn(valueFrom, valueTo)
        if (initialValue != null && storage?.contains(key) == false) saveInt(currentValue)
    }

    fun valueLabel(value: Int): String? = presentation.first?.format(value) ?: value.toString().takeIf { presentation.second }
}
