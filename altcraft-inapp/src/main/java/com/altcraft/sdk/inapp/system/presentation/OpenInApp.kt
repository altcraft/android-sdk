package com.altcraft.sdk.inapp.system.presentation

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Activity
import android.app.Dialog
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import com.altcraft.sdk.coordination.SDKScope
import com.altcraft.sdk.inapp.InApp
import com.altcraft.sdk.inapp.data.Constants.BASE_URL
import com.altcraft.sdk.inapp.data.Constants.CUSTOM_IN_APP
import com.altcraft.sdk.inapp.data.Constants.FLOATING_BAR_IN_APP
import com.altcraft.sdk.inapp.data.Constants.FULL_SCREEN_IN_APP
import com.altcraft.sdk.inapp.data.Constants.MODAL_IN_APP
import com.altcraft.sdk.inapp.data.Constants.SLIDE_UP_IN_APP
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.platform.event.EventProcess.sendErrorInAppEvent
import com.altcraft.sdk.inapp.platform.event.EventProcess.sendInAppEvent
import com.altcraft.sdk.inapp.system.animator.AnimatorImpl
import com.altcraft.sdk.inapp.system.dialog.InAppDialog
import com.altcraft.sdk.inapp.system.dialog.InAppDialog.attachWebView
import com.altcraft.sdk.inapp.system.dialog.InAppDialog.createDialog
import com.altcraft.sdk.inapp.system.dialog.InAppDialog.showFullscreen
import com.altcraft.sdk.inapp.system.dialog.ProtectedInsets
import com.altcraft.sdk.inapp.system.lifecycle.ActivityTracker
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.InAppEventType
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.SlidePosition
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.SlideReadyPayload
import com.altcraft.sdk.inapp.system.presentation.bridge.InAppEventProcessing.setJavascriptInterface
import com.altcraft.sdk.inapp.system.webview.Client
import com.altcraft.sdk.inapp.system.webview.InAppWebView.createWebView
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.util.StringBuilder.fringeSizeInAppScript
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Opens and displays in-app messages.
 *
 * Creates a WebView, loads in-app content, connects JavaScript callbacks,
 * sends protected screen insets to JavaScript, applies native container layout,
 * emits display events, and safely destroys WebView resources on close.
 *
 * All display types are initially opened in an invisible fullscreen staging
 * dialog. When JavaScript sends `ready`, Android sends protected top and bottom
 * screen insets to the WebView.
 *
 * Fullscreen-like in-app messages are revealed after the inset script has been
 * evaluated. Slide-up messages remain invisible until JavaScript sends
 * `slideReady`, after which Android resizes and positions the dialog container.
 */
internal class OpenInApp {

    private companion object {

        /**
         * Prevents multiple in-app messages from being opened simultaneously.
         *
         * Shared at the companion level because a new [OpenInApp] instance is
         * created for every presentation call, so an instance-level mutex
         * would never guard two sessions against each other.
         */
        val inAppMutex = Mutex()

        /**
         * Delay before a slide-up message falls back to fullscreen presentation.
         */
        val SLIDE_READY_FALLBACK_DELAY_MS = 1.seconds
    }

    /**
     * Defines the native display type of in-app message.
     *
     * Values are mapped from the backend `display_type` field.
     *
     * [FLOATING_BAR], [FULLSCREEN], [CUSTOM], and [MODAL] currently use
     * fullscreen container behavior.
     *
     * [SLIDE_UP] waits for JavaScript layout data and then applies a smaller
     * top- or bottom-aligned native container.
     *
     * @property value Raw backend display type value.
     */
    internal enum class InAppDisplayType(
        val value: String
    ) {
        FLOATING_BAR(FLOATING_BAR_IN_APP),
        FULLSCREEN(FULL_SCREEN_IN_APP),
        SLIDE_UP(SLIDE_UP_IN_APP),
        CUSTOM(CUSTOM_IN_APP),
        MODAL(MODAL_IN_APP);

        companion object {

            /**
             * Maps a raw backend value to [InAppDisplayType].
             *
             * Unknown and missing values are treated as [FULLSCREEN].
             *
             * @param value Raw backend display type.
             * @return Resolved native display type.
             */
            fun from(value: String?): InAppDisplayType = entries.firstOrNull {
                    it.value == value
                } ?: FULLSCREEN
        }
    }

