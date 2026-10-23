@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Availability
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Policy
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.RawInApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.RawPlacement
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerEvent
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppPlacementEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppTriggerEventEntity
import com.altcraft.sdk.inapp.data.mapper.InAppMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InAppMapperTest
 *
 * Positive scenarios:
 *  - test_1: toDto maps InAppDbModel fields to InApp correctly.
 *  - test_2: toDto maps placements and trigger events.
 *  - test_3: toDbModel merges RawInApp and RawPlacement into InAppDbModel.
 *  - test_4: toDbModel uses first trigger event for actionTrigger fields.
 *  - test_5: rawToTestInApp creates test InApp with default values.
 */
class InAppMapperTest {

    private companion object {
        const val COMPANY_ID = 42
        const val SMID = "smid-test-1"
        const val CONTENT_URL = "https://example.com/campaign"
        const val CHECKSUM = "abc123"
        const val PLACEMENT = "home_banner"
    }

    // ── toDto ──────────────────────────────────────────────────────────────

    /** test_1: toDto maps scalar fields from entity to InApp. */
    @Test
    fun test_1_toDto_mapsScalarFields() {
        val entity = inAppEntity()
        val model = InAppDbModel(entity, placements(), triggerEvents())

        val result = InAppMapper.toDto(model)

        assertEquals(COMPANY_ID, result.companyId)
        assertEquals(SMID, result.smid)
        assertEquals(CONTENT_URL, result.contentUrl)
        assertEquals(CHECKSUM, result.checksum)
        assertEquals(InAppContentType.CUSTOM, result.type)
        assertEquals(1000L, result.deadline)
        assertEquals(2000L, result.lastCompanyUpdate)
        assertEquals("open_app", result.actionTriggerName)
        assertEquals(TriggerType.DEFAULT, result.actionTriggerType)
    }

    /** test_2: toDto maps placements and trigger events. */
    @Test
    fun test_2_toDto_mapsPlacementsAndTriggerEvents() {
        val entity = inAppEntity()
        val placement = InAppPlacementEntity(companyId = COMPANY_ID, placement = PLACEMENT)
        val trigger = InAppTriggerEventEntity(companyId = COMPANY_ID, type = "custom", name = "btn", delay = 5)
        val model = InAppDbModel(entity, listOf(placement), listOf(trigger))

        val result = InAppMapper.toDto(model)

        assertEquals(listOf(PLACEMENT), result.options.placements)
        assertEquals(1, result.options.triggerEvents.size)
        assertEquals("custom", result.options.triggerEvents[0].type)
        assertEquals("btn", result.options.triggerEvents[0].name)
        assertEquals(5, result.options.triggerEvents[0].delay)
    }

    // ── toDbModel ──────────────────────────────────────────────────────────

    /** test_3: toDbModel merges RawInApp and RawPlacement into InAppDbModel. */
    @Test
    fun test_3_toDbModel_mergesRawData() {
        val rawInApp = RawInApp(
            companyId = COMPANY_ID,
            contentUrl = CONTENT_URL,
            smid = SMID,
            type = "json",
            deadline = 1000L,
            lastCompanyUpdate = 2000L
        )
        val rawPlacement = RawPlacement(
            id = COMPANY_ID,
            checksum = CHECKSUM,
            options = PlacementOptions(
                endTime = 500L,
                isAnonim = true,
                placements = listOf(PLACEMENT),
                triggerEvents = listOf(TriggerEvent(type = "default", name = "open_app", delay = 3))
            )
        )

        val result = InAppMapper.toDbModel(rawInApp, rawPlacement)

        assertEquals(COMPANY_ID, result.entity.companyId)
        assertEquals(CONTENT_URL, result.entity.contentUrl)
        assertEquals(CHECKSUM, result.entity.checksum)
        assertEquals("json", result.entity.type)
        assertEquals(SMID, result.entity.smid)
        assertEquals(1, result.placements.size)
        assertEquals(PLACEMENT, result.placements[0].placement)
        assertEquals(1, result.triggerEvents.size)
        assertEquals("default", result.triggerEvents[0].type)
        assertEquals("open_app", result.triggerEvents[0].name)
    }

    /** test_4: toDbModel sets actionTrigger from first trigger event. */
    @Test
    fun test_4_toDbModel_setsActionTriggerFromFirstEvent() {
        val rawInApp = RawInApp(companyId = COMPANY_ID, contentUrl = CONTENT_URL)
        val rawPlacement = RawPlacement(
            id = COMPANY_ID,
            checksum = CHECKSUM,
            options = PlacementOptions(
                endTime = 0,
                triggerEvents = listOf(
                    TriggerEvent(type = "default", name = "open_page", delay = 0),
                    TriggerEvent(type = "custom", name = "button_x", delay = 5)
                )
            )
        )

        val result = InAppMapper.toDbModel(rawInApp, rawPlacement)

        assertEquals("open_page", result.entity.actionTriggerName)
        assertEquals("default", result.entity.actionTriggerType)
    }

    // ── rawToTestInApp ─────────────────────────────────────────────────────

    /** test_5: rawToTestInApp creates test InApp with defaults. */
    @Test
    fun test_5_rawToTestInApp_createsTestInApp() {
        val rawInApp = RawInApp(
            companyId = COMPANY_ID,
            contentUrl = CONTENT_URL,
            smid = SMID,
            type = "block"
        )

        val result = InAppMapper.rawToTestInApp(rawInApp)

        assertEquals(COMPANY_ID, result.companyId)
        assertEquals(CONTENT_URL, result.contentUrl)
        assertEquals(SMID, result.smid)
        assertEquals(InAppContentType.BLOCK, result.type)
        assertTrue(result.isTest)
        assertEquals("", result.checksum)
        assertEquals(0, result.deadline)
        assertEquals(0, result.options.endTime)
        assertNull(result.options.policy)
        assertTrue(result.options.isAnonim)
        assertTrue(result.options.placements.isEmpty())
        assertTrue(result.options.appVersions.isEmpty())
        assertEquals(TriggerType.CUSTOM, result.actionTriggerType)
        assertEquals("", result.actionTriggerName)
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun inAppEntity() = InAppEntity(
        companyId = COMPANY_ID,
        contentUrl = CONTENT_URL,
        checksum = CHECKSUM,
        endDate = 500L,
        type = "custom",
        smid = SMID,
        appVersions = listOf("1.0", "2.0"),
        appVersionCondition = ">=",
        isAnonim = false,
        impressionsLimit = 3,
        priority = 5,
        policy = Policy(limitBySession = 2),
        availability = Availability(),
        actionTriggerName = "open_app",
        actionTriggerType = "default",
        deadline = 1000L,
        lastCompanyUpdate = 2000L
    )

    private fun placements() = listOf(
        InAppPlacementEntity(companyId = COMPANY_ID, placement = PLACEMENT)
    )

    private fun triggerEvents() = listOf(
        InAppTriggerEventEntity(companyId = COMPANY_ID, type = "default", name = "open_app", delay = 0)
    )
}
