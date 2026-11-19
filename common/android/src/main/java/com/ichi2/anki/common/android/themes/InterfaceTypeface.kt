// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.common.android.themes

import android.content.Context
import android.graphics.Typeface

/**
 * The typeface of interface text, at its regular weight. Derive other weights with
 * `Typeface.create(typeface, weight, false)`.
 *
 * It is the system sans-serif (ADR-0006): Roboto for Latin script, and the system's CJK font for
 * Chinese and Japanese, whose Han characters take the forms of the text's language. No interface
 * font is bundled.
 */
@Suppress("UnusedReceiverParameter") // kept on Context so callers need not change when the face does
fun Context.interfaceTypeface(): Typeface = Typeface.SANS_SERIF
