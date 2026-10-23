package com.altcraft.sdk.inapp.data.models

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.ResponseModel.BaseResponseData
import com.altcraft.sdk.inapp.data.Constants.ALL_SMID_KEY
import com.altcraft.sdk.inapp.data.mapper.InAppMapper
import com.altcraft.sdk.inapp.data.room.entity.InAppDbModel
import com.altcraft.sdk.inapp.data.room.entity.InAppsEntity
import com.altcraft.sdk.util.AppVersionMatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models of the Altcraft In-App module.
 *
 * Contains:
 * - [InAppModels.InAppModel] — campaign, placement, trigger, and rendering models;
 * - [InAppModels.ResponseModel] — server response models.
 */
internal object InAppModels {

    /**
     * Contains models used by In-App functionality:
     * - raw backend campaigns and placements;
     * - normalized In-App campaign data;
     * - placement options, policies, and availability rules;
     * - trigger events and trigger metadata;
     * - content type and rendering resources;
     * - events sent from rendered In-App content.
     */
    object InAppModel {

        /**
         * Raw in-app campaign received from backend.
         *
         * @property companyId Campaign identifier.
         * @property contentUrl Campaign content url.
         * @property isTest Whether the campaign is testing.
         * @property smid Message identifier.
         * @property type Campaign content type.
         * @property deadline Campaign deadline in RFC3339 format.
         * @property lastCompanyUpdate Last campaign update timestamp.
         */
        @Serializable
        data class RawInApp(
            @SerialName("id")
            val companyId: Int? = 0,
            @SerialName("link")
            val contentUrl: String,
            @SerialName("is_test")
            val isTest: Boolean = false,
            val smid: String = "",
            val type: String = "custom",
            val deadline: Long = 0,
            @SerialName("last_company_update")
            val lastCompanyUpdate: Long = 0
        )

        /**
         * Raw placement settings received from backend.
         *
         * @property id Campaign identifier.
         * @property checksum Placement settings checksum.
         * @property options Placement configuration.
         */
        @Serializable
        data class RawPlacement(
            val id: Int,
            val checksum: String,
            val options: PlacementOptions
        )

        /**
         * In-app placement campaign.
         *
         * @property companyId Campaign identifier.
         * @property smid Message identifier.
         * @property type Campaign content type.
         * @property contentUrl Campaign content url.
         * @property checksum Placement settings checksum.
         * @property options Placement configuration.
         * @property deadline Campaign deadline in RFC3339 format.
         * @property isTest Whether the campaign is testing.
         * @property lastCompanyUpdate Last campaign update timestamp.
         * @property actionTriggerName Action trigger name.
         * @property actionTriggerType Action trigger type.
         */
        @Serializable
        data class InApp(
            val companyId: Int,
            val smid: String,
            val type: InAppContentType,
            val contentUrl: String,
            val checksum: String,
            val options: PlacementOptions,
            val deadline: Long = 0,
            val isTest: Boolean = false,
            val lastCompanyUpdate: Long,
            var actionTriggerName: String = "",
            var actionTriggerType: TriggerType = TriggerType.CUSTOM,
        )

        /**
         * Placement settings.
         *
         * @property endTime Campaign end date in RFC3339 format.
         * @property policy Display limit rules.
         * @property isAnonim Whether the campaign is available for anonymous users.
         * @property placements Allowed placement identifiers.
         * @property appVersions Allowed app versions.
         * @property appVersionConditionRaw Raw app version comparison condition.
         * @property priority Placement priority from 0 to 10.
         * @property triggerEvents Events that trigger the placement.
         * @property impressionsLimit Max impressions allowed for one device during the whole campaign.
         * @property availability Time-based display restrictions.
         */
        @Serializable
        @Suppress("SpellCheckingInspection")
        data class PlacementOptions(
            @SerialName("end_date")
            val endTime: Long,
            @SerialName("placement_policy")
            val policy: Policy? = null,
            @SerialName("is_anonim")
            val isAnonim: Boolean = false,
            val placements: List<String> = emptyList(),
            @SerialName("app_versions")
            val appVersions: List<String> = emptyList(),
            @SerialName("app_version_condition")
            val appVersionConditionRaw: String? = null,
            val priority: Int = 0,
            @SerialName("trigger_events")
            val triggerEvents: List<TriggerEvent> = emptyList(),
            @SerialName("impressions_limit")
            val impressionsLimit: Int = 0,
            val availability: Availability? = null
        ) {

            /**
             * Parsed app version comparison condition.
             */
            val appVersionCondition: AppVersionMatcher.Operator?
                get() = AppVersionMatcher.Operator.fromValue(
                    appVersionConditionRaw
                )
        }

