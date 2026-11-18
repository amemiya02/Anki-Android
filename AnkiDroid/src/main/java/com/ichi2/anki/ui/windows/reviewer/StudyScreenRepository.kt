// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.annotation.WorkerThread
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.MetaDB
import com.ichi2.anki.common.android.appContext
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.common.utils.ext.cardStateCustomizer
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.preferences.reviewer.MenuDisplayType
import com.ichi2.anki.preferences.reviewer.ReviewerMenuRepository
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.PrefsRepository
import com.ichi2.anki.settings.enums.ToolbarPosition
import com.ichi2.anki.utils.CollectionPreferences

class StudyScreenRepository(
    private val prefs: PrefsRepository = Prefs,
) {
    val isMarkShownInToolbar: Boolean
    val isFlagShownInToolbar: Boolean

    /** Whether the whiteboard was on, for every Deck, before it was remembered for each Deck */
    var isWhiteboardEnabled by prefs.booleanPref(PREF_WHITEBOARD_ENABLED, false)

    /** Whether the voice recorder was on, for every Deck, before it was remembered for each Deck */
    var isRecordVoiceEnabled by prefs.booleanPref(PREF_RECORD_VOICE_ENABLED, false)
    val isHtmlTypeAnswerEnabled get() = prefs.isHtmlTypeAnswerEnabled

    init {
        val actions =
            ReviewerMenuRepository(prefs.sharedPrefs)
                .getActionsByMenuDisplayTypes(
                    MenuDisplayType.ALWAYS,
                ).getValue(MenuDisplayType.ALWAYS)
        val isToolbarShown = prefs.toolbarPosition != ToolbarPosition.NONE
        isMarkShownInToolbar = isToolbarShown && ViewerAction.MARK in actions
        isFlagShownInToolbar = isToolbarShown && ViewerAction.FLAG_MENU in actions
    }

    /** Whether the whiteboard is on when studying [did]; a Deck never set follows [isWhiteboardEnabled] */
    @WorkerThread
    fun isWhiteboardEnabled(did: DeckId): Boolean =
        if (MetaDB.hasWhiteboardState(appContext, did)) MetaDB.getWhiteboardState(appContext, did) else isWhiteboardEnabled

    @WorkerThread
    fun setWhiteboardEnabled(
        did: DeckId,
        isEnabled: Boolean,
    ) = MetaDB.storeWhiteboardState(appContext, did, isEnabled)

    /** Whether the voice recorder shows when studying [did]; a Deck never set follows [isRecordVoiceEnabled] */
    @WorkerThread
    fun isRecordVoiceEnabled(did: DeckId): Boolean =
        if (MetaDB.hasMicToolbarState(appContext, did)) MetaDB.getMicToolbarState(appContext, did) else isRecordVoiceEnabled

    @WorkerThread
    fun setRecordVoiceEnabled(
        did: DeckId,
        isEnabled: Boolean,
    ) = MetaDB.storeMicToolbarState(appContext, did, isEnabled)

    fun generateStateMutationKey(): String = TimeManager.time.intTimeMS().toString()

    suspend fun getCustomSchedulingJs(): String = CollectionManager.withCol { config.cardStateCustomizer }

    suspend fun getShouldShowNextTimes(): Boolean = CollectionPreferences.getShowIntervalOnButtons()

    companion object {
        private const val PREF_WHITEBOARD_ENABLED = "whiteboardEnabled"
        private const val PREF_RECORD_VOICE_ENABLED = "recordVoiceEnabled"
    }
}
