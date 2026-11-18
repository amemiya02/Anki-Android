// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StudyOptionsFinishingTest : RobolectricTest() {
    @Test
    fun `study options refresh queued before finishing exposes lifecycle misuse`() =
        assertLifecycleFailure("activity is finishing") {
            addBasicNote()
            val controller = Robolectric.buildActivity(StudyOptionsActivity::class.java).setup()
            saveControllerForCleanup(controller)
            advanceUntilIdle()
            advanceRobolectricLooper()
            val activity = controller.get()
            val fragment = assertIs<StudyOptionsFragment>(activity.supportFragmentManager.findFragmentById(R.id.studyoptions_frame))
            val initialState = assertIs<StudyOptionsState.StudyOptions>(fragment.viewModel.state)
            assertEquals(1, initialState.data.newCardsToday)
            addBasicNote()

            activity.lifecycleScope
                .launch {
                    fragment.refreshInterface()
                    activity.finish()
                }.join()
            advanceUntilIdle()
            advanceRobolectricLooper()

            assertTrue(activity.isFinishing)
            assertSame(activity, fragment.activity)
            assertSame(initialState, fragment.viewModel.state)
        }

    private fun assertLifecycleFailure(
        message: String,
        block: suspend TestScope.() -> Unit,
    ) {
        val failure = assertFailsWith<IllegalStateException> { runTest(testBody = block) }
        assertContains(failure.message.orEmpty(), message)
    }
}
