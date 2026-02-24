package com.altcraft.sdk.profile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep

/** Object containing public function for updating profile fields. */
@Keep
object PublicProfileFunctions {

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
        ProfileUpdate.updateProfileFields(
            context = context,
            profileFields = profileFields,
            skipTriggers = skipTriggers
        )
    }
}