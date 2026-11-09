package test.platform.authenticate

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.core.environment.Environment
import com.altcraft.sdk.data.Preferences.getCurrentRToken
import com.altcraft.sdk.data.Preferences.setCurrentRToken
import com.altcraft.sdk.data.room.DB
import com.altcraft.sdk.data.room.dao.AuthRequestDao
import com.altcraft.sdk.data.room.dao.AuthorizationDao
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.data.room.entities.AuthRequestEntity
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTInterface
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTProvider
import com.altcraft.sdk.platform.authenticate.internal.AuthenticateProcess
import com.altcraft.sdk.util.SubFunction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.room.TestRoom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * AuthenticateDeadlockInstrumentedTest
 *
 * Reproduces the self-deadlock of the AuthenticateProcess mutex: a JWT read
 * performed inside a mutex-protected authentication flow detects an RToken
 * change whose notification chain used to call `AuthenticateProcess.logOut`
 * and suspended forever on the same non-reentrant mutex.
 *
 * With the fix the notification chain uses `AuthenticateProcess.logOutUnlocked`
 * and every protected entry point completes within the timeout:
 * - test_1: isRetry — the stale pending request is dropped, the new rToken
 *   is saved and the anonymous session is restored.
 * - test_2: isAuthenticated — returns false after the identity reset.
 * - test_3: authenticate — the command block completes and stores the request.
 */
@RunWith(AndroidJUnit4::class)
class AuthenticateDeadlockInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var wm: WorkManager
    private lateinit var db: TestRoom
    private lateinit var authStateDao: AuthorizationDao
    private lateinit var authRequestDao: AuthRequestDao

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)

        wm = WorkManager.getInstance(appContext)
        wm.cancelAllWork().result.get()

        db = Room.inMemoryDatabaseBuilder(appContext, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()

        authStateDao = mockk(relaxed = true)
        authRequestDao = mockk(relaxed = true)

        mockkObject(DB.Companion)
        val sdkDBWrapper = mockk<DB>(relaxed = true)
        every { DB.getDb(any()) } returns sdkDBWrapper
        every { sdkDBWrapper.configurationDao() } returns db.configurationDao()
        every { sdkDBWrapper.authStateDao() } returns authStateDao
        every { sdkDBWrapper.authRequestDao() } returns authRequestDao

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred(Unit)

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true

        coEvery { authStateDao.get() } returns authenticatedEntity()

        ConfigManager.conf = jwtModeConfig()
        setCurrentRToken(appContext, RTOKEN_OLD)
    }

    @After
    fun tearDown() {
        JWTProvider.provider = null
        ConfigManager.conf = null
        runCatching { wm.cancelAllWork().result.get() }
        unmockkAll()
        db.close()
    }

    /**
     * - test_1: the RToken change is detected by the JWT read inside isRetry;
     * the notification chain completes, the stale request is dropped and the
     * anonymous session is restored.
     */
    @Test
    fun isRetry_rTokenChangeDuringJwtRead_completesWithoutDeadlock() = runBlocking {
        coEvery { authRequestDao.get() } returns AuthRequestEntity(
            0, REQUEST_ID, STALE_USER
        )
        JWTProvider.provider = StaticJWTProvider(JWT_CHANGED)

        val result = withTimeout(DEADLOCK_TIMEOUT_MS) {
            AuthenticateProcess.isRetry(appContext)
        }

        assertFalse(MSG_STALE_REQUEST_DROPPED, result)
        assertEquals(MSG_TOKEN_SAVED, RTOKEN_NEW, getCurrentRToken(appContext))
        coVerify(exactly = 1) {
            authStateDao.insert(any<AuthenticationEntity>())
        }
    }

    /**
     * - test_2: the RToken change is detected by the JWT read inside
     * isAuthenticated; the notification chain completes and the check
     * reports the reset identity.
     */
    @Test
    fun isAuthenticated_rTokenChangeDuringJwtRead_completesWithoutDeadlock() = runBlocking {
        JWTProvider.provider = StaticJWTProvider(JWT_CHANGED)

        val result = withTimeout(DEADLOCK_TIMEOUT_MS) {
            AuthenticateProcess.isAuthenticated(
                Environment.create(appContext)
            )
        }

        assertFalse(MSG_IDENTITY_RESET, result)
        assertEquals(MSG_TOKEN_SAVED, RTOKEN_NEW, getCurrentRToken(appContext))
        coVerify(exactly = 1) {
            authStateDao.insert(any<AuthenticationEntity>())
        }
    }

    /**
     * - test_3: the RToken change is detected by the JWT read inside the
     * authenticate command block; the block completes and stores the
     * authentication request.
     */
    @Test
    fun authenticate_rTokenChangeDuringJwtRead_completesWithoutDeadlock() {
        val done = CountDownLatch(1)

        coEvery { authRequestDao.get() } returns null
        coEvery { authRequestDao.insert(any()) } coAnswers {
            done.countDown()
        }
        JWTProvider.provider = StaticJWTProvider(JWT_CHANGED)

        AuthenticateProcess.authenticate(appContext)

        assertTrue(
            MSG_COMMAND_COMPLETED,
            done.await(DEADLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        )
        assertEquals(MSG_TOKEN_SAVED, RTOKEN_NEW, getCurrentRToken(appContext))
        coVerify(exactly = 1) {
            authStateDao.insert(any<AuthenticationEntity>())
        }
    }

    /** Internal auth state with an identified current user. */
    private fun authenticatedEntity() = AuthenticationEntity(
        id = 0,
        anonimAcid = ANON_ACID,
        acid = ANON_ACID,
        deviceId = DEVICE_ID,
        currentUser = CURRENT_USER,
        startAnonSession = 1L
    )

    /** JWT-mode configuration: no resource token, the JWT is the auth source. */
    private fun jwtModeConfig() = ConfigurationEntity(
        id = 0,
        apiUrl = API_URL,
        rToken = null,
        appInfo = null
    )
}

private const val API_URL = "https://api.example.com"
private const val RTOKEN_OLD = "rt-OLD"
private const val RTOKEN_NEW = "rt-NEW"
private const val REQUEST_ID = "request-1"
private const val STALE_USER = "stale-user"
private const val ANON_ACID = "anon-acid"
private const val DEVICE_ID = "device-id"
private const val CURRENT_USER = "current-user"
private const val DEADLOCK_TIMEOUT_MS = 10_000L

/**
 * JWT payload carrying both the changed role token and a matching claim,
 * so the user tag can be resolved from the same read that detects the change.
 */
private val JWT_CHANGED =
    "x." + Base64.encodeToString(
        """{"rtoken":"$RTOKEN_NEW","matching":{"db_id":1,"matching":"email","profile_id":"p1"}}"""
            .toByteArray(),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    ) + ".x"

private const val MSG_STALE_REQUEST_DROPPED =
    "The stale authentication request must be dropped after the identity reset"
private const val MSG_IDENTITY_RESET =
    "The identity must be reported as reset after the RToken change"
private const val MSG_COMMAND_COMPLETED =
    "The authenticate command must complete within the timeout"
private const val MSG_TOKEN_SAVED =
    "The new rToken must be saved by the notification chain"

/** JWT provider always returning the same token. */
private class StaticJWTProvider(
    private val token: String
) : JWTInterface {

    override fun getJWT(): String? = token
}
