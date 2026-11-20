// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat
import kotlinx.parcelize.Parcelize

/** The ratings recorded and answer time spent in this visit to the reviewer. */
@Parcelize
data class ReviewSessionSummary(
    val again: Int,
    val hard: Int,
    val good: Int,
    val easy: Int,
    val elapsedMillis: Long,
) : Parcelable {
    val reviewCount: Int get() = again + hard + good + easy

    internal fun toIntent(): Intent = Intent().putExtra(ARG_KEY, this)

    companion object {
        internal const val ARG_KEY = "com.ichi2.anki.reviewSessionSummary"

        fun fromIntent(intent: Intent?): ReviewSessionSummary? =
            intent?.let { IntentCompat.getParcelableExtra(it, ARG_KEY, ReviewSessionSummary::class.java) }
    }
}
