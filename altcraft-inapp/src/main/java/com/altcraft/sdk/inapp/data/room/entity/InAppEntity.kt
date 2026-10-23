package com.altcraft.sdk.inapp.data.room.entity

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Availability
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Policy

/**
 * Main in-app campaign entity.
 *
 * Stores scalar campaign fields shared by all related child rows.
 *
 * The campaign is uniquely identified by companyId.
 */
@Entity(
    tableName = "in_app",
    primaryKeys = ["company_id"],
    indices = [
        Index("company_id"),
        Index("priority"),
        Index("last_company_update")
    ]
)
@Suppress("SpellCheckingInspection")
internal data class InAppEntity(
    @ColumnInfo(name = "company_id")
    val companyId: Int,
    @ColumnInfo(name = "content_url")
    val contentUrl: String,
    @ColumnInfo(name = "checksum")
    val checksum: String,
    @ColumnInfo(name = "end_date")
    val endDate: Long,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "smid")
    val smid: String,
    @ColumnInfo(name = "app_versions")
    val appVersions: List<String>,
    @ColumnInfo(name = "app_ver_condition")
    val appVersionCondition: String,
    @ColumnInfo(name = "is_anonim")
    val isAnonim: Boolean,
    @ColumnInfo(name = "impressions_limit")
    val impressionsLimit: Int,
    @ColumnInfo(name = "priority")
    val priority: Int,
    @ColumnInfo(name = "policy")
    val policy: Policy?,
    @ColumnInfo(name = "availability")
    val availability: Availability?,
    @ColumnInfo(name = "action_trigger_name")
    val actionTriggerName: String,
    @ColumnInfo(name = "action_trigger_type")
    val actionTriggerType: String,
    @ColumnInfo(name = "deadline")
    val deadline: Long,
    @ColumnInfo(name = "last_company_update")
    val lastCompanyUpdate: Long
)

/**
 * Child entity storing placement identifiers for one in-app campaign.
 *
 * Linked to parent in-app campaign by companyId.
 */
@Entity(
    tableName = "in_app_placement",
    foreignKeys = [
        ForeignKey(
            entity = InAppEntity::class,
            parentColumns = ["company_id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("company_id"),
        Index("placement"),
        Index(value = ["company_id", "placement"])
    ]
)
internal data class InAppPlacementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "company_id")
    val companyId: Int,

    @ColumnInfo(name = "placement")
    val placement: String
)

/**
 * Child entity storing trigger events for one in-app campaign.
 *
 * Linked to parent in-app campaign by companyId.
 */
@Entity(
    tableName = "in_app_trigger_event",
    foreignKeys = [
        ForeignKey(
            entity = InAppEntity::class,
            parentColumns = ["company_id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("company_id"),
        Index(value = ["type", "name"]),
        Index(value = ["company_id", "type", "name"])
    ]
)
internal data class InAppTriggerEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "company_id")
    val companyId: Int,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "delay")
    val delay: Int
)

/**
 * Aggregated in-app database model.
 *
 * Combines one parent entity with related placement and trigger event rows.
 *
 * @property entity Main in-app campaign entity.
 * @property placements Placement rows linked to the campaign.
 * @property triggerEvents Trigger event rows linked to the campaign.
 */
internal data class InAppDbModel(
    val entity: InAppEntity,
    val placements: List<InAppPlacementEntity>,
    val triggerEvents: List<InAppTriggerEventEntity>
)

/**
 * Per-user policy entity for one in-app campaign.
 *
 * Stores personal counters independently of the main campaign data.
 *
 * The policy row is uniquely identified by `smid`.
 */
@Entity(
    tableName = "personal_policy",
    primaryKeys = ["smid"],
    indices = [
        Index("smid")
    ]
)
internal data class PersonalPolicyEntity(
    @ColumnInfo(name = "smid")
    val smid: String,
    @ColumnInfo(name = "acid")
    val acid: String,
    @ColumnInfo(name = "delivery_report")
    val deliveryReport: Boolean = false,
    @ColumnInfo(name = "open_count")
    val openCount: Int = 0
)