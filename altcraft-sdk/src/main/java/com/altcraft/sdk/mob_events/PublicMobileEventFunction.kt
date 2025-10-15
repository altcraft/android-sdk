package com.altcraft.sdk.mob_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.mob_events.MobileEvent.sendMobileEvent

/** Object containing public function for sending mobile event to server. */
object PublicMobileEventFunction {

    /**
     * Public function for sending mobile events to server.
     *
     * This function prepares and triggers the delivery of a mobile event composed of
     * mandatory identifiers and optional metadata.
     *
     * @param context Application or module context initiating the send operation.
     * @param sid The string ID of the pixel.
     * @param eventName Event name.
     * @param payload Arbitrary event data as a map; will be serialized to JSON.
     * @param matching Matching parameters; will be serialized to JSON.
     * @param sendMessageId Send Message ID.
     * @param profileFields Optional profile fields to include in the request.
     * @param subscription The subscription that will be added to the profile.
     * @param utm Optional UTM tags for campaign attribution.
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