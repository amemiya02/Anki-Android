// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.util.Xml
import androidx.annotation.XmlRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.content.res.use
import com.ichi2.anki.R
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.preferences.ExtendedPreferenceCategory
import org.xmlpull.v1.XmlPullParser.END_DOCUMENT
import org.xmlpull.v1.XmlPullParser.END_TAG
import org.xmlpull.v1.XmlPullParser.START_TAG

/** Definitions for the Compose Settings rows, backed by the existing shared preferences. */
open class Preference(
    val context: Context,
    attrs: AttributeSet? = null,
) {
    private val attributes =
        context.obtainStyledAttributes(attrs, R.styleable.SettingsPreference).use {
            Attributes(
                key = it.getString(R.styleable.SettingsPreference_android_key).orEmpty(),
                title = it.getText(R.styleable.SettingsPreference_android_title),
                summary = it.getText(R.styleable.SettingsPreference_android_summary),
                isPersistent = it.getBoolean(R.styleable.SettingsPreference_android_persistent, true),
                icon = it.getResourceId(R.styleable.SettingsPreference_android_icon, 0),
                fragment = it.getString(R.styleable.SettingsPreference_android_fragment),
                defaultValue = it.getString(R.styleable.SettingsPreference_android_defaultValue),
                dialogTitle = it.getText(R.styleable.SettingsPreference_android_dialogTitle),
                useSimpleSummary = it.getBoolean(R.styleable.SettingsPreference_useSimpleSummaryProvider, false),
            )
        }

    var key = attributes.key
    var title by mutableStateOf(attributes.title)
    private var summaryValue by mutableStateOf(attributes.summary)
    private var computeSummary: ((Preference) -> CharSequence?)? by mutableStateOf(null)
    open var summary: CharSequence?
        get() = computeSummary?.invoke(this) ?: summaryValue
        set(value) {
            summaryValue = value
        }
    var isVisible by mutableStateOf(true)
    var isEnabled by mutableStateOf(true)
    var isSelectable = true
        protected set
    val icon = attributes.icon
    val fragment = attributes.fragment
    val dialogTitle = attributes.dialogTitle
    protected val initialValue = attributes.defaultValue
    protected val useSimpleSummary = attributes.useSimpleSummary

    private var onChange: ((Preference, Any?) -> Boolean)? = null
    private var onClick: ((Preference) -> Boolean)? = null

    internal var page: PreferenceScreen? = null
        private set
    protected var storage: SharedPreferences? = null
        private set

    internal fun attach(
        page: PreferenceScreen,
        storage: SharedPreferences?,
    ) {
        this.page = page
        this.storage = storage.takeIf { attributes.isPersistent && key.isNotEmpty() }
        loadValue()
    }

    protected open fun loadValue() = Unit

    protected fun saveString(value: String?) = storage?.edit { putString(key, value) }

    protected fun saveInt(value: Int) = storage?.edit { putInt(key, value) }

    protected fun saveBoolean(value: Boolean) = storage?.edit { putBoolean(key, value) }

    fun setSummaryProvider(provider: (Preference) -> CharSequence?) {
        computeSummary = provider
    }

    fun setOnPreferenceChangeListener(listener: (Preference, Any?) -> Boolean) {
        onChange = listener
    }

    fun setOnPreferenceClickListener(listener: (Preference) -> Boolean) {
        onClick = listener
    }

    fun callChangeListener(value: Any?): Boolean = onChange?.invoke(this, value) ?: true

    fun performClick() {
        if (!isEnabled || !isSelectable) return
        page?.onSettingTapped?.invoke(this)
        if (onClick?.invoke(this) != true) activate()
    }

    protected open fun activate() {
        if (fragment != null) page?.openSubpage?.invoke(fragment)
    }

    private data class Attributes(
        val key: String,
        val title: CharSequence?,
        val summary: CharSequence?,
        val isPersistent: Boolean,
        val icon: Int,
        val fragment: String?,
        val defaultValue: String?,
        val dialogTitle: CharSequence?,
        val useSimpleSummary: Boolean,
    )
}

open class PreferenceGroup(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    private val contents = mutableListOf<Preference>()
    val children: List<Preference> get() = contents

    fun addPreference(preference: Preference) {
        contents.add(preference)
    }
}

open class PreferenceCategory(
    context: Context,
    attrs: AttributeSet?,
) : PreferenceGroup(context, attrs) {
    init {
        isSelectable = false
    }
}

