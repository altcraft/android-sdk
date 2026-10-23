package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration as WMConfiguration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.coordination.InitBarrier
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.data.models.CoreModels.ProfileModel
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.data.models.error
import com.altcraft.sdk.data.models.retry
import com.altcraft.sdk.data.room.entities.AuthenticationEntity
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.inapp.data.Constants
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppSubscribeDao
import com.altcraft.sdk.inapp.data.room.entity.InAppSubscribeEntity
import com.altcraft.sdk.inapp.platform.subscription.subscribe.request.SubscriptionRequest
import com.altcraft.sdk.workers.onetime.Request as WorkerRequest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * InAppSubscribeInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: Subscribe command inserts entity and enqueues worker.
 *  - test_3: Retry under limit keeps entity and returns true.
 *  - test_4: Retry under limit with newer worker request keeps entity and returns false.
 *  - test_6: Successful retry deletes entity and saves subscription status.
 *  - test_8: Saved subscribed status returns true.
 *
 * Negative scenarios:
 *  - test_2: Invalid custom fields trigger error and no entity is inserted.
 *  - test_5: Retry over limit deletes entity and returns false.
 *  - test_7: Error response deletes entity without saving subscription status.
 *  - test_9: Saved unsubscribed status returns false.
 */
@RunWith(AndroidJUnit4::class)
class InAppSubscribeInstrumentedTest {

    private companion object {
        private const val TEST_USER_TAG = "user-tag-123"
        private const val TEST_ACID = "acid-123"
        private const val TEST_DEVICE_ID = "device-123"

        private const val MSG_ENTITY_INSERTED = "In-app subscribe entity should be inserted"
        private const val MSG_ENTITY_NOT_INSERTED = "In-app subscribe entity should not be inserted"
        private const val MSG_ENTITY_KEPT = "In-app subscribe entity should stay in database"
        private const val MSG_ENTITY_DELETED = "In-app subscribe entity should be deleted"
        private const val MSG_WORKER_ENQUEUED = "In-app subscribe worker should be enqueued"
        private const val MSG_WORKER_STATE = "Worker should be in ENQUEUED state"
        private const val MSG_SHOULD_RETRY_TRUE = "Retry should be requested"
        private const val MSG_SHOULD_RETRY_FALSE = "Retry should not be requested"
        private const val MSG_USER_TAG = "Unexpected user tag"
        private const val MSG_STATUS = "Unexpected subscription status"
        private const val MSG_SYNC = "Unexpected sync value"
        private const val MSG_ACID = "Unexpected acid"
        private const val MSG_DEVICE_ID = "Unexpected device id"
        private const val MSG_REPLACE = "Unexpected replace flag"
        private const val MSG_SKIP_TRIGGERS = "Unexpected skipTriggers flag"
        private const val MSG_CATS_SIZE = "Unexpected categories size"
        private const val MSG_CAT_NAME = "Unexpected category name"
        private const val MSG_CAT_ACTIVE = "Unexpected category active flag"
        private const val MSG_CUSTOM_FIELD_A = "Custom field 'a' should be stored"
        private const val MSG_CUSTOM_FIELD_B = "Custom field 'b' should be stored"
        private const val MSG_PROFILE_FIELD_P = "Profile field 'p' should be stored"
        private const val MSG_STATUS_SAVED = "Subscription status should be saved"
        private const val MSG_STATUS_NOT_SAVED = "Subscription status should not be saved"
        private const val MSG_STATUS_TRUE = "Subscription status should be true"
        private const val MSG_STATUS_FALSE = "Subscription status should be false"
    }

    private lateinit var context: Context
    private lateinit var db: DB
    private lateinit var dao: InAppSubscribeDao

    private val testAuth = AuthenticationEntity(
        id = 0,
        anonimAcid = "anon-acid",
        acid = TEST_ACID,
        deviceId = TEST_DEVICE_ID,
        currentUser = TEST_USER_TAG,
        startAnonSession = System.currentTimeMillis(),
        isTest = false
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            wmConfig
        )

