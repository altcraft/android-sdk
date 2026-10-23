package test.additional

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.util.MapBuilder
import com.altcraft.sdk.config.ConfigManager
import com.altcraft.sdk.data.models.CoreModels.PropertyModel
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import com.altcraft.sdk.core.environment.Environment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MapBuilderInstrumentedTest
 *
 * Positive scenarios:
 * - test_1: Returns a non-null map and preserves custom fields passed to unionMaps().
 * - test_2: Merges AppInfo into the result map — fills "_app_id", "_app_iid", "_app_ver".
 *
 * Negative scenarios:
 * - test_3: When AppInfo is null, unionMaps() omits all "_app_*" keys in the result.
 */
@RunWith(AndroidJUnit4::class)
class MapBuilderInstrumentedTest {

    private companion object {
        const val KEY_APP_ID  = "_app_id"
        const val KEY_APP_IID = "_app_iid"
        const val KEY_APP_VER = "_app_ver"

        const val MSG_NOT_NULL         = "unionMaps must return non-null map"
        const val MSG_CUSTOM_PRESERVED = "Custom field must be preserved"
        const val MSG_APP_ID_MERGED    = "_app_id must be merged from AppInfo"
        const val MSG_APP_IID_MERGED   = "_app_iid must be merged from AppInfo"
        const val MSG_APP_VER_MERGED   = "_app_ver must be merged from AppInfo"
        const val MSG_APP_ID_OMITTED   = "_app_id must be omitted when AppInfo is null"
        const val MSG_APP_IID_OMITTED  = "_app_iid must be omitted when AppInfo is null"
        const val MSG_APP_VER_OMITTED  = "_app_ver must be omitted when AppInfo is null"
    }

    private fun configurationDao() =
        Environment.create(ApplicationProvider.getApplicationContext())
            .room
            .configurationDao()

    @Before
    fun setUp() {
        runBlocking {
            ConfigManager.conf = null
            configurationDao().delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            configurationDao().delete()
            ConfigManager.conf = null
        }
    }

    /** - test_1: unionMaps() returns non-null map and preserves custom fields. */
    @Test
    fun unionMaps_does_not_throw_and_preserves_custom_fields() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val custom = mapOf("x" to 1)

        val result = MapBuilder.unionMaps(context, custom)
        assertNotNull(MSG_NOT_NULL, result)
        assertEquals(MSG_CUSTOM_PRESERVED, 1, result["x"])
    }

    /** - test_2: unionMaps() merges AppInfo fields into result map. */
    @Test
    fun unionMaps_merges_appInfo_when_present() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val appInfo = PropertyModel.AppInfo(appID = "id123", appIID = "iid456", appVer = "1.0.0")
        val dao = configurationDao()
        dao.insert(
            ConfigurationEntity(
                id = 0,
                apiUrl = "https://api.test",
                rToken = null,
                appInfo = appInfo
            )
        )

        val result = MapBuilder.unionMaps(context, emptyMap())
        assertNotNull(MSG_NOT_NULL, result)
        assertEquals(MSG_APP_ID_MERGED,  "id123", result[KEY_APP_ID])
        assertEquals(MSG_APP_IID_MERGED, "iid456", result[KEY_APP_IID])
        assertEquals(MSG_APP_VER_MERGED, "1.0.0", result[KEY_APP_VER])
    }

    /** - test_3: unionMaps() omits "_app_*" keys when AppInfo is null. */
    @Test
    fun unionMaps_omits_appInfo_keys_when_null() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dao = configurationDao()
        dao.insert(
            ConfigurationEntity(
                id = 0,
                apiUrl = "https://api.test",
                rToken = null,
                appInfo = null
            )
        )

        val result = MapBuilder.unionMaps(context, emptyMap())
        assertNotNull(MSG_NOT_NULL, result)
        assertFalse(MSG_APP_ID_OMITTED,  result.containsKey(KEY_APP_ID))
        assertFalse(MSG_APP_IID_OMITTED, result.containsKey(KEY_APP_IID))
        assertFalse(MSG_APP_VER_OMITTED, result.containsKey(KEY_APP_VER))
    }
}
