// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.NavigationDrawerActivity.Companion.EXTRA_STARTED_WITH_SHORTCUT
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * @see PreviewerFragment
 * @see TemplatePreviewerFragment
 */
class CardViewerActivity : SingleFragmentActivity() {
    override fun shortcutEntryIntent(): Intent? {
        if (intent.action != Intent.ACTION_VIEW ||
            intent.getStringExtra(EXTRA_FRAGMENT_NAME) != ReviewerFragment::class.java.name ||
            !intent.getBooleanExtra(EXTRA_STARTED_WITH_SHORTCUT, false)
        ) {
            return null
        }
        return DeckPicker.getEntryIntent(this, intent, resetTask = true)
    }

    companion object {
        fun getIntent(
            context: Context,
            fragmentClass: KClass<out Fragment>,
            arguments: Bundle? = null,
        ): Intent =
            Intent(context, CardViewerActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_NAME, fragmentClass.jvmName)
                putExtra(EXTRA_FRAGMENT_ARGS, arguments)
            }
    }
}
