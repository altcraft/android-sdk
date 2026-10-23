package com.altcraft.sdk.inapp.system.animator

// Created by Andrey Pogodin.
//
// Copyright © 2026 Altcraft. All rights reserved.

import android.view.View
import android.view.ViewPropertyAnimator

/**
 * Default [Animator] implementation for a specific [View].
 *
 * Applies immediate view changes in [prepare] and runs property animations
 * through [View.animate] in [animate].
 */
internal class AnimatorImpl(
    override val companyId: Int,
    private val view: View
) : Animator {

    /**
     * Applies the given block directly to the target [view]
     * without starting an animation.
     */
    override fun prepare(block: View.() -> Unit) {
        view.block()
    }

    /**
     * Configures and starts a [ViewPropertyAnimator] for the target [view].
     */
    override fun animate(block: ViewPropertyAnimator.() -> Unit) {
        view.animate().apply(block).start()
    }
}