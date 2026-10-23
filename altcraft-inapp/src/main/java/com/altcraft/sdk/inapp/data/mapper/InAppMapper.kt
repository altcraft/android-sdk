package com.altcraft.sdk.inapp.data.mapper

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.RawInApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.RawPlacement
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerEvent
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppPlacementEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppTriggerEventEntity

@Suppress("SpellCheckingInspection")
/**
 * Maps in-app models between DTO layer and Room entities.
 *
 * Mapper responsibilities:
 * - convert one in-app DTO into the main Room entity
 * - split nested collections into child tables:
 *   - placements
 *   - trigger events
 * - restore DTO back from aggregated Room model
 *
 * Database structure:
 * - [com.altcraft.sdk.inapp.data.room.entity.InAppEntity] stores main campaign data
 * - [com.altcraft.sdk.inapp.data.room.entity.InAppPlacementEntity] stores placement identifiers linked by companyId
 * - [com.altcraft.sdk.inapp.data.room.entity.InAppTriggerEventEntity] stores trigger events linked by companyId
 *
 * Source payload semantics:
 * backend returns campaign list and campaign placement settings separately.
 * After parsing, the application operates on unified [InApp] DTO,
 * and this mapper persists that unified model into Room.
 */
internal object InAppMapper {

    /**
     * Restores one in-app DTO from aggregated Room model.
     *
     * Input model contains:
     * - one main entity
     * - zero or more placements
     * - zero or more trigger events
     *
     * The method rebuilds [InApp] by:
     * - taking scalar fields from [InAppEntity]
     * - collecting placements from [InAppPlacementEntity]
     * - converting trigger rows into [TriggerEvent]
     *
     * @param model aggregated Room model.
     * @return restored in-app DTO.
     */
    fun toDto(model: InAppDbModel): InApp =
        InApp(
            companyId = model.entity.companyId,
            contentUrl = model.entity.contentUrl,
            checksum = model.entity.checksum,
            type = InAppContentType.from(model.entity.type),
            smid = model.entity.smid,
            options = PlacementOptions(
                endTime = model.entity.endDate,
                policy = model.entity.policy,
                isAnonim = model.entity.isAnonim,
                placements = model.placements.map { it.placement },
                appVersions = model.entity.appVersions,
                appVersionConditionRaw = model.entity.appVersionCondition,
                priority = model.entity.priority,
                triggerEvents = model.triggerEvents.map { triggerEvent ->
                    TriggerEvent(
                        type = triggerEvent.type,
                        name = triggerEvent.name,
                        delay = triggerEvent.delay
                    )
                },
                impressionsLimit = model.entity.impressionsLimit,
                availability = model.entity.availability
            ),
            actionTriggerName = model.entity.actionTriggerName,
            actionTriggerType = TriggerType.fromValue(
                model.entity.actionTriggerType
            ),
            deadline = model.entity.deadline,
            lastCompanyUpdate = model.entity.lastCompanyUpdate
        )

    /**
     * Builds aggregated Room model from raw in-app campaign data
     * and its placement settings.
     *
     * The backend returns campaign fields and placement settings separately:
     * - [RawInApp] contains campaign scalar fields
     * - [RawPlacement] contains checksum and placement options
     *
     * This method merges both payload parts into [InAppDbModel]:
     * - creates main [InAppEntity]
     * - maps placement names to [InAppPlacementEntity]
     * - maps trigger events to [InAppTriggerEventEntity]
     *
     * The first trigger event is duplicated into [InAppEntity] as
     * `actionTriggerName` and `actionTriggerType` for quick access.
     *
     * @param rawInApp raw campaign data from response.
     * @param rawPlacement raw placement settings for the same campaign.
     * @return aggregated Room model ready to be saved.
     */
    fun toDbModel(
        rawInApp: RawInApp,
        rawPlacement: RawPlacement
    ): InAppDbModel {
        val actionTrigger = rawPlacement.options.triggerEvents.firstOrNull()

        return InAppDbModel(
            entity = InAppEntity(
                companyId = rawInApp.companyId ?: 0,
                contentUrl = rawInApp.contentUrl,
                checksum = rawPlacement.checksum,
                endDate = rawPlacement.options.endTime,
                type = rawInApp.type,
                smid = rawInApp.smid,
                appVersions = rawPlacement.options.appVersions,
                appVersionCondition = rawPlacement.options.appVersionConditionRaw.orEmpty(),
                isAnonim = rawPlacement.options.isAnonim,
                impressionsLimit = rawPlacement.options.impressionsLimit,
                priority = rawPlacement.options.priority,
                policy = rawPlacement.options.policy,
                availability = rawPlacement.options.availability,
                actionTriggerName = actionTrigger?.name.orEmpty(),
                actionTriggerType = actionTrigger?.type.orEmpty(),
                deadline = rawInApp.deadline,
                lastCompanyUpdate = rawInApp.lastCompanyUpdate
            ),
            placements = rawPlacement.options.placements.map { placementName ->
                InAppPlacementEntity(
                    companyId = rawInApp.companyId ?: 0,
                    placement = placementName
                )
            },
            triggerEvents = rawPlacement.options.triggerEvents.map { triggerEvent ->
                InAppTriggerEventEntity(
                    companyId = rawInApp.companyId ?: 0,
                    type = triggerEvent.type,
                    name = triggerEvent.name,
                    delay = triggerEvent.delay
                )
            }
        )
    }

    /**
     * Converts raw test in-app payload into runtime [InApp].
     *
     * Missing fields are replaced with default values.
     *
     * @param rawInApp raw test campaign data.
     * @return runtime test in-app DTO.
     */
    fun rawToTestInApp(
        rawInApp: RawInApp
    ): InApp {
        return InApp(
            companyId = rawInApp.companyId ?: 0,
            smid = rawInApp.smid,
            type = InAppContentType.from(rawInApp.type),
            contentUrl = rawInApp.contentUrl,
            checksum = "",
            options = PlacementOptions(
                endTime = 0,
                policy = null,
                isAnonim = true,
                placements = emptyList(),
                appVersions = emptyList(),
                appVersionConditionRaw = null,
                priority = 0,
                triggerEvents = emptyList(),
                impressionsLimit = 0,
                availability = null
            ),
            deadline = 0,
            isTest = true,
            lastCompanyUpdate = rawInApp.lastCompanyUpdate,
            actionTriggerName = "",
            actionTriggerType = TriggerType.CUSTOM
        )
    }
}