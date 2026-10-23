package com.altcraft.sdk.inapp.system.presentation

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.system.policy.Check
import com.altcraft.sdk.inapp.util.LIFOQueue
import com.altcraft.sdk.inapp.system.presentation.DelayTask.delayTask
import com.altcraft.sdk.inapp.system.presentation.DelayTask.getTriggerDelay
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.getCurrentScreen
import com.altcraft.sdk.coordination.SDKScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles In-App trigger events and starts campaign selection/presentation flow.
 *
 * The object keeps an internal LIFO queue, processes incoming triggers one by one,
 * selects the first campaign that satisfies all display checks, and either presents
 * it immediately or schedules delayed presentation.
 */
internal object InAppProcTrigger {

    /**
     * LIFO queue for sequential processing of incoming In-App triggers.
     */
    private val queue = LIFOQueue(
        scope = SDKScope.default,
        process = ::triggerProcessing
    )

    /**
     * Triggers processing of In-App campaigns associated with the given event.
     *
     * @param context Application context.
     * @param trigger In-App event used as a trigger.
     */
    fun trigger(context: Context, trigger: InAppTrigger) = SDKScope.default.launch {
        queue.add(
            context = context.applicationContext,
            item = trigger
        )
    }

    /**
     * Processes an incoming In-App event and attempts to display
     * the first matching campaign.
     *
     * Flow:
     * - initializes SDK environment;
     * - retrieves campaigns matching the event and current placement;
     * - filters them by availability, app version, deadline,
     *   common policy, subscription status, and personal policy;
     * - assigns trigger metadata to each candidate;
     * - if a trigger delay is configured, schedules delayed presentation;
     * - otherwise presents the campaign immediately.
     *
     * All exceptions are caught and logged.
     *
     * @param context application context.
     * @param trigger event that triggered the In-App processing.
     */
    private suspend fun triggerProcessing(
        context: Context,
        trigger: InAppTrigger
    ) {
        try {
            val env = Environment.create(context)

            val inApp = env.room.dao.inApp.getTargetList(
                trigger.type.value,
                trigger.name,
                getCurrentScreen()
            ).firstOrNull { item ->
                Check.Version.canShow(
                    context, item
                ) && Check.canShow(
                    context, item, trigger
                )
            } ?: return

            inApp.actionTriggerName = trigger.name
            inApp.actionTriggerType = trigger.type

            val delay = getTriggerDelay(inApp)

            if (delay > 0) {
                delayTask(context,
                    inApp = inApp,
                    trigger,
                    delay
                )
                return
            }

            Presenter.present(
                context,
                inApp = inApp,
                trigger = trigger
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("eventProcessing", e)
        }
    }
}