        db = Room.inMemoryDatabaseBuilder(
            context,
            DB::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = db.dao.subscribe

        MockKAnnotations.init(
            this,
            relaxUnitFun = true
        )

        if (!SubscriptionProcess.snapshotTaken.isCompleted) {
            SubscriptionProcess.snapshotTaken.complete(Unit)
        }

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false

        mockkObject(InitBarrier)
        every { InitBarrier.current() } returns CompletableDeferred<Unit>().apply {
            complete(Unit)
        }

        mockkObject(CoreEnvironmentProvider)
        coEvery { CoreEnvironmentProvider.isAuth() } returns true

        mockkObject(Events)
        every { Events.error(any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.event(any(), any()) } answers {
            SDKEventModel.Event(function = firstArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.event(any(), any(), any()) } answers {
            SDKEventModel.Event(function = firstArg())
        }

        mockkObject(SubscriptionRequest)
        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns SDKEventModel.Event("ok")

        mockkObject(WorkerRequest)
        coEvery {
            WorkerRequest.hasNewRequest(any(), any(), any())
        } returns false

        mockkObject(Environment.Companion)

        val env = mockk<Environment>(relaxed = true)

        every { env.room } returns db
        coEvery { env.userTag() } returns TEST_USER_TAG
        coEvery { env.internalAuth() } returns testAuth

        every { Environment.create(any()) } returns env
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    /** - test_1: Subscribe command inserts entity and enqueues worker. */
    @Test
    fun inAppSubscribe_enqueues_and_inserts() = runBlocking {
        SubscriptionProcess.inAppSubscribe(
            context = context,
            sync = true,
            status = Constants.SUBSCRIBED,
            customFields = mapOf(
                "a" to 1,
                "b" to "x"
            ),
            profileFields = mapOf(
                "p" to true
            ),
            cats = listOf(
                ProfileModel.CategoryData(
                    name = "news",
                    active = true
                )
            ),
            replace = true,
            skipTriggers = false
        )

        var subscriptions = emptyList<InAppSubscribeEntity>()

        for (i in 1..20) {
            Thread.sleep(200)

            subscriptions = dao.getAllEntitiesByTag(
                TEST_USER_TAG
            )

            if (subscriptions.isNotEmpty()) {
                break
            }
        }

        assertThat(
            MSG_ENTITY_INSERTED,
            subscriptions.isNotEmpty(),
            `is`(true)
        )

        val entity = subscriptions.first()

        assertThat(MSG_USER_TAG, entity.userTag, `is`(TEST_USER_TAG))
        assertThat(MSG_STATUS, entity.status, `is`(Constants.SUBSCRIBED))
        assertThat(MSG_SYNC, entity.sync, `is`(true))
        assertThat(MSG_ACID, entity.acid, `is`(TEST_ACID))
        assertThat(MSG_DEVICE_ID, entity.deviceId, `is`(TEST_DEVICE_ID))
        assertThat(MSG_REPLACE, entity.replace, `is`(true))
        assertThat(MSG_SKIP_TRIGGERS, entity.skipTriggers, `is`(false))

        assertThat(MSG_CATS_SIZE, entity.cats?.size, `is`(1))
        assertThat(MSG_CAT_NAME, entity.cats?.first()?.name, `is`("news"))
        assertThat(MSG_CAT_ACTIVE, entity.cats?.first()?.active, `is`(true))

        val customFields = entity.customFields.toString()
        assertThat(MSG_CUSTOM_FIELD_A, customFields.contains("\"a\":1"), `is`(true))
        assertThat(MSG_CUSTOM_FIELD_B, customFields.contains("\"b\":\"x\""), `is`(true))

        val profileFields = entity.profileFields.toString()
        assertThat(MSG_PROFILE_FIELD_P, profileFields.contains("\"p\":true"), `is`(true))

        val infos = WorkManager.getInstance(context)
            .getWorkInfosByTag(
                Constants.IN_APP_SUBSCRIBE_O_WORK_TAG
            )
            .get()

        assertThat(MSG_WORKER_ENQUEUED, infos.isNotEmpty(), `is`(true))
        assertThat(MSG_WORKER_STATE, infos.first().state, `is`(WorkInfo.State.ENQUEUED))
    }

    /** - test_2: Invalid custom fields trigger error and no entity is inserted. */
    @Test
    fun inAppSubscribe_invalid_custom_fields_triggers_error_no_insert() = runBlocking {
        SubscriptionProcess.inAppSubscribe(
            context = context,
            customFields = mapOf(
                "bad" to mapOf(
                    "nested" to 123
                )
            )
        )

        Thread.sleep(500)

        verify {
            Events.error(
                eq("[altcraft-core] inAppSubscribe"),
                any()
            )
        }

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(
            MSG_ENTITY_NOT_INSERTED,
            subscriptions.isEmpty(),
            `is`(true)
        )
    }

    /** - test_3: Retry under limit keeps entity and returns true. */
    @Test
    fun isRetry_under_limit_returns_true_and_keeps_entity() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            profileFields = Json.parseToJsonElement("""{"p":true}"""),
            customFields = Json.parseToJsonElement("""{"a":1}"""),
            cats = listOf(
                ProfileModel.CategoryData(
                    name = "news",
                    active = true
                )
            )
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns retry(
            function = "inAppSubscribeRequest"
        )

        coEvery {
            WorkerRequest.hasNewRequest(any(), any(), any())
        } returns false

        val shouldRetry = SubscriptionProcess.isRetry(
            context
        )

        assertThat(MSG_SHOULD_RETRY_TRUE, shouldRetry, `is`(true))

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(MSG_ENTITY_KEPT, subscriptions.isNotEmpty(), `is`(true))
    }

    /** - test_4: Retry under limit with newer worker request keeps entity and returns false. */
    @Test
    fun isRetry_under_limit_with_new_worker_request_returns_false_and_keeps_entity() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}""")
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns retry(
            function = "inAppSubscribeRequest"
        )

        coEvery {
            WorkerRequest.hasNewRequest(any(), any(), any())
        } returns true

        val shouldRetry = SubscriptionProcess.isRetry(
            context
        )

        assertThat(MSG_SHOULD_RETRY_FALSE, shouldRetry, `is`(false))

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(MSG_ENTITY_KEPT, subscriptions.isNotEmpty(), `is`(true))
    }

    /** - test_5: Retry over limit deletes entity and returns false. */
    @Test
    fun isRetry_over_limit_entity_deleted() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}"""),
            retryCount = 6,
            maxRetryCount = 5
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns retry(
            function = "inAppSubscribeRequest"
        )

