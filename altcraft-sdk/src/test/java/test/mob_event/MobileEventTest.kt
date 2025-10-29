@file:Suppress("SpellCheckingInspection")

package test.mob_event

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.auth.AuthManager
import com.altcraft.sdk.concurrency.CommandQueue
import com.altcraft.sdk.concurrency.InitBarrier
import com.altcraft.sdk.config.ConfigSetup
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.data.room.RoomRequest
import com.altcraft.sdk.data.room.SDKdb
import com.altcraft.sdk.device.DeviceInfo
import com.altcraft.sdk.mob_events.MobileEvent
import com.altcraft.sdk.network.Request
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.workers.coroutine.LaunchFunctions
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MobileEventUnitTest
 *
 * Positive scenarios:
 *  - test_1: sendMobileEvent — correctly builds the entity, inserts via RoomRequest.entityInsert,
 *            and starts the worker via startMobileEventCoroutineWorker.
 *  - test_3: isRetry — successful (non-retry) response → entity is deleted, function returns false.
 *  - test_4: isRetry — retry response and limit NOT reached → returns true, entity is not deleted.
 *  - test_5: isRetry — retry response and limit reached → returns false.
 *
 * Negative/behavioral:
 *  - test_2: sendMobileEvent — invalid payload (nested objects) → emits error and DOES NOT insert.
 *  - test_6: isRetry — exception during processing → retry event is emitted and function returns true.
 */
class MobileEventUnitTest {

    private lateinit var ctx: Context

