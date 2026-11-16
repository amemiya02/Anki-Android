// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.settings.enums.Theme

/** Supplies every app theme (Light, Black and Dark) to a preview wrapped in [AnkiDroidPreview]. */
class AnkiDroidThemeProvider : PreviewParameterProvider<Theme> {
    override val values: Sequence<Theme> = DayTheme.entries.asSequence() + NightTheme.entries.asSequence()

    override fun getDisplayName(index: Int): String =
        values
            .elementAt(index)
            .toString()
            .lowercase()
            .replaceFirstChar(Char::titlecase)
}
