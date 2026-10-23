package com.altcraft.sdk.inapp.system.policy

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.content.res.Configuration
import com.altcraft.sdk.util.AppVersionMatcher
import com.altcraft.sdk.inapp.data.Preferenses.getInAppLastOpenTime
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TimePeriod
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TimePoint
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess.getInAppSubscriptionStatus
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.getCurrentScreen
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.getOpenBySession
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.getTriggerCount
import java.util.Calendar
import java.util.TimeZone
import kotlin.collections.contains

/**
 * Collection of checks that determine whether an In-App campaign
 * can be displayed based on policy, time availability, deadline, and placement.
 */
internal object Check {

    /**
     * Checks common runtime conditions required to display an In-App campaign.
     *
     * @param context Application context.
     * @param inApp In-App campaign to check.
     * @param trigger Trigger associated with the campaign.
     *
     * @return `true` if all runtime display conditions are satisfied.
     */
    suspend fun canShow(
        context: Context,
        inApp: InApp,
        trigger: InAppTrigger
    ): Boolean {
        return Availability.canShow(
            inApp
        ) && Deadline.canShow(
            context, inApp
        ) && Orientation.canShow(
            context
        ) && CommonPolicy.canShow(
            context, inApp, trigger
        ) && SubStatusCheck.canShow(
            context, inApp
        ) && PersonalPolicy.canShow(
            context, inApp
        )
    }

    /**
     * Screen orientation checks.
     */
    object Orientation {

        /**
         * Checks whether current screen orientation allows In-App display.
         *
         * In-App messages are not allowed in landscape orientation.
         */
        fun canShow(context: Context) = context.resources.configuration.orientation !=
                Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Policy-based checks for display frequency and session limits.
     */
    object CommonPolicy {

        /**
         * Validates whether an In-App campaign can be shown according to policy settings.
         *
         * The check includes:
         * - trigger count limit
         * - session display limit
         * - total open count limit
         * - minimal interval between displays
         *
         * @param context Application context used to read persisted display data.
         * @param inApp In-App placement campaign.
         *
         * @return `true` if the campaign is allowed to be shown by policy.
         */
        suspend fun canShow(
            context: Context,
            inApp: InApp,
            trigger: InAppTrigger
        ): Boolean {
            val policy = inApp.options.policy ?: return true

            val lastOpenTime = getInAppLastOpenTime(context)

            val triggerCount = getTriggerCount(trigger.type, trigger.name)

            val triggerLimitExceeded = policy.limitByTrigger in 1..triggerCount

            val sessionLimitExceeded =
                policy.limitBySession > 0 && getOpenBySession() >= policy.limitBySession

            val minIntervalNotPassed =
                policy.minIntervalSec > 0 &&
                        System.currentTimeMillis() < lastOpenTime + policy.minIntervalSec * 1000

            return !(triggerLimitExceeded || sessionLimitExceeded || minIntervalNotPassed)
        }
    }

    /**
     * Policy-based checks for display frequency and session limits.
     */
    object PersonalPolicy {

        /**
         * Checks if the In-App campaign can be shown based on open count limit.
         *
         * @param context Application context used to access the local database.
         * @param inApp In-App placement campaign.
         *
         * @return `true` if the campaign can be shown, otherwise `false`.
         */
        suspend fun canShow(
            context: Context,
            inApp: InApp
        ): Boolean {
            val limitByOpenCount = inApp.options.impressionsLimit

            val currentOpenCount = Environment.create(context)
                .room
                .dao
                .inApp
                .getOpenCount(
                    inApp.smid
                ) ?: 0

            return limitByOpenCount == 0 || currentOpenCount < limitByOpenCount
        }

