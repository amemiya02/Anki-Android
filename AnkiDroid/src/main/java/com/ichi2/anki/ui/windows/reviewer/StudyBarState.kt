// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

/**
 * What the study bar above the Card shows.
 *
 * @param deckPath the Deck of the current Card, one entry per level (`中文::HSK 4` is `[中文, HSK 4]`)
 * @param counts the remaining counts, or `null` when the collection hides the remaining count
 * @param minutesLeft the time-remaining estimate in minutes, or `null` when it is not shown
 * @param progress how much of the session is done, from 0 to 1, or `null` when it is not shown
 */
data class StudyBarState(
    val deckPath: List<String> = emptyList(),
    val counts: StudyCounts? = null,
    val minutesLeft: Int? = null,
    val progress: Float? = null,
)
