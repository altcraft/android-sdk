package com.altcraft.sdk.inapp.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Contains constant values used by the in-app module.
 */
@Suppress("SpellCheckingInspection")
internal object Constants {

    /** Unique in-app module identifier used for core module registration. */
    const val MODULE_NAME: String = "altcraft-inapp"

    /** In-app module Room database name. */
    const val DB_NAME = "AltcraftSDK_InApp.db"

    //status
    const val SUBSCRIBED: String = "subscribed"
    const val UNSUBSCRIBED: String = "unsubscribed"

    //base
    const val BASE_URL = "https://localhost/"

    //json
    const val SUBSCRIPTION_ID: String = "subscription_id"
    const val PROFILE_FIELDS: String = "profile_fields"
    const val SUBSCRIPTION: String = "subscription"
    const val FIELDS = "fields"
    const val CATS = "cats"
    const val STATUS = "status"
    const val TIME = "time"
    const val REPLACE = "replace"
    const val SYNC = "sync"
    const val ACID = "acid"
    const val SKIP_TRIGGERS = "skip_triggers"
    const val CATS_NAME = "name"
    const val CATS_ACTIVE = "active"
    const val CATS_TITLE = "title"
    const val CATS_STEADY = "steady"
    const val SMID = "smid"
    const val NAME = "name"
    const val CODE = "code"
    const val LINK = "link_url"
    const val DEVICE_ID = "device_id"
    const val LAST_PUBLISH = "last_publish"
    const val PLACEMENT_RULES_CHECKSUM = "placement_rules_checksum"

    //event value
    const val REJECTED_COMPANY_IDS = "rejectedCompanyIds"
    const val RECEIVED = "received"
    const val LIMIT = "limit"

    // in-app bridge
    const val JS_INTERFACE_NAME = "InAppBridge"
    const val IN_APP_SLIDE_READY = "slideReady"
    const val IN_APP_DELIVERY = "deliv"
    const val IN_APP_SHOW = "show"
    const val IN_APP_CLICK = "click"
    const val IN_APP_ERROR = "error"
    const val IN_APP_CLOSE = "close"
    const val IN_APP_READY = "ready"

    //in-app default event name
    const val OPEN_APP_EVENT = "open_app"
    const val OPEN_PAGE_EVENT = "open_page"

    //in-app display content type
    const val FLOATING_BAR_IN_APP = "floating_bar"
    const val FULL_SCREEN_IN_APP = "fullscreen"
    const val SLIDE_UP_IN_APP = "slideup"
    const val CUSTOM_IN_APP = "custom"
    const val MODAL_IN_APP = "modal"

    //retry
    const val MAX_RETRY_COUNT = 14
    const val START_RETRY_COUNT = 0

    // in-app placements configuration
    /** Default interval between automatic in-app placements requests, in milliseconds. */
    const val DEFAULT_PLACEMENTS_REQ_INTERVAL_MS = 30 * 60 * 1000L

    /** Default maximum number of in-app placements kept in local storage. */
    const val DEFAULT_MAX_PLACEMENTS = 500

    //coroutine worker tags
    const val IN_APP_EVENT_O_WORK_TAG: String = "IN_APP_EVENT_CWT"
    const val IN_APP_STATIC_O_WORK_TAG: String = "IN_APP_STATIC_CWT"
    const val IN_APP_SUBSCRIBE_O_WORK_TAG: String = "IN_APP_SUBSCRIBE_CWT"
    const val IN_APP_PLACEMENTS_O_WORK_TAG: String = "IN_APP_PLACEMENTS_CWT"

    val ONE_TIME_WORKER_TAGS = listOf(
        IN_APP_EVENT_O_WORK_TAG,
        IN_APP_STATIC_O_WORK_TAG,
        IN_APP_SUBSCRIBE_O_WORK_TAG,
        IN_APP_PLACEMENTS_O_WORK_TAG
    )

    //periodical worker tags
    const val IN_APP_P_WORK_TAG: String = "IN_APP_PW"

    val WORKER_TAGS = ONE_TIME_WORKER_TAGS + listOf(
        IN_APP_P_WORK_TAG
    )

    //command queue names
    const val IN_APP_SUB_QUEUE = "inAppSubscribeCommandQueue"
    const val IN_APP_EVENT_QUEUE = "InAppEventCommandQueue"
    const val IN_APP_PLACEMENTS_QUEUE = "InAppPlacementsCommandQueue"

    //api
    const val IN_APP_PLACEMENTS_REQUEST = "inapp/placements"
    const val IN_APP_UNSUBSCRIBE_REQUEST = "inapp/unsubscribe"
    const val IN_APP_SUBSCRIBE_REQUEST = "inapp/subscribe"
    const val IN_APP_STATUS_REQUEST = "inapp/status"
    const val IN_APP_EVENT_REQUEST = "inapp/event"

    //event map keys
    const val ERROR_KEY = "error"
    const val ALL_SMID_KEY = "all_smid"
    const val SMID_KEY = "smid"
    const val TYPE_KEY = "type"
    const val MESSAGE = "message"
    const val DATA = "data"
}