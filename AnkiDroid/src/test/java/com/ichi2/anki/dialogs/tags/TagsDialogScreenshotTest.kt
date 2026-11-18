// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextReplacement
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.CommonString
import com.ichi2.anki.DialogScreenshotTest
import com.ichi2.anki.dialogs.tags.TagsDialog.DialogType
import org.junit.Rule
import org.junit.Test

class TagsDialogScreenshotTest : DialogScreenshotTest() {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun `empty tags`() =
        runTest {
            val dialog = TagsDialog().withArguments(targetContext, DialogType.EDIT_TAGS)
            withDialog(dialog) {
                dialog.viewModel.tags.await()
                compose.onNodeWithText(targetContext.getString(CommonString.no_tags)).assertIsDisplayed()
                captureScreen("empty_tags")
            }
        }

    @Test
    fun `tag selection and search`(
        @TestParameter type: DialogType,
        @TestParameter landscape: Boolean,
    ) = runTest {
        val orientation = setOrientation(landscape)
        val name = "${orientation}_${type.name.lowercase()}"
        val note = addBasicNote()
        col.tags.bulkAdd(
            listOf(note.id),
            "biology::animals biology::plants chemistry geography history languages::English mathematics physics revision::priority",
        )
        val dialog =
            TagsDialog().withArguments(
                targetContext,
                type,
                noteIds = if (type == DialogType.CUSTOM_STUDY) listOf(note.id) else emptyList(),
                checkedTags = arrayListOf("biology::plants", "chemistry"),
            )
        withDialog(dialog) {
            dialog.viewModel.tags.await()
            val list = compose.onNodeWithTag("tag_list")
            list.performScrollToKey("chemistry")
            compose.onNodeWithContentDescription("chemistry").assertIsDisplayed()
            captureScreen(name)

            compose.onNodeWithContentDescription(targetContext.getString(CommonString.filter_tags)).performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("biology")
            compose.onNodeWithText("chemistry").assertDoesNotExist()
            list.performScrollToKey("biology")
            compose.onNodeWithContentDescription("biology").assertIsDisplayed()
            captureScreen("${name}_search")
        }
    }
}
