// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.CardId
import com.ichi2.anki.observability.undoableOp
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.undoAndShowSnackbar
import com.ichi2.anki.withProgress
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

/**
 * Allows a user to grade a card without the use of the study screen
 *
 * For example, a forgotten card can be marked as 'again' before it's due
 *
 * Discussion: https://github.com/ankitects/anki/pull/3840
 *
 * @see net.ankiweb.rsdroid.Backend.gradeNow
 */
// TODO: handle rotation, via a DialogFragment with IdsFile handling or Fragment Result API
@NeedsTest("Suspended card handling")
object GradeNowDialog {
    fun showDialog(
        context: AppCompatActivity,
        cardIds: List<CardId>,
    ) {
        if (!cardIds.any()) {
            Timber.w("no selected cards")
            return
        }

        Timber.i("Opening 'Grade Now'")

        val title = with(context) { TR.sentenceCase.gradeNow }
        createPaperDialog(context) { dialog ->
            SingleChoiceDialog(
                title = title,
                choices = Grade.entries.map { DialogChoice(it.getLabel(), icon = it.iconRes) },
                selectedIndex = null,
                onDismissRequest = dialog::dismiss,
                onSelect = { index ->
                    dialog.dismiss()
                    val selectedGrade = Grade.entries[index]
                    Timber.i("selected '%s'", selectedGrade.name)
                    context.gradeNow(cardIds, selectedGrade)
                },
            )
        }.show()
    }

    private fun AppCompatActivity.gradeNow(
        ids: List<CardId>,
        grade: Grade,
    ) = launchCatchingTask {
        Timber.d("Grading %d cards as %s", ids.size, grade.name)
        withProgress {
            undoableOp { this.backend.gradeNow(ids, grade.rating) }
        }
        showSnackbar(TR.schedulingGradedCardsDone(ids.size)) {
            setAction(CommonString.undo) { launchCatchingTask { undoAndShowSnackbar() } }
        }
    }
}

private enum class Grade(
    val rating: Rating,
    @DrawableRes val iconRes: Int,
    val getLabel: () -> String,
) {
    Again(Rating.AGAIN, R.drawable.symbol_close_again, { TR.studyingAgain() }),
    Hard(Rating.HARD, R.drawable.symbol_check_small_hard, { TR.studyingHard() }),
    Good(Rating.GOOD, R.drawable.symbol_check_good, { TR.studyingGood() }),
    Easy(Rating.EASY, R.drawable.symbol_done_all_easy, { TR.studyingEasy() }),
}
