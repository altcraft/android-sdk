package test.push.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.push.data.room.DB
import com.altcraft.sdk.push.data.room.dao.PushMessageDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PushMessageDaoInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: first seen uid is unique.
 *  - test_2: distinct uids are each unique.
 *  - test_3: trim keeps only the latest 100 uids; a trimmed out uid becomes
 *    unique again.
 *
 * Negative scenarios:
 *  - test_4: an already recorded uid is reported as a duplicate.
 */
@RunWith(AndroidJUnit4::class)
class PushMessageDaoInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
    private lateinit var messageDao: PushMessageDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()
        messageDao = db.dao.message
    }

    @After
    fun teardown() {
        db.close()
    }

    /** - test_1: First seen uid is unique. */
    @Test
    fun first_seen_uid_is_unique() = runBlocking {
        Assert.assertTrue(messageDao.isMessageUnique("uid-1"))
    }

    /** - test_2: Distinct uids are each unique. */
    @Test
    fun distinct_uids_are_each_unique() = runBlocking {
        for (i in 1..3) {
            Assert.assertTrue("uid-$i", messageDao.isMessageUnique("uid-$i"))
        }
    }

    /** - test_3: Trim keeps only the latest 100 uids. */
    @Test
    fun trim_keeps_only_latest_100_uids() = runBlocking {
        for (i in 1..101) {
            Assert.assertTrue("uid-$i", messageDao.isMessageUnique("uid-$i"))
        }

        Assert.assertTrue(messageDao.isMessageUnique("uid-1"))
        Assert.assertFalse(messageDao.isMessageUnique("uid-101"))
    }

    /** - test_4: An already recorded uid is reported as a duplicate. */
    @Test
    fun recorded_uid_is_duplicate() = runBlocking {
        Assert.assertTrue(messageDao.isMessageUnique("uid-dup"))
        Assert.assertFalse(messageDao.isMessageUnique("uid-dup"))
    }
}