    /**
     * Opens an in-app message.
     *
     * The presentation waits for a resumed activity, then creates and configures
     * a new presentation session. Only one in-app presentation is initialized
     * at a time.
     *
     * The presentation epoch is captured before parking on a resumed activity;
     * if the SDK clears in-app data while parked, the pending presentation is
     * discarded and the dismissal callback releases the caller.
     *
     * If the session cannot be created or started, the failure is logged and
     * the dismissal callback is invoked so awaiting presentations never stall.
     *
     * @param smid In-app message identifier.
     * @param companyId Company identifier.
     * @param content HTML content.
     * @param static Static JavaScript and CSS resources.
     * @param displayType Native display type.
     * @param onShown Callback invoked after the in-app becomes visible.
     * @param onDismiss Callback invoked after the in-app is closed.
     */
    fun openInApp(
        smid: String,
        companyId: Int,
        content: String,
        static: InAppModel.InAppStatic,
        displayType: InAppDisplayType = InAppDisplayType.FULLSCREEN,
        onShown: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val epochAtOpen = PresentationGate.epoch

        ActivityTracker.runOnNextResumed { activity ->
            SDKScope.main.launch {
                inAppMutex.withLock {
                    if (PresentationGate.epoch != epochAtOpen) {
                        onDismiss?.invoke()
                        return@launch
                    }

                    try {
                        PresentationSession(
                            activity = activity,
                            smid = smid,
                            companyId = companyId,
                            displayType = displayType,
                            onShown = onShown,
                            onDismiss = onDismiss
                        ).open(
                            content = content,
                            static = static
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error("openInApp", e)
                        onDismiss?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Owns all state and resources of a single in-app presentation.
     *
     * The session manages the dialog, WebView, JavaScript callbacks, lifecycle
     * callbacks, slide layout, fallback presentation, display events, and cleanup.
     *
     * @property activity Activity hosting the in-app presentation.
     * @property smid In-app message identifier.
     * @property companyId Company identifier.
     * @property displayType Native display type.
     * @property onShown Callback invoked after the in-app becomes visible.
     * @property onDismiss Callback invoked after the in-app is closed.
     */
    private class PresentationSession(
        private val activity: Activity,
        private val smid: String,
        private val companyId: Int,
        private val displayType: InAppDisplayType,
        private val onShown: (() -> Unit)?,
        private val onDismiss: (() -> Unit)?
    ) {

        private val dialog: Dialog = createDialog(
            activity = activity,
            fullScreen = false
        )

        private val webView: WebView = createWebView(activity)

        private var ready = false
        private var fringeSent = false
        private var shown = false
        private var finished = false
        private var closingBySdk = false

        private var insets: ProtectedInsets? = null
        private var pendingSlidePayload: SlideReadyPayload? = null

        private var fallbackJob: Job? = null
        private var orientationCallback: ComponentCallbacks? = null

        /**
         * Checks whether the session may still interact with its host activity.
         *
         * A session becomes invalid once it has finished or once the host
         * activity is finishing or already destroyed. All deferred presentation
         * callbacks are dispatched on the main thread, so this synchronous
         * check closes the race between an asynchronously scheduled close and
         * pending presentation work.
         *
         * @return `true` if the session can proceed with presentation work.
         */
        private fun isSessionValid(): Boolean =
            !finished && !activity.isFinishing && !activity.isDestroyed

        /**
         * Configures and loads the current in-app.
         *
         * Registers lifecycle callbacks, configures the dialog and JavaScript
         * bridge, attaches the WebView, installs the WebView client, and loads
         * the supplied HTML content.
         *
         * @param content HTML content to load into the WebView.
         * @param static Static JavaScript and CSS resources.
         */
        fun open(
            content: String,
            static: InAppModel.InAppStatic
        ) {
            try {
                setupLifecycle()
                setupDialog()

                attachWebView(
                    dialog = dialog,
                    webView = webView
                )

                setupJavascriptInterface()
                setupWebViewClient(static)

                webView.loadDataWithBaseURL(
                    BASE_URL,
                    content,
                    "text/html",
                    "utf-8",
                    null
                )
            } catch (e: Exception) {
                fallbackJob?.cancel()

                sendErrorInAppEvent(
                    activity,
                    smid,
                    e.message
                )

                error("openInApp", e)

                finish()
            }
        }

        /**
         * Registers lifecycle callbacks used to close the in-app when its
         * host activity changes configuration or finishes.
         */
        private fun setupLifecycle() {
            orientationCallback = object : ComponentCallbacks {

                /** Closes the in-app session when the configuration changes. */
                override fun onConfigurationChanged(
                    newConfig: Configuration
                ) {
                    close()
                }

                /** No-op: the session owns no memory-sensitive resources. */
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onLowMemory() = Unit
            }

            activity.registerComponentCallbacks(orientationCallback)

            ActivityTracker.setOnCurrentActivityFinished {
                if (it === activity) {
                    close()
                }
            }
        }

        /**
         * Configures native dialog close callbacks.
         *
         * Handles the Android back button, external dialog dismissal,
         * and dialog cancellation.
         */
        private fun setupDialog() {
            dialog.setOnKeyListener { _, keyCode, event ->
                if (
                    keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP
                ) {
                    close()
                    true
                } else {
                    false
                }
            }

            dialog.setOnDismissListener {
                if (!closingBySdk) {
                    close()
                }
            }

            dialog.setOnCancelListener {
                if (!closingBySdk) {
                    close()
                }
            }
        }

        /**
         * Connects JavaScript callbacks to the current presentation.
         *
         * Handles JavaScript readiness, slide layout readiness, and explicit
         * close requests from the in-app content.
         */
        private fun setupJavascriptInterface() {
            setJavascriptInterface(
                context = activity,
                webView = webView,
                smid = smid,
                onReady = {
                    SDKScope.main.launch {
                        if (!isSessionValid()) {
                            finish()
                            return@launch
                        }

                        ready = true
                        trySendFringe()
                    }
                },
                onSlideReady = { payload ->
                    SDKScope.main.launch {
                        if (!isSessionValid()) {
                            finish()
                            return@launch
                        }

                        if (shown) {
                            return@launch
                        }

                        fallbackJob?.cancel()
                        pendingSlidePayload = payload

                        tryShowSlide()
                    }
                },
                onClose = {
                    close()
                }
            )
        }

        /**
         * Configures the WebView client and handles page loading completion.
         *
         * After the page loads, the invisible fullscreen staging dialog is shown
         * and protected screen insets are collected. The method then attempts to
         * send the insets to JavaScript and starts the slide-ready fallback.
         *
         * @param static Static JavaScript and CSS resources.
         */
        private fun setupWebViewClient(
            static: InAppModel.InAppStatic
        ) {
            webView.webViewClient = Client(
                staticBundle = static,
                onPageLoaded = {
                    SDKScope.main.launch {
                        if (!isSessionValid()) {
                            finish()
                            return@launch
                        }

                        if (!dialog.isShowing) {
                            val shown = showFullscreen(dialog) { protectedInsets ->
                                insets = protectedInsets
                                trySendFringe()
                            }

                            if (!shown) {
                                finish()
                                return@launch
                            }
                        } else {
                            trySendFringe()
                        }

                        startFallback()
                    }
                }
            )
        }

        /**
         * Sends protected screen insets to JavaScript.
         *
         * The method waits until JavaScript is ready and protected top and bottom
         * screen insets are available. The inset script is evaluated only once.
         */
        private fun trySendFringe() {
            if (!ready || fringeSent) {
                return
            }

            val currentInsets = insets ?: return

            fringeSent = true

            webView.evaluateJavascript(
                fringeSizeInAppScript(
                    top = currentInsets.top,
                    bottom = currentInsets.bottom
                ),
                null
            )
        }

        /**
         * Applies a pending slide layout when the required presentation state
         * is ready.
         *
         * For [InAppDisplayType.SLIDE_UP], the dialog is resized and positioned
         * using the JavaScript payload before being revealed. Other display types
         * are revealed without applying slide layout.
         */
        private fun tryShowSlide() {
            if (shown || !fringeSent) {
                return
            }

            val payload = pendingSlidePayload ?: return

            if (displayType == InAppDisplayType.SLIDE_UP) {
                applySlideLayout(payload)
            }

            pendingSlidePayload = null

            show()
        }

        /**
         * Starts the fallback timer for in-app presentation.
         *
         * If JavaScript does not provide usable slide layout information within
         * [SLIDE_READY_FALLBACK_DELAY_MS], the staging dialog remains fullscreen
         * and is revealed.
         */
        private fun startFallback() {
            fallbackJob?.cancel()

            fallbackJob = SDKScope.main.launch {
                delay(SLIDE_READY_FALLBACK_DELAY_MS)

                if (!isSessionValid()) {
                    finish()
                    return@launch
                }

                if (shown) {
                    return@launch
                }

                pendingSlidePayload = null

                webView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                webView.requestLayout()

                trySendFringe()

                show()
            }
        }

        /**
         * Applies the final slide layout to the dialog and WebView.
         *
         * JavaScript dimensions are expressed in CSS pixels and are converted
         * to physical Android pixels using the current display density.
         *
         * The payload height defines the dialog height. The edge-specific
         * padding defines the external offset from the selected screen edge.
         *
         * @param payload Final slide layout received from JavaScript.
         */
        private fun applySlideLayout(
            payload: SlideReadyPayload
        ) {
            val metrics = webView.resources.displayMetrics
            val density = metrics.density

            val heightPx = (
                    payload.height.coerceAtLeast(0) * density
                    )
                .roundToInt()
                .coerceIn(
                    minimumValue = 1,
                    maximumValue = metrics.heightPixels
                )

            val offsetCssPx = when (payload.position) {
                SlidePosition.TOP -> payload.paddingTop
                SlidePosition.BOTTOM -> payload.paddingBottom
            }.coerceAtLeast(0)

            val positionOffsetPx = (
                    offsetCssPx * density
                    ).roundToInt()

            InAppDialog.applySlideLayout(
                dialog = dialog,
                heightPx = heightPx,
                position = payload.position,
                positionOffsetPx = positionOffsetPx
            )

            webView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )

            webView.requestLayout()
        }

        /**
         * Reveals the current in-app and reports it as shown exactly once.
         *
         * Sends the SHOW event, provides an [AnimatorImpl] to the global
         * in-app shown listener, and invokes the presentation callback.
         */
        private fun show() {
            if (shown) {
                return
            }

            shown = true

            InAppDialog.reveal(dialog)

            sendInAppEvent(
                context = webView.context.applicationContext,
                type = InAppEventType.SHOW,
                smid = smid,
                data = null
            )

            InApp.onInAppShownListener?.invoke(
                AnimatorImpl(
                    companyId = companyId,
                    view = webView
                )
            )

            onShown?.invoke()
        }

        /**
         * Schedules presentation cleanup on the SDK main scope.
         *
         * Cancels the active fallback job before finishing the presentation.
         */
        private fun close() {
            SDKScope.main.launch {
                fallbackJob?.cancel()
                finish()
            }
        }

        /**
         * Finishes the current presentation exactly once.
         *
         * Removes lifecycle callbacks, dismisses the dialog, destroys the WebView,
         * and invokes the dismissal callback.
         */
        private fun finish() {
            if (finished) {
                return
            }

            finished = true

            fallbackJob?.cancel()
            fallbackJob = null

            ActivityTracker.setOnCurrentActivityFinished(null)

            orientationCallback?.let {
                try {
                    dialog.context.unregisterComponentCallbacks(it)
                } catch (_: Throwable) {
                }
            }

            orientationCallback = null

            if (dialog.isShowing) {
                closingBySdk = true
                dialog.dismiss()
            }

            destroyWebView()

            onDismiss?.invoke()
        }

        /**
         * Safely destroys the WebView and releases associated resources.
         *
         * Stops loading, clears navigation state, detaches the WebView from
         * its parent, and destroys the underlying WebView instance.
         */
        private fun destroyWebView() {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()

                (webView.parent as? ViewGroup)?.removeView(webView)

                webView.destroy()
            } catch (_: Throwable) {
            }
        }
    }
}