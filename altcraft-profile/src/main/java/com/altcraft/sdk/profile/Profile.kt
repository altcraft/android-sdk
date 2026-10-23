package com.altcraft.sdk.profile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.profile.events.ModuleEvents
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess.updateFields

/**
 * Provides access to the Profile module through [AltcraftSDK].
 */
@get:Keep
val AltcraftSDK.profile: Profile get() = Profile

/** Object containing public functions for updating profile fields. */
@Keep
object Profile {

    /**
     * Profile module events.
     */
    @Keep
    val moduleEvents = ModuleEvents

    /**
     * Public API for updating profile fields.
     *
     * @param context Application context initiating the update.
     * @param profileFields Profile fields to update (optional).
     * @param skipTriggers Optional flag to skip triggers on the server side.
     */
    fun updateProfileFields(
        context: Context,
        profileFields: Map<String, Any?>? = null,
        skipTriggers: Boolean? = null
    ) {
        updateFields(
            context = context,
            profileFields = profileFields,
            skipTriggers = skipTriggers
        )
    }
}
