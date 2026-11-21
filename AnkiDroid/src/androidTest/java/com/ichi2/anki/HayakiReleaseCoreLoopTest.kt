// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.AnkiDroidFolder
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import com.ichi2.anki.startup.resetAnkiDroidDirectory
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission.storagePermission
import com.ichi2.anki.testutil.awaitDisplayed
import com.ichi2.anki.testutil.awaitJavascript
import com.ichi2.anki.testutil.disableIntroductionSlide
import com.ichi2.anki.testutil.ensureWebViewIsSupported
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.testutil.notificationPermission
import com.ichi2.anki.ui.internationalization.sentenceCase
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import com.ichi2.anki.common.android.R as CommonR

/** The same fresh collection and ratings as iOS StudyUITests. Run via the release device workflow. */
@RunWith(AndroidJUnit4::class)
class HayakiReleaseCoreLoopTest : InstrumentedTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val runtimePermissionRule = grantPermissions(storagePermission, notificationPermission)

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private lateinit var app: Context
    private val reveal get() = string(CommonR.string.show_answer)

    @Test
    fun studyAllFinishesGeographyAndAllowsLeavingSpanishWithWorkRemaining() {
        val args = InstrumentationRegistry.getArguments()
        // The dedicated invocation gets a fresh instrumentation collection and fresh app preferences.
        assumeTrue(args.getString("hayakiReleaseDevice") == "true")
        ensureWebViewIsSupported()
        val collectionDirectory = getDefaultAnkiDroidDirectory(testContext, folder = AnkiDroidFolder.APP_PRIVATE)
        resetAnkiDroidDirectory(testContext, collectionDirectory)
        importFixture()
        disableIntroductionSlide()
        val preferences = testContext.sharedPrefs()
        InitialActivity.setUpgradedToLatestVersion(preferences)
        preferences.edit { putBoolean("backupPromptDisabled", true) }

        ActivityScenario.launch(DeckPicker::class.java).use { scenario ->
            scenario.onActivity { activity ->
                app = activity
                val config = app.resources.configuration
                val expectedLocale = Locale.forLanguageTag(checkNotNull(args.getString("hayakiLocale")))
                assertEquals(expectedLocale.language, config.locales[0].language)
                val expectedFontScale = checkNotNull(args.getString("hayakiFontScale")).toFloat()
                assertEquals(expectedFontScale, config.fontScale)
                assertEquals(expectedFontScale == 2f, config.smallestScreenWidthDp >= 600)
                val direction = if (expectedLocale.language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                assertEquals(direction, config.layoutDirection)
            }

            assertDailySummary(left = 9, reviewed = 0, new = 5, learning = 2, due = 2)
            capture("01-decks")
            visible(string(CommonR.string.deck_list_study_all)).performClick()

            visible("Geography")
            visible(reveal)
            val webView = cardWebView()
            webView.awaitJavascript("document.querySelector('#qa .cloze')?.textContent.includes('[...]') === true")
            capture("02-question")

            repeat(3) { index ->
                visible(reveal).performClick()
                compose.mainClock.advanceTimeBy(Prefs.doubleTapInterval.toLong())
                if (index == 0) {
                    webView.awaitJavascript(
                        "Boolean(document.querySelector('#qa .cloze')?.textContent) && !document.querySelector('#qa .cloze').textContent.includes('[...]')",
                    )
                    listOf(
                        CommonR.string.ease_button_again,
                        CommonR.string.ease_button_hard,
                        CommonR.string.ease_button_good,
                        CommonR.string.ease_button_easy,
                    ).forEach { rating(it).assertIsDisplayed() }
                    capture("03-answer")
                }
                rating(CommonR.string.ease_button_easy).performClick()
                compose.mainClock.advanceTimeBy(Prefs.doubleTapInterval.toLong())
            }

            val done = string(CommonR.string.finished_done)
            awaitVisible(hasText(string(CommonR.string.finished_reviewed)) and hasText(number(3)))
            visible(string(CommonR.string.finished_time_spent))
            visible(string(CommonR.string.finished_rating_count, string(CommonR.string.ease_button_easy), 3))
            visible(string(CommonR.string.finished_rating_count, string(CommonR.string.ease_button_again), 0))
            visible(string(CommonR.string.finished_rating_count, string(CommonR.string.ease_button_hard), 0))
            visible(string(CommonR.string.finished_rating_count, string(CommonR.string.ease_button_good), 0))
            capture("04-finished-summary")
            val finished = string(CommonR.string.finished_title_done)
            scrollIntoView(finished)
            scrollIntoView(with(app) { TR.sentenceCase.customStudy })
            scrollIntoView(with(app) { TR.sentenceCase.deckOptions })
            scrollIntoView(done)
            capture("05-finished-actions")
            compose.onNodeWithText(done).performClick()

            visible(reveal)
            visible("Spanish", substring = true)
            capture("06-next-deck")
            val back = instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK")
            AutoCloseInputStream(back).use { it.readBytes() }

            assertDailySummary(left = 6, reviewed = 3, new = 3, learning = 2, due = 1)
            visible(string(CommonR.string.deck_list_study_all))
            val geographyRow = hasTestTag("${testContext.packageName}:id/deck_name") and hasText("Geography")
            awaitVisible(hasAnyAncestor(geographyRow) and hasText(finished))
            capture("07-decks-after-exit")
        }
    }

    private fun assertDailySummary(
        left: Int,
        reviewed: Int,
        new: Int,
        learning: Int,
        due: Int,
    ) {
        visible(app.resources.getQuantityString(CommonR.plurals.deck_list_due_today, left, left))
        visible(app.resources.getQuantityString(CommonR.plurals.deck_list_studied_today, reviewed, reviewed))
        visible(string(CommonR.string.deck_list_count_description, TR.actionsNew(), new))
        visible(string(CommonR.string.deck_list_count_description, TR.schedulingLearning(), learning))
        visible(string(CommonR.string.deck_list_count_description, TR.decksReviewHeader(), due))
    }

    private fun importFixture() {
        val temporaryPackage = File(testContext.cacheDir, "hayaki-release.colpkg")
        try {
            instrumentation.context.assets.open("hayaki-release/collection.colpkg").use { source ->
                temporaryPackage.outputStream().use { source.copyTo(it) }
            }
            runBlocking { CollectionManager.importColpkg(temporaryPackage.path) }
        } finally {
            temporaryPackage.delete()
        }
    }

    private fun cardWebView(): WebView {
        lateinit var webView: WebView
        onView(withId(R.id.web_view_layout)).check { view, error ->
            if (error != null) throw error
            webView = (view as ViewGroup).getChildAt(0) as WebView
        }
        return webView
    }

    private fun scrollIntoView(text: String) {
        val node = compose.onNodeWithText(text)
        if (!node.isDisplayed()) node.performScrollTo()
        node.assertIsDisplayed()
    }

    private fun capture(name: String) {
        // Semantics can be ready while the Activity's first surface is still being drawn.
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        device.waitForIdle()
        val directory = File(checkNotNull(testContext.getExternalFilesDir(null)), "hayaki-release-evidence")
        assertTrue(directory.isDirectory || directory.mkdirs())
        assertTrue(device.takeScreenshot(File(directory, "$name.png")), "Capture $name")
    }

    private fun visible(
        text: String,
        substring: Boolean = false,
    ) = awaitVisible(hasText(text, substring = substring))

    private fun awaitVisible(matcher: SemanticsMatcher) = compose.awaitDisplayed(timeout = 45.seconds) { onNode(matcher) }

    private fun rating(id: Int) = visible("\n${string(id)}", substring = true)

    private fun string(
        id: Int,
        vararg args: Any,
    ) = app.getString(id, *args)

    private fun number(value: Int): String = NumberFormat.getIntegerInstance(app.resources.configuration.locales[0]).format(value)
}
