// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import com.ichi2.anki.R
import com.ichi2.anki.utils.ext.usingStyledAttributes

/**
 * Non-clickable preference that shows help text.
 *
 * The summary is parsed as a format string containing HTML,
 * and may contain up to three format specifiers as understood by `Sting.format()`,
 * arguments for which can be specified using XML attributes such as `app:substitution1`.
 *
 *     <com.ichi2.preferences.HtmlHelpPreference
 *         android:summary="@string/format_string"
 *         app:substitution1="@string/substitution1"
 *         />
 *
 * The summary supports HTML formatting,
 * including new lines and links. Raw HTML can be entered using the CDATA tag, e.g.
 *
 *     <string name="format_string"><![CDATA[<b>Hello, %s</b>]]></string>
 */
class HtmlHelpPreference(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {
    init {
        isSelectable = false
    }

    private val substitutions =
        context.usingStyledAttributes(attrs, R.styleable.HtmlHelpPreference) {
            arrayOf(
                getString(R.styleable.HtmlHelpPreference_substitution1),
                getString(R.styleable.HtmlHelpPreference_substitution2),
                getString(R.styleable.HtmlHelpPreference_substitution3),
            )
        }

    val htmlSummary get() = summary.toString().format(*substitutions)
}
