// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.libanki.sched.Counts.Queue
import com.ichi2.compose.theme.FoundationColors

@Composable
internal fun Queue.label(): String =
    remember(this) {
        when (this) {
            Queue.NEW -> TR.actionsNew()
            Queue.LRN -> TR.schedulingLearning()
            Queue.REV -> TR.decksReviewHeader()
        }
    }

internal fun Queue.color(colors: FoundationColors): Color =
    when (this) {
        Queue.NEW -> colors.countNew
        Queue.LRN -> colors.countLearning
        Queue.REV -> colors.countDue
    }
