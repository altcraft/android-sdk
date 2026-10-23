package com.altcraft.sdk.push.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Contains constants used by the Push module.
 */
object Constants {

    // General
    internal const val MODULE_NAME = "altcraft-push"
    internal const val PUSH_SUB_QUEUE = "pushSubscribeCommandQueue"

    // Database
    internal const val DB_NAME = "altcraft_push.db"
    internal const val ERROR_KEY = "error"
    internal const val FUNCTION_EVENT = "sendPushEvent"
    internal const val FUNCTION_SUBSCRIBE = "pushSubscribe"

    // Providers
    const val FCM_PROVIDER: String = "android-firebase"
    const val HMS_PROVIDER: String = "android-huawei"
    const val RUS_PROVIDER: String = "android-rustore"
    internal val validProviders = setOf(
        FCM_PROVIDER,
        HMS_PROVIDER,
        RUS_PROVIDER
    )

    // Status
    internal const val SUBSCRIBED: String = "subscribed"
    internal const val UNSUBSCRIBED: String = "unsubscribed"
    internal const val SUSPENDED: String = "suspended"

    //event map keys
    internal const val UID_KEY = "uid"
    internal const val TYPE_KEY = "type"

    // Channels
    internal val allSignal = "allSignal" to "vibration and sound enabled"
    internal val soundless = "soundless" to "vibration and sound disabled"
    internal val onlySound = "onlySound" to "sound enabled, vibration disabled"

    // Push map keys
    internal const val AC_PUSH = "_ac_push"
    internal const val UID = "_uid"
    internal const val URL = "_url"
    internal const val MSG_ID = "_message_id"
    internal const val EXTRA = "_extra"
    internal const val MESSAGE = "message"
    internal const val RECEIVER = "receiver"
    internal const val PUSH = "push"

    // Push receiver
    internal const val PUSH_RECEIVER = ".AltcraftPushReceiver"

    // WorkManager - One-time workers
    internal const val PUSH_EVENT_O_WORK_TAG: String = "PUSH_EVENT_CWT"
    internal const val PUSH_SUBSCRIBE_O_WORK_TAG: String = "PUSH_SUBSCRIBE_CWT"
    internal const val TOKEN_UPDATE_O_WORK_TAG: String = "TOKEN_UPDATE_CWT"

    internal val ONE_TIME_WORKER_TAGS = listOf(
        PUSH_SUBSCRIBE_O_WORK_TAG,
        TOKEN_UPDATE_O_WORK_TAG,
        PUSH_EVENT_O_WORK_TAG
    )

    // WorkManager - Periodic workers
    internal const val PUSH_P_WORK_TAG: String = "ALTCRAFT_PUSH_PW"
    internal val WORKER_TAGS = ONE_TIME_WORKER_TAGS + listOf(
        PUSH_P_WORK_TAG
    )

    //request name
    internal const val PUSH_UNSUBSCRIBE_REQUEST = "push/unsubscribe"
    internal const val PUSH_SUBSCRIBE_REQUEST = "push/subscribe"
    internal const val PUSH_SUSPEND_REQUEST = "push/suspend"
    internal const val PUSH_STATUS_REQUEST = "push/status"
    internal const val UNSUSPEND_REQUEST = "push/unsuspend"
    internal const val TOKEN_UPDATE_REQUEST = "push/update"
    internal const val PUSH_EVENT_REQUEST = "event/push"

    // JSON keys
    internal const val SUBSCRIPTION_ID: String = "subscription_id"
    internal const val PROFILE_FIELDS: String = "profile_fields"
    internal const val SUBSCRIPTION: String = "subscription"
    internal const val PROVIDER: String = "provider"
    internal const val FIELDS = "fields"
    internal const val CATS = "cats"
    internal const val STATUS = "status"
    internal const val TIME = "time"
    internal const val REPLACE = "replace"
    internal const val SYNC = "sync"
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
    internal const val TOKEN = "token"

    // Retry
    internal const val MAX_RETRY_COUNT = 14
    internal const val START_RETRY_COUNT = 0
}
