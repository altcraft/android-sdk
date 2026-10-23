package com.altcraft.sdk.inapp.system.presentation.bridge

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.webkit.WebView
import com.altcraft.sdk.inapp.data.Constants.IN_APP_CLICK
import com.altcraft.sdk.inapp.data.Constants.IN_APP_CLOSE
import com.altcraft.sdk.inapp.data.Constants.IN_APP_DELIVERY
import com.altcraft.sdk.inapp.data.Constants.IN_APP_ERROR
import com.altcraft.sdk.inapp.data.Constants.IN_APP_READY
import com.altcraft.sdk.inapp.data.Constants.IN_APP_SHOW
import com.altcraft.sdk.inapp.data.Constants.IN_APP_SLIDE_READY
import com.altcraft.sdk.inapp.data.Constants.JS_INTERFACE_NAME
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppEventData
import com.altcraft.sdk.inapp.platform.event.EventProcess.sendInAppEvent
import com.altcraft.sdk.network.json.Converter.json
import androidx.core.net.toUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import com.altcraft.sdk.inapp.util.Log.error

/**
 * Handles in-app events received from JavaScript inside WebView.
 *
 * Adds a JavaScript bridge, deserializes incoming event payloads,
 * sends SDK in-app events, opens links, handles ready state,
 * handles slide layout readiness, and requests dialog closing when needed.
 */
internal object InAppEventProcessing {

    /**
     * Injects the JavaScript interface into the provided [WebView].
     *
     * JavaScript must send events directly to the native bridge:
     *
     * window[JS_INTERFACE_NAME].sendEvent(jsonString)
     *
     * No postMessage forwarding listener is installed here.
     *
     * @param context Context used for event sending and link opening.
     * @param webView WebView where the JavaScript bridge is injected.
     * @param smid Send Message ID associated with the current in-app message.
     * @param onReady Callback invoked when JavaScript reports that it is ready.
     * @param onSlideReady Callback invoked when JavaScript reports slide layout size.
     * @param onClose Callback that performs safe in-app closing.
     */
    fun setJavascriptInterface(
        context: Context,
        webView: WebView,
        smid: String,
        onReady: () -> Unit,
        onSlideReady: (SlideReadyPayload) -> Unit,
        onClose: () -> Unit
    ) {
        webView.addJavascriptInterface(
            InAppBridge {
                inAppEventProcessing(
                    context = context,
                    data = it,
                    smid = smid,
                    onReady = onReady,
                    onSlideReady = onSlideReady,
                    onClose = onClose
                )
            },
            JS_INTERFACE_NAME
        )
    }

    /**
     * Represents the type of in-app event.
     */
    @Serializable
    internal enum class InAppEventType(
        val value: String
    ) {
        @SerialName(IN_APP_DELIVERY)
        DELIVERY(IN_APP_DELIVERY),

        @SerialName(IN_APP_SHOW)
        SHOW(IN_APP_SHOW),

        @SerialName(IN_APP_CLICK)
        CLICK(IN_APP_CLICK),

        @SerialName(IN_APP_ERROR)
        ERROR(IN_APP_ERROR),

        @SerialName(IN_APP_CLOSE)
        CLOSE(IN_APP_CLOSE),

        @SerialName(IN_APP_READY)
        READY(IN_APP_READY),

        @SerialName(IN_APP_SLIDE_READY)
        SLIDE_READY(IN_APP_SLIDE_READY)
    }

    /**
     * Represents slide in-app vertical position.
     */
    @Serializable
    internal enum class SlidePosition {
        @SerialName("top")
        TOP,

        @SerialName("bottom")
        BOTTOM
    }

    /**
     * Layout payload received from JavaScript for slide in-app containers.
     *
     * Values are expected to be provided in CSS pixels.
     *
     * @param height Main slide content height.
     * @param paddingTop Additional top padding around the slide content.
     * @param paddingBottom Additional bottom padding around the slide content.
     * @param position Vertical position of the slide container.
     */
    @Serializable
    internal data class SlideReadyPayload(
        val height: Int,
        val paddingTop: Int = 0,
        val paddingBottom: Int = 0,
        val position: SlidePosition = SlidePosition.BOTTOM
    )

    /**
     * Raw JavaScript event envelope.
     *
     * Payload is kept as [JsonElement] so service events and layout events
     * can be decoded into different payload models.
     */
    @Serializable
    private data class InAppEventEnvelope(
        val type: InAppEventType,
        val payload: JsonElement? = null
    )

    /**
     * Processes an in-app event received from JavaScript.
     *
     * @param context Context used for opening links and sending events.
     * @param data Raw JSON event payload received from JavaScript.
     * @param smid Send Message ID associated with the current in-app message.
     * @param onReady Callback invoked when JavaScript reports that it is ready.
     * @param onSlideReady Callback invoked when JavaScript reports slide layout size.
     * @param onClose Callback that performs safe in-app closing.
     */
    internal fun inAppEventProcessing(
        context: Context,
        data: String,
        smid: String,
        onReady: () -> Unit,
        onSlideReady: (SlideReadyPayload) -> Unit,
        onClose: () -> Unit
    ) {
        try {
            val event = json.decodeFromString<InAppEventEnvelope>(data)

            when (event.type) {
                InAppEventType.READY -> {
                    onReady()
                    return
                }

                InAppEventType.SLIDE_READY -> {
                    val payload = json.decodeFromJsonElement<SlideReadyPayload>(
                        event.payload ?: return
                    )

                    onSlideReady(payload)
                    return
                }

                else -> Unit
            }

            val payload = event.payload?.let {
                json.decodeFromJsonElement<InAppEventData>(it)
            }

            event.type.handleInAppInteraction(
                context = context,
                payload = payload,
                onClose = onClose
            )

            val eventData = buildInAppEventData(
                type = event.type,
                payload = payload
            )

            sendInAppEvent(
                context = context,
                type = event.type,
                smid = smid,
                data = eventData
            )
        } catch (e: Exception) {
            error("inAppEventProcessing", e)
        }
    }

    /**
     * Builds normalized in-app event payload.
     *
     * @param type In-app event type.
     * @param payload Original event payload.
     * @return Normalized event payload.
     */
    private fun buildInAppEventData(
        type: InAppEventType,
        payload: InAppEventData?
    ): InAppEventData? =
        if (type == InAppEventType.ERROR) {
            InAppEventData(
                code = 400,
                message = payload?.message
            )
        } else {
            payload
        }

    /**
     * Applies UI side effects for the received in-app event.
     *
     * @param context Context used for opening links.
     * @param payload Optional event payload received from JavaScript.
     * @param onClose Callback that performs safe in-app closing.
     */
    private fun InAppEventType.handleInAppInteraction(
        context: Context,
        payload: InAppEventData?,
        onClose: () -> Unit
    ) {
        payload?.href?.let {
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW
                ).setData(it.toUri())
                context.startActivity(intent)
            } catch (e: Exception) {
                error("openLink", e)
            }
        }

        when (this) {
            InAppEventType.SLIDE_READY -> Unit
            InAppEventType.CLICK -> onClose()
            InAppEventType.CLOSE -> onClose()
            InAppEventType.DELIVERY -> Unit
            InAppEventType.READY -> Unit
            InAppEventType.ERROR -> Unit
            InAppEventType.SHOW -> Unit
        }
    }
}