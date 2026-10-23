@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppSubStateDao
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.inapp.data.room.dao.InAppSubscribeDao
import com.altcraft.sdk.inapp.data.room.entity.InAppSubStateEntity
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class InAppSubscribeTest {

    private lateinit var context: Context
    private lateinit var env: Environment
    private lateinit var subscribeDao: InAppSubscribeDao

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        mockkObject(Environment.Companion)
        env = mockk(relaxed = true)
        subscribeDao = mockk(relaxed = true)

        every { Environment.create(any()) } returns env
        every { env.room } returns mockk(relaxed = true)
        every { env.room.dao.subscribe } returns subscribeDao
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun test_1_isRetry_returnsFalse_whenNoPendingEntities() = runTest {
        coEvery { subscribeDao.getAllEntitiesByTag(any()) } returns emptyList()

        assertFalse(SubscriptionProcess.isRetry(context))
    }

    @Test
    fun test_2_isRetry_forwardsWorkerId() = runTest {
        coEvery { subscribeDao.getAllEntitiesByTag(any()) } returns emptyList()

        SubscriptionProcess.isRetry(context, UUID.randomUUID())

        coVerify(exactly = 1) { subscribeDao.getAllEntitiesByTag(any()) }
    }

    @Test
    fun test_3_isRetry_returnsTrue_whenLogicThrows() = runTest {
        mockkObject(Events)
        every { Events.retry(any(), any()) } returns mockk()

        every { Environment.create(any()) } throws RuntimeException("boom")

        assertTrue(SubscriptionProcess.isRetry(context))

        verify(exactly = 1) { Events.retry("[altcraft-inapp] isRetry :: inAppSubscribe", any()) }
    }

    @Test
    fun test_4_isRetry_rethrowsCancellation() = runTest {
        every { Environment.create(any()) } throws CancellationException("cancelled")

        try {
            SubscriptionProcess.isRetry(context)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun test_5_snapshotTaken_canComplete() {
        assertFalse(SubscriptionProcess.snapshotTaken.isCompleted)
    }

    @Test
    fun test_6_getSubscriptionStatus_subscribed_returnsTrue() = runTest {
        val auth = mockk<AuthenticationEntity>(relaxed = true)
        val subStateDao = mockk<InAppSubStateDao>(relaxed = true)
        val db = mockk<DB>(relaxed = true)

        every { Environment.create(any()) } returns env
        coEvery { env.internalAuth() } coAnswers { auth }
        every { auth.currentUser } returns "tag1"
        coEvery { env.userTag() } coAnswers { "tag1" }
        every { env.room } returns db
        every { db.dao.subState } returns subStateDao
        coEvery { subStateDao.get("tag1") } coAnswers { InAppSubStateEntity(
            userTag = "tag1", status = "subscribed"
        ) }

        assertTrue(SubscriptionProcess.getInAppSubscriptionStatus(context))
    }

    @Test
    fun test_7_getSubscriptionStatus_unsubscribed_returnsFalse() = runTest {
        val auth = mockk<AuthenticationEntity>(relaxed = true)
        val subStateDao = mockk<InAppSubStateDao>(relaxed = true)
        val db = mockk<DB>(relaxed = true)

        every { Environment.create(any()) } returns env
        coEvery { env.internalAuth() } coAnswers { auth }
        every { auth.currentUser } returns "tag1"
        coEvery { env.userTag() } coAnswers { "tag1" }
        every { env.room } returns db
        every { db.dao.subState } returns subStateDao
        coEvery { subStateDao.get("tag1") } coAnswers { InAppSubStateEntity(
            userTag = "tag1", status = "unsubscribed"
        ) }

        assertFalse(SubscriptionProcess.getInAppSubscriptionStatus(context))
    }

    @Test
    fun test_8_getSubscriptionStatus_nullUserTag_returnsFalse() = runTest {
        val auth = mockk<AuthenticationEntity>(relaxed = true)

        every { Environment.create(any()) } returns env
        coEvery { env.internalAuth() } coAnswers { auth }
        every { auth.currentUser } returns null

        assertFalse(SubscriptionProcess.getInAppSubscriptionStatus(context))
    }
}
