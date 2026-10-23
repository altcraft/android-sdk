package com.altcraft.sdk.inapp.system.dialog

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.altcraft.sdk.inapp.R
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.SlidePosition
import com.altcraft.sdk.inapp.util.Log

/**
 * Creates and controls the in-app dialog container.
 *
 * The dialog can draw its content under system bars.
 * System bars remain visible and are not hidden here.
 */
internal object InAppDialog {

    /**
     * Creates a transparent dialog for displaying in-app content.
     *
     * The dialog starts with alpha 0f so it can be shown before WebView content
     * is revealed.
     *
     * When [fullScreen] is `true`, dialog system cancellation is disabled.
     * This prevents Android from closing the dialog immediately on Back press.
     * Back press can then be handled manually by the caller.
     *
     * @param activity Current activity used as dialog owner.
     * @param fullScreen Whether dialog should behave as fullscreen in-app container.
     * @return Configured in-app dialog.
     */
    fun createDialog(
        activity: Activity,
        fullScreen: Boolean = true
    ): Dialog {
        return Dialog(activity, R.style.AltcraftSdk_InAppDialogTheme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            setCancelable(!fullScreen)

            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0f)

                attributes = attributes.apply {
                    alpha = 0f
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                }
            }
        }
    }

    /**
     * Attaches WebView as the main dialog content.
     *
     * FrameLayout layout params are used because Dialog content is hosted
     * inside a FrameLayout-based container that measures children with margins.
     *
     * @param dialog Target in-app dialog.
     * @param webView WebView with in-app HTML content.
     */
    fun attachWebView(dialog: Dialog, webView: WebView) {
        dialog.setContentView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    /**
     * Shows dialog in fullscreen edge-to-edge mode.
     *
     * The content is drawn under status and navigation bars.
     * System bars remain visible.
     *
     * The dialog remains transparent and invisible until [reveal] is called.
     *
     * Showing is skipped when the hosting activity is finishing or already
     * destroyed; a window token invalidated between the check and
     * [Dialog.show] is caught and reported as a failed attempt instead of
     * throwing [android.view.WindowManager.BadTokenException] to the caller.
     *
     * @param dialog Target in-app dialog.
     * @param onInsetsReady Callback with protected top and bottom insets in physical pixels.
     * @return `true` when the dialog is shown; `false` when the host activity
     * is finishing or destroyed, or the window token became invalid.
     */
    fun showFullscreen(
        dialog: Dialog,
        onInsetsReady: ((ProtectedInsets) -> Unit)? = null
    ): Boolean {
        val hostActivity = dialog.context.findHostActivity()

        if (
            hostActivity == null ||
            hostActivity.isFinishing ||
            hostActivity.isDestroyed
        ) {
            return false
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            Log.error("showFullscreen", e)
            return false
        }

        val window = dialog.window ?: return false

        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.setDimAmount(0f)
        window.decorView.setPadding(0, 0, 0, 0)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams
                        .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        window.attributes = window.attributes.apply {
            alpha = 0f
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.NO_GRAVITY
            y = 0
        }

        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        onInsetsReady?.let { callback ->
            dialog.getProtectedInsets(callback)
        }

        return true
    }

    /**
     * Applies slide container layout to the dialog window.
     *
     * The dialog remains in edge-to-edge mode so that its position is always
     * calculated relative to the physical screen edge. Protected system insets are
     * not applied natively because they are delivered to JavaScript through the
     * `fringeSize` event.
     *
     * The payload edge padding is used as the only native window offset:
     * - [SlidePosition.TOP] uses `paddingTop`;
     * - [SlidePosition.BOTTOM] uses `paddingBottom`.
     *
     * @param dialog Target in-app dialog.
     * @param heightPx Final dialog height in physical pixels.
     * @param position Slide vertical position.
     * @param positionOffsetPx Offset from the selected physical screen edge.
     */
    fun applySlideLayout(
        dialog: Dialog,
        heightPx: Int,
        position: SlidePosition,
        positionOffsetPx: Int = 0
    ) {
        val window = dialog.window ?: return

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.setDimAmount(0f)
        window.decorView.setPadding(0, 0, 0, 0)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )

        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            heightPx
        )

        window.attributes = window.attributes.apply {
            alpha = 0f
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = heightPx

            gravity = when (position) {
                SlidePosition.TOP -> Gravity.TOP
                SlidePosition.BOTTOM -> Gravity.BOTTOM
            }

            y = positionOffsetPx.coerceAtLeast(0)
        }
    }

    /**
     * Reveals dialog after content is ready.
     *
     * @param dialog Target in-app dialog.
     */
    fun reveal(dialog: Dialog) {
        dialog.window?.apply {
            attributes = attributes.apply {
                alpha = 1f
            }
        }
    }

    /**
     * Resolves the activity that backs the given context.
     *
     * Dialog owns a themed context wrapper rather than the host activity
     * itself, so the base context chain is unwrapped until an activity is
     * found.
     *
     * @receiver Context to resolve.
     * @return Backing activity or `null` when the context chain has none.
     */
    private tailrec fun Context.findHostActivity(): Activity? {
        if (this is Activity) return this


        val base = (this as? ContextWrapper)?.baseContext ?: return null

        return base.findHostActivity()
    }
}