package com.altcraft.sdk.target

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.target.data.models.TargetModels.UTM
import com.altcraft.sdk.target.data.models.TargetModels.SubscriptionModel.Subscription
import com.altcraft.sdk.target.platform.target.TargetProcess.target
import com.altcraft.sdk.target.events.ModuleEvents

/**
 * Provides access to the Target module through [AltcraftSDK].
 */
@get:Keep
val AltcraftSDK.target: Target get() = Target

/** Object containing public functions for sending target events to the server. */
@Keep
object Target {

    /**
     * Target module events.
     */
    @Keep
    val moduleEvents = ModuleEvents

    /**
     * Public API for sending a target event to the server.
     *
     * @param context Application context initiating to send.
     * @param sid The string ID of the pixel.
     * @param eventName Event name.
     * @param sendMessageId Message identifier to link the event.
     * @param payload Event payload data.
     * @param matching Matching parameters.
     * @param matchingType Matching mode or type.
     * @param profileFields Profile fields.
     * @param subscription Subscription attached to the profile.
     * @param utm UTM tags used for attribution.
     */
    fun sendTarget(
        context: Context,
        sid: String,
        eventName: String,
        sendMessageId: String? = null,
        payload: Map<String, Any?>? = null,
        matching: Map<String, Any?>? = null,
        matchingType: String? = null,
        profileFields: Map<String, Any?>? = null,
        subscription: Subscription? = null,
        utm: UTM? = null
    ) {
        target(
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