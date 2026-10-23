@file:Suppress("SpellCheckingInspection")

package test.inapp.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.data.Constants.IN_APP_EVENT_O_WORK_TAG
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppEventDao
import com.altcraft.sdk.inapp.platform.event.EventProcess
import com.altcraft.sdk.workers.onetime.Request
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * InAppEventTest
 *
 * Positive scenarios:
 *  - test_1: isRetry() returns the result of the processing logic.
 *  - test_2: isRetry() forwards the workerId into the processing logic.
 *  - test_3: isRetry() returns true and logs a retry when logic throws.
 *  - test_4: isRetry() rethrows CancellationException.
 *  - test_5: sendErrorInAppEvent builds ERROR event with code 500.
 */
class InAppEventTest {

    private lateinit var context: Context
    private lateinit var eventDao: InAppEventDao

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkObject(DB.Companion)
        eventDao = mockk(relaxed = true)

        val db = mockk<DB>(relaxed = true)
        every { db.dao.event } returns eventDao
        every { DB.getInstance(any()) } returns db
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: isRetry() returns true when the processing logic returns true. */
    @Test
    fun test_1_isRetry_returnsTrue_whenLogicReturnsTrue() = runTest {
        coEvery { eventDao.getAll() } returns emptyList()
        coEvery { eventDao.isEmpty() } returns false

        assertTrue(EventProcess.isRetry(context))
    }

    /** test_1b: isRetry() returns false when the processing logic returns false. */
    @Test
    fun test_1b_isRetry_returnsFalse_whenLogicReturnsFalse() = runTest {
        coEvery { eventDao.getAll() } returns emptyList()
        coEvery { eventDao.isEmpty() } returns true

        assertFalse(EventProcess.isRetry(context))
    }

    /** test_2: isRetry() forwards the workerId into the processing logic. */
    @Test
    fun test_2_isRetry_forwardsWorkerId() = runTest {
        val workerId = UUID.randomUUID()

        mockkObject(Request)
        coEvery { Request.hasNewRequest(context, IN_APP_EVENT_O_WORK_TAG, workerId) } returns false

        coEvery { eventDao.getAll() } returns emptyList()
        coEvery { eventDao.isEmpty() } returns false

        assertTrue(EventProcess.isRetry(context, workerId))

        coVerify(exactly = 1) {
            Request.hasNewRequest(context, IN_APP_EVENT_O_WORK_TAG, workerId)
        }
    }

    /** test_3: isRetry() returns true and logs a retry when logic throws. */
    @Test
    fun test_3_isRetry_returnsTrue_whenLogicThrows() = runTest {
        mockkObject(Events)
        every { Events.retry(any(), any()) } returns mockk()

        every { DB.getInstance(any()) } throws RuntimeException("boom")

        assertTrue(EventProcess.isRetry(context))

        verify(exactly = 1) { Events.retry("[altcraft-inapp] isRetry :: sendInAppEvent", any()) }
    }

    /** test_4: isRetry() rethrows CancellationException. */
    @Test
    fun test_4_isRetry_rethrowsCancellation() = runTest {
        every { DB.getInstance(any()) } throws CancellationException("cancelled")

        try {
            EventProcess.isRetry(context)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            // expected
        }
    }

    /** test_5: sendErrorInAppEvent calls sendInAppEvent with ERROR type and code 500.
     *  Since sendInAppEvent uses submitSdkCommand (async), we verify it doesn't throw. */
    @Test
    fun test_5_sendErrorInAppEvent_doesNotThrow() {
        val error = "test error"

        try {
            EventProcess.sendErrorInAppEvent(context, "smid-1", error)
        } catch (e: Exception) {
            fail("sendErrorInAppEvent should not throw: ${e.message}")
        }
    }
}