class PreferenceScreen(
    context: Context,
    attrs: AttributeSet? = null,
) : PreferenceGroup(context, attrs) {
    internal var onSettingTapped: ((Preference) -> Unit)? = null
    internal var openEditor: ((DialogPreference) -> Unit)? = null
    internal var openSubpage: ((String) -> Unit)? = null

    fun attach(storage: SharedPreferences? = context.sharedPrefs()) {
        allPreferences().forEach { it.attach(this, storage) }
    }

    fun allPreferences(): List<Preference> =
        buildList {
            fun append(group: PreferenceGroup) {
                for (preference in group.children) {
                    add(preference)
                    if (preference is PreferenceGroup) append(preference)
                }
            }
            append(this@PreferenceScreen)
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : Preference> findPreference(key: String): T? = allPreferences().firstOrNull { it.key == key } as T?
}

open class DialogPreference(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    override fun activate() {
        page?.openEditor?.invoke(this)
    }
}

class SwitchPreference(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    private var currentChecked by mutableStateOf(false)
    var isChecked: Boolean
        get() = currentChecked
        set(value) {
            currentChecked = value
            saveBoolean(value)
        }

    override fun loadValue() {
        val default = initialValue.toBoolean()
        currentChecked = storage?.getBoolean(key, default) ?: default
        if (initialValue != null && storage?.contains(key) == false) saveBoolean(currentChecked)
    }

    override fun activate() {
        val next = !isChecked
        if (callChangeListener(next)) isChecked = next
    }
}

open class ListPreference(
    context: Context,
    attrs: AttributeSet?,
) : DialogPreference(context, attrs) {
    var entries: Array<out CharSequence> by mutableStateOf(arrayOf())
    var entryValues: Array<out CharSequence> by mutableStateOf(arrayOf())

    private var currentValue by mutableStateOf<String?>(null)
    var value: String?
        get() = currentValue
        set(value) {
            currentValue = value
            saveString(value)
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SettingsChoice).use {
            entries = it.getTextArray(R.styleable.SettingsChoice_android_entries).orEmpty()
            entryValues = it.getTextArray(R.styleable.SettingsChoice_android_entryValues).orEmpty()
        }
        if (useSimpleSummary) {
            setSummaryProvider { entries.getOrNull(findIndexOfValue(value)) ?: context.getString(androidx.preference.R.string.not_set) }
        }
    }

    override fun loadValue() {
        currentValue = storage?.getString(key, initialValue) ?: initialValue
        if (initialValue != null && storage?.contains(key) == false) saveString(currentValue)
    }

    fun findIndexOfValue(value: String?): Int = entryValues.indexOfLast { it.toString() == value }
}

internal fun Context.loadSettings(
    @XmlRes resource: Int,
    storage: SharedPreferences? = sharedPrefs(),
): PreferenceScreen =
    resources.getXml(resource).use { xml ->
        var screen: PreferenceScreen? = null
        val path = ArrayDeque<PreferenceGroup>()
        var event = xml.next()
        while (event != END_DOCUMENT) {
            when (event) {
                START_TAG -> {
                    val attrs = Xml.asAttributeSet(xml)
                    val preference =
                        when (xml.name.substringAfterLast('.')) {
                            "PreferenceScreen" -> PreferenceScreen(this, attrs)
                            "PreferenceCategory" -> PreferenceCategory(this, attrs)
                            "ExtendedPreferenceCategory" -> ExtendedPreferenceCategory(this, attrs)
                            "Preference" -> Preference(this, attrs)
                            "SwitchPreferenceCompat" -> SwitchPreference(this, attrs)
                            "ListPreference" -> ListPreference(this, attrs)
                            "ControlPreference" -> ControlPreference(this, attrs)
                            "ReviewerControlPreference" -> ReviewerControlPreference(this, attrs)
                            "WhiteboardControlPreference" -> WhiteboardControlPreference(this, attrs)
                            "SliderPreference" -> SliderPreference(this, attrs)
                            "NumberRangePreferenceCompat" -> NumberRangePreferenceCompat(this, attrs)
                            "IncrementerNumberRangePreferenceCompat" -> IncrementerNumberRangePreferenceCompat(this, attrs)
                            "HtmlHelpPreference" -> HtmlHelpPreference(this, attrs)
                            "ExternalDirectorySelectionPreference" -> ExternalDirectorySelectionPreference(this, attrs)
                            "VersatileTextPreference" -> VersatileTextPreference(this, attrs)
                            "VersatileTextWithASwitchPreference" -> VersatileTextWithASwitchPreference(this, attrs)
                            else -> error("Unknown Settings row: ${xml.name}")
                        }
                    if (preference is PreferenceScreen) screen = preference
                    path.lastOrNull()?.addPreference(preference)
                    if (preference is PreferenceGroup) path.addLast(preference)
                }
                END_TAG -> if (xml.depth == path.size) path.removeLast()
            }
            event = xml.next()
        }
        checkNotNull(screen).also { it.attach(storage) }
    }
