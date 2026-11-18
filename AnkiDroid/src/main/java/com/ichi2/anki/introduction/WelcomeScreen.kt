// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.introduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.account.SyncPrimaryButton
import com.ichi2.anki.preferences.SettingsChevron
import com.ichi2.anki.preferences.SettingsDivider
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.compose.theme.foundation

@Composable
internal fun Welcome(
    bottomClearance: Int,
    onStartEmpty: () -> Unit,
    onImport: () -> Unit,
    onSetupSync: () -> Unit,
) {
    var showMigration by rememberSaveable { mutableStateOf(false) }
    BackHandler(showMigration) { showMigration = false }
    val cornerInsets = Modifier.windowInsetsPadding(WindowInsets(bottom = bottomClearance))

    if (showMigration) {
        MigrationGuide(cornerInsets, onBack = { showMigration = false }, onImport = onImport)
    } else {
        WelcomeScreen(cornerInsets, onStartEmpty, onImport, onSetupSync, onMigration = { showMigration = true })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WelcomeScreen(
    modifier: Modifier,
    onStartEmpty: () -> Unit,
    onImport: () -> Unit,
    onSetupSync: () -> Unit,
    onMigration: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val packageName = LocalContext.current.packageName
    Column(
        Modifier
            .fillMaxSize()
            .background(foundation.colors.background)
            .then(modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = foundation.spacing.screenMargin),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(painterResource(R.drawable.hayaki_mark), contentDescription = null, modifier = Modifier.offset(x = (-8).dp).size(88.dp))
            Text(
                stringResource(CommonString.paper_welcome_title),
                modifier = Modifier.semantics { heading() },
                style = foundation.type.screenTitle,
                color = foundation.colors.onBackground,
            )
            Text(
                stringResource(CommonString.paper_welcome_lead),
                style = foundation.type.lead,
                color = foundation.colors.onBackgroundMuted,
            )
            SyncPrimaryButton(
                stringResource(CommonString.paper_welcome_start_empty),
                onStartEmpty,
                Modifier
                    .heightIn(min = foundation.spacing.primaryButton)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("$packageName:id/start_with_no_decks"),
            )
            Column {
                SettingsDivider()
                SettingsRow(
                    title = stringResource(CommonString.paper_welcome_import),
                    summary = stringResource(CommonString.paper_welcome_import_hint),
                    icon = R.drawable.symbol_file_open,
                    onClick = onImport,
                    trailing = { SettingsChevron() },
                )
                SettingsRow(
                    title = stringResource(CommonString.paper_welcome_sync),
                    summary = stringResource(CommonString.paper_welcome_sync_hint),
                    icon = R.drawable.symbol_dns,
                    onClick = onSetupSync,
                    trailing = { SettingsChevron() },
                )
            }
            TextButton(
                onClick = onMigration,
                colors = ButtonDefaults.textButtonColors(contentColor = foundation.colors.emphasis),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Icon(painterResource(R.drawable.symbol_cloud_off), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(CommonString.paper_sync_from_ankiweb), style = foundation.type.rowTitle)
            }
        }
        Text(
            stringResource(CommonString.paper_welcome_unofficial),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = foundation.type.caption,
            color = foundation.colors.onBackgroundMuted,
        )
    }
}

@Composable
private fun MigrationGuide(
    modifier: Modifier,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    SettingsPage(
        title = stringResource(CommonString.paper_welcome_migration_title),
        onBack = onBack,
        modifier = modifier,
        titleBelowNavigation = true,
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
            Text(
                stringResource(CommonString.paper_welcome_migration_lead),
                style = foundation.type.lead,
                color = foundation.colors.onBackgroundMuted,
            )
            Spacer(Modifier.height(20.dp))
            SettingsDivider()
            MigrationStep(
                number = 1,
                title = stringResource(CommonString.paper_welcome_migration_sync),
                detail = stringResource(CommonString.paper_welcome_migration_sync_hint),
            )
            MigrationStep(
                number = 2,
                title = stringResource(CommonString.paper_welcome_migration_export),
                detail = stringResource(CommonString.paper_welcome_migration_export_hint),
            )
            MigrationStep(
                number = 3,
                title = stringResource(CommonString.paper_welcome_migration_import),
                detail = stringResource(CommonString.paper_welcome_migration_import_hint),
            )
        }
        SyncPrimaryButton(
            stringResource(CommonString.paper_sync_choose_file),
            onClick = onImport,
            modifier = Modifier.padding(vertical = 12.dp).heightIn(min = foundation.spacing.primaryButton),
            icon = R.drawable.symbol_file_open,
        )
    }
}

@Composable
private fun MigrationStep(
    number: Int,
    title: String,
    detail: String,
) {
    val foundation = MaterialTheme.foundation
    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(30.dp).border(foundation.shapes.hairline, foundation.colors.emphasis, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(CommonString.paper_welcome_migration_step, number),
                style = foundation.type.caption,
                color = foundation.colors.emphasis,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = foundation.type.rowTitle, color = foundation.colors.onBackground)
            Text(detail, style = foundation.type.body, color = foundation.colors.onBackgroundMuted)
        }
    }
    SettingsDivider()
}
