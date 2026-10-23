package com.altcraft.sdk.inapp.system.presentation

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.data.Constants.OPEN_PAGE_EVENT
import com.altcraft.sdk.inapp.data.Preferenses.setInAppLastOpenTime
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.system.policy.Check.PersonalPolicy.checkPersonalPolicy
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger.trigger
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe session-scoped state used for In-App display conditions.
 */
internal object InAppVariables {

    /** Current screen or placement where In-App messages may be shown. */
    private var currentScreen: String? = null

    /** Number of In-App messages opened during the current session. */
    private var openBySession: Int = 0

    /** Number of In-App messages opened by default SDK-generated triggers. */
    private val triggerBySessionDefault = mutableMapOf<String, Int>()

    /** Number of In-App messages opened by custom user-defined triggers. */
    private val triggerBySessionCustom = mutableMapOf<String, Int>()

    /**
     * Returns the current placement.
     *
     * @return Current screen or `null` if not set.
     */
    @Synchronized
    fun getCurrentScreen(): String? = currentScreen

    /**
     * Sets the current screen for In-App placement matching.
     *
     * @param value Screen identifier used to match available In-App campaigns.
     */
    @Synchronized
    fun setCurrentScreen(context: Context, value: String?) {
        if (currentScreen != value) trigger(
            context = context,
            trigger = InAppTrigger(
                name = OPEN_PAGE_EVENT,
                type = TriggerType.DEFAULT
            )
        )
        currentScreen = value
    }

    /**
     * Returns the number of In-App messages opened in the current session.
     *
     * @return Session open count.
     */
    @Synchronized
    fun getOpenBySession(): Int = openBySession

    /**
     * Increments the number of In-App messages opened in the current session.
     */
    @Synchronized
    fun openBySessionInc() {
        openBySession++
    }

    /**
     * Updates In-App session state after display.
     *
     * Increments session counters for the committed display
     * (both immediate and delayed), checks open count limits,
     * and saves the last display time.
     *
     * @param context Application context.
     * @param inApp Displayed In-App campaign.
     * @param trigger Trigger that started the In-App campaign.
     */
    suspend fun updateInAppVariables(
        context: Context,
        inApp: InApp,
        trigger: InAppTrigger,
    ) {
        try {
            openBySessionInc()
            triggerBySessionInc(trigger)

            setInAppLastOpenTime(context)

            checkPersonalPolicy(
                context, inApp
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("updateInAppVariables", e)
        }
    }

    /**
     * Returns the number of opens for the specified trigger.
     *
     * @param triggerType Trigger type used to select the corresponding counter map.
     * @param triggerName Trigger name used as the counter key.
     *
     * @return Number of In-App opens for the specified trigger.
     */
    @Synchronized
    fun getTriggerCount(
        triggerType: TriggerType,
        triggerName: String
    ): Int {
        return when (triggerType) {
            TriggerType.CUSTOM -> triggerBySessionCustom[triggerName] ?: 0
            TriggerType.DEFAULT -> triggerBySessionDefault[triggerName] ?: 0
        }
    }

    /**
     * Increments the open counter for the trigger associated with the given In-App.
     *
     * @param trigger In-App campaign containing trigger type and trigger name.
     */
    @Synchronized
    fun triggerBySessionInc(trigger: InAppTrigger) {
        when (trigger.type) {
            TriggerType.CUSTOM ->
                triggerBySessionCustom[trigger.name] =
                    (triggerBySessionCustom[trigger.name] ?: 0) + 1

            TriggerType.DEFAULT ->
                triggerBySessionDefault[trigger.name] =
                    (triggerBySessionDefault[trigger.name] ?: 0) + 1
        }
    }

    /**
     * Clears all session-scoped In-App state and counters.
     */
    @Synchronized
    fun reset() {
        currentScreen = null
        openBySession = 0
        triggerBySessionDefault.clear()
        triggerBySessionCustom.clear()
    }
}