package com.altcraft.sdk.inapp.data.room.dao

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppPlacementEntity
import com.altcraft.sdk.inapp.data.room.entity.InAppTriggerEventEntity
import com.altcraft.sdk.inapp.data.room.entity.PersonalPolicyEntity
import com.altcraft.sdk.inapp.data.mapper.InAppMapper

/**
 * DAO for in-app campaign operations.
 */
@Dao
internal interface InAppDao {

    /**
     * Inserts main in-app entities.
     *
     * @param items entities to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(items: List<InAppEntity>)

    /**
     * Inserts placement entities.
     *
     * @param items placements to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacements(items: List<InAppPlacementEntity>)

    /**
     * Inserts trigger event entities.
     *
     * @param items trigger events to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggerEvents(items: List<InAppTriggerEventEntity>)

    /**
     * Deletes one in-app record by company id.
     *
     * Related placements and trigger events
     * are deleted automatically via foreign key cascade.
     *
     * Personal policy rows are not affected.
     *
     * @param companyId campaign identifier.
     */
    @Query(
        """
        DELETE FROM in_app
        WHERE company_id = :companyId
        """
    )
    suspend fun deleteInApp(companyId: Int)

    /**
     * Saves full list of in-app database models.
     *
     * Existing in-app records are deleted first.
     * Related placements and trigger events are deleted automatically via foreign key cascade.
     * Main entities and all related child rows are inserted in batches.
     *
     * If the list is empty, all in-app records and related child rows are deleted.
     *
     * Personal policy rows are not affected.
     *
     * @param items in-app database models to save.
     */
    @Transaction
    suspend fun saveInApps(items: List<InAppDbModel>) {
        deleteAllInApps()

        if (items.isEmpty()) return

        val entities = items.map { it.entity }
        val placements = items.flatMap { it.placements }
        val events = items.flatMap { it.triggerEvents }

        insertEntities(entities)

        if (placements.isNotEmpty()) insertPlacements(placements)
        if (events.isNotEmpty()) insertTriggerEvents(events)
    }

    /**
     * Returns company ids of target in-app records filtered by trigger conditions
     * and optional placement.
     *
     * Sorting rules:
     * - lower priority first
     * - for equal priority, non-anonymous in-apps first
     * - for equal priority and anonymity flag, higher company id first
     *
     * Placement rules:
     * - if placement is null or empty, placement filtering is skipped
     *   and all matching in-apps are returned
     * - if placement is not empty, in-apps without placements are returned too,
     *   as well as in-apps containing the given placement
     *
     * @param triggerType trigger type.
     * @param eventName event name.
     * @param placement optional placement.
     * @return ordered list of matching company ids.
     */
    @Query(
        """
        SELECT DISTINCT i.company_id
        FROM in_app AS i
        INNER JOIN in_app_trigger_event AS te
            ON te.company_id = i.company_id
        WHERE te.type = :triggerType
          AND te.name = :eventName
          AND (
                :placement IS NULL
                OR :placement = ''
                OR NOT EXISTS (
                    SELECT 1
                    FROM in_app_placement AS p
                    WHERE p.company_id = i.company_id
                )
                OR EXISTS (
                    SELECT 1
                    FROM in_app_placement AS p
                    WHERE p.company_id = i.company_id
                      AND p.placement = :placement
                )
          )
        ORDER BY i.priority ASC,
                 i.is_anonim ASC,
                 i.company_id DESC
        """
    )
    suspend fun getTargetCompanyIds(
        triggerType: String,
        eventName: String,
        placement: String?
    ): List<Int>

    /**
     * Returns main in-app entities for the specified company ids.
     *
     * @param companyIds campaign identifiers.
     * @return list of matching entities.
     */
    @Query(
        """
        SELECT *
        FROM in_app
        WHERE company_id IN (:companyIds)
        """
    )
    suspend fun getEntitiesByCompanyIds(companyIds: List<Int>): List<InAppEntity>

    /**
     * Returns placement entities for the specified company ids.
     *
     * @param companyIds campaign identifiers.
     * @return list of matching placement entities.
     */
    @Query(
        """
        SELECT *
        FROM in_app_placement
        WHERE company_id IN (:companyIds)
        """
    )
    suspend fun getPlacementsByCompanyIds(companyIds: List<Int>): List<InAppPlacementEntity>

    /**
     * Returns trigger event entities for the specified company ids.
     *
     * @param companyIds campaign identifiers.
     * @return list of matching trigger event entities.
     */
    @Query(
        """
        SELECT *
        FROM in_app_trigger_event
        WHERE company_id IN (:companyIds)
        """
    )
    suspend fun getEventsByCompanyIds(companyIds: List<Int>): List<InAppTriggerEventEntity>

