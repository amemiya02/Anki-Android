// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs.help

import android.app.Dialog
import android.os.Bundle
import androidx.activity.addCallback
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.ankiActivity
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.anki.common.analytics.AnalyticsEvent.LinkClicked
import com.ichi2.anki.common.analytics.LinkAction
import com.ichi2.anki.dialogs.help.HelpItem.Action.OpenUrl
import com.ichi2.anki.dialogs.help.HelpItem.Action.OpenUrlResource
import com.ichi2.anki.dialogs.help.HelpItem.Action.Rate
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.anki.common.android.R as CommonR

/**
 * [DialogFragment] responsible for showing the help/support menus.
 */
class HelpDialog : AnalyticsDialogFragment() {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var actionsDispatcher: HelpItemActionsDispatcher

    private var submenuId by mutableStateOf<Long?>(null)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        ankiActivity?.let { actionsDispatcher = AnkiActivityHelpActionsDispatcher(it) }
        if (savedInstanceState?.containsKey(STATE_SUBMENU) == true) {
            submenuId = savedInstanceState.getLong(STATE_SUBMENU)
        }
        val initialItems = requireArgsHelpEntries().asList()
        return createPaperDialog(requireContext()) {
            val id = submenuId
            val items = if (id == null) initialItems else childHelpMenuItems.filter { it.parentId == id }
            val close = if (id == null) CommonString.close else CommonString.previewer_back
            key(id) {
                PaperDialog(
                    title = getString(requireArguments().getInt(ARG_MENU_TITLE)),
                    onDismissRequest = ::back,
                    actions = listOf(DialogAction(getString(close), ::back)),
                ) {
                    items.forEach { item ->
                        DialogChoiceRow(
                            DialogChoice(getString(item.titleResId), icon = item.iconResId),
                            checked = false,
                            onClick = { select(item) },
                            multiple = false,
                        )
                    }
                }
            }
        }.apply {
            onBackPressedDispatcher.addCallback(this@HelpDialog, true) { back() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        submenuId?.let { outState.putLong(STATE_SUBMENU, it) }
    }

    private fun back() {
        if (submenuId != null) submenuId = null else dismiss()
    }

    private fun select(item: HelpItem) {
        Analytics.send(LinkClicked(item.analyticsId))
        when (val action = item.action) {
            is OpenUrl -> actionsDispatcher.onOpenUrl(action.url)
            is OpenUrlResource -> actionsDispatcher.onOpenUrlResource(action.urlResourceId)
            Rate -> actionsDispatcher.onRate()
            null -> if (childHelpMenuItems.any { it.parentId == item.id }) submenuId = item.id
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val ARG_MENU_TITLE = "arg_menu_title"
        private const val STATE_SUBMENU = "state_submenu"

        fun newHelpInstance(): HelpDialog {
            Analytics.send(LinkClicked(LinkAction.OPENED_HELP_DIALOG))
            return HelpDialog().apply {
                arguments =
                    Bundle().apply {
                        putInt(ARG_MENU_TITLE, CommonString.help)
                        putParcelableArray(ARG_MENU_ITEMS, mainHelpMenuItems)
                    }
            }
        }

        fun newPrivacyPolicyInstance(): HelpDialog {
            Analytics.send(LinkClicked(LinkAction.OPENED_PRIVACY))
            val privacyId = mainHelpMenuItems.single { it.analyticsId == LinkAction.OPENED_PRIVACY }.id
            val privacyItems = childHelpMenuItems.filter { it.parentId == privacyId }
            return HelpDialog().apply {
                arguments =
                    Bundle().apply {
                        putInt(ARG_MENU_TITLE, CommonString.help_title_privacy)
                        putParcelableArray(ARG_MENU_ITEMS, privacyItems.toTypedArray())
                    }
            }
        }

        /**
         * @param canRateApp a boolean that indicates if the system has an app to open to rate AnkiDroid
         */
        fun newSupportInstance(canRateApp: Boolean): HelpDialog {
            Analytics.send(LinkClicked(LinkAction.OPENED_SUPPORT_ANKIDROID))
            val actualMenuItems =
                supportMenuItems
                    .filterNot { it.action is Rate && !canRateApp }
                    .filterNot { it.analyticsId == LinkAction.OPENED_DONATE && !BuildConfig.SHOW_DONATE_LINKS }
            return HelpDialog().apply {
                arguments =
                    Bundle().apply {
                        putInt(ARG_MENU_TITLE, CommonString.help_title_support_ankidroid)
                        putParcelableArray(ARG_MENU_ITEMS, actualMenuItems.toTypedArray())
                    }
            }
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun Fragment.requireArgsHelpEntries(): Array<HelpItem> {
    requireArguments().classLoader = javaClass.classLoader
    val retrievedItems =
        BundleCompat.getParcelableArray(
            requireArguments(),
            ARG_MENU_ITEMS,
            HelpItem::class.java,
        ) ?: error("Unable to retrieve current help menu items")
    return retrievedItems.map { it as HelpItem }.toTypedArray()
}

internal const val ARG_MENU_ITEMS = "arg_menu_items"

/**
 * The top level menu items being shown in the help menu.
 * The help menu is a two level menu and for simplicity and ease of handling the corresponding menu
 * items are split in two arrays: this one which shows the main items and [childHelpMenuItems] which
 * groups all the children of the menu items from this array.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val mainHelpMenuItems =
    arrayOf(
        HelpItem(
            titleResId = CommonString.help_title_using_ankidroid,
            iconResId = R.drawable.symbol_menu_book,
            analyticsId = LinkAction.OPENED_USING_ANKIDROID,
            id = 1,
        ),
        HelpItem(
            titleResId = CommonString.help_title_get_help,
            iconResId = CommonR.drawable.symbol_help,
            analyticsId = LinkAction.OPENED_GET_HELP,
            id = 2,
        ),
        HelpItem(
            titleResId = CommonString.help_title_community,
            iconResId = R.drawable.symbol_group,
            analyticsId = LinkAction.OPENED_COMMUNITY,
            id = 3,
        ),
        HelpItem(
            titleResId = CommonString.help_title_privacy,
            iconResId = R.drawable.symbol_privacy_tip,
            analyticsId = LinkAction.OPENED_PRIVACY,
            id = 4,
        ),
    )

/** The menu items that are shown in the support menu. */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val supportMenuItems =
    arrayOf(
        HelpItem(
            titleResId = CommonString.help_item_support_opencollective_donate,
            iconResId = R.drawable.symbol_favorite,
            analyticsId = LinkAction.OPENED_DONATE,
            id = 5,
            action = OpenUrlResource(R.string.link_opencollective_donate),
        ),
        HelpItem(
            titleResId = CommonString.multimedia_editor_trans_translate,
            iconResId = R.drawable.symbol_language,
            analyticsId = LinkAction.OPENED_TRANSLATE,
            id = 6,
            action = OpenUrlResource(R.string.link_translation),
        ),
        HelpItem(
            titleResId = CommonString.help_item_support_develop_ankidroid,
            iconResId = R.drawable.symbol_build,
            analyticsId = LinkAction.OPENED_DEVELOP,
            id = 7,
            action = OpenUrlResource(R.string.link_ankidroid_development_guide),
        ),
        HelpItem(
            titleResId = CommonString.help_item_support_rate_ankidroid,
            iconResId = R.drawable.symbol_star,
            analyticsId = LinkAction.OPENED_RATE,
            id = 8,
            action = Rate,
        ),
        HelpItem(
            titleResId = CommonString.help_item_support_other_ankidroid,
            iconResId = R.drawable.symbol_volunteer_activism,
            analyticsId = LinkAction.OPENED_OTHER,
            id = 9,
            action = OpenUrlResource(R.string.link_contribution),
        ),
        HelpItem(
            titleResId = CommonString.send_feedback,
            iconResId = R.drawable.symbol_mail,
            analyticsId = LinkAction.OPENED_SEND_FEEDBACK,
            id = 10,
            action = OpenUrl(AnkiDroidApp.feedbackUrl),
        ),
    )

/** This array contains all the children of the top level menu items from the help menu. */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal val childHelpMenuItems =
    arrayOf(
        HelpItem(
            titleResId = CommonString.help_item_ankidroid_manual,
            iconResId = R.drawable.symbol_menu_book,
            analyticsId = LinkAction.OPENED_ANKIDROID_MANUAL,
            id = 100,
            parentId = 1,
            action = OpenUrl(AnkiDroidApp.manualUrl),
        ),
        HelpItem(
            titleResId = CommonString.help_item_anki_manual,
            iconResId = R.drawable.symbol_menu_book,
            analyticsId = LinkAction.OPENED_ANKI_MANUAL,
            id = 101,
            parentId = 1,
            action = OpenUrlResource(R.string.link_anki_manual),
        ),
        HelpItem(
            titleResId = CommonString.help_item_ankidroid_faq,
            iconResId = CommonR.drawable.symbol_help,
            analyticsId = LinkAction.OPENED_ANKIDROID_FAQ,
            id = 102,
            parentId = 1,
            action = OpenUrlResource(R.string.link_ankidroid_faq),
        ),
        HelpItem(
            titleResId = CommonString.help_item_mailing_list,
            iconResId = R.drawable.symbol_mail,
            analyticsId = LinkAction.OPENED_MAILING_LIST,
            id = 200,
            parentId = 2,
            action = OpenUrlResource(R.string.link_forum),
        ),
        HelpItem(
            titleResId = CommonString.help_item_report_bug,
            iconResId = R.drawable.symbol_bug_report,
            analyticsId = LinkAction.OPENED_REPORT_BUG,
            id = 201,
            parentId = 2,
            action = OpenUrl(AnkiDroidApp.feedbackUrl),
        ),
        HelpItem(
            titleResId = CommonString.help_item_anki_forums,
            iconResId = R.drawable.symbol_forum,
            analyticsId = LinkAction.OPENED_ANKI_FORUMS,
            id = 300,
            parentId = 3,
            action = OpenUrlResource(R.string.link_anki_forum),
        ),
        HelpItem(
            titleResId = CommonString.help_item_mailing_list,
            iconResId = R.drawable.symbol_mail,
            analyticsId = LinkAction.OPENED_MAILING_LIST,
            id = 301,
            parentId = 3,
            action = OpenUrlResource(R.string.link_forum),
        ),
        HelpItem(
            titleResId = CommonString.help_item_reddit,
            iconResId = R.drawable.symbol_link,
            analyticsId = LinkAction.OPENED_REDDIT,
            id = 302,
            parentId = 3,
            action = OpenUrlResource(R.string.link_reddit),
        ),
        HelpItem(
            titleResId = CommonString.help_item_discord,
            iconResId = R.drawable.symbol_link,
            analyticsId = LinkAction.OPENED_DISCORD,
            id = 303,
            parentId = 3,
            action = OpenUrlResource(R.string.link_discord),
        ),
        HelpItem(
            titleResId = CommonString.help_item_facebook,
            iconResId = R.drawable.symbol_link,
            analyticsId = LinkAction.OPENED_FACEBOOK,
            id = 304,
            parentId = 3,
            action = OpenUrlResource(R.string.link_facebook),
        ),
        HelpItem(
            titleResId = CommonString.help_item_twitter,
            iconResId = R.drawable.symbol_link,
            analyticsId = LinkAction.OPENED_TWITTER,
            id = 305,
            parentId = 3,
            action = OpenUrlResource(R.string.link_twitter),
        ),
        HelpItem(
            titleResId = CommonString.help_item_ankidroid_privacy_policy,
            iconResId = R.drawable.symbol_policy,
            analyticsId = LinkAction.OPENED_ANKIDROID_PRIVACY_POLICY,
            id = 400,
            parentId = 4,
            action = OpenUrlResource(R.string.link_ankidroid_privacy_policy),
        ),
        HelpItem(
            titleResId = CommonString.help_item_ankiweb_privacy_policy,
            iconResId = R.drawable.symbol_policy,
            analyticsId = LinkAction.OPENED_ANKIWEB_PRIVACY_POLICY,
            id = 401,
            parentId = 4,
            action = OpenUrlResource(R.string.link_ankiweb_privacy_policy),
        ),
        HelpItem(
            titleResId = CommonString.help_item_ankiweb_terms_and_conditions,
            iconResId = R.drawable.symbol_description,
            analyticsId = LinkAction.OPENED_ANKIWEB_TERMS_AND_CONDITIONS,
            id = 402,
            parentId = 4,
            action = OpenUrlResource(R.string.link_ankiweb_terms_and_conditions),
        ),
    )
