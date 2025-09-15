package com.altcraft.sdk.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.json.Converter.json

/**
 * Extracts and provides access to push notification fields from a `Map<String, String>`.
 * Parses values like body, title, icon, image, URL, color, vibration, soundless, and buttons.
 *
 * @param message The raw data map from the push notification.
 */
internal class PushData(message: Map<String, String>) {

    val data: Map<String, String> = message

    /** Unique message identifiers. */
    val uid = data["_uid"] ?: ""

    /** The body text of the push notification, or an empty string if not provided. */
    val body = data["_body"] ?: ""

    /** The title of the push notification, or an empty string if not provided. */
    val title = data["_title"] ?: ""

    /** The icon URL for the push notification, or an empty string if not provided. */
    val icon = data["_icon"] ?: ""

    /** The image URL for the push notification, or an empty string if not provided. */
    val image = data["_image"] ?: ""

    /** URL to open on push click, or empty if not set. */
    val url = data["_click_action"] ?: ""

    /** The color specified for the notification, or an empty string if not provided. */
    val color = data["_color"] ?: ""

    /**
     * Indicates whether vibration is enabled for the push notification.
     * Returns `true` if the `_vibration` field is set to "true"; otherwise, returns `false`.
     */
    val vibration: Boolean
        get() = data["_vibration"] == "true"

    /**
     * Indicates whether the notification should be soundless.
     * Returns `true` if the `_soundless` field is set to "true"; otherwise, returns `false`.
     */
    val soundless: Boolean
        get() = data["_soundless"] == "true"

    /**
     * Parses and returns an array of `ButtonStructure` objects from the push notification data,
     * if available.
     * Returns an array of `ButtonStructure` if the `_buttons` field is a string and can be parsed;
     * otherwise, returns `null`.
     */
    val buttons: List<DataClasses.ButtonStructure>?
        get() = try {
            data["_buttons"]?.let {
                json.decodeFromString(it)
            }
        } catch (_: Exception) {
            null
        }
}