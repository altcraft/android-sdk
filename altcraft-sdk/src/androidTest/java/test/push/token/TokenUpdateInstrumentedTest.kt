package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
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
import com.altcraft.sdk.workers.coroutine.Request.hasNewRequest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Executors

/**
 * TokenUpdateInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: pushTokenUpdate() in foreground with changed token enqueues work with
 *    UPDATE_C_WORK_TAG.
 *  - test_2: isRetry() with retry response returns true and DOES NOT call setCurrentToken().
 *  - test_3: isRetry() with success Event returns false and calls setCurrentToken()
 *    with the current token.
 *
 * Negative scenario:
 *  - test_4: pushTokenUpdate() exits early and does not enqueue any work when saved
 *    token equals the current token.
 *
 * Notes:
 *  - WorkManager test environment is initialized; enqueued work is asserted via tag.
 *  - Environment is mocked through its companion object.
 *  - Suspend methods (token(), config(), tokenUpdateRequest(), hasNewRequest()) are
 *    mocked with coEvery.
 */
@RunWith(AndroidJUnit4::class)
class TokenUpdateInstrumentedTest {

    private lateinit var context: Context
    private lateinit var env: Environment
    private lateinit var configuration: ConfigurationEntity

    private companion object {
        private const val TOKEN_OLD = "tok-OLD"
        private const val TOKEN_NEW = "tok-NEW"
        private const val TOKEN_SAME = "tok-SAME"
    }

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
            apiUrl = "https://api.example.com",
            rToken = "user-tag-123",
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        every { Environment.create(any()) } returns env

        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns true

        every { env.savedToken } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)

        coEvery { env.config() } returns configuration
        coEvery { env.token() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        mockkObject(Preferenses)
        every { Preferenses.setCurrentToken(any(), any()) } just runs

        mockkObject(Request)
        coEvery {
            Request.tokenUpdateRequest(any(), any())
        } returns DataClasses.Event(function = "updateRequest")

        mockkObject(com.altcraft.sdk.workers.coroutine.Request)
        coEvery {
            hasNewRequest(any(), any(), any())
        } returns false
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    /**
     * test_1:
     * Foreground + changed token -> pushTokenUpdate() should enqueue a work
     * with UPDATE_C_WORK_TAG.
     */
    @Test
    fun pushTokenUpdate_foreground_enqueues_update_work() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        every { env.savedToken } returns TokenData(Constants.FCM_PROVIDER, TOKEN_OLD)
        coEvery { env.token() } returns TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)

        TokenUpdate.pushTokenUpdate(context)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.UPDATE_C_WORK_TAG)
            .get()

        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_2:
     * isRetry() -> retry response:
     *  - returns true when there is no newer work.
     *  - DOES NOT call setCurrentToken().
     */
    @Test
    fun isRetry_retry_event_returns_true_and_does_not_set_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)
        coEvery { env.token() } returns token

        coEvery {
            Request.tokenUpdateRequest(any(), any())
        } returns retry(function = "updateRequest")

        coEvery {
            hasNewRequest(any(), any(), any())
        } returns false

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdate.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(true))
        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    /**
     * test_3:
     * isRetry() -> success Event:
     *  - returns false.
     *  - calls setCurrentToken() with the current token.
     */
    @Test
    fun isRetry_success_event_returns_false_and_sets_token() = runTest {
        val token = TokenData(Constants.FCM_PROVIDER, TOKEN_NEW)
        coEvery { env.token() } returns token

        coEvery {
            Request.tokenUpdateRequest(any(), any())
        } returns DataClasses.Event(function = "updateRequest")

        val workerId = UUID.randomUUID()
        val shouldRetry = TokenUpdate.isRetry(context, workerId)

        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(context, token) }
    }

    /**
     * test_4:
     * savedToken equals current token ->
     * pushTokenUpdate() exits early and no work with UPDATE_C_WORK_TAG is enqueued.
     */
    @Test
    fun tokenUpdate_same_pushToken_does_not_enqueue_work() = runTest {
        every { SubFunction.isAppInForegrounded() } returns true

        val same = TokenData(Constants.FCM_PROVIDER, TOKEN_SAME)
        every { env.savedToken } returns same
        coEvery { env.token() } returns same

        TokenUpdate.pushTokenUpdate(context)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.UPDATE_C_WORK_TAG)
            .get()

        assertThat(infos.isEmpty(), `is`(true))
    }
}