        /**
         * Placement display policy.
         *
         * @property limitBySession Max displays per session.
         * @property limitByTrigger Max displays per trigger in session.
         * @property limitByPeriod Max displays allowed for a period.
         * @property minIntervalSec Min interval between displays in seconds.
         */
        @Serializable
        data class Policy(
            @SerialName("limit_by_session")
            val limitBySession: Int = 0,
            @SerialName("session_trigger_limit")
            val limitByTrigger: Int = 0,
            @SerialName("limit_by_period")
            val limitByPeriod: LimitByPeriod = LimitByPeriod(),
            @SerialName("min_interval_sec")
            val minIntervalSec: Long = 0,
        )

        /**
         * Display limit for a time period.
         *
         * @property periodDays Period length in days.
         * @property limit Max displays in the period.
         */
        @Serializable
        data class LimitByPeriod(
            @SerialName("period_days")
            val periodDays: Int = 0,

            @SerialName("limit")
            val limit: Int = 0
        )

        /**
         * Event that can trigger placement display.
         *
         * @property type Event type.
         * @property name Event name.
         * @property delay Delay before display in seconds.
         */
        @Serializable
        data class TriggerEvent(
            val type: String = "",
            val name: String = "",
            val delay: Int = 0
        )

        /**
         * Placement availability rules.
         *
         * @property periods Allowed time periods during a day.
         * @property mdays Allowed days of month.
         * @property wdays Allowed days of week.
         * @property months Allowed months.
         * @property years Allowed years.
         * @property tz Time zone for availability checks.
         */
        @Suppress("SpellCheckingInspection")
        @Serializable
        data class Availability(
            val periods: List<TimePeriod> = emptyList(),
            val mdays: List<Int> = emptyList(),
            val wdays: List<Int> = emptyList(),
            val months: List<Int> = emptyList(),
            val years: List<Int> = emptyList(),
            val tz: String = "",
            @SerialName("wdayords")
            val wdayOrders: List<Int>? = emptyList()
        )

        /**
         * Daily allowed time period.
         *
         * @property from Start time.
         * @property to End time.
         */
        @Serializable
        data class TimePeriod(
            val from: TimePoint = TimePoint(),
            val to: TimePoint = TimePoint()
        )

        /**
         * Time of day.
         *
         * @property hour Hour value.
         * @property min Minute value.
         */
        @Serializable
        data class TimePoint(
            val hour: Int = 0,
            val min: Int = 0
        )

        /**
         * In-app trigger type.
         *
         * @property value Serialized trigger type value.
         */
        enum class TriggerType(val value: String) {
            DEFAULT("default"),
            CUSTOM("custom");

            companion object {

                /**
                 * Returns trigger type by serialized [value].
                 */
                fun fromValue(value: String?): TriggerType {
                    return entries.firstOrNull {
                        it.value.equals(value, ignoreCase = true)
                    } ?: CUSTOM
                }
            }
        }

        /**
         * In-app display trigger.
         *
         * @property name Trigger name.
         * @property type Trigger type.
         */
        @Serializable
        data class InAppTrigger(
            val name: String,
            val type: TriggerType = TriggerType.CUSTOM
        )

        /**
         * In-app content type.
         */
        enum class InAppContentType {
            JSON,
            BLOCK,
            CUSTOM;

            companion object {

                /**
                 * Returns content type by raw [value].
                 */
                fun from(value: String?): InAppContentType {
                    return when (value?.lowercase()) {
                        "json" -> JSON
                        "block" -> BLOCK
                        else -> CUSTOM
                    }
                }
            }
        }

        /**
         * Static resources required for in-app rendering.
         *
         * @property inAppLibraryJs In-app library JavaScript.
         * @property blockJs Block renderer JavaScript.
         * @property blockCss Block renderer CSS.
         */
        data class InAppStatic(
            val inAppLibraryJs: String?,
            val blockJs: String?,
            val blockCss: String?
        )

