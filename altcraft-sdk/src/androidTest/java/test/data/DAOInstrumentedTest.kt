package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.data.room.DAO
import test.room.TestRoom
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.PushEventEntity
import com.altcraft.sdk.data.room.SubscribeEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import org.junit.*
import org.junit.runner.RunWith

/**
 * DaoInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: config_insert_get_updateProviderList_and_delete
 *            insert ConfigurationEntity, verify getConfig(), updateProviderPriorityList,
 *            and deleteConfig clears table.
 *  - test_2: subscribe_insert_query_order_and_exists
 *            insert multiple SubscribeEntity, verify allSubscriptionsByTag ordering and
 *            subscriptionsExistsByTag true/false.
 *  - test_3: subscribe_increaseRetry_and_deleteByUid
 *            insert SubscribeEntity, increase retry count, then delete by UID.
 *  - test_4: subscribe_deleteAll
 *            insert multiple SubscribeEntity, deleteAllSubscriptions clears table.
 *  - test_5: push_insert_query_order_desc_and_exists_count
 *            insert multiple PushEventEntity, verify ordering (DESC), exists, and count.
 *  - test_6: push_increaseRetry_getOldest_limit_and_delete
 *            verify increasePushEventRetryCount, getOldestPushEvents(limit),
 *            delete by UID, and batch delete.
 *  - test_7: push_deleteAll
 *            insert multiple PushEventEntity, deleteAllPushEvents clears table.
 *
 * Negative scenarios:
 *  - Not directly tested; invalid/malformed data prevented at Room level.
 *
 * Notes:
 *  - Instrumented Android tests (androidTest), run with real in-memory Room database.
 *  - Uses TestRoom with production entities and converters.
 *  - Ensures DAO queries perform correctly on real Room-generated implementation.
 */
