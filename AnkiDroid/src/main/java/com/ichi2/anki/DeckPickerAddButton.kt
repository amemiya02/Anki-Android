// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

/** The containing View also serves as the snackbar and system-inset anchor for the Compose button. */
class DeckPickerAddButton internal constructor(
    context: Context,
    icon: Int,
    private val primary: Boolean,
) : AbstractComposeView(context) {
    var icon by mutableIntStateOf(icon)
    var text by mutableStateOf<CharSequence?>(null)
    var isExtended by mutableStateOf(!primary)
    internal var action: (() -> Unit)? = null
    internal var onEscape: (() -> Boolean)? = null

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun performClick(): Boolean {
        val click = action ?: return false
        click()
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> return performClick()
                KeyEvent.KEYCODE_ESCAPE -> if (onEscape?.invoke() == true) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Composable
    override fun Content() {
        AnkiDroidTheme {
            val foundation = MaterialTheme.foundation
            val label = text?.toString().takeIf { isExtended }
            Surface(
                onClick = { performClick() },
                shape = foundation.shapes.addButton,
                modifier =
                    Modifier.heightIn(min = if (primary) 56.dp else 48.dp)
                        .widthIn(min = if (primary) 56.dp else 48.dp)
                        .then(if (primary) Modifier.testTag("deck-add") else Modifier),
                shadowElevation = if (primary) 4.dp else 2.dp,
                color = if (primary) foundation.colors.primary else foundation.colors.background,
                contentColor = if (primary) foundation.colors.onPrimary else foundation.colors.onBackground,
            ) {
                Row(
                    Modifier.padding(horizontal = if (label == null) 0.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(
                        painterResource(icon),
                        contentDescription = if (label == null) context.getString(CommonR.string.menu_add) else null,
                        modifier = Modifier.size(24.dp),
                    )
                    if (label != null) Text(label, style = foundation.type.button)
                }
            }
        }
    }
}
