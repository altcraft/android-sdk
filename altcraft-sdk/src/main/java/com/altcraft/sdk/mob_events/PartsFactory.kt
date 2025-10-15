package com.altcraft.sdk.mob_events

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.ALTCRAFT_CLIENT_ID
import com.altcraft.sdk.data.Constants.MOB_EVENT_NAME
import com.altcraft.sdk.data.Constants.MATCHING_MOB
import com.altcraft.sdk.data.Constants.MATCHING_TYPE
import com.altcraft.sdk.data.Constants.PAYLOAD
import com.altcraft.sdk.data.Constants.PROFILE_FIELDS_MOB
import com.altcraft.sdk.data.Constants.SMID_MOB
import com.altcraft.sdk.data.Constants.SUBSCRIPTION_MOB
import com.altcraft.sdk.data.Constants.TIME_MOB
import com.altcraft.sdk.data.Constants.TIME_ZONE
import com.altcraft.sdk.data.Constants.UTM_CAMPAIGN
import com.altcraft.sdk.data.Constants.UTM_CONTENT
import com.altcraft.sdk.data.Constants.UTM_KEYWORD
import com.altcraft.sdk.data.Constants.UTM_MEDIUM
import com.altcraft.sdk.data.Constants.UTM_SOURCE
import com.altcraft.sdk.data.Constants.UTM_TEMP
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.extension.ListExtension.addJsonPart
import com.altcraft.sdk.extension.ListExtension.addTextPart
import com.altcraft.sdk.json.Converter.fromStringJson
import okhttp3.MultipartBody
import com.altcraft.sdk.sdk_events.Events.error

internal object PartsFactory {

    /**
     * Builds multipart/form-data parts from MobileEventEntity.
     *
     * @param entity source data used to populate text and JSON form fields.
     * @return an immutable list of multipart parts, or null if an unrecoverable error occurs.
     */
    fun createMobileEventParts(entity: MobileEventEntity) = try {
        val utm = entity.utmTags.fromStringJson<DataClasses.UTM>(
            "createMobileEventParts"
        )

        buildList<MultipartBody.Part>(15) {
            addTextPart(TIME_ZONE, entity.timeZone.toString())
            addTextPart(TIME_MOB, (entity.time / 1000).toString())
            addTextPart(ALTCRAFT_CLIENT_ID, entity.altcraftClientID)
            addTextPart(MOB_EVENT_NAME, entity.eventName)
            addTextPart(MATCHING_TYPE, entity.matchingType)
            addTextPart(UTM_CAMPAIGN, utm?.campaign)
            addTextPart(UTM_CONTENT, utm?.content)
            addTextPart(UTM_KEYWORD, utm?.keyword)
            addTextPart(UTM_MEDIUM, utm?.medium)
            addTextPart(UTM_SOURCE, utm?.source)
            addTextPart(UTM_TEMP, utm?.temp)

            addJsonPart(PAYLOAD, entity.payload)
            addJsonPart(SMID_MOB, entity.sendMessageId)
            addJsonPart(MATCHING_MOB, entity.matching)
            addJsonPart(SUBSCRIPTION_MOB, entity.subscription)
            addJsonPart(PROFILE_FIELDS_MOB, entity.profileFields)
        }
    } catch (e: Exception) {
        error("createMobileEventParts", e)
        null
    }
}