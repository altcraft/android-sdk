package com.altcraft.sdk

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.platform.authenticate.AuthAPI
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.core.Clear
import com.altcraft.sdk.core.Init
import com.altcraft.sdk.events.SDKEvents

/**
 * Central control point of the Altcraft SDK.
 * Provides public APIs for interacting with the Altcraft platform.
 */
@Keep
object AltcraftSDK {

    /** Provides access to authentication operations. */
    @Keep
    val auth = AuthAPI

    /** Provides access to the SDK events API. */
    @Keep
    val sdkEvents = SDKEvents

    /**
     * Initializes the Altcraft SDK.
     *
     * @param context Application context.
     * @param configuration SDK configuration.
     * @param complete Optional callback with the initialization result.
     */
    @Keep
    fun initialization(
        context: Context,
        configuration: AltcraftConfiguration,
        complete: ((Result<Unit>) -> Unit)? = null
    ) {
        Init.init(context, configuration, complete)
    }

    /**
     * Clears SDK data and stops active SDK background work.
     *
     * Fire-and-forget: cleanup runs in the background and [onComplete] is
     * invoked on the main thread once it completes.
     *
     * @param context Application context used for cleanup.
     * @param onComplete Optional callback invoked on the main thread
     * after cleanup completes.
     */
    @Keep
    fun clear(
        context: Context,
        onComplete: (() -> Unit)? = null
    ) {
        Clear.clear(context, onComplete)
    }

    /** Allows re-running initial retry/scheduling/token-check
     *  logic in this process on next [initialization]. */
    @Keep
    fun unlockInitialOperationsInThisSession() {
        Init.initPhaseStarted.set(false)
    }
}