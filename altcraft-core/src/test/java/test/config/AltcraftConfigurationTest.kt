@file:Suppress("SpellCheckingInspection")

package test.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.core.modules.ModuleConfiguration
import com.altcraft.sdk.data.models.CoreModels.PropertyModel
import com.altcraft.sdk.data.room.entities.ConfigurationEntity
import org.junit.Test
import org.junit.Assert.*

/**
 * AltcraftConfigurationTest
 *
 * Positive scenarios:
 *  - test_1: Valid apiUrl → build() non-null; toEntity() returns entity.
 *  - test_2: Valid apiUrl with rToken → toEntity() returns entity; rToken preserved.
 *  - test_3: Valid apiUrl with appInfo → toEntity() returns entity; appInfo preserved.
 *  - test_4: Valid apiUrl with enableLogging → build() non-null.
 *  - test_6: addModuleConfiguration → config returned by module id.
 *  - test_7: build() produces immutable snapshot of module configurations.
 *  - test_8: addModuleConfiguration replaces configuration of the same module.
 */
class AltcraftConfigurationTest {

    private companion object {
        private const val API_URL = "https://api.example.com"
        private const val RTOKEN = "rt_test123"

        private const val MODULE_A = "module-a"
        private const val MODULE_B = "module-b"

        private const val MSG_EXPECT_ENTITY_VALID = "Expected non-null config for valid parameters"
        private const val MSG_EXPECT_RTOKEN = "Expected rToken to be preserved"
        private const val MSG_EXPECT_APPINFO = "Expected appInfo to be preserved"
        private const val MSG_EXPECT_MODULE_CONFIG = "Expected module configuration to be stored"
        private const val MSG_EXPECT_NO_MODULE_CONFIG = "Expected no module configuration"
    }

    /** Simple module configuration used for builder tests. */
    private class TestModuleConfiguration(
        override val moduleId: String
    ) : ModuleConfiguration

    /** - test_1: Valid apiUrl → entity created. */
    @Test
    fun toEntity_valid_apiUrl_pass() {
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL
        ).build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        val entity: ConfigurationEntity = cfg!!.toEntity()
        assertEquals(API_URL, entity.apiUrl)
    }

    /** - test_2: Valid apiUrl with rToken → entity created; rToken preserved. */
    @Test
    fun toEntity_valid_withRToken_pass() {
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            rToken = RTOKEN
        ).build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        val entity = cfg!!.toEntity()
        assertEquals(MSG_EXPECT_RTOKEN, RTOKEN, entity.rToken)
    }

    /** - test_3: Valid apiUrl with appInfo → entity created; appInfo preserved. */
    @Test
    fun toEntity_valid_withAppInfo_pass() {
        val appInfo = PropertyModel.AppInfo("app1", "iid1", "1.0.0")
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            appInfo = appInfo
        ).build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        val entity = cfg!!.toEntity()
        assertEquals(MSG_EXPECT_APPINFO, appInfo, entity.appInfo)
    }

    /** - test_4: Valid apiUrl with enableLogging → config created. */
    @Test
    fun toEntity_valid_withEnableLogging_pass() {
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL,
            enableLogging = true
        ).build()

        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)
    }

    /** - test_6: addModuleConfiguration → config stored and returned by module id. */
    @Test
    fun addModuleConfiguration_configStored_pass() {
        val moduleConfig = TestModuleConfiguration(MODULE_A)
        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL
        ).addModuleConfiguration(moduleConfig).build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        assertEquals(
            MSG_EXPECT_MODULE_CONFIG,
            moduleConfig,
            cfg!!.getModuleConfiguration(MODULE_A)
        )
        assertNull(
            MSG_EXPECT_NO_MODULE_CONFIG,
            cfg.getModuleConfiguration(MODULE_B)
        )
    }

    /** - test_7: build() produces immutable snapshot of module configurations. */
    @Test
    fun build_moduleConfigurationsSnapshot_immutable() {
        val builder = AltcraftConfiguration.Builder(apiUrl = API_URL)
        val moduleA = TestModuleConfiguration(MODULE_A)

        builder.addModuleConfiguration(moduleA)
        val cfg = builder.build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        builder.addModuleConfiguration(TestModuleConfiguration(MODULE_B))

        assertEquals(
            MSG_EXPECT_MODULE_CONFIG,
            moduleA,
            cfg!!.getModuleConfiguration(MODULE_A)
        )
        assertNull(
            MSG_EXPECT_NO_MODULE_CONFIG,
            cfg.getModuleConfiguration(MODULE_B)
        )
    }

    /** - test_8: addModuleConfiguration replaces configuration of the same module. */
    @Test
    fun addModuleConfiguration_configReplaced_pass() {
        val oldConfig = TestModuleConfiguration(MODULE_A)
        val newConfig = TestModuleConfiguration(MODULE_A)

        val cfg = AltcraftConfiguration.Builder(
            apiUrl = API_URL
        )
            .addModuleConfiguration(oldConfig)
            .addModuleConfiguration(newConfig)
            .build()
        assertNotNull(MSG_EXPECT_ENTITY_VALID, cfg)

        assertEquals(
            MSG_EXPECT_MODULE_CONFIG,
            newConfig,
            cfg!!.getModuleConfiguration(MODULE_A)
        )
    }
}
