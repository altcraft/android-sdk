package com.altcraft.sdk.inapp.system.presentation

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.ResponseModel
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.inapp.platform.event.EventProcess.sendErrorInAppEvent
import com.altcraft.sdk.inapp.platform.static.StaticProcess.loadStatic
import com.altcraft.sdk.inapp.system.image.InAppImageLoader.loadImages
import com.altcraft.sdk.inapp.system.policy.Check
import com.altcraft.sdk.inapp.system.presentation.InAppVariables.updateInAppVariables
import com.altcraft.sdk.inapp.system.presentation.OpenInApp.InAppDisplayType
import com.altcraft.sdk.inapp.util.StringBuilder.inAppImagesErrorMessage
import com.altcraft.sdk.network.json.Converter.json
import com.altcraft.sdk.network.request.RequestExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Presents In-App campaigns and waits until they are shown and dismissed.
 */
internal object Presenter {

    /** Mutex that prevents multiple In-App campaigns from being presented at the same time. */
    private val presentMutex = Mutex()

    /**
     * Prepared in-app content with native display type.
     *
     * @param content Final HTML content.
     * @param static Static resources required by WebView client.
     * @param displayType Native display type resolved from backend content.
     */
    private data class PreparedInAppContent(
        val content: String,
        val static: InAppModel.InAppStatic,
        val displayType: InAppDisplayType
    )

    /**
     * Presents an In-App campaign if it passes placement and runtime checks.
     *
     * For delayed presentation, availability, policy, and deadline
     * are revalidated before showing the campaign.
     *
     * Presentations invalidated by [PresentationGate.invalidate] are
     * discarded: while waiting for the presentation mutex and after
     * content loading, before the campaign is displayed.
     *
     * @param context Application context.
     * @param inApp In-App campaign to present.
     * @param delayTask `true` if presentation was triggered by a delayed task.
     * @param trigger Trigger that started this presentation.
     */
    suspend fun present(
        context: Context,
        inApp: InApp,
        delayTask: Boolean = false,
        trigger: InAppTrigger? = null,
    ) {
        val scheduledEpoch = PresentationGate.epoch
        presentMutex.withLock {
            try {
                coroutineScope {
                    if (PresentationGate.epoch != scheduledEpoch)
                        return@coroutineScope

                    if (!canPresent(context, inApp, delayTask, trigger))
                        return@coroutineScope

                    val content = loadInAppContent(context, inApp, scheduledEpoch)
                        ?: return@coroutineScope

                    if (PresentationGate.epoch != scheduledEpoch)
                        return@coroutineScope

                    val dismissDeferred = CompletableDeferred<Unit>()
                    val shownDeferred = CompletableDeferred<Unit>()

                    OpenInApp().openInApp(
                        smid = inApp.smid,
                        companyId = inApp.companyId,
                        content = content.content,
                        static = content.static,
                        displayType = content.displayType,
                        onShown = {
                            launch {
                                try {
                                    if (!inApp.isTest && trigger != null) {
                                        updateInAppVariables(
                                            context, inApp, trigger
                                        )
                                    }
                                } finally {
                                    if (!shownDeferred.isCompleted)
                                        shownDeferred.complete(Unit)
                                }
                            }
                        },
                        onDismiss = {
                            if (!shownDeferred.isCompleted)
                                shownDeferred.complete(Unit)

                            if (!dismissDeferred.isCompleted)
                                dismissDeferred.complete(Unit)
                        }
                    )

                    shownDeferred.await()
                    dismissDeferred.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("present", e)
            }
        }
    }

    /**
     * Checks whether an In-App campaign can be presented.
     *
     * Test campaigns are always allowed.
     * Non-test campaigns require a trigger and valid placement.
     * For delayed presentation, runtime display conditions are revalidated.
     *
     * @param context Application context.
     * @param inApp In-App campaign to check.
     * @param delayTask `true` if presentation was triggered by a delayed task.
     * @param trigger Trigger associated with the campaign.
     * @return `true` if the campaign can be presented.
     */
    private suspend fun canPresent(
        context: Context,
        inApp: InApp,
        delayTask: Boolean,
        trigger: InAppTrigger?
    ): Boolean {
        if (inApp.isTest) return true
        if (trigger == null) return false
        if (!Check.Placement.canShow(inApp)) return false

        return !delayTask || Check.canShow(
            context = context, inApp = inApp, trigger = trigger
        )
    }

    /**
     * Loads and parses in-app content from the content URL.
     *
     * @param inApp In-app campaign.
     * @return Parsed in-app content.
     */
    private suspend fun loadContent(inApp: InApp): ResponseModel.InAppContent? {
        val element = RequestExecutor.getRaw(
            inApp.contentUrl
        ) ?: return null

        return json.decodeFromJsonElement(
            ResponseModel.InAppContent.serializer(), element
        )
    }

    /**
     * Loads and prepares in-app content for display.
     *
     * Preloads images, loads static resources,
     * resolves display type, and builds final content for OpenInApp.
     *
     * JSON campaigns are emitted to the subscriber only when the presentation
     * epoch is unchanged, so cleared campaigns are never delivered.
     *
     * @param context Application context.
     * @param inApp In-app campaign.
     * @param scheduledEpoch Presentation epoch captured when presentation started.
     * @return Prepared in-app content or `null`.
     */
    private suspend fun loadInAppContent(
        context: Context,
        inApp: InApp,
        scheduledEpoch: Long
    ): PreparedInAppContent? = coroutineScope {
        try {
            val static = loadStatic(context)
                ?: exception(
                    ModuleEvents.inAppStaticErrorLoad
                )

            val content = loadContent(inApp)
                ?: exception(
                    ModuleEvents.inAppContentErrorLoad
                )

            if (inApp.type == InAppContentType.JSON) {
                if (PresentationGate.epoch == scheduledEpoch) {
                    JsonContentAPI.emit(content.content)
                }
                return@coroutineScope null
            }

            loadImages(context, content.images).let {
                if (!it.success) throw Exception(
                    inAppImagesErrorMessage(
                        it.failedUrls
                    )
                )
            }

            val displayType = content.displayType
            val inAppLibraryJs = static.inAppLibraryJs
            val isCustom = inApp.type == InAppContentType.CUSTOM
            val blockJs = if (isCustom) null else static.blockJs
            val blockCss = if (isCustom) null else static.blockCss

            PreparedInAppContent(
                content = content.content,
                static = InAppModel.InAppStatic(
                    inAppLibraryJs = inAppLibraryJs,
                    blockJs = blockJs,
                    blockCss = blockCss
                ),
                displayType = InAppDisplayType.from(displayType)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sendErrorInAppEvent(context, inApp.smid, e.message)
            error("loadInAppContent", e)
            null
        }
    }
}