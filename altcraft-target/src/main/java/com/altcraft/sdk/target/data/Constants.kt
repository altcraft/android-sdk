package com.altcraft.sdk.target.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Contains constants used by the Target module.
 *
 * Includes module identifiers, command queue names, worker tags,
 * subscription channel values, request paths, and compact field keys
 * used when building Target API requests.
 */
internal object Constants {

    // General
    const val MODULE_NAME = "altcraft-target"
    const val TARGET_NAME_KEY = "target_name"

    // Database
    const val DB_NAME = "altcraft_target.db"
    const val ERROR_KEY = "error"
    const val TARGET_QUEUE = "targetCommandQueue"
    const val TARGET_REQUEST = "event/post"
    const val FUNCTION = "target"

    // Work managers
    const val TARGET_O_WORK_TAG: String = "TARGET_CWT"
    const val TARGET_P_WORK_TAG: String = "TARGET_PW"

    val ONE_TIME_WORKER_TAGS = listOf(
        TARGET_O_WORK_TAG
    )

    val WORKER_TAGS = ONE_TIME_WORKER_TAGS + listOf(
        TARGET_P_WORK_TAG
    )

    // Subscription channels
    const val EMAIL_CHANNEL = "email"
    const val PUSH_CHANNEL = "push"
    const val SMS_CHANNEL = "sms"

    // Target data
    const val TIME_ZONE = "tz"
    const val TIME_MOB = "t"
    const val ALTCRAFT_CLIENT_ID = "aci"
    const val MOB_EVENT_NAME = "wn"
    const val PAYLOAD = "wd"
    const val SMID_MOB = "mi"
    const val MATCHING_MOB = "ma"
    const val MATCHING_TYPE = "mm"
    const val TRACKER_MOB = "px"
    const val TYPE_MOB = "open"
    const val VERSION_MOB = "2"
    const val PROFILE_FIELDS_MOB = "pf"
    const val SUBSCRIPTION_MOB = "sn"
    const val UTM_CAMPAIGN = "cn"
    const val UTM_CONTENT = "cc"
    const val UTM_KEYWORD = "ck"
    const val UTM_MEDIUM = "cm"
    const val UTM_SOURCE = "cs"
    const val UTM_TEMP = "ct"

    // Retry limits
    const val MAX_RETRY_COUNT = 14
    const val START_RETRY_COUNT = 0
}