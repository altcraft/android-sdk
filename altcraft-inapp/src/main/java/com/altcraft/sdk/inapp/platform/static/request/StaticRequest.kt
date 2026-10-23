package com.altcraft.sdk.inapp.platform.static.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.events.SDKException.exception
import com.altcraft.sdk.inapp.events.ModuleEvents
import com.altcraft.sdk.network.request.RequestExecutor
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads in-app static resources from configured static endpoints.
 */
internal object StaticRequest {

    /**
     * Loads the common in-app library JS and block-specific CSS/JS.
     *
     * @param context Application context.
     * @return loaded [StaticLoadModel], or `null` if downloading failed.
     */
    suspend fun inAppStaticRequest(context: Context): StaticLoadModel? {
        return try {
            val blockCss = RequestExecutor.getRawText(
                context,
                Path.inAppBlockCssPath()
            ).orEmpty()
            val blockJs = RequestExecutor.getRawText(
                context,
                Path.inAppBlockJsPath()
            ).orEmpty()
            val libraryJs = RequestExecutor.getRawText(
                context,
                Path.inAppLibJsPath()
            ).orEmpty()

            if (
                blockCss.isBlank() ||
                blockJs.isBlank() ||
                libraryJs.isBlank()
            ) exception(
                ModuleEvents.invalidStaticField
            )

            StaticLoadModel(
                blockCss = blockCss,
                blockJs = blockJs,
                inAppLibraryJs = libraryJs,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error("loadStaticResources", e)
            null
        }
    }
}