        val shouldRetry = SubscriptionProcess.isRetry(
            context
        )

        assertThat(MSG_SHOULD_RETRY_FALSE, shouldRetry, `is`(false))

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(MSG_ENTITY_DELETED, subscriptions.isEmpty(), `is`(true))
    }

    /** - test_6: Successful retry deletes entity and saves subscription status. */
    @Test
    fun isRetry_success_deletes_entity_and_saves_subscription_status() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}""")
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns SDKEventModel.Event("ok")

        val shouldRetry = SubscriptionProcess.isRetry(
            context
        )

        assertThat(MSG_SHOULD_RETRY_FALSE, shouldRetry, `is`(false))

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(MSG_ENTITY_DELETED, subscriptions.isEmpty(), `is`(true))

        val status = db.dao.subState
            .get(TEST_USER_TAG)
            ?.status

        assertThat(MSG_STATUS_SAVED, status, `is`(Constants.SUBSCRIBED))
    }

    /** - test_7: Error response deletes entity without saving subscription status. */
    @Test
    fun isRetry_error_deletes_entity_without_saving_status() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}""")
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns error(
            function = "inAppSubscribeRequest"
        )

        val shouldRetry = SubscriptionProcess.isRetry(
            context
        )

        assertThat(MSG_SHOULD_RETRY_FALSE, shouldRetry, `is`(false))

        val subscriptions = dao.getAllEntitiesByTag(
            TEST_USER_TAG
        )

        assertThat(MSG_ENTITY_DELETED, subscriptions.isEmpty(), `is`(true))

        val status = db.dao.subState
            .get(TEST_USER_TAG)
            ?.status

        assertThat(MSG_STATUS_NOT_SAVED, status == null, `is`(true))
    }

    /** - test_8: Saved subscribed status returns true. */
    @Test
    fun getSubscriptionStatus_returns_true_when_saved_status_is_subscribed() = runTest {
        insertSubscribeEntity(
            status = Constants.SUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}""")
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns SDKEventModel.Event("ok")

        SubscriptionProcess.isRetry(
            context
        )

        val status = SubscriptionProcess.getInAppSubscriptionStatus(
            context
        )

        assertThat(MSG_STATUS_TRUE, status, `is`(true))
    }

    /** - test_9: Saved unsubscribed status returns false. */
    @Test
    fun getSubscriptionStatus_returns_false_when_saved_status_is_not_subscribed() = runTest {
        insertSubscribeEntity(
            status = Constants.UNSUBSCRIBED,
            customFields = Json.parseToJsonElement("""{"a":1}""")
        )

        coEvery {
            SubscriptionRequest.inAppSubscribeRequest(any(), any())
        } returns SDKEventModel.Event("ok")

        SubscriptionProcess.isRetry(
            context
        )

        val status = SubscriptionProcess.getInAppSubscriptionStatus(
            context
        )

        assertThat(MSG_STATUS_FALSE, status, `is`(false))
    }

    private suspend fun insertSubscribeEntity(
        status: String,
        sync: Boolean? = true,
        profileFields: kotlinx.serialization.json.JsonElement? = null,
        customFields: kotlinx.serialization.json.JsonElement? = null,
        cats: List<ProfileModel.CategoryData>? = null,
        replace: Boolean? = null,
        skipTriggers: Boolean? = null,
        retryCount: Int = 0,
        maxRetryCount: Int = 5
    ) {
        dao.insert(
            InAppSubscribeEntity(
                userTag = TEST_USER_TAG,
                status = status,
                sync = sync,
                profileFields = profileFields,
                customFields = customFields,
                cats = cats,
                replace = replace,
                skipTriggers = skipTriggers,
                acid = TEST_ACID,
                deviceId = TEST_DEVICE_ID,
                retryCount = retryCount,
                maxRetryCount = maxRetryCount
            )
        )
    }
}
