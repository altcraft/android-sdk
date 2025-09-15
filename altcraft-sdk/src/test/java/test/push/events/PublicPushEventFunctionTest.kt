@file:Suppress("SpellCheckingInspection")

package test.push.events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.additional.SubFunction
import com.altcraft.sdk.data.Constants
import com.altcraft.sdk.push.events.PushEvent
import com.altcraft.sdk.push.events.PublicPushEventFunctions
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * PublicPushEventFunctionsTest
 *
 * Positive scenarios:
 *  - test_delivery_withAltcraftMessage_usesUidFromMessage
 *  - test_delivery_withAltcraftMessage_withoutUid_fallsBackToMessageUid
 *  - test_open_withMessageUidOnly_usesProvidedUid
 *  - test_open_bothMessageAndMessageUid_prefersUidFromMessage
 *
 * Negative scenarios:
 *  - test_delivery_nonAltcraftMessage_isIgnored
 *  - test_open_bothNullUids_callsSendWithNull
 *
 */
class PublicPushEventFunctionsTest {

    private companion object {
        private const val UID_FROM_MESSAGE = "uid_from_message"
        private const val UID_FROM_PARAM = "uid_from_param"
        private const val TIMEOUT_MS = 500L
    }

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        // Mock required singletons/objects
        mockkObject(SubFunction)
        mockkObject(PushEvent)

        // Default: altcraftPush returns true (Altcraft message)
        every { SubFunction.altcraftPush(any()) } returns true

        // Default: sendPushEvent does nothing
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** Positive: Altcraft message with _uid → take UID from message and type DELIVERY */
    @Test
    fun test_delivery_withAltcraftMessage_usesUidFromMessage() = runBlocking {
        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)

        PublicPushEventFunctions.deliveryEvent(
            context = ctx,
            message = message,
            messageUID = UID_FROM_PARAM // should be ignored
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.DELIVERY, UID_FROM_MESSAGE)
        }
        confirmVerified(PushEvent)
    }

    /** Positive: Altcraft message without _uid → fallback to messageUID */
    @Test
    fun test_delivery_withAltcraftMessage_withoutUid_fallsBackToMessageUid() = runBlocking {
        val message = mapOf("some_key" to "some_value") // no _uid

        PublicPushEventFunctions.deliveryEvent(
            context = ctx,
            message = message,
            messageUID = UID_FROM_PARAM
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.DELIVERY, UID_FROM_PARAM)
        }
        confirmVerified(PushEvent)
    }

    /** Negative: non-Altcraft message → should not call sendPushEvent */
    @Test
    fun test_delivery_nonAltcraftMessage_isIgnored() = runBlocking {
        every { SubFunction.altcraftPush(any()) } returns false

        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)

        PublicPushEventFunctions.deliveryEvent(
            context = ctx,
            message = message,
            messageUID = UID_FROM_PARAM
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 0) {
            PushEvent.sendPushEvent(any(), any(), any())
        }
    }

    /** Positive: only messageUID provided (message == null) → OPEN with UID_FROM_PARAM */
    @Test
    fun test_open_withMessageUidOnly_usesProvidedUid() = runBlocking {
        PublicPushEventFunctions.openEvent(
            context = ctx,
            message = null,
            messageUID = UID_FROM_PARAM
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, UID_FROM_PARAM)
        }
        confirmVerified(PushEvent)
    }

    /** Positive: both message and messageUID provided → prefer UID from message */
    @Test
    fun test_open_bothMessageAndMessageUid_prefersUidFromMessage() = runBlocking {
        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)

        PublicPushEventFunctions.openEvent(
            context = ctx,
            message = message,
            messageUID = UID_FROM_PARAM // should be ignored
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, UID_FROM_MESSAGE)
        }
        confirmVerified(PushEvent)
    }

    /** Negative: both message and messageUID are null → call with null UID */
    @Test
    fun test_open_bothNullUids_callsSendWithNull() = runBlocking {
        PublicPushEventFunctions.openEvent(
            context = ctx,
            message = null,
            messageUID = null
        )

        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, null)
        }
        confirmVerified(PushEvent)
    }
}