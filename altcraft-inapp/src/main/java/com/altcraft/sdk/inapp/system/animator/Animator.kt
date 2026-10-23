package com.altcraft.sdk.inapp.system.animator

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.view.View
import android.view.ViewPropertyAnimator

/**
 * Provides animation hooks for a ready in-app view.
 */
interface Animator {

    /**
     * Identifier of the placement associated with this animator.
     */
    val companyId: Int

    /**
     * Applies the given block to the view immediately,
     * without running an animation.
     */
    fun prepare(block: View.() -> Unit)

    /**
     * Configures and runs a property animation for the view.
     */
    fun animate(block: ViewPropertyAnimator.() -> Unit)
}