// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.use
import com.ichi2.anki.R

/** A text field with a configurable keyboard and validation while editing. */
open class VersatileTextPreference(
    context: Context,
    attrs: AttributeSet?,
) : DialogPreference(context, attrs) {
    fun interface Validator {
        @Throws(Exception::class)
        fun validate(value: String)
    }

    val dialogHint: CharSequence? =
        context.obtainStyledAttributes(attrs, R.styleable.CustomPreference).use {
            it.getText(R.styleable.CustomPreference_dialogHint)
        }

    val inputType: Int =
        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.inputType)).use {
            it.getInt(0, InputType.TYPE_CLASS_TEXT)
        }

    var continuousValidator: Validator? = null
    private var currentText by mutableStateOf<String?>(null)
    var text: String?
        get() = currentText
        set(value) {
            currentText = value
            saveString(value)
        }

    init {
        if (useSimpleSummary) {
            setSummaryProvider { text?.takeIf(String::isNotEmpty) ?: context.getString(androidx.preference.R.string.not_set) }
        }
    }

    override fun loadValue() {
        currentText = storage?.getString(key, initialValue) ?: initialValue
        if (initialValue != null && storage?.contains(key) == false) saveString(currentText)
    }
}
