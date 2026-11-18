// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Akshay Jadhav <jadhavakshay0701@gmail.com>

package com.ichi2.anki

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.android.animationEnabled
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.common.android.R as CommonR

class DeckPickerFloatingActionMenu internal constructor(
    private val context: Context,
    homescreenBinding: DeckPickerLayout,
    private val deckPicker: DeckPicker,
    private val toggleListener: FloatingActionBarToggleListener,
    initiallyOpen: Boolean,
) {
    private val binding = homescreenBinding.deckPickerPane.floatingActionButton
    private val deckList = homescreenBinding.deckPickerPane.deckpickerView
    private val studyPane = homescreenBinding.studyoptionsFrame
    private val options = listOf(binding.addSharedButton, binding.addDeckButton, binding.addFilteredDeckButton)
    private var pendingMenuReset = false

    var isFABOpen = false
        private set

    init {
        binding.fabMain.action = { if (isFABOpen) addNote() else showFloatingActionMenu() }
        binding.fabMain.onEscape = {
            val wasOpen = isFABOpen
            if (wasOpen) closeFloatingActionMenu(CloseAnimation.SHORT)
            wasOpen
        }
        binding.fabBGLayout.setOnClickListener { closeFloatingActionMenu(CloseAnimation.SHORT) }
        binding.fabLinearLayout.setOnClickListener { if (isFABOpen) closeFloatingActionMenu(CloseAnimation.SHORT) }
        binding.addSharedButton.action = { selectOption(deckPicker::openAnkiWebSharedDecks) }
        binding.addFilteredDeckButton.action = { selectOption(deckPicker::showCreateFilteredDeckDialog) }
        binding.addDeckButton.action = { selectOption(deckPicker::showCreateDeckDialog) }
        deckPicker.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) = resetMenuAfterAdding()

                override fun onResume(owner: LifecycleOwner) = resetMenuAfterAdding()
            },
        )
        if (initiallyOpen) showFloatingActionMenu()
    }

    fun showFloatingActionMenu() {
        if (pendingMenuReset) return
        deckPicker.activeSnackBar?.dismiss()
        isFABOpen = true
        setMenuBackdrop(true)
        binding.addDeckButton.text = with(context) { TR.sentenceCase.createDeck }
        binding.addDeckButton.isExtended = true
        binding.fabMain.apply {
            text = context.getString(CommonString.menu_add)
            icon = R.drawable.symbol_note_add
            isExtended = true
        }
        for (option in options) {
            option.animate().setListener(null).cancel()
            option.isVisible = true
            option.alpha = 1f
            option.translationY = 0f
        }
    }

    fun closeFloatingActionMenu(
        style: CloseAnimation,
        animate: Boolean = deckPicker.animationEnabled(),
    ) {
        isFABOpen = false
        setMenuBackdrop(false)
        binding.fabMain.apply {
            text = null
            icon = CommonR.drawable.symbol_add
            isExtended = false
        }
        for (option in options) {
            option.animate().cancel()
            if (animate && option.isVisible) {
                option.animate().alpha(0f).translationY(style.offset).setDuration(100).withEndAction {
                    if (!isFABOpen && !pendingMenuReset) option.isVisible = false
                }
            } else {
                option.isVisible = false
            }
        }
    }

    fun showFloatingActionButton() {
        binding.fabMain.isVisible = true
    }

    fun hideFloatingActionButton() {
        if (binding.fabMain.isVisible) closeFloatingActionMenu(CloseAnimation.LONG)
        binding.fabMain.isVisible = false
    }

    private fun addNote() {
        if (pendingMenuReset) return
        pendingMenuReset = true
        isFABOpen = false
        deckPicker.addNote()
    }

    internal fun resetMenuAfterAdding() {
        if (!pendingMenuReset) return
        pendingMenuReset = false
        closeFloatingActionMenu(CloseAnimation.LONG, animate = false)
    }

    private fun selectOption(action: () -> Unit) {
        if (!isFABOpen) return
        closeFloatingActionMenu(CloseAnimation.LONG)
        action()
    }

    private fun setMenuBackdrop(open: Boolean) {
        toggleListener.onBeginToggle(open)
        val alpha = if (open) 0.5f else 1f
        val importance = if (open) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        deckList.alpha = alpha
        deckList.importantForAccessibility = importance
        studyPane?.alpha = alpha
        studyPane?.importantForAccessibility = importance
        binding.fabBGLayout.isVisible = open
    }

    enum class CloseAnimation(
        val offset: Float,
    ) {
        SHORT(300f),
        LONG(400f),
    }

    fun interface FloatingActionBarToggleListener {
        fun onBeginToggle(isOpening: Boolean)
    }
}
