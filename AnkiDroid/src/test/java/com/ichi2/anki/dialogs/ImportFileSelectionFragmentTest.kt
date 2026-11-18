// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.dialogs

import android.content.Intent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.ImportFileSelectionFragment.Companion.buildImportFilePickerIntent
import com.ichi2.anki.dialogs.ImportFileSelectionFragment.ImportOptions
import com.ichi2.anki.utils.MimeTypeUtils
import com.ichi2.testutils.launchFragment
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportFileSelectionFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `picker intent disallows multiple files by default`() {
        val intent = buildImportFilePickerIntent()
        assertThat(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), equalTo(false))
    }

    @Test
    fun `CSV picker intent disallows multiple files and carries csv mime types`() {
        val intent = buildImportFilePickerIntent(extraMimes = MimeTypeUtils.CSV_TSV_MIME_TYPES)

        assertThat(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), equalTo(false))
        assertThat(
            "CSV picker must pass CSV/TSV mime types as EXTRA_MIME_TYPES",
            intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList(),
            equalTo(MimeTypeUtils.CSV_TSV_MIME_TYPES.toList()),
        )
    }

    @Test
    fun `picker intent uses ACTION_OPEN_DOCUMENT with openable category`() {
        val intent = buildImportFilePickerIntent()
        assertThat(intent.action, equalTo(Intent.ACTION_OPEN_DOCUMENT))
        assertThat(
            "Intent must request an openable document",
            intent.categories?.contains(Intent.CATEGORY_OPENABLE),
            equalTo(true),
        )
    }

    @Test
    fun `dialog with all import options shows three entries`() =
        assertChoices(
            ImportOptions(true, true, true),
            CommonString.import_deck_package,
            CommonString.import_collection_package,
            CommonString.import_csv,
        )

    @Test
    fun `dialog with only colpkg option shows only colpkg entry (restore backup scenario)`() =
        assertChoices(ImportOptions(true, false, false), CommonString.import_collection_package)

    @Test
    fun `dialog with only apkg option shows only apkg entry`() =
        assertChoices(ImportOptions(false, true, false), CommonString.import_deck_package)

    @Test
    fun `dialog with only csv option shows only csv entry`() = assertChoices(ImportOptions(false, false, true), CommonString.import_csv)

    private fun assertChoices(
        options: ImportOptions,
        vararg expected: Int,
    ) {
        launchFragment<ImportFileSelectionFragment>(
            fragmentArgs = ImportFileSelectionFragment.newInstance(options).arguments,
        ).use {
            val choices = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).fetchSemanticsNodes()
            val labels = choices.map { node -> node.config[SemanticsProperties.Text].single().text }
            assertThat(labels, equalTo(expected.map(targetContext::getString)))
        }
    }
}
