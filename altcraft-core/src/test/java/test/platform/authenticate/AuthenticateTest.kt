@file:Suppress("SpellCheckingInspection")

package test.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.platform.authenticate.AuthManager
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.platform.authenticate.external.ExternalAuth
import com.altcraft.sdk.coordination.CommandQueue
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.room.dao.AuthorizationDao
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.util.SubFunction
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * AuthenticateTest
 *
 * Internal auth operations:
 * - setAuth: creates anonymous auth when none exists; no-op when already present.
 * - isAuthenticated: checks currentUserTag against userTag.
 * - authenticate: enqueues auth command; creates request and starts worker.
 * - logOut: restores anonymous auth and emits UserLogOut.
 */
class AuthenticateTest {

    private companion object {
        const val MSG_FALSE = "Expected false"
        const val MSG_TRUE = "Expected true"
        const val LATCH_TIMEOUT_SEC = 3L
    }

    private lateinit var ctx: Context
    private lateinit var env: Environment
    private lateinit var room: DB
    private lateinit var authStateDao: AuthorizationDao
    private lateinit var authRequestDao: AuthRequestDao
    private lateinit var authManager: AuthManager
    private lateinit var externalAuth: ExternalAuth

    @Before
    fun setUp() {
        ctx = mockk(relaxed = true)
        every { ctx.applicationContext } returns ctx

        authStateDao = mockk(relaxed = true)
        authRequestDao = mockk(relaxed = true)

        room = mockk(relaxed = true)
        every { room.authStateDao() } returns authStateDao
        every { room.authRequestDao() } returns authRequestDao
        every { room.dao.authState } returns authStateDao
        every { room.dao.authRequest } returns authRequestDao

        authManager = mockk(relaxed = true)
        externalAuth = mockk(relaxed = true)
        every { authManager.external } returns externalAuth

        env = mockk(relaxed = true)
        every { env.room } returns room
        every { env.auth } returns authManager

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true

        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        mockkObject(Environment.Companion)
        every { Environment.create(any()) } returns env

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred(Unit)

        mockkConstructor(CommandQueue.SerialCommandQueue::class)
        every {
            anyConstructed<CommandQueue.SerialCommandQueue>().submit(any())
        } answers {
            val block = firstArg<suspend () -> Unit>()
            runBlocking { block() }
        }
    }

    @After
    fun tearDown() = unmockkAll()

    // ── setAuth ──────────────────────────────────────────────────────────

    /** setAuth: no existing auth → creates anonymous AuthenticationEntity. */
    @Test
    fun setAuth_whenNoAuth_createsAnonymousEntity() = runBlocking {
        coEvery { authStateDao.get() } returns null
        coJustRun { authStateDao.insert(any()) }

        AuthenticateProcess.setAuth(ctx)

        coVerify(exactly = 1) {
            authStateDao.insert(any<AuthenticationEntity>())
        }
        coVerify(exactly = 1) { authStateDao.get() }
    }

    /** setAuth: auth already exists → no insert. */
    @Test
    fun setAuth_whenAuthExists_doesNotInsert() = runBlocking {
        val existing = AuthenticationEntity(
            id = 0,
            anonimAcid = "a1",
            acid = "a1",
            deviceId = "d1",
            currentUser = null,
            startAnonSession = 100L
        )

        coEvery { authStateDao.get() } returns existing

        AuthenticateProcess.setAuth(ctx)

        coVerify(exactly = 0) { authStateDao.insert(any()) }
        coVerify(exactly = 1) { authStateDao.get() }
    }

    // ── isAuthenticated ──────────────────────────────────────────────────

    /** isAuthenticated: currentUserTag matches userTag → true. */
    @Test
    fun isAuthenticated_whenTagsMatch_returnsTrue() = runBlocking {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = "alice",
            startAnonSession = 1L
        )

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "alice"

        val result = AuthenticateProcess.isAuthenticated(env)