@RunWith(AndroidJUnit4::class)
class DaoInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: TestRoom
    private lateinit var dao: DAO

    // Real provider constants
    private companion object {
        const val FCM_PROVIDER = "android-firebase"
        const val HMS_PROVIDER = "android-huawei"
        const val RUS_PROVIDER = "android-rustore"

        const val SUBSCRIBED = "subscribed"
        const val UNSUBSCRIBED = "unsubscribed"
        const val SUSPENDED = "suspended"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.request()
    }

    @After
    fun teardown() {
        db.close()
    }

    /** Verifies insert/get/updateProviderPriorityList/delete for configurationTable. */
    @Test
    fun config_insert_get_updateProviderList_and_delete() = runBlocking {
        val cfg = ConfigurationEntity(
            id = 0,
            icon = null,
            apiUrl = "https://api.example.com",
            rToken = "rt",
            appInfo = DataClasses.AppInfo(appID = "demo", appVer = "1.0", appIID = ""),
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = listOf(FCM_PROVIDER, HMS_PROVIDER),
            providerPriorityList = listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER),
            pushChannelName = "ch",
            pushChannelDescription = "cd"
        )

        dao.insertConfig(cfg)

        val saved1 = dao.getConfig()
        Assert.assertNotNull(saved1)
        Assert.assertEquals("https://api.example.com", saved1!!.apiUrl)
        Assert.assertEquals(
            listOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER),
            saved1.providerPriorityList
        )

        dao.updateProviderPriorityList(listOf(HMS_PROVIDER, FCM_PROVIDER))

        val saved2 = dao.getConfig()
        Assert.assertNotNull(saved2)
        Assert.assertEquals(
            listOf(HMS_PROVIDER, FCM_PROVIDER),
            saved2!!.providerPriorityList
        )

        val deleted = dao.deleteConfig()
        Assert.assertTrue(deleted >= 1)
        Assert.assertNull(dao.getConfig())
    }

    /** Verifies insert/orderBy ASC/exists for subscribeTable. */
    @Test
    fun subscribe_insert_query_order_and_exists() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val e1 = SubscribeEntity(
            userTag = "tagA",
            status = SUBSCRIBED,
            sync = null,
            profileFields = JsonNull,
            customFields = JsonNull,
            cats = null,
            replace = null,
            skipTriggers = null,
            uid = "sub-1",
            time = now - 10,
            retryCount = 0,
            maxRetryCount = 3
        )
        val e2 = e1.copy(uid = "sub-2", time = now - 5)
        val e3 = e1.copy(uid = "sub-3", time = now)

        dao.insertSubscribe(e1)
        dao.insertSubscribe(e2)
        dao.insertSubscribe(e3)

        val list = dao.allSubscriptionsByTag("tagA")
        Assert.assertEquals(listOf("sub-1", "sub-2", "sub-3"), list.map { it.uid })

        Assert.assertTrue(dao.subscriptionsExistsByTag("tagA"))
        Assert.assertFalse(dao.subscriptionsExistsByTag("tagB"))
    }

    /** Verifies increaseSubscribeRetryCount and deleteSubscribeByUid. */
    @Test
    fun subscribe_increaseRetry_and_deleteByUid() = runBlocking {
        val e = SubscribeEntity(
            userTag = "tagX",
            status = SUSPENDED,
            sync = null,
            profileFields = JsonNull,
            customFields = JsonNull,
            cats = null,
            replace = null,
            skipTriggers = null,
            uid = "sub-x",
            time = System.currentTimeMillis() / 1000,
            retryCount = 1,
            maxRetryCount = 5
        )
        dao.insertSubscribe(e)

        dao.increaseSubscribeRetryCount("sub-x", e.retryCount + 1)
        val afterInc = dao.allSubscriptionsByTag("tagX").first { it.uid == "sub-x" }
        Assert.assertEquals(2, afterInc.retryCount)

        val del = dao.deleteSubscribeByUid("sub-x")
        Assert.assertEquals(1, del)
        Assert.assertFalse(dao.subscriptionsExistsByTag("tagX"))
    }

    /** Verifies deleteAllSubscriptions clears subscribeTable. */
    @Test
    fun subscribe_deleteAll() = runBlocking {
        dao.insertSubscribe(
            SubscribeEntity("t", SUBSCRIBED, null, JsonNull, JsonNull, null, null, null, "a")
        )
        dao.insertSubscribe(
            SubscribeEntity("t", UNSUBSCRIBED, null, JsonNull, JsonNull, null, null, null, "b")
        )
        Assert.assertEquals(2, dao.allSubscriptionsByTag("t").size)

        val deleted = dao.deleteAllSubscriptions()
        Assert.assertEquals(2, deleted)
        Assert.assertTrue(dao.allSubscriptionsByTag("t").isEmpty())
    }

    /** Verifies insert/getAll DESC/exists/count for pushEventTable. */
    @Test
    fun push_insert_query_order_desc_and_exists_count() = runBlocking {
        val now = System.currentTimeMillis() / 1000
        val p1 = PushEventEntity(uid = "p1", type = "opened", time = now - 10)
        val p2 = PushEventEntity(uid = "p2", type = "opened", time = now - 1)
        val p3 = PushEventEntity(uid = "p3", type = "received", time = now)

        dao.insertPushEvent(p1)
        dao.insertPushEvent(p2)
        dao.insertPushEvent(p3)

        val list = dao.getAllPushEvents()
        Assert.assertEquals(listOf("p3", "p2", "p1"), list.map { it.uid })

        Assert.assertTrue(dao.pushEventsExists())
        Assert.assertEquals(3, dao.getPushEventCount())
    }

    /** Verifies increasePushEventRetryCount/getOldest(limit)/delete operations. */
    @Test
    fun push_increaseRetry_getOldest_limit_and_delete() = runBlocking {
        val base = System.currentTimeMillis() / 1000
        val p1 = PushEventEntity(uid = "pa", type = "opened", time = base - 30, retryCount = 0)
        val p2 = PushEventEntity(uid = "pb", type = "opened", time = base - 20, retryCount = 1)
        val p3 = PushEventEntity(uid = "pc", type = "opened", time = base - 10, retryCount = 2)
        val p4 = PushEventEntity(uid = "pd", type = "opened", time = base, retryCount = 0)

        dao.insertPushEvent(p1)
        dao.insertPushEvent(p2)
        dao.insertPushEvent(p3)
        dao.insertPushEvent(p4)

        dao.increasePushEventRetryCount("pb", 5)
        val pb = dao.getAllPushEvents().first { it.uid == "pb" }
        Assert.assertEquals(5, pb.retryCount)

        val oldest2 = dao.getOldestPushEvents(2)
        Assert.assertEquals(listOf("pa", "pb"), oldest2.map { it.uid })

        val delOne = dao.deletePushEventByUid("pc")
        Assert.assertEquals(1, delOne)

        dao.deletePushEvents(oldest2)
        val remaining = dao.getAllPushEvents().map { it.uid }
        Assert.assertEquals(listOf("pd"), remaining)
    }

    /** Verifies deleteAllPushEvents clears pushEventTable. */
    @Test
    fun push_deleteAll() = runBlocking {
        dao.insertPushEvent(PushEventEntity("u1", "opened"))
        dao.insertPushEvent(PushEventEntity("u2", "received"))
        Assert.assertTrue(dao.pushEventsExists())

        dao.deleteAllPushEvents()
        Assert.assertFalse(dao.pushEventsExists())
        Assert.assertEquals(0, dao.getPushEventCount())
    }
}
