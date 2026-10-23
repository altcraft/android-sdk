package com.altcraft.sdk.inapp.system.presentation

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.coordination.SDKScope
import kotlinx.coroutines.launch
import com.altcraft.sdk.inapp.util.Log.error
import kotlin.time.Duration.Companion.seconds
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles delayed presentation of In-App campaigns triggered by events.
 */
internal object DelayTask {

    /**
     * Returns the configured delay for the trigger that started the In-App campaign.
     *
     * The trigger is matched by both name and type.
     *
     * @param inApp In-App campaign.
     *
     * @return Delay in seconds for the matched trigger, or `0` if not set.
     */
    fun getTriggerDelay(inApp: InApp): Int =
        inApp.options.triggerEvents.firstOrNull {
            it.name == inApp.actionTriggerName &&
                    it.type.equals(
                        inApp.actionTriggerType.value,
                        ignoreCase = true
                    )
        }?.delay ?: 0

    /**
     * Schedules delayed presentation of an In-App campaign.
     *
     * Before presentation, the matched trigger delay is reset to `0`
     * in the copied campaign instance to avoid repeated delayed processing.
     *
     * Display counters are not touched here: they are incremented
     * at shown-time by [InAppVariables.updateInAppVariables], so the
     * delayed revalidation sees only committed displays.
     *
     * The presentation epoch is captured at scheduling time; if the SDK
     * clears in-app data while the task is waiting, the stale task is
     * discarded instead of presenting the campaign.
     *
     * @param context Application context.
     * @param inApp In-App campaign.
     * @param trigger Trigger that started the In-App campaign.
     * @param delay Delay before presentation in seconds.
     */
    fun delayTask(
        context: Context,
        inApp: InApp,
        trigger: InAppModel.InAppTrigger,
        delay: Int
    ) {
        val scheduledEpoch = PresentationGate.epoch
        SDKScope.default.launch {
            try {
                kotlinx.coroutines.delay(
                    delay.toLong().seconds
                )

                if (PresentationGate.epoch != scheduledEpoch)
                    return@launch

                val inAppWithoutTriggerDelay = inApp.copy(
                    options = inApp.options.copy(
                        triggerEvents = inApp.options.triggerEvents.map {
                            if (
                                it.name == trigger.name &&
                                it.type.equals(
                                    trigger.type.value,
                                    ignoreCase = true
                                )
                            ) {
                                it.copy(delay = 0)
                            } else {
                                it
                            }
                        }
                    )
                )

                Presenter.present(
                    context,
                    inAppWithoutTriggerDelay,
                    trigger = trigger,
                    delayTask = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("delayTask", e)
            }
        }
    }
}