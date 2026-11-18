// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** The switch can enable a saved address, and clearing the address disables it. */
class VersatileTextWithASwitchPreference(
    context: Context,
    attrs: AttributeSet?,
) : VersatileTextPreference(context, attrs) {
    private val switchKey get() = key + SWITCH_SUFFIX
    private var checked by mutableStateOf(false)
    var switchChecked: Boolean
        get() = checked && !text.isNullOrEmpty()
        set(value) {
            checked = value && !text.isNullOrEmpty()
            storage?.edit { putBoolean(switchKey, checked) }
        }

    override fun loadValue() {
        super.loadValue()
        checked = storage?.getBoolean(switchKey, false) ?: false
    }

    fun onDialogClosedAndNewTextSet() {
        switchChecked = !text.isNullOrEmpty()
    }

    companion object {
        const val SWITCH_SUFFIX = "_switch"
    }
}
