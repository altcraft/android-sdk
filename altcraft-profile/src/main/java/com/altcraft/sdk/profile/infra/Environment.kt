package com.altcraft.sdk.profile.infra

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.core.environment.CoreEnvironmentProvider
import com.altcraft.sdk.profile.data.room.DB

/**
 * Provides dependencies required by the Profile module.
 *
 * Shared SDK data is requested through [CoreEnvironmentProvider].
 */
internal class Environment private constructor(context: Context) {

    private val appContext = context.applicationContext

    /** Profile module Room database. */
    val room: DB by lazy {
        DB.getInstance(appContext)
    }

    /**
     * Returns the current user tag.
     *
     * @return Current user tag.
     */
    suspend fun userTag() = CoreEnvironmentProvider.user()

    companion object {

        /**
         * Creates a Profile module environment.
         *
         * @param context Android context.
         * @return Profile module environment.
         */
        fun create(context: Context) = Environment(context)
    }
}