    private val config = ConfigurationEntity(
        id = 1,
        icon = null,
        apiUrl = "https://api.example.com",
        rToken = "rt-1",
        appInfo = null,
        usingService = false,
        serviceMessage = null,
        pushReceiverModules = null,
        providerPriorityList = null,
        pushChannelName = null,
        pushChannelDescription = null
    )

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)

        mockkObject(CommandQueue.MobileEventCommandQueue)
        every { CommandQueue.MobileEventCommandQueue.submit(any()) } answers {
            runBlocking { firstArg<suspend () -> Unit>().invoke() }
        }

        mockkObject(InitBarrier)
        val done = CompletableDeferred<Unit>().also { it.complete(Unit) }
        every { InitBarrier.current() } returns done

        mockkObject(ConfigSetup, AuthManager)
        coEvery { ConfigSetup.getConfig(any()) } returns config
        every { AuthManager.getUserTag(config.rToken) } returns "user-tag"

        mockkObject(DeviceInfo)
        every { DeviceInfo.getTimeZoneForMobEvent() } returns 180

        mockkObject(RoomRequest)
        coJustRun { RoomRequest.entityInsert(any(), any()) }
        coJustRun { RoomRequest.entityDelete(any(), any()) }
        coEvery { RoomRequest.isRetryLimit(any(), any()) } returns false

        mockkObject(LaunchFunctions)
        justRun { LaunchFunctions.startMobileEventCoroutineWorker(any()) }

        mockkObject(Request)
        coEvery { Request.mobileEventRequest(any(), any()) } returns DataClasses.Event(
            function = "mobileEventRequest", eventCode = 200, eventMessage = "ok"
        )

        mockkObject(Events)
        every { Events.error(any(), any()) } returns DataClasses.Error("errFn", 500, "err", null)
        every { Events.retry(any(), any()) } returns DataClasses.RetryError(
            "retryFn",
            102,
            "retry",
            null
        )
    }

    @After
    fun tearDown() = unmockkAll()

    // -------------------- sendMobileEvent --------------------

    /** test_1: sendMobileEvent → entityInsert + worker start */
    @Test
    fun test_1_sendMobileEvent_success_inserts_and_startsWorker() = runBlocking {
        val utm = DataClasses.UTM(
            campaign = "winter-sale",
            content = "banner-1",
            keyword = "boots",
            medium = "cpc",
            source = "google",
            temp = "x"
        )

        MobileEvent.sendMobileEvent(
            context = ctx,
            sid = "pixel-1",
            eventName = "purchase",
            sendMessageId = "smid-1",
            payloadFields = mapOf("amount" to 9.99, "ok" to true),
            matching = mapOf("device_id" to "d-123"),
            matchingType = "device",
            profileFields = mapOf("age" to 30),
            subscription = null,
            utmTags = utm,
            altcraftClientID = "cid-1",
        )

        coVerify {
            RoomRequest.entityInsert(eq(ctx), withArg { e ->
                val me = e as MobileEventEntity
                assertEquals("user-tag", me.userTag)
                assertEquals("pixel-1", me.sid)
                assertEquals("purchase", me.eventName)
                assertEquals("smid-1", me.sendMessageId)
                assertEquals("device", me.matchingType)
                assertEquals(180, me.timeZone)
                assertEquals("cid-1", me.altcraftClientID)
            })
        }
        io.mockk.verify { LaunchFunctions.startMobileEventCoroutineWorker(ctx) }
    }

    /** test_2: sendMobileEvent with nested object in payload → emits error and no insert */
    @Test
    fun test_2_sendMobileEvent_invalidPayload_emitsError_and_noInsert() = runBlocking {
        MobileEvent.sendMobileEvent(
            context = ctx,
            sid = "px",
            eventName = "bad",
            sendMessageId = "smid-bad",
            payloadFields = mapOf("complex" to mapOf("x" to 1))
        )

        io.mockk.verify { Events.error(eq("sendMobileEvent"), any()) }
        coVerify(exactly = 0) { RoomRequest.entityInsert(any(), any()) }
        io.mockk.verify(exactly = 0) { LaunchFunctions.startMobileEventCoroutineWorker(any()) }
    }

    // -------------------- isRetry --------------------

    /** test_3: isRetry — success → entity deleted, returns false */
    @Test
    fun test_3_isRetry_success_deletes_and_returnsFalse() = runBlocking {
        val sdkDb = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns sdkDb
        every { sdkDb.request() } returns dao

        val e = MobileEventEntity(
            id = 10,
            userTag = "user-tag",
            timeZone = 0,
            time = System.currentTimeMillis(),
            sid = "s",
            altcraftClientID = "c",
            eventName = "open",
            payload = null,
            matching = null,
            matchingType = "device",
            profileFields = null,
            subscription = null,
            sendMessageId = "smid",
            utmTags = null
        )
        coEvery { dao.allMobileEventsByTag("user-tag") } returns listOf(e)

        val res = MobileEvent.isRetry(ctx)
        assertFalse(res)

        coVerify { RoomRequest.entityDelete(sdkDb, e) }
        coVerify(exactly = 0) { RoomRequest.isRetryLimit(any(), any()) }
    }

    /** test_4: isRetry — retry response and NOT limit → true, no delete */
    @Test
    fun test_4_isRetry_retry_and_notLimit_returnsTrue() = runBlocking {
        val sdkDb = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns sdkDb
        every { sdkDb.request() } returns dao

        val e = MobileEventEntity(
            id = 11,
            userTag = "user-tag",
            timeZone = 0,
            time = System.currentTimeMillis(),
            sid = "s",
            altcraftClientID = "c",
            eventName = "open",
            payload = null,
            matching = null,
            matchingType = null,
            profileFields = null,
            subscription = null,
            sendMessageId = "smid",
            utmTags = null
        )
        coEvery { dao.allMobileEventsByTag("user-tag") } returns listOf(e)

        coEvery { Request.mobileEventRequest(any(), any()) } returns DataClasses.RetryError(
            function = "mobileEventRequest"
        )
        coEvery { RoomRequest.isRetryLimit(sdkDb, e) } returns false

        val res = MobileEvent.isRetry(ctx)
        assertTrue(res)

        coVerify(exactly = 0) { RoomRequest.entityDelete(any(), any()) }
        coVerify { RoomRequest.isRetryLimit(sdkDb, e) }
    }

    /** test_5: isRetry — retry response and limit reached → false */
    @Test
    fun test_5_isRetry_retry_and_limitReached_returnsFalse() = runBlocking {
        val sdkDb = mockk<SDKdb>(relaxed = true)
        val dao = mockk<DAO>(relaxed = true)

        mockkObject(SDKdb.Companion)
        every { SDKdb.getDb(any()) } returns sdkDb
        every { sdkDb.request() } returns dao

        val e = MobileEventEntity(
            id = 12,
            userTag = "user-tag",
            timeZone = 0,
            time = System.currentTimeMillis(),
            sid = "s",
            altcraftClientID = "c",
            eventName = "open",
            payload = null,
            matching = null,
            matchingType = "device",
            profileFields = null,
            subscription = null,
            sendMessageId = "smid",
            utmTags = null
        )
        coEvery { dao.allMobileEventsByTag("user-tag") } returns listOf(e)

        coEvery { Request.mobileEventRequest(any(), any()) } returns DataClasses.RetryError(
            function = "mobileEventRequest"
        )
        coEvery { RoomRequest.isRetryLimit(sdkDb, e) } returns true

        val res = MobileEvent.isRetry(ctx)
        assertFalse(res)

        coVerify { RoomRequest.isRetryLimit(sdkDb, e) }
        coVerify(exactly = 0) { RoomRequest.entityDelete(any(), any()) }
    }

    /** test_6: isRetry — on exception emits retry and returns true */
    @Test
    fun test_6_isRetry_onException_emitsRetry_and_returnsTrue() = runBlocking {
        coEvery { ConfigSetup.getConfig(any()) } throws RuntimeException("boom")

        val res = MobileEvent.isRetry(ctx)
        assertTrue(res)
        io.mockk.verify { Events.retry(eq("isRetry :: mobileEvent"), any()) }
    }
}
