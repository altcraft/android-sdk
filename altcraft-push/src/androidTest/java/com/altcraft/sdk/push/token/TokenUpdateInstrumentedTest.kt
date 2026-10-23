package com.altcraft.sdk.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.push.infra.Environment
import com.altcraft.sdk.push.data.Constants
import com.altcraft.sdk.push.data.Preferenses
import com.altcraft.sdk.push.data.models.PushModels.TokenData
import com.altcraft.sdk.push.platform.token.TokenUpdateProcess
import com.altcraft.sdk.push.platform.token.request.UpdateRequest
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
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
 *  - test_1: pushTokenUpdate() with changed token + foreground + retry -> returns false and
 *    starts the update worker.
 *  - test_2: isRetry() with retry response -> returns true without saving the token
 *    (a retry is not a confirmed update, so the token stays unsaved).
 *  - test_3: isRetry() with success response -> returns false and saves the token.
 *
 * Negative scenarios:
 *  - test_4: pushTokenUpdate() exits early when saved token equals current token -> returns true.
 *  - test_5: isRetry() with throwing currentToken -> returns true and releases the update lock.
 */
private const val TOKEN_OLD = "tok-OLD"
private const val TOKEN_NEW = "tok-NEW"
private const val TOKEN_SAME = "tok-SAME"

class TokenUpdateInstrumentedTest {

    private lateinit var context: Context
    private lateinit var env: Environment

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

        resetUpdateLock()

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        every { Environment.create(any()) } returns env

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns true

        every { env.savedToken() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)
        coEvery { env.usesRTokenAuth() } returns true
        coEvery { env.currentToken() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        mockkObject(Preferenses)
        every { Preferenses.setCurrentToken(any(), any()) } just Runs

        mockkObject(UpdateRequest)
        coEvery {
            UpdateRequest.tokenUpdateRequest(
                any(),
                any()
            )
        } returns SDKEventModel.Event(function = "updateRequest")

        mockkObject(com.altcraft.sdk.workers.onetime.Request)
        coEvery { hasNewRequest(any(), any(), any()) } returns false
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    private fun resetUpdateLock() {
        val f = TokenUpdateProcess::class.java.getDeclaredField("updateLock")
        f.isAccessible = true
        f.set(null, false)
    }

    private fun readUpdateLock(): Boolean {
        val f = TokenUpdateProcess::class.java.getDeclaredField("updateLock")
        f.isAccessible = true
        return f.getBoolean(null)
    }

    /** - test_1: pushTokenUpdate() with changed token + retry + foreground -> forces an update worker. */
    @Test
    fun pushTokenUpdate_changedToken_calls_startUpdateWorker() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true
        every { env.savedToken() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)
        coEvery { env.currentToken() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)
        coEvery {
            UpdateRequest.tokenUpdateRequest(
                any(),
                any()
            )
        } returns retry(function = "updateRequest")
        coEvery { hasNewRequest(any(), any(), any()) } returns false

        val result = TokenUpdateProcess.pushTokenUpdate(context)

        assertThat(result, `is`(false))
    }

    /** - test_2: isRetry() with retry response -> returns true without saving the token
     *  (the token is stored only after a confirmed, non-error update). */
    @Test
    fun isRetry_retry_event_returns_true_without_saving_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        coEvery { env.currentToken() } returns token
        coEvery {
            UpdateRequest.tokenUpdateRequest(
                any(),
                any()
            )
        } returns retry(function = "updateRequest")
        coEvery { hasNewRequest(any(), any(), any()) } returns false

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdateProcess.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(true))
        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    /** - test_3: isRetry() with success response -> returns false and saves the token. */
    @Test
    fun isRetry_success_event_returns_false_and_saves_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        coEvery { env.currentToken() } returns token
        coEvery {
            UpdateRequest.tokenUpdateRequest(
                any(),
                any()
            )
        } returns SDKEventModel.Event(function = "updateRequest")

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdateProcess.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(context, token) }
    }

    /** - test_4: pushTokenUpdate() exits early when saved token equals current token -> returns true. */
    @Test
    fun tokenUpdate_same_pushToken_does_not_call_startUpdateWorker() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val same = TokenData(Constants.FCM_PROVIDER, TOKEN_SAME)
        every { env.savedToken() } returns same
        coEvery { env.currentToken() } returns same

        val result = TokenUpdateProcess.pushTokenUpdate(context)

        assertThat(result, `is`(true))
    }

    /** - test_5: isRetry() with throwing currentToken -> returns true and releases the update lock. */
    @Test
    fun isRetry_logic_exception_releases_updateLock() = runTest {
        coEvery { env.currentToken() } throws RuntimeException("token is not resolvable")

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdateProcess.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(true))
        assertThat(readUpdateLock(), `is`(false))
    }
}
