@file:Suppress("SpellCheckingInspection")

package test.inapp.placements

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppEntity
import com.altcraft.sdk.inapp.platform.placements.PlacementCountManager
import com.altcraft.sdk.inapp.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * PlacementsTrimTest
 *
 * Positive scenarios:
 *  - test_1: a list at or below the limit is returned unchanged.
 *  - test_2: a list above the limit keeps the newest entries by companyId.
 *  - test_3: a single-entry list is returned unchanged.
 *  - test_4: an empty limit returns an empty list.
 */
class PlacementsTrimTest {

    @Before
    fun setUp() {
        val event = mockk<SDKEventModel.Event>(relaxed = true)
        val error = mockk<SDKEventModel.Error>(relaxed = true)
        mockkObject(Log)
        every { Log.event(any(), any(), any(), any()) } returns event
        every { Log.error(any(), any(), any(), any()) } returns error
    }

    @After
    fun tearDown() = unmockkAll()

    /**
     * Builds a minimal [InAppDbModel] keyed by [companyId].
     */
    private fun model(companyId: Int) = InAppDbModel(
        entity = InAppEntity(
            companyId = companyId,
            contentUrl = "https://content/$companyId",
            checksum = "checksum-$companyId",
            endDate = 0L,
            type = "custom",
            smid = "smid-$companyId",
            appVersions = emptyList(),
            appVersionCondition = "",
            isAnonim = false,
            impressionsLimit = 0,
            priority = 0,
            policy = null,
            availability = null,
            actionTriggerName = "",
            actionTriggerType = "custom",
            deadline = 0L,
            lastCompanyUpdate = 0L
        ),
        placements = emptyList(),
        triggerEvents = emptyList()
    )

    /** test_1: a list at or below the limit is returned unchanged. */
    @Test
    fun test_1_atOrBelowLimit_returnsUnchanged() {
        val items = (1..500).map { model(it) }

        val result = PlacementCountManager.trimToMax(items, 500)

        assertEquals(500, result.size)
        assertEquals(items, result)

        val below = (1..300).map { model(it) }
        val belowResult = PlacementCountManager.trimToMax(below, 500)
        assertEquals(below, belowResult)
    }

    /** test_2: a list above the limit keeps the newest entries by companyId (ascending). */
    @Test
    fun test_2_overLimit_keepsNewestByCompanyId() {
        val items = (1..700).map { model(it) }

        val result = PlacementCountManager.trimToMax(items, 500)

        assertEquals(500, result.size)
        val ids = result.map { it.entity.companyId }
        assertEquals((201..700).toList(), ids)
    }

    /** test_3: a single-entry list is returned unchanged. */
    @Test
    fun test_3_singleEntry_returnsUnchanged() {
        val items = listOf(model(1))

        val result = PlacementCountManager.trimToMax(items, 1)

        assertEquals(1, result.size)
        assertEquals(1, result[0].entity.companyId)
    }

    /** test_4: a non-positive limit returns an empty list. */
    @Test
    fun test_4_nonPositiveLimit_returnsEmpty() {
        val items = (1..10).map { model(it) }

        val result = PlacementCountManager.trimToMax(items, 0)

        assertEquals(emptyList<InAppDbModel>(), result)
    }
}