        /**
         * Payload for event sent from rendered in-app content.
         *
         * @property name Event name.
         * @property href Link url.
         * @property code Error code.
         * @property message Error message.
         */
        @Serializable
        data class InAppEventData(
            val name: String? = null,
            val href: String? = null,
            val code: Int? = null,
            val message: String? = null
        )
    }

    /**
     * Contains parsed in-app server response models and response processing helpers.
     */
    object ResponseModel {

        /**
         * Response containing in-app campaigns and their placement settings.
         *
         * Raw campaigns and placement options are returned as separate lists.
         *
         * Regular campaigns are joined by campaign id with placement settings
         * and mapped to Room models.
         *
         * Test campaigns are not persisted in Room and are converted directly
         * into runtime [InAppModel.InApp] models.
         *
         * @property error Response error code.
         * @property errorText Response error message.
         * @property lastPublish Last in-app publish timestamp.
         * @property checksum Placement rules checksum.
         * @property rawInApps Raw in-app campaigns.
         * @property rawPlacements Raw placement settings.
         */
        @Serializable
        @Suppress("SpellCheckingInspection")
        internal data class ResponseWithInApps(
            override val error: Int? = null,
            @SerialName("error_text")
            override val errorText: String? = null,
            @SerialName("last_publish")
            val lastPublish: Long = 0,
            @SerialName("placement_rules_checksum")
            val checksum: String = "",
            @SerialName("inapps")
            private val rawInApps: List<InAppModel.RawInApp> = emptyList(),
            @SerialName("placements")
            private val rawPlacements: List<InAppModel.RawPlacement> = emptyList()
        ) : BaseResponseData {
            override val eventData: Map<String, Any?>
                get() = super.eventData + (ALL_SMID_KEY to rawInApps.map { it.smid })

            /**
             * Regular in-app campaigns mapped to Room database models.
             *
             * Only campaigns where `is_test != true` are persisted.
             *
             * Raw campaigns are joined with placement settings by campaign id.
             * Items without matching placement settings are skipped.
             */
            val inApps: List<InAppDbModel>
                get() {
                    val placementsById = rawPlacements.associateBy { it.id }

                    return rawInApps
                        .filter { !it.isTest }
                        .mapNotNull { rawInApp ->
                            val rawPlacement = placementsById[
                                rawInApp.companyId
                            ] ?: return@mapNotNull null

                            InAppMapper.toDbModel(
                                rawInApp = rawInApp,
                                rawPlacement = rawPlacement
                            )
                        }
                }

            /**
             * Test in-app campaigns converted directly to runtime models.
             *
             * Test campaigns are selected by `is_test == true`.
             * They do not require placement settings and are not saved to Room.
             *
             * Missing placement-related fields are replaced with default values
             * inside [InAppMapper.rawToTestInApp].
             */
            val testInApps: List<InAppModel.InApp>
                get() {
                    return rawInApps
                        .filter { it.isTest }
                        .map { rawInApp ->
                            InAppMapper.rawToTestInApp(
                                rawInApp
                            )
                        }
                }

            /**
             * Creates persisted in-app synchronization state
             * from response metadata and current request context.
             *
             * @param acid Altcraft identifier.
             * @param deviceId Device identifier.
             * @param lastRequestTime Last request timestamp.
             * @return Persisted in-app synchronization state entity.
             */
            fun toStateEntity(
                acid: String,
                deviceId: String,
                lastRequestTime: Long,
            ): InAppsEntity {
                return InAppsEntity(
                    acid = acid,
                    deviceId = deviceId,
                    lastPublish = lastPublish,
                    placementRulesChecksum = checksum,
                    lastRequestTime = lastRequestTime
                )
            }
        }

        /**
         * Response containing in-app notification content.
         *
         * @property content In-app notification content.
         * @property images Image links used in the in-app notification content.
         */
        @Serializable
        internal data class InAppContent(
            val content: String = "",
            @SerialName("images_links")
            val images: List<String> = emptyList(),
            @SerialName("display_type")
            val displayType: String,
        )
    }
}
