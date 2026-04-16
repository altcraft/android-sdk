package com.altcraft.sdk.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.annotation.Keep

/**
 * Contains constant values used in the SDK.
 */
@Keep
object Constants {

    //providers names
    const val FCM_PROVIDER: String = "android-firebase"
    const val HMS_PROVIDER: String = "android-huawei"
    const val RUS_PROVIDER: String = "android-rustore"

    //status
    const val SUBSCRIBED: String = "subscribed"
    const val UNSUBSCRIBED: String = "unsubscribed"
    const val SUSPENDED: String = "suspended"

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

    //channel
    internal val allSignal = "allSignal" to "vibration and sound enabled"
    internal val soundless = "soundless" to "vibration and sound disabled"
    internal val onlySound = "onlySound" to "sound enabled, vibration disabled"

    //set of valid providers names
    internal val validProviders = setOf(FCM_PROVIDER, HMS_PROVIDER, RUS_PROVIDER)

    //retry function name
    internal const val INIT_OPERATION_FUNC = "InitialOperation"

    //auth
    internal const val MATCHING = "matching"

    internal const val SHA256 = "SHA-256"
    internal const val DB_ID = "db_id"
    internal const val MATCHING_ID = "matching_identifier"

    //matching mode
    internal const val PUSH_SUB_MATCHING = "push_sub"

    //advertising Id
    internal const val CLS_ADS_ID_CLIENT =
        "com.google.android.gms.ads.identifier.AdvertisingIdClient"
    internal const val CLS_ADS_ID_INFO =
        "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info"
    internal const val M_GET_INFO = "getAdvertisingIdInfo"
    internal const val M_GET_ID = "getId"
    internal const val M_IS_LIMIT = "isLimitAdTrackingEnabled"

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

    //mobile evens
    internal const val TIME_ZONE = "tz"
    internal const val TIME_MOB = "t"
    internal const val ALTCRAFT_CLIENT_ID = "aci"
    internal const val MOB_EVENT_NAME = "wn"
    internal const val PAYLOAD = "wd"
    internal const val SMID_MOB = "mi"
    internal const val MATCHING_MOB = "ma"
    internal const val MATCHING_TYPE = "mm"
    internal const val TRACKER_MOB = "px"
    internal const val TYPE_MOB = "open"
    internal const val VERSION_MOB = "2"
    internal const val PROFILE_FIELDS_MOB = "pf"
    internal const val SUBSCRIPTION_MOB = "sn"
    internal const val UTM_CAMPAIGN = "cn"
    internal const val UTM_CONTENT = "cc"
    internal const val UTM_KEYWORD = "ck"
    internal const val UTM_MEDIUM = "cm"
    internal const val UTM_SOURCE = "cs"
    internal const val UTM_TEMP = "ct"

    //subscription channels
    internal const val EMAIL_CHANNEL = "email"
    internal const val SMS_CHANNEL = "sms"
    internal const val PUSH_CHANNEL = "push"

    //push events
    internal const val DELIVERY = "delivery"
    internal const val OPEN = "open"

    //retry
    internal const val RETRY_TIME_P_WORK: Long = 3 //HOURS
    internal const val RETRY_TIME_C_WORK: Long = 5 //SECONDS
    internal const val MAX_RETRY_COUNT = 14
    internal const val START_RETRY_COUNT = 0

    //coroutine worker
    internal const val PUSH_EVENT_C_WORK_TAG: String = "PUSH_EVENT_CWT"
    internal const val MOB_EVENT_C_WORK_TAG: String = "MOBILE_EVENT_CWT"
    internal const val SUBSCRIBE_C_WORK_TAG: String = "SUBSCRIBE_CWT"
    internal const val TN_UPDATE_C_WORK_TAG: String = "TOKEN_UPDATE_CWT"
    internal const val PR_UPDATE_C_WORK_TAG: String = "PROFILE_UPDATE_CWT"
    internal const val PID = "pid"

    //periodical worker
    internal const val SUB_P_WORK_NAME: String = "SUB_PW"
    internal const val TOKEN_UPDATE_P_WORK_NAME: String = " UPDATE_PW"
    internal const val PUSH_EVENT_P_WORK_NAME: String = "PUSH_EVENT_PW"
    internal const val MOBILE_EVENT_P_WORK_NAME: String = "MOBILE_EVENT_PW"
    internal const val PROFILE_UPDATE_P_WORK_NAME: String = "PROFILE_UPDATE_PW"

    //push map keys
    internal const val UID_KEY = "_uid"
    internal const val AC_PUSH = "_ac_push"

    //event map keys and intent extras keys
    internal const val TOKEN = "token"
    internal const val MESSAGE = "message"
    internal const val RESPONSE_WITH_HTTP_CODE = "response_with_http_code"
    internal const val TYPE = "type"
    internal const val NAME = "mobile_event_name"
    internal const val UID = "_uid"
    internal const val URL = "_url"
    internal const val MSG_ID = "_message_id"
    internal const val EXTRA = "_extra"

    //push/status mode
    internal const val LATEST_SUBSCRIPTION = "LATEST_SUBSCRIPTION"
    internal const val LATEST_FOR_PROVIDER = "LATEST_FOR_PROVIDER"
    internal const val MATCH_CURRENT_CONTEXT = "MATCH_CURRENT_CONTEXT"

    //success request message
    internal const val SUCCESS_REQUEST = "successful request:"

    //api
    @Suppress("SpellCheckingInspection")
    internal const val UNSUSPEND_REQUEST = "push/unsuspend"
    internal const val UNSUBSCRIBE_REQUEST = "push/unsubscribe"
    internal const val SUBSCRIBE_REQUEST = "push/subscribe"
    internal const val SUSPEND_REQUEST = "push/suspend"
    internal const val STATUS_REQUEST = "push/status"
    internal const val TOKEN_UPDATE_REQUEST = "push/update"
    internal const val PUSH_EVENT_REQUEST = "event/push"
    internal const val MOBILE_EVENT_REQUEST = "event/post"
    internal const val PROFILE_UPDATE_REQUEST = "profile/update"

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