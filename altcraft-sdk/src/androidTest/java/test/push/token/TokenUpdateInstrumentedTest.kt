package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.core.Environment
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.data.retry
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Executors

/**
 * TokenUpdateInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: pushTokenUpdate() with changed token + foreground + retry -> startUpdateWorker called.
 *  - test_2: isRetry() with retry response -> returns true and does not call setCurrentToken().
 *  - test_3: isRetry() with success response -> returns false and calls setCurrentToken().
 *
 * Negative scenarios:
 *  - test_4: pushTokenUpdate() exits early when saved token equals current token -> no startUpdateWorker call.
 */
private const val TOKEN_OLD = "tok-OLD"
private const val TOKEN_NEW = "tok-NEW"
private const val TOKEN_SAME = "tok-SAME"

private const val RTOKEN = "user-tag-123"

private const val API_URL = "https://api.example.com"

class TokenUpdateInstrumentedTest {

    private lateinit var context: Context
    private lateinit var env: Environment
    private lateinit var configuration: ConfigurationEntity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)
        WorkManager.getInstance(context).cancelAllWork().result.get()

        MockKAnnotations.init(this, relaxUnitFun = true)

        configuration = ConfigurationEntity(
            id = 0,
            icon = null,
            apiUrl = API_URL,
            rToken = RTOKEN,
            appInfo = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        every { Environment.create(any()) } returns env

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns true

        every { env.savedToken } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)
        coEvery { env.config() } returns configuration
        coEvery { env.token() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        mockkObject(Preferenses)
        every { Preferenses.setCurrentToken(any(), any()) } just Runs

        mockkObject(Request)
        coEvery { Request.tokenUpdateRequest(any(), any()) } returns DataClasses.Event(function = "updateRequest")

        mockkObject(com.altcraft.sdk.workers.coroutine.Request)
        coEvery { hasNewRequest(any(), any(), any()) } returns false

        mockkObject(LaunchFunctions)
        coJustRun { LaunchFunctions.startTokenUpdateCoroutineWorker(any()) }
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    /** - test_1: pushTokenUpdate() with changed token + foreground + retry -> startUpdateWorker called. */
    @Test
    fun pushTokenUpdate_changedToken_calls_startUpdateWorker() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true
        every { env.savedToken } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)
        coEvery { env.token() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)
        coEvery { Request.tokenUpdateRequest(any(), any()) } returns retry(function = "updateRequest")
        coEvery { hasNewRequest(any(), any(), any()) } returns false

        TokenUpdate.pushTokenUpdate(context)

        coVerify(exactly = 1) { LaunchFunctions.startTokenUpdateCoroutineWorker(context) }
    }

    /** - test_2: isRetry() with retry response -> returns true and does not call setCurrentToken(). */
    @Test
    fun isRetry_retry_event_returns_true_and_does_not_set_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        coEvery { env.token() } returns token
        coEvery { Request.tokenUpdateRequest(any(), any()) } returns retry(function = "updateRequest")
        coEvery { hasNewRequest(any(), any(), any()) } returns false

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdate.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(true))
        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    /** - test_3: isRetry() with success response -> returns false and calls setCurrentToken(). */
    @Test
    fun isRetry_success_event_returns_false_and_sets_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        coEvery { env.token() } returns token
        coEvery { Request.tokenUpdateRequest(any(), any()) } returns DataClasses.Event(function = "updateRequest")

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdate.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(context, token) }
    }

    /** - test_4: pushTokenUpdate() exits early when saved token equals current token -> no startUpdateWorker call. */
    @Test
    fun tokenUpdate_same_pushToken_does_not_call_startUpdateWorker() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val same = TokenData(Constants.FCM_PROVIDER, TOKEN_SAME)
        every { env.savedToken } returns same
        coEvery { env.token() } returns same

        TokenUpdate.pushTokenUpdate(context)

        coVerify(exactly = 0) { LaunchFunctions.startTokenUpdateCoroutineWorker(any()) }
    }
}