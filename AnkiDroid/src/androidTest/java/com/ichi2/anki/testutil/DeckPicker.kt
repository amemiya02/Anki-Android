// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Arthur Milchior <arthur@milchior.fr>

package com.ichi2.anki.testutil

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.ui.internationalization.sentenceCase

/**
 * This file contains utility methods to interact with the DeckPicker.
 */

fun closeGetStartedScreenIfExists() {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    device.findObject(By.res(packageName, "start_with_no_decks"))?.click()
}

fun closeBackupCollectionDialogIfExists() {
    val later = InstrumentationRegistry.getInstrumentation().targetContext.getString(CommonString.button_backup_later)
    device.findObject(By.text(later))?.click()
}

/**
 * Dismiss the welcome and backup reminder if present.
 */
fun discardPreliminaryViews() {
    closeGetStartedScreenIfExists()
    closeBackupCollectionDialogIfExists()
}

/**
 * Create a deck with a unique name (based on the timestamp) return the name.
 * Must be called from a clean deck picker.
 */
fun createDeckWithUniqueName(): String {
    val testString = System.currentTimeMillis().toString()
    val deckName = "TestDeck$testString"
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    checkNotNull(device.wait(Until.findObject(By.desc(context.getString(CommonString.menu_add))), DECK_TIMEOUT_MS)).click()
    checkNotNull(device.wait(Until.findObject(By.text(with(context) { TR.sentenceCase.createDeck })), DECK_TIMEOUT_MS)).click()
    val name = checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), DECK_TIMEOUT_MS))
    name.text = deckName
    val create = InstrumentationRegistry.getInstrumentation().targetContext.getString(CommonString.dialog_positive_create)
    checkNotNull(device.wait(Until.findObject(By.text(create)), DECK_TIMEOUT_MS)).click()
    return deckName
}

/**
 * This must be called on a clean deck picker.
 */
fun clickOnDeckWithName(deckName: String) {
    checkNotNull(device.wait(Until.findObject(By.text(deckName)), DECK_TIMEOUT_MS)) { "Deck '$deckName' not shown" }.click()
}

private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

private const val DECK_TIMEOUT_MS = 10_000L

/**
 * This must be called on a clean deck picker. It taps on the deck, and if needed on the "study" button.
 */
fun reviewDeckWithName(deckName: String) {
    clickOnDeckWithName(deckName)
    // Adding cards directly to the database while in the Deck Picker screen
    // will not update the page with correct card counts. Hence, clicking
    // on the deck will bring us to the study options page where we need to
    // click on the Study button. If we have added cards to the database
    // before the Deck Picker screen has fully loaded, then we skip clicking
    // the Study button
    clickOnStudyButtonIfExists()
}
