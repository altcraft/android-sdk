package com.altcraft.sdk.inapp.platform.placements

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.config.InAppConfiguration
import com.altcraft.sdk.inapp.data.Constants.LIMIT
import com.altcraft.sdk.inapp.data.Constants.RECEIVED
import com.altcraft.sdk.inapp.data.Constants.REJECTED_COMPANY_IDS
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles placement count limits and their changes.
 */
internal object PlacementCountManager {

    private const val FUNCTION = "inAppPlacements"

    /**
     * Indicates whether the maximum placement limit has changed.
     */
    internal val maxPlacementsChanged = AtomicBoolean(false)

    /**
     * Checks whether the maximum placement limit differs from the stored value.
     *
     * Resets the placement rules checksum when the limit has changed.
     *
     * @param context Application context.
     * @param conf In-app configuration containing the new placement limit.
     */
    suspend fun checkMaxPlacementChange(
        context: Context,
        conf: InAppConfiguration
    ) {
        val dao = Environment.create(context).room.dao
        val saved = dao.config.get()?.maxPlacements
            ?: return

        if (saved == conf.maxPlacements)
            return

        maxPlacementsChanged.set(true)
        dao.inApps.resetChecksum()
    }

    /**
     * Trims in-app placements to the configured maximum.
     *
     * When the limit is exceeded, placements with the largest `companyId`
     * are kept and an event containing rejected company IDs is emitted.
     *
     * @param items Received in-app placements.
     * @param max Maximum number of placements to keep.
     * @return At most [max] in-app placements.
     */
    fun trimToMax(
        items: List<InAppDbModel>,
        max: Int
    ): List<InAppDbModel> {
        if (items.size <= max) return items

        val sorted = items.sortedBy {
            it.entity.companyId
        }
        val rejectedCompanyIds = sorted.dropLast(max).map {
                it.entity.companyId
            }

        sendLimitExceededEvent(
            rejectedCompanyIds = rejectedCompanyIds,
            received = items.size,
            limit = max
        )

        return sorted.takeLast(max)
    }

    /**
     * Emits an event when received placements exceed the configured limit.
     *
     * @param rejectedCompanyIds Company IDs rejected due to the limit.
     * @param received Number of received placements.
     * @param limit Configured placement limit.
     */
    private fun sendLimitExceededEvent(
        rejectedCompanyIds: List<Int>,
        received: Int,
        limit: Int
    ) {
        val value = mapOf(
            REJECTED_COMPANY_IDS to rejectedCompanyIds,
            RECEIVED to received,
            LIMIT to limit
        )

        Log.event(
            function = FUNCTION,
            event = ModuleEvents.placementsLimitExceeded,
            eventValue = value,
            logValue = value
        )
    }
}