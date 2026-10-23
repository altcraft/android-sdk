package com.altcraft.sdk.profile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Contains constants used by the Profile module.
 *
 * Includes module identifiers, command queue names, worker tags,
 * request paths, and JSON field keys used when building profile update
 * API requests.
 */
internal object Constants {

    // General
    const val MODULE_NAME = "altcraft-profile"
    const val PROFILE_QUEUE = "profileUpdateCommandQueue"

    // Database
    const val DB_NAME = "altcraft_profile.db"
    const val ERROR_KEY = "error"
    const val PROFILE_REQUEST = "profile/update"
    const val FUNCTION = "updateProfileFields"

    // Work managers
    const val PROFILE_O_WORK_TAG: String = "PROFILE_CWT"
    const val PROFILE_P_WORK_TAG: String = "PROFILE_PW"

    val ONE_TIME_WORKER_TAGS = listOf(
        PROFILE_O_WORK_TAG
    )

    val WORKER_TAGS = ONE_TIME_WORKER_TAGS + listOf(
        PROFILE_P_WORK_TAG
    )

    // Retry
    const val MAX_RETRY_COUNT = 14
    const val START_RETRY_COUNT = 0

    // JSON keys
    const val PROFILE_FIELDS = "profile_fields"
    const val SKIP_TRIGGERS = "skip_triggers"
}