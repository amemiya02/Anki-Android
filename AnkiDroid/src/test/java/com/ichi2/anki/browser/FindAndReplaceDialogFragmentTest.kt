// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.os.Bundle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.Note
import com.ichi2.anki.libanki.NoteId
import com.ichi2.anki.ui.internationalization.sentenceCase
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class FindAndReplaceDialogFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val dialog get() = FindReplaceTestUi(compose)

    @Test
    fun `missing selection dismisses find and replace`() = assertUnavailableSelection { assertTrue(delete()) }

    @Test
    fun `truncated selection dismisses find and replace`() = assertUnavailableSelection { writeBytes(byteArrayOf(0)) }

    private fun assertUnavailableSelection(changeFile: IdsFile.() -> Unit) =
        runTest {
            val file = IdsFile(targetContext.cacheDir, listOf(addBasicNote().id), IdsFile.Purpose.PREVIEW).apply(changeFile)
            val fragment =
                FindAndReplaceDialogFragment().apply {
                    arguments = Bundle().apply { putParcelable(FindAndReplaceDialogFragment.ARG_IDS, file) }
                }
            Robolectric.buildActivity(FragmentActivity::class.java).use { controller ->
                controller.get().setTheme(CommonR.style.Theme_Light)
                val activity = controller.setup().get()
                activity.setContentView(CoordinatorLayout(activity).apply { id = R.id.root_layout })
                fragment.show(activity.supportFragmentManager, "find-replace")
                advanceRobolectricLooper()
                advanceUntilIdle()
                advanceRobolectricLooper()
                assertNull(activity.supportFragmentManager.findFragmentByTag("find-replace"))
            }
        }

    @Test
    fun `with no selected notes 'only selected notes' check box is not actionable`() =
        withFindReplace(emptyList()) {
            dialog.selectedNotes.assertIsOff().assertIsNotEnabled()
            dialog.ignoreCase.assertIsOn()
            dialog.regex.assertIsOff()
            dialog.openFields()
            assertThat(dialog.fieldNames(), equalTo(defaultTargets))
        }

    @Test
    fun `shows expected find-replace targets for selected notes`() {
        val n1 = createFindReplaceTestNote("A", "Car", "Lion")
        val n2 = createFindReplaceTestNote("B", "Train", "Chicken")
        createFindReplaceTestNote("C", "Plane", "Vulture")
        withFindReplace(listOf(n1.id, n2.id)) {
            dialog.openFields()
            val allTargets = defaultTargets + listOf("Afield0", "Afield1", "Bfield0", "Bfield1")
            assertThat(dialog.fieldNames(), equalTo(allTargets))
        }
    }

    // see #19929
    @Test
    fun `'selected notes only' status is correct after fragment restore`() {
        val note = createFindReplaceTestNote("A", "kart", "kilogram")
        withFindReplace(listOf(note.id)) { scenario ->
            val allTargets = defaultTargets + listOf("Afield0", "Afield1")
            dialog.selectedNotes.assertIsOn().assertIsEnabled()
            dialog.openFields()
            assertThat(dialog.fieldNames(), equalTo(allTargets))
            dialog.field("Afield0").performClick()

            scenario.recreate()

            dialog.selectedNotes.assertIsOn().assertIsEnabled()
            dialog.openFields()
            assertThat(dialog.fieldNames(), equalTo(allTargets))
        }
    }

    private fun withFindReplace(
        noteIds: List<NoteId>,
        action: (FragmentScenario<FindAndReplaceDialogFragment>) -> Unit,
    ) {
        val file = IdsFile(targetContext.cacheDir, noteIds, IdsFile.Purpose.PREVIEW)
        FragmentScenario
            .launch(
                fragmentClass = FindAndReplaceDialogFragment::class.java,
                fragmentArgs = Bundle().apply { putParcelable(FindAndReplaceDialogFragment.ARG_IDS, file) },
                themeResId = CommonR.style.Theme_Light,
            ).use { scenario ->
                advanceRobolectricLooper()
                action(scenario)
            }
    }

    /**
     * 3 notetypes available(named A, B and C) each with two fields.
     * Fields names follow the pattern: "${NotetypeName}field${0/1}" (ex: "Afield1").
     * "C" notetype has the same name for field 1 as notetype B!
     * second is a [Pair] representing the field data(the note has only two fields)
     */
    private fun createFindReplaceTestNote(
        notetypeName: String,
        field0: String,
        field1: String,
    ): Note {
        addStandardNoteType("A", arrayOf("Afield0", "Afield1"), "", "")
        addStandardNoteType("B", arrayOf("Bfield0", "Bfield1"), "", "")
        addStandardNoteType("C", arrayOf("Cfield0", "Bfield1"), "", "")
        return addNoteUsingNoteTypeName(notetypeName, field0, field1)
    }

    private val defaultTargets get() = listOf(with(targetContext) { TR.sentenceCase.allFields }, TR.editingTags())
}

internal class FindReplaceTestUi(
    private val compose: SemanticsNodeInteractionsProvider,
) {
    private val fieldChoice = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and hasAnyAncestor(isDialog())

    val ignoreCase get() = compose.onNodeWithText(TR.browsingIgnoreCase())
    val selectedNotes get() = compose.onNodeWithText(TR.browsingSelectedNotesOnly())
    val regex get() = compose.onNodeWithText(TR.browsingTreatInputAsRegularExpression())

    fun openFields() = compose.onNodeWithText(label(TR.browsingIn())).performScrollTo().performClick()

    fun fieldNames(): List<String> =
        compose.onAllNodes(fieldChoice).fetchSemanticsNodes().map { node -> node.config[SemanticsProperties.Text].single().text }

    fun field(name: String) = compose.onNode(hasText(name) and fieldChoice)

    fun enterText(
        search: String,
        replacement: String,
    ) {
        compose.onNode(hasText(label(TR.browsingFind())) and hasSetTextAction()).performTextReplacement(search)
        compose.onNode(hasText(label(TR.browsingReplaceWith())) and hasSetTextAction()).performTextReplacement(replacement)
    }

    fun confirm(text: String) = compose.onNodeWithText(text).performClick()

    private fun label(html: String) = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}
