package test.push.token

//  Created by Andrey Pogodin.
//
//  Copyright © 2024 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.Executors

// SDK imports
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.DataClasses.TokenData
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.push.token.TokenManager
import com.altcraft.sdk.data.Preferenses
import com.altcraft.sdk.push.token.TokenUpdate
import com.altcraft.sdk.data.room.ConfigurationEntity

/**
 * TokenUpdateInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: tokenUpdate when app is foregrounded and token changed enqueues UpdateCoroutineWorker (UPDATE_C_WORK_TAG).
 *  - test_2: isRetry returns true when Request.updateRequest returns RetryError and setCurrentToken is NOT called.
 *  - test_3: isRetry returns false when Request.updateRequest returns success Event and setCurrentToken is called.
 *
 * Negative scenario:
 *  - test_4: tokenUpdate does nothing (no work enqueued) when saved token equals current token.
 *
 * Notes:
 *  - WorkManager test environment is initialized; enqueued work is asserted via tag.
 *  - All singleton objects are mocked via mockkObject(…).
 *  - suspend functions are mocked with coEvery/coVerify.
 *  - Token values are TokenData(provider, token), not plain String.
 */
@RunWith(AndroidJUnit4::class)
class TokenUpdateInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // WorkManager test env (uses a single-thread executor for determinism)
        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, wmConfig)
        WorkManager.getInstance(context).cancelAllWork().result.get()

        MockKAnnotations.init(this, relaxUnitFun = true)

        // --- ConfigSetup.getConfig: usingService=false -> LaunchFunctions.startUpdateCoroutineWorker path
        mockkObject(ConfigSetup)
        coEvery { ConfigSetup.getConfig(any()) } returns ConfigurationEntity(
            id = 0,
            icon = null,
            apiUrl = "https://api.example.com",
            rToken = "user-tag-123",
            appInfo = null,
            usingService = false, // force CoroutineWorker instead of foreground Service
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = null,
            pushChannelDescription = null
        )

        // --- Foreground/background switch
        mockkObject(SubFunction)
        every { SubFunction.isAppInForegrounded() } returns true // override per-test

        // --- Token APIs & preferences
        mockkObject(TokenManager)
        mockkObject(Preferenses)

        // Defaults (overridden per-test)
        coEvery { TokenManager.getCurrentToken(any()) } returns TokenData(
            provider = Constants.FCM_PROVIDER, token = "tok-NEW"
        )
        every { Preferenses.getSavedToken(any()) } returns TokenData(
            provider = Constants.FCM_PROVIDER, token = "tok-OLD"
        )
        every { Preferenses.setCurrentToken(any(), any()) } just Runs

        // --- Events: return proper DataClasses objects, not Unit
        mockkObject(Events)
        coEvery { Events.error(any(), any()) } answers { DataClasses.Error(function = firstArg()) }
        coEvery {
            Events.error(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Error(function = firstArg()) }
        coEvery {
            Events.retry(
                any(),
                any()
            )
        } answers { DataClasses.RetryError(function = firstArg()) }
        coEvery {
            Events.retry(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.RetryError(function = firstArg()) }
        coEvery { Events.event(any(), any()) } answers { DataClasses.Event(function = firstArg()) }
        coEvery {
            Events.event(
                any(),
                any(),
                any()
            )
        } answers { DataClasses.Event(function = firstArg()) }

        // --- Network: by default "success"
        mockkObject(Request)
        coEvery {
            Request.updateRequest(
                any(),
                any()
            )
        } returns DataClasses.Event(function = "updateRequest")
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork().result.get()
        unmockkAll()
    }

    /**
     * test_1
     * Foreground + changed token -> tokenUpdate should enqueue a work with UPDATE_C_WORK_TAG.
     */
    @Test
    fun tokenUpdate_foreground_enqueues_update_work() = runTest {
        // Arrange
        every { SubFunction.isAppInForegrounded() } returns true
        every { Preferenses.getSavedToken(any()) } returns TokenData(
            Constants.FCM_PROVIDER,
            "tok-OLD"
        )
        coEvery { TokenManager.getCurrentToken(any()) } returns TokenData(
            Constants.FCM_PROVIDER,
            "tok-NEW"
        )

        // Act
        TokenUpdate.tokenUpdate(context)

        // Assert
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.UPDATE_C_WORK_TAG)
            .get()
        assertThat(infos.isNotEmpty(), `is`(true))
        assertThat(infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /**
     * test_2
     * isRetry -> RetryError => returns true and DOES NOT set current token.
     */
    @Test
    fun isRetry_retry_event_returns_true_and_does_not_set_token() = runTest {
        // Arrange
        coEvery { TokenManager.getCurrentToken(any()) } returns TokenData(
            Constants.FCM_PROVIDER,
            "tok-NEW"
        )
        coEvery { Request.updateRequest(any(), any()) } returns com.altcraft.sdk.data.retry(
            function = "updateRequest"
        )

        // Act
        val shouldRetry = TokenUpdate.isRetry(context, "req-1")

        // Assert
        assertThat(shouldRetry, `is`(true))
        verify(exactly = 0) { Preferenses.setCurrentToken(any(), any()) }
    }

    /**
     * test_3
     * isRetry -> success Event => returns false and sets current token with the same TokenData.
     */
    @Test
    fun isRetry_success_event_returns_false_and_sets_token() = runTest {
        // Arrange
        val tok = TokenData(Constants.FCM_PROVIDER, "tok-NEW")
        coEvery { TokenManager.getCurrentToken(any()) } returns tok
        coEvery {
            Request.updateRequest(
                any(),
                any()
            )
        } returns DataClasses.Event(function = "updateRequest")

        // Act
        val shouldRetry = TokenUpdate.isRetry(context, "req-2")

        // Assert
        assertThat(shouldRetry, `is`(false))
        verify(exactly = 1) { Preferenses.setCurrentToken(context, tok) }
    }

    /**
     * test_4 (negative)
     * saved token equals current token -> tokenUpdate exits early; no update work enqueued.
     */
    @Test
    fun tokenUpdate_same_token_does_not_enqueue_work() = runTest {
        // Arrange
        every { SubFunction.isAppInForegrounded() } returns true
        val same = TokenData(Constants.FCM_PROVIDER, "tok-SAME")
        coEvery { TokenManager.getCurrentToken(any()) } returns same
        every { Preferenses.getSavedToken(any()) } returns same

        // Act
        TokenUpdate.tokenUpdate(context)

        // Assert
        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(Constants.UPDATE_C_WORK_TAG)
            .get()
        assertThat(infos.isEmpty(), `is`(true))
    }
}