        /**
         * Updates personal open count for the given in-app.
         *
         * Creates a new record if none exists, otherwise increments the counter.
         * Deletes the in-app when the configured open limit is reached.
         *
         * @param context context to access database.
         * @param inApp target in-app.
         */
        suspend fun checkPersonalPolicy(context: Context, inApp: InApp) {
            val env = Environment.create(context)
            val inAppDao = env.room.dao.inApp

            val currentOpenCount = inAppDao.getOpenCount(inApp.smid)

            val limit = inApp.options.impressionsLimit

            val newOpenCount = currentOpenCount?.plus(1) ?: 1

            inAppDao.incrementOpenCount(inApp.smid)

            if (limit in 1..newOpenCount) inAppDao.deleteInApp(
                inApp.companyId
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    /**
     * Time-based checks for campaign availability.
     */
    object Availability {

        /**
         * Checks whether the current moment matches the configured availability rules.
         *
         * The check includes:
         * - years
         * - months
         * - month days
         * - week days
         * - week day order in month
         * - time periods
         *
         * @param inApp In-App placement campaign.
         * @param nowMillis Current time in milliseconds. Uses system time by default.
         *
         * @return `true` if the current time is within the allowed schedule.
         */
        fun canShow(
            inApp: InApp,
            nowMillis: Long = System.currentTimeMillis()
        ): Boolean {
            val availability = inApp.options.availability ?: return true

            val timeZone = try {
                TimeZone.getTimeZone(availability.tz)
            } catch (_: Throwable) {
                TimeZone.getTimeZone("UTC")
            }

            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
            }

            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentMinutesOfDay = currentHour * 60 + currentMinute

            val currentWday = mapDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentMday = calendar.get(Calendar.DAY_OF_MONTH)
            val wdayOrders = availability.wdayOrders ?: emptyList()

            val yearOk = availability.years.isEmpty() || currentYear in availability.years
            val monthOk = availability.months.isEmpty() || currentMonth in availability.months
            val mdayOk = availability.mdays.isEmpty() || currentMday in availability.mdays
            val wdayOk = availability.wdays.isEmpty() || currentWday in availability.wdays
            val wdayOrderOk = wdayOrders.isEmpty() || wdayOrders.any {
                matchesWeekdayOrderInMonth(it, calendar)
            }
            val periodOk = availability.periods.isEmpty() || availability.periods.any {
                matchesPeriod(it, currentMinutesOfDay)
            }

            return yearOk && monthOk && mdayOk && wdayOk && wdayOrderOk && periodOk
        }

        /**
         * Converts [Calendar.DAY_OF_WEEK] value to SDK weekday format.
         *
         * Sunday is mapped to `0`, Monday to `1`, Saturday to `6`.
         *
         * @param calendarDayOfWeek Day of week from [Calendar].
         *
         * @return Weekday number in SDK format.
         */
        private fun mapDayOfWeek(calendarDayOfWeek: Int): Int {
            return when (calendarDayOfWeek) {
                Calendar.SUNDAY -> 0
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> -1
            }
        }

        /**
         * Checks whether current date matches configured weekday order in month.
         *
         * Rules:
         * - `0` means the filter is disabled.
         * - `1..5` mean the first through fifth occurrence of the same weekday in the current month.
         * - `-1` means the last occurrence of the same weekday in the current month.
         * - Unknown values are treated as allowed to keep backend compatibility.
         *
         * @param order Weekday order in month.
         * @param calendar Current date calendar.
         *
         * @return `true` if current date matches the order.
         */
        private fun matchesWeekdayOrderInMonth(
            order: Int,
            calendar: Calendar
        ): Boolean {
            if (order == 0) return true

            return when (order) {
                in 1..5 -> {
                    var currentWeekIndex = 1

                    val previousSameWeekday = calendar.clone() as Calendar

                    while (true) {
                        previousSameWeekday.add(Calendar.DAY_OF_MONTH, -7)

                        if (
                            previousSameWeekday.get(Calendar.MONTH)
                            != calendar.get(Calendar.MONTH) ||
                            previousSameWeekday.get(Calendar.YEAR)
                            != calendar.get(Calendar.YEAR)
                        ) {
                            break
                        }

                        currentWeekIndex++
                    }

                    currentWeekIndex == order
                }

                -1 -> {
                    val nextSameWeekday = calendar.clone() as Calendar
                    nextSameWeekday.add(Calendar.DAY_OF_MONTH, 7)

                    nextSameWeekday.get(Calendar.MONTH) != calendar.get(Calendar.MONTH) ||
                            nextSameWeekday.get(Calendar.YEAR) != calendar.get(Calendar.YEAR)
                }

                else -> true
            }
        }

        /**
         * Checks whether current time matches the specified time period.
         *
         * Supports both regular ranges and ranges crossing midnight.
         *
         * @param period Configured availability period.
         * @param currentMinutesOfDay Current time as minutes from start of day.
         *
         * @return `true` if current time is inside the period.
         */
        private fun matchesPeriod(
            period: TimePeriod,
            currentMinutesOfDay: Int
        ): Boolean {
            val from = period.from.toMinutesOfDay()
            val to = period.to.toMinutesOfDay()

            return when {
                from == to -> true
                from < to -> currentMinutesOfDay in from..<to
                else -> currentMinutesOfDay !in to..<from
            }
        }

        /**
         * Converts a time point to minutes from the start of day.
         *
         * @receiver Time point with hour and minute values.
         *
         * @return Total minutes from `00:00`.
         */
        private fun TimePoint.toMinutesOfDay(): Int {
            return hour * 60 + min
        }
    }

    /**
     * Expiration checks for campaign lifetime.
     */
    object Deadline {

        /**
         * Checks whether an In-App campaign is still valid by deadline and end time.
         *
         * If the campaign is expired, it is removed from the local database.
         *
         * @param context Application context used to access the local database.
         * @param inApp In-App placement campaign.
         *
         * @return `true` if the campaign is still active.
         */
        suspend fun canShow(
            context: Context,
            inApp: InApp
        ): Boolean {
            if (inApp.deadline == 0L) return true
            val currentTime = System.currentTimeMillis()
            if (inApp.deadline < currentTime || inApp.options.endTime < currentTime) {
                Environment.create(context).room.dao.inApp.deleteInApp(inApp.companyId)
                return false
            } else return true
        }
    }

    /**
     * Placement checks for campaign display location.
     */
    object Placement {

        /**
         * Checks whether the current placement is allowed for an In-App campaign.
         *
         * @param inApp In-App placement campaign.
         *
         * @return `true` if placement matches or no placement restrictions are configured.
         */
        fun canShow(inApp: InApp): Boolean {
            return !(inApp.options.placements.isNotEmpty() && !inApp.options.placements.contains(
                getCurrentScreen()
            ))
        }
    }

    /**
     * App version checks.
     */
    object Version {

        /**
         * Checks whether the campaign can be shown for the current app version.
         *
         * Allows display when versions, condition, or current app version are missing.
         */
        suspend fun canShow(
            context: Context,
            inApp: InApp
        ): Boolean {
            val appVersions = inApp.options.appVersions
            if (appVersions.isEmpty()) return true

            val operator = inApp.options.appVersionCondition ?: return true

            val appVersion = Environment.create(context)
                .appVersion()
                .orEmpty()

            if (appVersion.isBlank()) return true

            return AppVersionMatcher.matches(
                appVersion = appVersion, appVersions = appVersions, operator = operator
            )
        }
    }

    /**
     * Subscription status checks.
     */
    object SubStatusCheck {

        /**
         * Checks whether the current user is allowed to receive the in-app.
         *
         * Anonymous in-app campaigns are always allowed.
         * Non-anonymous campaigns require the user to have SUBSCRIBED status.
         *
         * @param context Android context.
         * @param inApp In-App placement campaign.
         * @return `true` if the campaign can be shown for the current user.
         */
        suspend fun canShow(
            context: Context,
            inApp: InApp
        ): Boolean {
            if (inApp.options.isAnonim) return true

            return getInAppSubscriptionStatus(context)
        }
    }
}