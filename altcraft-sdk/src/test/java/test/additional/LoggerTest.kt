@file:Suppress("SpellCheckingInspection")

package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.util.Log
import com.altcraft.sdk.additional.Logger
import com.altcraft.sdk.data.Constants.LOG_NULL
import com.altcraft.sdk.data.Constants.LOG_TAG
import com.altcraft.sdk.sdk_events.Message.LOG_HINT
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * LoggerTest
 *
 * Positive scenarios:
 *  - test_1: log() with loggingStatus = true logs provided message.
 *  - test_2: log() with loggingStatus = true and null message logs LOG_NULL.
 *  - test_3: log() with loggingStatus = null logs integration hint only once.
 *  - test_4: log() after integration hint and loggingStatus = true logs message.
 *
 * Negative scenarios:
 *  - test_5: log() with loggingStatus = false does not log anything.
 */
class LoggerTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        resetLoggerState()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun resetLoggerState() {
        Logger.loggingStatus = null
        resetIntegrationHintFlag()
    }

    private fun resetIntegrationHintFlag() {
        val field = Logger::class.java.getDeclaredField("integrationHintLogged")
        field.isAccessible = true
        val atomic = field.get(Logger) as java.util.concurrent.atomic.AtomicBoolean
        atomic.set(false)
    }

    /** test_1: log() with loggingStatus = true logs provided message. */
    @Test
    fun log_withLoggingEnabled_logsMessage() {
        Logger.loggingStatus = true
        Logger.log("hello")

        verify(exactly = 1) { Log.d(LOG_TAG, "hello") }
    }

    /** test_2: log() with loggingStatus = true and null message logs LOG_NULL. */
    @Test
    fun log_withLoggingEnabled_nullMessage_logsLogNull() {
        Logger.loggingStatus = true
        Logger.log(null)

        verify(exactly = 1) { Log.d(LOG_TAG, LOG_NULL) }
    }

    /** test_3: log() with loggingStatus = null logs integration hint only once. */
    @Test
    fun log_withStatusNull_logsIntegrationHintOnce() {
        Logger.loggingStatus = null
        Logger.log("first")
        Logger.log("second")

        verify(exactly = 1) { Log.d(LOG_TAG, LOG_HINT) }
        verify(exactly = 0) { Log.d(LOG_TAG, "first") }
        verify(exactly = 0) { Log.d(LOG_TAG, "second") }
    }

    /** test_4: log() after integration hint and loggingStatus = true logs message. */
    @Test
    fun log_afterHint_andLoggingEnabled_logsMessage() {
        Logger.loggingStatus = null
        Logger.log("ignored")

        Logger.loggingStatus = true
        Logger.log("real")

        verify(exactly = 1) { Log.d(LOG_TAG, LOG_HINT) }
        verify(exactly = 1) { Log.d(LOG_TAG, "real") }
    }

    /** test_5: log() with loggingStatus = false does not log anything. */
    @Test
    fun log_withLoggingDisabled_doesNotLog() {
        Logger.loggingStatus = false
        Logger.log("should-not-be-logged")
        Logger.log(null)

        verify(exactly = 0) { Log.d(any(), any()) }
    }
}