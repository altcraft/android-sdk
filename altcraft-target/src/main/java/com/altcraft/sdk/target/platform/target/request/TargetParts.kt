package com.altcraft.sdk.target.platform.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.network.json.Converter.fromStringJson
import com.altcraft.sdk.target.data.Constants.ALTCRAFT_CLIENT_ID
import com.altcraft.sdk.target.data.Constants.MATCHING_MOB
import com.altcraft.sdk.target.data.Constants.MATCHING_TYPE
import com.altcraft.sdk.target.data.Constants.MOB_EVENT_NAME
import com.altcraft.sdk.target.data.Constants.PAYLOAD
import com.altcraft.sdk.target.data.Constants.PROFILE_FIELDS_MOB
import com.altcraft.sdk.target.data.Constants.SMID_MOB
import com.altcraft.sdk.target.data.Constants.SUBSCRIPTION_MOB
import com.altcraft.sdk.target.data.Constants.TIME_MOB
import com.altcraft.sdk.target.data.Constants.TIME_ZONE
import com.altcraft.sdk.target.data.Constants.UTM_CAMPAIGN
import com.altcraft.sdk.target.data.Constants.UTM_CONTENT
import com.altcraft.sdk.target.data.Constants.UTM_KEYWORD
import com.altcraft.sdk.target.data.Constants.UTM_MEDIUM
import com.altcraft.sdk.target.data.Constants.UTM_SOURCE
import com.altcraft.sdk.target.data.Constants.UTM_TEMP
import com.altcraft.sdk.target.data.room.Entity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.altcraft.sdk.target.util.Log.error
import com.altcraft.sdk.target.data.models.TargetModels.UTM

/**
 * Builds multipart parts for mobile event requests.
 */
internal object TargetParts {

    /**
     * Builds multipart/form-data parts from [Entity].
     *
     * @param entity mobile event entity containing request data.
     * @return list of parts, or `null` on error.
     */
    fun createRequestParts(entity: Entity) = try {
        val utm = entity.utmTags.fromStringJson<UTM>(
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
        error("createRequestParts", e)
        null
    }


    private val TEXT = "text/plain; charset=utf-8".toMediaType()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Adds a text multipart field when [value] is not null.
     *
     * @receiver target multipart list
     * @param name field name
     * @param value field value
     */
    internal fun MutableList<MultipartBody.Part>.addTextPart(
        name: String, value: String?
    ) = value?.let {
        add(
            MultipartBody.Part.createFormData(
                name, null, value.toRequestBody(
                    TEXT
                )
            )
        )
    }

    /**
     * Adds a JSON multipart field when [jsonStr] is not null.
     *
     * @receiver target multipart list
     * @param name field name
     * @param jsonStr raw JSON value
     */
    internal fun MutableList<MultipartBody.Part>.addJsonPart(
        name: String, jsonStr: String?
    ) = jsonStr?.let {
        add(
            MultipartBody.Part.createFormData(
                name, null, it.toRequestBody(
                    JSON
                )
            )
        )
    }
}
