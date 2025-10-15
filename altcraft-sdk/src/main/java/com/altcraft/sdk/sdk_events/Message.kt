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
    const val UPDATE_WORK_START = "::RetryTokenUpdateWorker is started"
    const val TOKEN_CHECK_WORK_START = "::TokenCheckWorker is started"
    const val PUSH_EVENT_WORK_START = "::RetryPushEventWorker is started"
    const val MOBILE_EVENT_WORK_START = "::RetryMobileEventWorker is started"

    //pushReceiver
    const val RECEIVER_REDEFINED = "receiver is redefined in:"

    //pushImage
    const val SUCCESS_IMG_LOAD = "successfully uploading an image for notification"

    //retry limit
    const val SUBSCRIBE_RETRY_LIMIT =
        "exceeded the retry limit for push subscribe operation for id:"
    const val PUSH_EVENT_RETRY_LIMIT =
        "exceeded the retry limit for push event operation for uid:"
    const val MOBILE_EVENT_RETRY_LIMIT =
        "exceeded the retry limit for mobile event operation for name:"
}