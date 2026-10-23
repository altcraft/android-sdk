@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

/**
 * InAppPlacementsTest
 *
 * Positive scenarios:
 *  - test_1: isRetry() returns true and logs a retry when the processing logic throws.
 *  - test_2: isRetry() rethrows CancellationException.
 *  - test_3: placementsReady() completes placementsReady deferred.
 *
 * Negative scenarios:
 *  - test_4: repeated placementsReady() call does not throw.
 *  - test_5: reset() releases the previous gate and installs a new closed one.
 */
class InAppPlacementsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: isRetry() returns true and logs a retry when the processing logic throws. */
    @Test
    fun test_1_isRetry_returnsTrue_whenLogicThrows() = runTest {
        mockkObject(Environment.Companion)
        mockkObject(Events)
        every { Events.retry(any(), any()) } returns mockk()

        every { Environment.create(any()) } throws RuntimeException("boom")

        assertTrue(PlacementsProcess.isRetry(context))

        verify(exactly = 1) { Events.retry("[altcraft-inapp] isRetry :: inAppPlacements", any()) }
    }

    /** test_2: isRetry() rethrows CancellationException. */
    @Test
    fun test_2_isRetry_rethrowsCancellation() = runTest {
        mockkObject(Environment.Companion)

        every { Environment.create(any()) } throws CancellationException("cancelled")

        try {
            PlacementsProcess.isRetry(context)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            // expected
        }
    }

    /** test_3: placementsReady() completes placementsReady deferred. */
    @Test
    fun test_3_placementsReady_completesDeferred() {
        PlacementsProcess.placementsReady()

        assertTrue(PlacementsProcess.placementsReady.isCompleted)
    }

    /** test_4: repeated placementsReady() call does not throw. */
    @Test
    fun test_4_placementsReady_repeatedCall_doesNotThrow() {
        PlacementsProcess.placementsReady()
        PlacementsProcess.placementsReady()

        assertTrue(PlacementsProcess.placementsReady.isCompleted)
    }

    /** test_5: reset() releases the previous gate and installs a new closed one. */
    @Test
    fun test_5_reset_releasesPreviousGate_andInstallsNew() {
        PlacementsProcess.placementsReady()
        val previous = PlacementsProcess.placementsReady
        assertTrue(previous.isCompleted)

        PlacementsProcess.reset()

        assertTrue(previous.isCompleted)
        assertFalse(PlacementsProcess.placementsReady.isCompleted)

        // Restore the open gate for subsequent tests.
        PlacementsProcess.placementsReady()
    }
}
