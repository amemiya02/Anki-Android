// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.google.android.material.theme.MaterialComponentsViewInflater
import java.lang.reflect.Constructor
import java.util.WeakHashMap

/**
 * Creates the views of every layout, as Material does, and draws inflated text in the interface
 * typeface's fallback chain ([interfaceTypeface]) at the weight its style asks for.
 *
 * A layout can only name one font file per weight, so its text would show Latin in the Chinese or
 * Japanese face, or Chinese and Japanese in the system font. Set as each theme's `viewInflaterClass`.
 *
 * Monospaced text keeps its typeface. Code that sets a typeface after inflation still wins.
 */
class FoundationViewInflater : MaterialComponentsViewInflater() {
    override fun createTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createTextView(context, attrs).withInterfaceTypeface()

    override fun createButton(
        context: Context,
        attrs: AttributeSet,
    ) = super.createButton(context, attrs).withInterfaceTypeface()

    override fun createEditText(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createEditText(context, attrs).withInterfaceTypeface()

    override fun createCheckBox(
        context: Context,
        attrs: AttributeSet,
    ) = super.createCheckBox(context, attrs).withInterfaceTypeface()

    override fun createRadioButton(
        context: Context,
        attrs: AttributeSet,
    ) = super.createRadioButton(context, attrs).withInterfaceTypeface()

    override fun createCheckedTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createCheckedTextView(context, attrs).withInterfaceTypeface()

    override fun createAutoCompleteTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createAutoCompleteTextView(context, attrs).withInterfaceTypeface()

    override fun createMultiAutoCompleteTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createMultiAutoCompleteTextView(context, attrs).withInterfaceTypeface()

    override fun createToggleButton(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createToggleButton(context, attrs).withInterfaceTypeface()

    /**
     * Text views the AppCompat inflater leaves to the platform: those named in full in a layout
     * (`com.google.android.material.button.MaterialButton`, the app's own text views) and platform
     * ones it doesn't replace, such as `Switch`. Every other view is left to the platform.
     */
    override fun createView(
        context: Context,
        name: String,
        attrs: AttributeSet,
    ): View? {
        val constructor = textViewConstructor(name, context.classLoader) ?: return null
        return constructor.newInstance(context, attrs).withInterfaceTypeface()
    }

    private companion object {
        /** By tag name; `null` when the tag isn't a text view */
        val constructors = HashMap<String, Constructor<out TextView>?>()

        fun textViewConstructor(
            name: String,
            classLoader: ClassLoader,
        ): Constructor<out TextView>? =
            synchronized(constructors) {
                constructors.getOrPut(name) {
                    val className = if ('.' in name) name else "android.widget.$name"
                    val viewClass = runCatching { Class.forName(className, false, classLoader) }.getOrNull()
                    if (viewClass == null || !TextView::class.java.isAssignableFrom(viewClass)) return@getOrPut null
                    viewClass.asSubclass(TextView::class.java).getConstructor(Context::class.java, AttributeSet::class.java)
                }
            }
    }
}

/**
 * Replaces the typeface the view's style gave it with the interface typeface's fallback chain at
 * the same weight. Before Android 10 the style's font is already the language's own face.
 */
fun <T : TextView> T.withInterfaceTypeface(): T {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return this
    val current = typeface ?: Typeface.DEFAULT
    if (current.isMonospace) return this
    typeface = Typeface.create(context.interfaceTypeface(), current.weight, current.isItalic)
    return this
}

private val monospace = WeakHashMap<Typeface, Boolean>()

/** Whether every letter of the typeface is as wide as the others: code, not interface text */
private val Typeface.isMonospace: Boolean
    get() =
        synchronized(monospace) {
            monospace.getOrPut(this) {
                val paint = Paint().also { it.typeface = this }
                paint.measureText("i") == paint.measureText("M")
            }
        }
