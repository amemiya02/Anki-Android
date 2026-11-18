// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Arthur Milchior <arthur@milchior.fr>

package com.ichi2.anki.testutil

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.testutil.ThreadUtils.sleep
import com.ichi2.anki.common.android.R as CommonR

/**
 * This file contains utility methods to interact with the DeckPicker.
 */

fun closeGetStartedScreenIfExists() {
    onView(withId(R.id.get_started)).withFailureHandler { _, _ -> }.perform(click())
}

fun closeBackupCollectionDialogIfExists() {
    onView(withText(CommonString.button_backup_later))
        .withFailureHandler { _, _ -> }
        .perform(click())
}

/**
 * Discard the Get Started and the Backup Collection dialog if they exists
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
    onView(withId(R.id.fab_main)).perform(click())
    onView(withId(R.id.add_deck_button)).perform(click())
    onView(withId(CommonR.id.dialog_text_input)).perform(typeText(deckName))
    onView(withText(CommonString.dialog_ok)).perform(click())
    return deckName
}

/** Taps the counts of the Deck named [deckName], which open its study options */
fun tapOnCountLayouts(deckName: String) {
    // The deck list is drawn by Compose: its row reads as the Deck's name, and the counts are the
    // row's one clickable child
    val row = checkNotNull(device.wait(Until.findObject(By.text(deckName)), DECK_TIMEOUT_MS)) { "Deck '$deckName' not shown" }
    checkNotNull(row.findObject(By.clickable(true))) { "Deck '$deckName' has no counts" }.click()

    // without this sleep, the study options fragment sometimes loses the "load and become active" race vs the assertion below.
    // It actually won the race sometimes so sleeping a full second is generous. This should be quite stable
    sleep(1000)
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
