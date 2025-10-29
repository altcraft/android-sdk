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
 *  - test_1: deliveryEvent() with Altcraft message containing _uid → uses UID from message
 *  - test_2: deliveryEvent() with Altcraft message missing _uid → falls back to messageUID
 *  - test_3: openEvent() with only messageUID → uses provided UID
 *  - test_4: openEvent() with both message and messageUID → prefers UID from message
 *
 * Negative scenarios:
 *  - test_5: deliveryEvent() with non-Altcraft message → ignored
 *  - test_6: openEvent() with both message and messageUID null → calls send with null
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
        mockkObject(SubFunction)
        mockkObject(PushEvent)
        every { SubFunction.altcraftPush(any()) } returns true
        coEvery { PushEvent.sendPushEvent(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: deliveryEvent with Altcraft message containing _uid → uses UID from message. */
    @Test
    fun test_delivery_withAltcraftMessage_usesUidFromMessage() = runBlocking {
        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)
        PublicPushEventFunctions.deliveryEvent(ctx, message, UID_FROM_PARAM)
        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.DELIVERY, UID_FROM_MESSAGE)
        }
        confirmVerified(PushEvent)
    }

    /** - test_2: deliveryEvent with Altcraft message missing _uid → falls back to messageUID. */
    @Test
    fun test_delivery_withAltcraftMessage_withoutUid_fallsBackToMessageUid() = runBlocking {
        val message = mapOf("some_key" to "some_value")
        PublicPushEventFunctions.deliveryEvent(ctx, message, UID_FROM_PARAM)
        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.DELIVERY, UID_FROM_PARAM)
        }
        confirmVerified(PushEvent)
    }

    /** - test_5: deliveryEvent with non-Altcraft message → ignored. */
    @Test
    fun test_delivery_nonAltcraftMessage_isIgnored() = runBlocking {
        every { SubFunction.altcraftPush(any()) } returns false
        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)
        PublicPushEventFunctions.deliveryEvent(ctx, message, UID_FROM_PARAM)
        coVerify(timeout = TIMEOUT_MS, exactly = 0) {
            PushEvent.sendPushEvent(any(), any(), any())
        }
    }

    /** - test_3: openEvent with only messageUID → uses provided UID. */
    @Test
    fun test_open_withMessageUidOnly_usesProvidedUid() = runBlocking {
        PublicPushEventFunctions.openEvent(ctx, null, UID_FROM_PARAM)
        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, UID_FROM_PARAM)
        }
        confirmVerified(PushEvent)
    }

    /** - test_4: openEvent with both message and messageUID → prefers UID from message. */
    @Test
    fun test_open_bothMessageAndMessageUid_prefersUidFromMessage() = runBlocking {
        val message = mapOf(Constants.UID_KEY to UID_FROM_MESSAGE)
        PublicPushEventFunctions.openEvent(ctx, message, UID_FROM_PARAM)
        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, UID_FROM_MESSAGE)
        }
        confirmVerified(PushEvent)
    }

    /** - test_6: openEvent with both message and messageUID null → calls send with null. */
    @Test
    fun test_open_bothNullUids_callsSendWithNull() = runBlocking {
        PublicPushEventFunctions.openEvent(ctx, null, null)
        coVerify(timeout = TIMEOUT_MS, exactly = 1) {
            PushEvent.sendPushEvent(ctx, Constants.OPEN, null)
        }
        confirmVerified(PushEvent)
    }
}
