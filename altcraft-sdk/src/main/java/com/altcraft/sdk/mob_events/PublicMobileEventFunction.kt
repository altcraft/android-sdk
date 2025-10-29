package com.altcraft.sdk.mob_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.mob_events.MobileEvent.sendMobileEvent

/** Object containing public function for sending mobile event to server. */
@Keep
object PublicMobileEventFunction {

    /**
     * Public API for sending a mobile event to the server.
     *
     * @param context Application context initiating the send.
     * @param sid The string ID of the pixel.
     * @param eventName Event name.
     * @param sendMessageId Message identifier to link the event (optional).
     * @param payload Event payload data (optional).
     * @param matching Matching parameters (optional).
     * @param matchingType Matching mode/type (optional).
     * @param profileFields Profile fields (optional).
     * @param subscription Subscription to attach to the profile (optional).
     * @param utm UTM tags for attribution (optional).
     */
    fun mobileEvent(
        context: Context,
        sid: String,
        eventName: String,
        sendMessageId: String? = null,
        payload: Map<String, Any?>? = null,
        matching: Map<String, Any?>? = null,
        matchingType: String? = null,
        profileFields: Map<String, Any?>? = null,
        subscription: DataClasses.Subscription? = null,
        utm: DataClasses.UTM? = null
    ){
        sendMobileEvent(
            context = context,
            sid = sid,
            eventName = eventName,
            sendMessageId = sendMessageId,
            payloadFields = payload,
            matching = matching,
            matchingType = matchingType,
            profileFields = profileFields,
            subscription = subscription,
            utmTags = utm
        )
    }
}