    /**
     * Returns mapped in-app list for the specified trigger conditions.
     *
     * The method:
     * - selects ordered company ids in SQL
     * - loads related rows only for selected company ids
     * - restores DTOs preserving SQL order
     *
     * @param triggerType trigger type.
     * @param eventName event name.
     * @param placement optional placement.
     * @return ordered list of mapped in-app models.
     */
    @Transaction
    suspend fun getTargetList(
        triggerType: String,
        eventName: String,
        placement: String?
    ): List<InApp> {
        val companyIds = getTargetCompanyIds(
            triggerType = triggerType,
            eventName = eventName,
            placement = placement
        )

        if (companyIds.isEmpty()) return emptyList()

        val entities = getEntitiesByCompanyIds(companyIds)
        val placements = getPlacementsByCompanyIds(companyIds)
        val events = getEventsByCompanyIds(companyIds)

        val entityMap = entities.associateBy { it.companyId }
        val placementMap = placements.groupBy { it.companyId }
        val eventMap = events.groupBy { it.companyId }

        return companyIds.mapNotNull { companyId ->
            val entity = entityMap[companyId] ?: return@mapNotNull null

            InAppMapper.toDto(
                InAppDbModel(
                    entity = entity,
                    placements = placementMap[companyId].orEmpty(),
                    triggerEvents = eventMap[companyId].orEmpty()
                )
            )
        }
    }

    /**
     * Inserts personal policy entities in a batch.
     *
     * Existing rows with the same primary key are ignored, so stored personal
     * policy state is not overwritten.
     *
     * @param entities personal policy entities to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPersonalPolicies(entities: List<PersonalPolicyEntity>)

    /**
     * Saves personal policy rows for the specified in-app database models.
     *
     * Existing rows with the same primary key are ignored, so previously stored
     * personal policy state such as open count and delivery report status is
     * preserved.
     *
     * @param acid current account identifier.
     * @param items in-app database models used to create personal policy rows.
     */
    @Transaction
    suspend fun savePersonalPolicies(
        acid: String,
        items: List<InAppDbModel>
    ) {
        if (items.isEmpty()) return

        val policies = items.map {
            PersonalPolicyEntity(
                acid = acid,
                smid = it.entity.smid,
                openCount = 0,
                deliveryReport = false
            )
        }

        insertPersonalPolicies(policies)
    }

    /**
     * Returns personal open count for the specified SMID.
     *
     * @param smid policy identifier.
     * @return open count or null if row does not exist.
     */
    @Query(
        """
        SELECT open_count
        FROM personal_policy
        WHERE smid = :smid
        LIMIT 1
        """
    )
    suspend fun getOpenCount(smid: String): Int?

    /**
     * Increments personal open count for the specified SMID.
     *
     * @param smid policy identifier.
     */
    @Query(
        """
        UPDATE personal_policy
        SET open_count = open_count + 1
        WHERE smid = :smid
        """
    )
    suspend fun incrementOpenCount(smid: String)

    /**
     * Marks delivery report as sent for the specified SMID.
     *
     * @param smid policy identifier.
     */
    @Query(
        """
        UPDATE personal_policy
        SET delivery_report = 1
        WHERE smid = :smid
        """
    )
    suspend fun setReportStatus(smid: String)

    /**
     * Returns company ids of in-apps whose delivery report has not been sent yet.
     *
     * @return company ids linked to personal policies with deliveryReport == false.
     */
    @Query(
        """
        SELECT ia.company_id
        FROM in_app AS ia
        INNER JOIN personal_policy AS pp
            ON pp.smid = ia.smid
        WHERE pp.delivery_report = 0
        """
    )
    suspend fun getUnreportedCompanyIds(): List<Int>

    /**
     * Returns in-apps whose delivery report has not been sent yet.
     *
     * @return in-app models linked to personal policies with deliveryReport == false.
     */
    @Transaction
    suspend fun getUnreportedInApps(): List<InApp> {
        val companyIds = getUnreportedCompanyIds()

        if (companyIds.isEmpty()) return emptyList()

        val entities = getEntitiesByCompanyIds(companyIds)
        val placements = getPlacementsByCompanyIds(companyIds)
        val events = getEventsByCompanyIds(companyIds)

        val entityMap = entities.associateBy { it.companyId }
        val placementMap = placements.groupBy { it.companyId }
        val eventMap = events.groupBy { it.companyId }

        return companyIds.mapNotNull { companyId ->
            val entity = entityMap[companyId] ?: return@mapNotNull null

            InAppMapper.toDto(
                InAppDbModel(
                    entity = entity,
                    placements = placementMap[companyId].orEmpty(),
                    triggerEvents = eventMap[companyId].orEmpty()
                )
            )
        }
    }

    /**
     * Deletes personal policy rows for the specified ACID whose SMID is no longer
     * present in the latest in-app database models.
     *
     * @param acid campaign identifier.
     * @param smidList actual policy identifiers for this ACID.
     */
    @Query(
        """
        DELETE FROM personal_policy
        WHERE acid = :acid
          AND smid NOT IN (:smidList)
        """
    )
    suspend fun deletePersonalPoliciesNotIn(
        acid: String,
        smidList: List<String>
    )

    /**
     * Deletes all in-app records.
     *
     * Related placements and trigger events
     * are deleted automatically via foreign key cascade.
     *
     * Personal policy rows are not affected.
     */
    @Query(
        """
        DELETE FROM in_app
        """
    )
    suspend fun deleteAllInApps()

    /**
     * Deletes all personal policy rows.
     */
    @Query(
        """
        DELETE FROM personal_policy
        """
    )
    suspend fun deleteAllInAppsPersonalPolicies()

    /**
     * Deletes all in-app data.
     *
     * Removes main in-app records, related child rows via foreign key cascade,
     * and all personal policy rows.
     */
    @Transaction
    suspend fun clearInAppData() {
        deleteAllInApps()
        deleteAllInAppsPersonalPolicies()
    }
}