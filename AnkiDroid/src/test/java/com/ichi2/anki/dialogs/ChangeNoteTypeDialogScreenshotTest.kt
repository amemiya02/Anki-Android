// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.core.view.WindowInsetsCompat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.ScreenshotTest.DeviceConfig.DESKTOP
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.dialogs.ChangeNoteTypeViewModel.Tab
import com.ichi2.anki.withCardBrowser
import com.ichi2.testutils.simulateSystemBars
import com.ichi2.testutils.windowInsetsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

@ScreenshotTest.IncludeDevices(DESKTOP)
class ChangeNoteTypeDialogScreenshotTest : ScreenshotTest() {
    @get:Rule val compose = createEmptyComposeRule()

    enum class LargeTextScreen(
        val qualifiers: String,
    ) {
        NARROW_PORTRAIT("+w320dp-h568dp-port"),
        LANDSCAPE("+land"),
    }

    @Test
    fun portrait() =
        captureChangeNoteType(
            "portrait",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
        ) { dialog -> assertTabsFillWidth(dialog) }

    @Test
    fun gestureNavigation() =
        captureChangeNoteType(
            "gesture_navigation",
            with(targetContext) { windowInsetsOf(navBarBottom = 24.dp) },
        )

    @Test
    fun portraitCutout() =
        captureChangeNoteType(
            "portrait_cutout",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp, cutoutTop = 40.dp) },
        )

    @Test
    fun landscape() {
        RuntimeEnvironment.setQualifiers("+land")
        captureChangeNoteType(
            "landscape",
            with(targetContext) { windowInsetsOf(navBarRight = 48.dp, cutoutLeft = 32.dp) },
        ) { dialog -> assertTabsFillWidth(dialog) }
    }

    @Test
    fun mappingsFitViewport(
        @TestParameter tab: Tab,
    ) = captureChangeNoteType(
        "${tab.name.lowercase()}_fit_viewport",
        with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
        tab = tab,
    ) { dialog ->
        val range = page(tab).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("The mappings fit without scrolling", 0f, range.maxValue(), 1f)
        scrollMappingsToBottom(dialog, tab, requireScrolling = false)
    }

    @Test
    fun longNoteTypeNamesWithLargeText(
        @TestParameter screen: LargeTextScreen,
    ) {
        RuntimeEnvironment.setQualifiers(screen.qualifiers)
        RuntimeEnvironment.setFontScale(2f)
        captureChangeNoteType(
            "long_note_type_names_large_text_${screen.name.lowercase()}",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
            prepareCollection = {
                val noteType = col.notetypes.byName("Basic")!!
                noteType.name = "A very long note type name that should leave room for mapping fields and templates"
                for (number in 3..10) {
                    col.notetypes.addFieldModChanged(noteType, col.notetypes.newField("Field $number"))
                }
                col.notetypes.save(noteType)
            },
        ) { dialog ->
            assertTrue("The mappings have a viewport", page(Tab.Fields).fetchSemanticsNode().boundsInRoot.height > 0)
            scrollMappingsToBottom(dialog, requireScrolling = false)
        }
    }

    @Test
    fun templatesTabWithLargeText() {
        RuntimeEnvironment.setQualifiers("+w320dp-h568dp-port")
        RuntimeEnvironment.setFontScale(2f)
        captureChangeNoteType(
            "templates_tab_large_text",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
            tab = Tab.Templates,
        ) { dialog ->
            assertTabsFillWidth(dialog)
            compose.onNodeWithText(TR.changeNotetypeTemplates()).assertIsDisplayed()
        }
    }

    @Test
    fun manyMappingsWithLargeTextInLandscape(
        @TestParameter tab: Tab,
    ) {
        RuntimeEnvironment.setQualifiers("+land")
        RuntimeEnvironment.setFontScale(2f)
        captureManyMappings("many_${tab.name.lowercase()}_large_text_landscape", tab)
    }

    @Test
    fun manyMappingsScrolledToBottom(
        @TestParameter tab: Tab,
    ) = captureManyMappings("many_${tab.name.lowercase()}_scrolled_to_bottom", tab)

    private fun captureManyMappings(
        name: String,
        tab: Tab,
    ) {
        val noteType = col.notetypes.byName("Basic")!!
        when (tab) {
            Tab.Fields -> {
                for (number in 3..30) {
                    val name = if (number == 30) "A long final field name that wraps onto several lines of text" else "Field $number"
                    col.notetypes.addFieldModChanged(noteType, col.notetypes.newField(name))
                }
            }
            Tab.Templates -> {
                for (number in 2..30) {
                    val name = if (number == 30) "A long final template name that wraps onto several lines of text" else "Card $number"
                    val template =
                        col.notetypes.newTemplate(name).apply {
                            qfmt = "{{Front}}"
                            afmt = "{{Back}}"
                        }
                    col.notetypes.addTemplateModChanged(noteType, template)
                }
            }
        }
        col.notetypes.save(noteType)

        captureChangeNoteType(
            name,
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
            tab = tab,
        ) { dialog ->
            scrollMappingsToBottom(dialog, tab)
        }
    }

    private fun page(tab: Tab) = compose.onNodeWithTag("mapping_page_${tab.position}")

    private fun assertTabsFillWidth(dialog: ChangeNoteTypeDialog) {
        val fields = compose.onNodeWithText(TR.changeNotetypeFields()).fetchSemanticsNode().boundsInRoot
        val templates = compose.onNodeWithText(TR.changeNotetypeTemplates()).fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("target_type").fetchSemanticsNode().boundsInRoot
        assertEquals(content.width, fields.width + templates.width, 2f)
        assertTrue(dialog.requireView().width >= content.width)
    }

    private fun scrollMappingsToBottom(
        dialog: ChangeNoteTypeDialog,
        tab: Tab = Tab.Fields,
        requireScrolling: Boolean = true,
    ) {
        val vm = androidx.lifecycle.ViewModelProvider(dialog)[ChangeNoteTypeViewModel::class.java]
        val count = if (tab == Tab.Fields) vm.outputNoteType.fieldsNames.size else vm.outputNoteType.templatesNames.size
        val list = page(tab)
        if (requireScrolling) assertTrue(list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0)
        list.performScrollToIndex(count + 1)
        compose
            .onNode(
                hasTestTag("mapping_${count - 1}") and hasAnyAncestor(hasTestTag("mapping_page_${tab.position}")),
            ).assertIsDisplayed()
    }

    private fun captureChangeNoteType(
        name: String,
        insets: WindowInsetsCompat,
        tab: Tab = Tab.Fields,
        prepareCollection: () -> Unit = {},
        beforeCapture: (ChangeNoteTypeDialog) -> Unit = {},
    ) = withCardBrowser(noteCount = 1) { browser ->
        prepareCollection()
        val dialog = ChangeNoteTypeDialog.newInstance(col.findNotes(""))
        dialog.showNow(browser.supportFragmentManager, "change_note_type")
        advanceRobolectricLooper()
        try {
            compose.onNodeWithText(if (tab == Tab.Fields) TR.changeNotetypeFields() else TR.changeNotetypeTemplates()).performClick()
            advanceRobolectricLooper()
            dialog.requireDialog().window!!.simulateSystemBars(insets)
            beforeCapture(dialog)
            captureScreen(name)
        } finally {
            dialog.dismissNow()
        }
    }
}
