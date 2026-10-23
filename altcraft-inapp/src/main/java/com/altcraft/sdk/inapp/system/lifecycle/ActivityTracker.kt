package com.altcraft.sdk.inapp.system.lifecycle

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.altcraft.sdk.inapp.data.Constants.OPEN_APP_EVENT
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger
import java.lang.ref.WeakReference

/**
 * Tracks the currently resumed activity and runs actions when it resumes.
 */
internal object ActivityTracker : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var currentResumedRef: WeakReference<Activity>? = null

    @Volatile
    private var isRegistered: Boolean = false

    @Volatile
    private var runOnceOnResumed: ((Activity) -> Unit)? = null

    @Volatile
    private var openAppTriggered: Boolean = false

    @Volatile
    private var onCurrentActivityFinished: ((Activity) -> Unit)? = null

    /**
     * Registers lifecycle callbacks for activity tracking.
     *
     * @param context Context used to obtain application instance.
     */
    fun onActivityLifecycle(context: Context) {
        val app = context.applicationContext as? Application ?: return
        if (isRegistered) return

        synchronized(this) {
            if (isRegistered) return
            app.registerActivityLifecycleCallbacks(this)
            isRegistered = true
        }
    }

    /**
     * Executes the given block when an activity is resumed.
     *
     * If an activity is already resumed, the block runs immediately.
     *
     * A resumed activity that is finishing or already destroyed is treated
     * as absent, so the block is deferred to the next live resume instead of
     * starting presentation work on a dying activity.
     *
     * A new pending block replaces the previous pending block.
     *
     * @param block Action executed with the resumed activity.
     */
    fun runOnNextResumed(block: (Activity) -> Unit) {
        val activity = currentResumedRef?.get()

        if (
            activity != null &&
            !activity.isFinishing &&
            !activity.isDestroyed
        ) {
            block(activity)
            return
        }

        synchronized(this) {
            runOnceOnResumed = block
        }
    }

    /**
     * Sets callback that is called when current activity is stopped or destroyed.
     *
     * Used by In-App presentation flow to release waiting Presenter state
     * if dialog disappears because of activity transition, push deep link,
     * or activity destruction without normal Dialog.OnDismiss callback.
     *
     * Pass `null` to clear current callback.
     *
     * @param block Callback with finished activity.
     */
    fun setOnCurrentActivityFinished(block: ((Activity) -> Unit)?) {
        synchronized(this) {
            onCurrentActivityFinished = block
        }
    }

    /**
     * Triggers the default open app in-app event once on first activity creation.
     */
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        synchronized(this) {
            if (openAppTriggered) return
            openAppTriggered = true
        }

        InAppProcTrigger.trigger(
            context = activity.applicationContext,
            trigger = InAppTrigger(
                name = OPEN_APP_EVENT,
                type = TriggerType.DEFAULT
            )
        )
    }

    /**
     * Stores resumed activity and runs pending action.
     */
    override fun onActivityResumed(activity: Activity) {
        currentResumedRef = WeakReference(activity)

        val action = synchronized(this) {
            val pending = runOnceOnResumed
            runOnceOnResumed = null
            pending
        }

        action?.invoke(activity)
    }

    /**
     * Clears paused activity reference.
     */
    override fun onActivityPaused(activity: Activity) {
        val current = currentResumedRef?.get()
        if (current === activity) currentResumedRef = null
    }

    /**
     * Notifies subscribers that activity was stopped.
     */
    override fun onActivityStopped(activity: Activity) {
        val callback = synchronized(this) {
            onCurrentActivityFinished
        }

        callback?.invoke(activity)
    }

    /**
     * Clears destroyed activity reference and notifies subscribers.
     */
    override fun onActivityDestroyed(activity: Activity) {
        val current = currentResumedRef?.get()
        if (current === activity) currentResumedRef = null

        val callback = synchronized(this) {
            onCurrentActivityFinished
        }

        callback?.invoke(activity)
    }

    /**
     * Handles activity start event.
     */
    override fun onActivityStarted(activity: Activity) {}

    /**
     * Handles activity state save event.
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}