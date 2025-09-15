package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.altcraft.sdk.services.SubscribeService
import com.altcraft.sdk.services.UpdateService

/**
 * Contains constant values used in the SDK.
 */
@Keep
object Constants {
    //providers names
    const val FCM_PROVIDER: String = "android-firebase"
    const val HMS_PROVIDER: String = "android-huawei"
    const val RUS_PROVIDER: String = "android-rustore"

    // SDK logs tag
    internal const val LOG_TAG = "altcraft_lib"
    internal const val LOG_NULL = "this log return null"

    //device type
    internal const val DEVICE_TYPE = "mob"

    //os name
    internal const val ANDROID_OS = "Android"

    //data base name
    internal const val ALTCRAFT_DB_NAME = "AltcraftSDK.db"

    //push receiver class name
    internal const val PUSH_RECEIVER = ".AltcraftPushReceiver"

    //chanel
    internal val allSignal = "allSignal" to "vibration and sound enabled"
    internal val soundless = "soundless" to "vibration and sound disabled"
    internal val onlySound = "onlySound" to "sound enabled, vibration disabled"

    //set of valid providers names
    internal val validProviders = setOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)

    //auth
    internal const val R_TOKEN_MATCHING = "push_sub"
    internal const val MATCHING = "matching"
    internal const val SHA256 = "SHA-256"
    internal const val DB_ID = "db_id"
    internal const val MATCHING_ID = "matching_identifier"

    //service message
    internal const val DEFAULT_SERVICES_MESSAGE_BODY = "background process"

    //json
    internal const val SUBSCRIPTION_ID: String = "subscription_id"
    internal const val PROFILE_FIELDS: String = "profile_fields"
    internal const val SUBSCRIPTION: String = "subscription"
    internal const val PROVIDER: String = "provider"
    internal const val FIELDS = "fields"
    internal const val CATS = "cats"
    internal const val STATUS = "status"
    internal const val TIME = "time"
    internal const val REPLACE = "replace"
    internal const val SKIP_TRIGGERS = "skip_triggers"
    internal const val CATS_NAME = "name"
    internal const val CATS_ACTIVE = "active"
    internal const val CATS_TITLE = "title"
    internal const val CATS_STEADY = "steady"
    internal const val SMID = "smid"
    internal const val OLD_TOKEN = "old_token"
    internal const val OLD_PROVIDER = "old_provider"
    internal const val NEW_TOKEN = "new_token"
    internal const val NEW_PROVIDER = "new_provider"

    //Status
    internal const val SUBSCRIBED: String = "subscribed"
    internal const val UNSUBSCRIBED: String = "unsubscribed"
    internal const val SUSPENDED: String = "suspended"

    //pushEvents
    internal const val DELIVERY = "delivery"
    internal const val OPEN = "open"

    //open push
    internal const val PACKAGE_NAME = "package_Name"

    //retry
    internal const val RETRY_TIME_P_WORK: Long = 3 //HOURS
    internal const val RETRY_TIME_C_WORK: Long = 5 //SECONDS
    internal const val MAX_RETRY_COUNT = 14
    internal const val START_RETRY_COUNT = 0
    internal const val COUNT_SERVICE_CLOSED = 3

    //service
    @RequiresApi(Build.VERSION_CODES.Q)
    internal const val SERVICE_TYPE_DATA = FOREGROUND_SERVICE_TYPE_DATA_SYNC
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    internal const val SERVICE_TYPE_MSG = FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING

    internal const val PUSH_SUBSCRIBE_SERVICE: String = "SubscribeService()"
    internal const val TOKEN_UPDATE_SERVICE: String = "UpdateService()"
    internal const val DEFAULT_SERVICE: String = "DefaultService()"
    internal const val STOP_SERVICE_ACTION: String = "STOP"

    //service classes
    internal val SUBSCRIBE_SERVICE = SubscribeService::class.java
    internal val UPDATE_SERVICE = UpdateService::class.java

    //coroutine worker
    internal const val PUSH_EVENT_C_WORK_TAG: String = "PUSH_EVENT_CWT"
    internal const val SUBSCRIBE_C_WORK_TAG: String = "SUBSCRIBE_CWT"
    internal const val UPDATE_C_WORK_TAG: String = "UPDATE_CWT"

    internal const val UPDATE_C_WORK_NANE: String = "UPDATE_CW"
    internal const val SUB_C_WORK_NANE: String = "SUBSCRIBE_CW"
    internal const val PUSH_EVENT_C_WORK_NAME: String = "PUSH_EVENT_CW"

    //periodical worker
    internal const val SUB_P_WORK_NANE: String = "SUB_PW"
    internal const val UPDATE_P_WORK_NANE: String = " UPDATE_PW"
    internal const val EVENT_P_WORK_NANE: String = "PUSH_EVENT_PW"
    internal const val CHECK_P_WORK_NANE: String = "CHECK_PW"

    //push map keys
    internal const val UID_KEY = "_uid"
    internal const val AC_PUSH = "_ac_push"

    //event map keys and intent extras keys
    internal const val TOKEN = "token"
    internal const val MESSAGE = "message"
    internal const val RESPONSE_WITH_HTTP_CODE = "response_with_http_code"
    internal const val TYPE = "type"
    internal const val UID = "uid"
    internal const val URL = "url"

    //push/status mode
    internal const val LATEST_SUBSCRIPTION = "LATEST_SUBSCRIPTION"
    internal const val LATEST_FOR_PROVIDER = "LATEST_FOR_PROVIDER"
    internal const val MATCH_CURRENT_CONTEXT = "MATCH_CURRENT_CONTEXT"

    //success request message
    internal const val SUCCESS_REQUEST = "successful request:"

    //api
    @Suppress("SpellCheckingInspection")
    internal const val UNSUSPEND_REQUEST = "push/unsuspend"
    internal const val SUBSCRIBE_REQUEST = "push/subscribe"
    internal const val STATUS_REQUEST = "push/status"
    internal const val UPDATE_REQUEST = "push/update"
    internal const val PUSH_EVENT_REQUEST = "event/push"

    //actionsFieldsBuilder
    internal const val ACTION = "action"
    internal const val VALUE = "value"
    internal const val SET = "set"
    internal const val UNSET = "unset"
    internal const val INCR = "incr"
    internal const val ADD = "add"
    internal const val DELETE = "delete"
    internal const val UPSERT = "upsert"
}