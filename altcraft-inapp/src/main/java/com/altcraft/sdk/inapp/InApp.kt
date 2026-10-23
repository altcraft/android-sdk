package com.altcraft.sdk.inapp

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.placements.PlacementsProcess.requestPlacements
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionAPI
import com.altcraft.sdk.inapp.system.animator.Animator
import com.altcraft.sdk.inapp.system.presentation.JsonContentAPI
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger.trigger
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.setCurrentScreen
import kotlin.jvm.Volatile

/**
 * Provides access to the In-App module through [AltcraftSDK].
 *
 * Usage: `AltcraftSDK.inApp.setScreen("home")`.
 */
@get:Keep
val AltcraftSDK.inApp: InApp get() = InApp

/**
 * Public facade for In-App functionality.
 *
 * Provides APIs for:
 * - In-App animation configuration;
 * - current screen tracking;
 * - custom trigger processing;
 * - In-App placement requests;
 * - activity lifecycle tracking;
 * - In-App subscription management.
 */
@Keep
object InApp {

    /**
     * Provides access to In-App subscription management.
     */
    @JvmField
    val subscription = SubscriptionAPI

    /**
     * Provides access to in-app placements with JSON content.
     */
    @JvmField
    val jsonContent = JsonContentAPI

    /**
     * In-App module events.
     */
    @Keep
    val moduleEvents = ModuleEvents


    /** @Volatile → written from the app's thread, read on the main thread. */
    @Volatile
    internal var onInAppShownListener: ((Animator) -> Unit)? = null

    /**
     * Sets animation for the in-app view.
     *
     * @param block Animation applied to the shown [Animator].
     */
    fun animation(block: Animator.() -> Unit) {
        onInAppShownListener = { animator ->
            animator.block()
        }
    }

    /**
     * Sets the current screen for In-App placement matching.
     *
     * @param screen Screen identifier used to match available In-App campaigns.
     */
    fun setScreen(context: Context, screen: String) = setCurrentScreen(
        context, screen
    )

    /**
     * Triggers processing of In-App campaigns associated with the given trigger.
     *
     * @param context Application context.
     * @param name In-App event used as a trigger.
     */
    fun trigger(context: Context, name: String) = trigger(
        context, InAppTrigger(
            name = name, type = InAppModel.TriggerType.CUSTOM
        )
    )

    /**
     * Public API for requesting available In-App placements.
     *
     * @param context Application context.
     */
    fun getPlacements(context: Context) = requestPlacements(
        context, userReq = true
    )
}