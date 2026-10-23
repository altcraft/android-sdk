package com.altcraft.sdk.inapp.system.dialog

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Dialog
import android.os.Build
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/**
 * Protected safe-area insets in CSS pixels.
 *
 * @property top Top protected inset, including status bar or display cutout.
 * @property bottom Bottom protected inset, including navigation bar.
 */
internal data class ProtectedInsets(
    val top: Int,
    val bottom: Int
)

/**
 * Retrieves protected top and bottom inset areas in CSS pixels.
 *
 * Top value includes status bar and display cutout safe area.
 * Bottom value includes navigation bar and display cutout safe area.
 * The callback is invoked once when inset information becomes available.
 *
 * @param onInsetsReady Callback receiving protected insets in CSS pixels.
 */
internal fun Dialog.getProtectedInsets(
    onInsetsReady: (ProtectedInsets) -> Unit
) {
    val decorView = window?.decorView ?: return

    var delivered = false

    ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars()
        )

        val displayCutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowInsets.displayCutout?.let { cutout ->
                Insets.of(
                    cutout.safeInsetLeft,
                    cutout.safeInsetTop,
                    cutout.safeInsetRight,
                    cutout.safeInsetBottom
                )
            } ?: Insets.NONE
        } else {
            Insets.NONE
        }

        val topInsetPx = maxOf(
            systemBars.top,
            displayCutout.top
        )

        val bottomInsetPx = maxOf(
            systemBars.bottom,
            displayCutout.bottom
        )

        if (!delivered && (topInsetPx > 0 || bottomInsetPx > 0)) {
            delivered = true

            val density = view.resources.displayMetrics.density

            val protectedInsets = ProtectedInsets(
                top = (topInsetPx / density).roundToInt(),
                bottom = (bottomInsetPx / density).roundToInt()
            )

            onInsetsReady(protectedInsets)
        }

        windowInsets
    }

    ViewCompat.requestApplyInsets(decorView)
}