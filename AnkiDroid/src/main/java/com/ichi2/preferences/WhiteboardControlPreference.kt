// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.preferences

import android.content.Context
import android.util.AttributeSet
import com.ichi2.anki.reviewer.CardSide

class WhiteboardControlPreference(
    context: Context,
    attrs: AttributeSet? = null,
) : ReviewerControlPreference(context, attrs) {
    override var side: CardSide? = CardSide.BOTH

    override fun getRelatedPreferences(): List<WhiteboardControlPreference> =
        relatedSettings.filterIsInstance<WhiteboardControlPreference>()
}
