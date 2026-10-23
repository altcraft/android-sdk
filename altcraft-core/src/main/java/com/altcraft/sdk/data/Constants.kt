package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.Keep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Contains constant values used in the SDK.
 */
@Keep
@PublishedApi
internal object Constants {

    //module names
    const val MODULE_NAME = "altcraft-core"

    //database name
    const val DB_NAME = "AltcraftSDK.db"

    // SDK logs tag
    const val LOG_TAG = "altcraft_lib"
    const val LOG_NULL = "this log return null"
    const val PROVIDER_NOT_INITIALIZED =
        "Core Environment Provider is not initialized"
    const val LOG_HINT = "Altcraft SDK is integrated into the project. " +
            "You can enable SDK logs by setting the `enableLogging` parameter " +
            "in the SDK configuration."

    //device type
    const val DEVICE_TYPE = "mob"

    //os name
    const val ANDROID_OS = "Android"

    //auth
    const val MATCHING = "matching"

    const val SHA256 = "SHA-256"
    const val DB_ID = "db_id"
    const val MATCHING_ID = "matching_identifier"

    //matching mode
    const val PUSH_SUB_MATCHING = "push_sub"

    //base
    const val ALPHABET =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"

    //json
    const val ACID = "acid"
    const val START_TIME = "start_time"

    //retry
    @PublishedApi
    internal const val RETRY_TIME_P_WORK: Long = 180 //MINUTES

    @PublishedApi
    internal const val RETRY_TIME_C_WORK: Long = 5 //SECONDS

    const val MAX_RETRY_COUNT = 14
    const val START_RETRY_COUNT = 0

    //retry-start await
    val RETRY_START_TIMEOUT: Duration = 3.seconds

    //coroutine worker
    const val PID = "pid"
    const val AUTH_REQUEST_O_WORK_TAG: String = "AUTH_REQUEST_CWT"

    // Work managers
    val ONE_TIME_WORKER_TAGS = listOf(
        AUTH_REQUEST_O_WORK_TAG
    )

    //periodical worker
    const val CORE_P_WORK_TAG: String = "ALTCRAFT_CORE_PW"

    val WORKER_TAGS = ONE_TIME_WORKER_TAGS + listOf(
        CORE_P_WORK_TAG
    )

    //event map keys
    const val ERROR_KEY = "error"
    const val PROFILE_KEY = "profile"
    const val ERROR_TEXT_KEY = "error_text"
    const val HTTP_CODE_KEY = "http_code"

    //command queue name
    const val INIT_QUEUE = "initCommandQueue"
    const val AUTH_QUEUE = "AuthCommandQueue"

    //api
    const val AUTHORIZE_REQUEST = "auth/authorize"

    //actionsFieldsBuilder
    const val ACTION = "action"
    const val VALUE = "value"
    const val SET = "set"
    const val UNSET = "unset"
    const val INCR = "incr"
    const val ADD = "add"
    const val DELETE = "delete"
    const val UPSERT = "upsert"
}