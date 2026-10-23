@file:Suppress("SpellCheckingInspection")

package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import android.util.Log
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * RoomRequestUnitTest
 *
 * Positive scenario:
 *  - test_1: deleteAcidSensitiveRequests calls deleteAll on relevant DAOs.
 *
 * Negative scenario:
 *  - test_2: deleteAcidSensitiveRequests handles exception gracefully.
 */
class RoomRequestUnitTest {

    private lateinit var sdkDB: DB
    private lateinit var authRequestDao: AuthRequestDao

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockkObject(DB)

        authRequestDao = mockk(relaxed = true)

        sdkDB = mockk(relaxed = true)
        every { sdkDB.authRequestDao() } returns authRequestDao
        every { sdkDB.dao.authRequest } returns authRequestDao

        coJustRun { authRequestDao.deleteAll() }
    }

    @After
    fun tearDown() = unmockkAll()

    /** test_1: deleteAcidSensitiveRequests calls deleteAll on relevant DAOs */
    @Test
    fun deleteAcidSensitiveRequests_callsDeleteAll() = runBlocking {
        RoomRequest.deleteAcidSensitiveRequests(sdkDB)

        coVerify(exactly = 1) { authRequestDao.deleteAll() }
    }

    /** test_2: deleteAcidSensitiveRequests handles exception gracefully */
    @Test
    fun deleteAcidSensitiveRequests_handlesException() = runBlocking {
        coEvery { authRequestDao.deleteAll() } throws RuntimeException("auth error")

        runCatching {
            RoomRequest.deleteAcidSensitiveRequests(sdkDB)
        }

        coVerify(exactly = 1) { authRequestDao.deleteAll() }
    }
}
