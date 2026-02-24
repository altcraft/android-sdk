package com.altcraft.sdk.sdk_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

/**
 * Contains message constants used in events
 */
internal object Message {

    //periodical worker
    const val SUB_WORK_START = "::RetryPushSubscribeWorker is started"
    const val PUSH_EVENT_WORK_START = "::RetryPushEventWorker is started"
    const val MOBILE_EVENT_WORK_START = "::RetryMobileEventWorker is started"
    const val TOKEN_UPDATE_WORK_START = "::RetryTokenUpdateWorker is started"
    const val PROFILE_UPDATE_WORK_START = "::RetryProfileUpdateWorker is started"

    //pushReceiver
    const val RECEIVER_REDEFINED = "receiver is redefined in:"

    //pushImage
    const val SUCCESS_IMG_LOAD = "successfully uploading an image for notification"

    //logs
    const val LOG_HINT = "Altcraft SDK is integrated into the project. " +
            "You can enable SDK logs by setting the `enableLogging` parameter " +
            "in the SDK configuration."

    //retry limit
    const val SUBSCRIBE_RETRY_LIMIT =
        "exceeded the retry limit for push subscribe operation for id:"
    const val PUSH_EVENT_RETRY_LIMIT =
        "exceeded the retry limit for push event operation for uid:"
    const val MOBILE_EVENT_RETRY_LIMIT =
        "exceeded the retry limit for mobile event operation for name:"
    const val PROFILE_UPDATE_RETRY_LIMIT =
        "exceeded the retry limit for profile update operation for id:"
}