        assertTrue(MSG_TRUE, result)
    }

    /** isAuthenticated: currentUserTag is null → false. */
    @Test
    fun isAuthenticated_whenTagIsNull_returnsFalse() = runBlocking {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = null,
            startAnonSession = 1L
        )

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "bob"

        val result = AuthenticateProcess.isAuthenticated(env)

        assertFalse(MSG_FALSE, result)
    }

    /** isAuthenticated: currentUserTag differs from userTag → false. */
    @Test
    fun isAuthenticated_whenTagsDiffer_returnsFalse() = runBlocking {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = "alice",
            startAnonSession = 1L
        )

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "bob"

        val result = AuthenticateProcess.isAuthenticated(env)

        assertFalse(MSG_FALSE, result)
    }

    // ── authenticate ─────────────────────────────────────────────────────

    /** authenticate: tag matches → returns early, no DAO insert. */
    @Test
    fun authenticate_whenTagMatches_returnsEarly() {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = "same-tag",
            startAnonSession = 1L
        )

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "same-tag"

        AuthenticateProcess.authenticate(ctx)

        coVerify(exactly = 0) { authRequestDao.insert(any()) }
        coVerify(exactly = 0) { authRequestDao.get() }
    }

    /** authenticate: tag differs, no existing request → inserts and starts worker. */
    @Test
    fun authenticate_whenTagDiffersAndNoRequest_insertsAndStartsWorker() {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = null,
            startAnonSession = 1L
        )

        val doneLatch = CountDownLatch(1)

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "new-user"
        coEvery { authRequestDao.get() } returns null

        coEvery { authRequestDao.insert(any()) } coAnswers {
            doneLatch.countDown()
        }

        AuthenticateProcess.authenticate(ctx)

        assertTrue(
            "authenticate must insert auth request",
            doneLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        )

        coVerify(exactly = 1) { authRequestDao.get() }
        coVerify(exactly = 1) { authRequestDao.insert(any()) }
    }

    /** authenticate: tag differs, existing request with same tag → no insert. */
    @Test
    fun authenticate_whenTagDiffersAndSameRequest_doesNotInsert() {
        val existing = AuthRequestEntity(
            id = 0,
            requestId = "r1",
            user = "new-user"
        )

        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = null,
            startAnonSession = 1L
        )

        val doneLatch = CountDownLatch(1)

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "new-user"

        coEvery { authRequestDao.get() } coAnswers {
            doneLatch.countDown()
            existing
        }

        AuthenticateProcess.authenticate(ctx)

        assertTrue(
            "authenticate must check existing auth request",
            doneLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        )

        coVerify(exactly = 1) { authRequestDao.get() }
        coVerify(exactly = 0) { authRequestDao.insert(any()) }
    }

    /** authenticate: tag differs, existing request with different tag → inserts new. */
    @Test
    fun authenticate_whenTagDiffersAndDifferentRequest_insertsNew() {
        val existing = AuthRequestEntity(
            id = 0,
            requestId = "r1",
            user = "old-user"
        )

        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "a",
            acid = "a",
            deviceId = "d",
            currentUser = null,
            startAnonSession = 1L
        )

        val doneLatch = CountDownLatch(1)

        coEvery { authManager.internal() } returns auth
        coEvery { externalAuth.user() } returns "new-user"
        coEvery { authRequestDao.get() } returns existing

        coEvery { authRequestDao.insert(any()) } coAnswers {
            doneLatch.countDown()
        }

        AuthenticateProcess.authenticate(ctx)

        assertTrue(
            "authenticate must insert replacement auth request",
            doneLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        )

        coVerify(exactly = 1) { authRequestDao.get() }
        coVerify(exactly = 1) { authRequestDao.insert(any()) }
    }

    // ── logOut ───────────────────────────────────────────────────────────

    /** logOut: auth exists → restores anonymous, emits UserLogOut. */
    @Test
    fun logOut_whenAuthExists_restoresAnonymousAndEmitsEvent() {
        val auth = AuthenticationEntity(
            id = 0,
            anonimAcid = "anon-1",
            acid = "cur-1",
            deviceId = "dev-1",
            currentUser = "user-1",
            startAnonSession = 100L
        )

        coEvery { authStateDao.get() } returns auth
        coEvery { authManager.internal() } returns auth

        val doneLatch = CountDownLatch(1)

        coEvery { authStateDao.insert(any()) } answers {
            doneLatch.countDown()
        }

        runBlocking { AuthenticateProcess.logOut(ctx) }

        assertTrue(
            "logOut must complete",
            doneLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        )

        coVerify(exactly = 1) { authStateDao.get() }
        coVerify(exactly = 1) { authStateDao.insert(any()) }
    }

    /** logOut: no auth → returns early, no insert. */
    @Test
    fun logOut_whenNoAuth_returnsEarly() {
        val doneLatch = CountDownLatch(1)

        coEvery { authStateDao.get() } coAnswers {
            doneLatch.countDown()
            null
        }

        runBlocking { AuthenticateProcess.logOut(ctx) }

        assertTrue(
            "logOut must complete",
            doneLatch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)
        )

        coVerify(exactly = 0) { authStateDao.insert(any()) }
    }
}