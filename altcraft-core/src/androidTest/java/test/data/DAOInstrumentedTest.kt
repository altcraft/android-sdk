package test.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.models.CoreModels.PropertyModel
import com.altcraft.sdk.data.room.dao.ConfigurationDao
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.room.TestRoom

/**
 * DaoInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: config_insert_get_and_delete
 */
@RunWith(AndroidJUnit4::class)
class DaoInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: TestRoom
    private lateinit var configDao: ConfigurationDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TestRoom::class.java)
            .allowMainThreadQueries()
            .build()
        configDao = db.configurationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    /** - test_1: Insert/get and delete from configuration table. */
    @Test
    fun config_insert_get_and_delete() = runBlocking {
        val cfg = ConfigurationEntity(
            id = 0,
            apiUrl = "https://api.example.com",
            rToken = "rt",
            appInfo = PropertyModel.AppInfo(appID = "demo", appVer = "1.0", appIID = "")
        )

        configDao.insert(cfg)

        val saved = configDao.get()
        Assert.assertNotNull(saved)
        Assert.assertEquals("https://api.example.com", saved!!.apiUrl)

        configDao.delete()
        Assert.assertNull(configDao.get())
    }
}
