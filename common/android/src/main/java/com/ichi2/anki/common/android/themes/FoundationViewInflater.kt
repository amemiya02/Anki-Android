// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.withStyledAttributes
import com.google.android.material.theme.MaterialComponentsViewInflater
import com.ichi2.anki.common.android.R
import java.lang.reflect.Constructor
import java.util.WeakHashMap

/**
 * Creates the views of every layout, as Material does, and draws inflated text in the interface
 * typeface's fallback chain ([interfaceTypeface]) at the weight its style asks for.
 *
 * A layout can only name one font file per weight, so its text would show Latin in the Chinese or
 * Japanese face, or Chinese and Japanese in the system font. Set as each theme's `viewInflaterClass`.
 *
 * The weight comes from the view's Foundation type style (`foundationTextWeight`): the themes set a
 * font family, which the platform and AppCompat apply over the type style's `textFontWeight`.
 * A heavier weight the view already has (bold set on the view, a medium font family) is kept.
 *
 * Monospaced text keeps its typeface. Code that sets a typeface after inflation still wins.
 */
class FoundationViewInflater : MaterialComponentsViewInflater() {
    override fun createTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createTextView(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createButton(
        context: Context,
        attrs: AttributeSet,
    ) = super.createButton(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createEditText(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createEditText(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createCheckBox(
        context: Context,
        attrs: AttributeSet,
    ) = super.createCheckBox(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createRadioButton(
        context: Context,
        attrs: AttributeSet,
    ) = super.createRadioButton(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createCheckedTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createCheckedTextView(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createAutoCompleteTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createAutoCompleteTextView(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createMultiAutoCompleteTextView(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createMultiAutoCompleteTextView(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    override fun createToggleButton(
        context: Context,
        attrs: AttributeSet?,
    ) = super.createToggleButton(context, attrs).withInterfaceTypeface(foundationWeight(context, attrs))

    /**
     * Text views the AppCompat inflater leaves to the platform: those named in full in a layout
     * (`com.google.android.material.button.MaterialButton`, the app's own text views) and platform
     * ones it doesn't replace, such as `Switch`. Also toolbars, whose title and subtitle views are
     * created in code when first set. Every other view is left to the platform.
     */
    override fun createView(
        context: Context,
        name: String,
        attrs: AttributeSet,
    ): View? {
        val constructor = constructorFor(name, context.classLoader) ?: return null
        return when (val view = constructor.newInstance(context, attrs)) {
            is TextView -> view.withInterfaceTypeface(foundationWeight(context, attrs))
            is Toolbar -> view.apply { setOnHierarchyChangeListener(TitleTypeface) }
            else -> view
        }
    }

    /** Gives a toolbar's title and subtitle the interface typeface as the toolbar adds them */
    private object TitleTypeface : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(
            parent: View?,
            child: View?,
        ) {
            (child as? TextView)?.withInterfaceTypeface()
        }

        override fun onChildViewRemoved(
            parent: View?,
            child: View?,
        ) = Unit
    }

    private companion object {
        /** By tag name; `null` when the tag is neither a text view nor a toolbar */
        val constructors = HashMap<String, Constructor<out View>?>()

        fun constructorFor(
            name: String,
            classLoader: ClassLoader,
        ): Constructor<out View>? =
            synchronized(constructors) {
                constructors.getOrPut(name) {
                    val className = if ('.' in name) name else "android.widget.$name"
                    val viewClass = runCatching { Class.forName(className, false, classLoader) }.getOrNull()
                    val handled = listOf(TextView::class.java, Toolbar::class.java)
                    if (viewClass == null || handled.none { it.isAssignableFrom(viewClass) }) return@getOrPut null
                    viewClass.asSubclass(View::class.java).getConstructor(Context::class.java, AttributeSet::class.java)
                }
            }
    }
}

/**
 * Replaces the typeface the view's style gave it with the interface typeface's fallback chain at
 * the same weight, or at [weight] when that is heavier. Before Android 10 the style's font is already the
 * language's own face.
 */
fun <T : TextView> T.withInterfaceTypeface(weight: Int? = null): T {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return this
    val current = typeface ?: Typeface.DEFAULT
    if (current.isMonospace) return this
    val drawnWeight = maxOf(current.weight, weight ?: 0)
    typeface = Typeface.create(context.interfaceTypeface(), drawnWeight, current.isItalic)
    return this
}

/**
 * The weight of the Foundation type style a layout gives a view (`android:textAppearance`, directly
 * or through its `style`, else the theme's), or null when that is not a Foundation type style.
 */
private fun foundationWeight(
    context: Context,
    attrs: AttributeSet?,
): Int? {
    var appearance = 0
    context.withStyledAttributes(attrs, intArrayOf(android.R.attr.textAppearance)) { appearance = getResourceId(0, 0) }
    if (appearance == 0) return null
    var weight: Int? = null
    context.withStyledAttributes(appearance, intArrayOf(R.attr.foundationTextWeight)) {
        if (hasValue(0)) weight = getInt(0, 0)
    }
    return weight
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
