// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.ichi2.anki.dialogs.GestureSelectionDialogUtils
import com.ichi2.anki.preferences.allPreferences
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.MappableBinding
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.ui.GesturePicker

/**
 * Maps keyboard, gesture, and joystick bindings to the action identified by [key].
 *
 * Users can add or remove bindings. Adding a binding can replace its assignment to another related action.
 */
open class ControlPreference : DialogPreference {
    @Suppress("unused")
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @Suppress("unused")
    constructor(context: Context) : super(context)

    open fun getMappableBindings(): List<MappableBinding> = MappableBinding.fromPreferenceString(value)

    open val areGesturesEnabled: Boolean = false

    internal var defaultBindingValue: String? = null

    internal open fun displayBinding(binding: MappableBinding): String = binding.toDisplayString(context)

    /** @return the title of the other action using [binding], or null if there is no conflict */
    open fun otherActionTitleForBinding(binding: Binding): CharSequence? {
        val bindingPreference = getPreferenceAssignedTo(binding) ?: return null
        if (bindingPreference == this) return null
        return bindingPreference.title ?: ""
    }

    var value: String?
        get() = getPersistedString(defaultBindingValue)
        set(value) {
            if (value != getPersistedString(null)) {
                persistString(value)
                notifyChanged()
            }
        }

    internal open fun createGesturePicker(): GesturePicker = GestureSelectionDialogUtils.getGesturePicker(context)

    internal fun removeBindingAt(index: Int) {
        val bindings = getMappableBindings().toMutableList()
        bindings.removeAt(index)
        value = bindings.toPreferenceString()
    }

    private fun removeMappableBinding(binding: MappableBinding) {
        val bindings = getMappableBindings().toMutableList()
        bindings.remove(binding)
        value = bindings.toPreferenceString()
    }

    internal fun addBinding(binding: Binding) {
        val newBinding = MappableBinding(binding)
        getPreferenceAssignedTo(binding)?.removeMappableBinding(newBinding)
        val bindings = getMappableBindings().toMutableList()
        bindings.add(newBinding)
        value = bindings.toPreferenceString()
    }

    /**
     * @return a list of preferences related to the same context or screen.
     */
    protected open fun getRelatedPreferences(): List<ControlPreference> =
        preferenceManager.preferenceScreen.allPreferences().filterIsInstance<ControlPreference>()

    /**
     * Checks if any other related preference
     * has the given [binding] assigned to.
     *
     * @see getRelatedPreferences
     */
    protected open fun getPreferenceAssignedTo(binding: Binding): ControlPreference? {
        for (pref in getRelatedPreferences()) {
            val bindings = pref.getMappableBindings().map { it.binding }
            if (binding in bindings) {
                return pref
            }
        }
        return null
    }
}
