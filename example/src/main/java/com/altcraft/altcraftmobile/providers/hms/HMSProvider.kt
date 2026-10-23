package com.altcraft.altcraftmobile.providers.hms

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import com.altcraft.sdk.push.platform.token.provider.HMSInterface
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.vk.push.common.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val APP_ID = "client/app_id"
private const val TOKEN_SCOPE = "HCM"

/**
 * HMS implementation for managing Huawei Mobile Services push tokens.
 *
 * Provides methods to retrieve and delete HMS tokens using the Huawei SDK.
 */
class HMSProvider : HMSInterface {

    /**
     * Retrieves the current HMS token.
     *
     * Checks HMS availability and fetches the token asynchronously. Returns `null` if HMS is
     * unavailable or an error occurs.
     *
     * @param context Context required for HMS operations.
     * @return The HMS token, or `null` on failure.
     */
    override suspend fun getToken(context: Context): String? =
        withContext(Dispatchers.IO) {
            try {
                val availability = HuaweiApiAvailability
                    .getInstance()
                    .isHuaweiMobileServicesAvailable(context)

                if (availability != ConnectionResult.SUCCESS) {
                    return@withContext null
                }

                val appId = AGConnectOptionsBuilder()
                    .build(context)
                    .getString(APP_ID)

                HmsInstanceId
                    .getInstance(context)
                    .getToken(appId, TOKEN_SCOPE)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("altcraft_lib", "HMSProvider.getToken failed", e)
                null
            }
        }

    /**
     * Deletes the HMS token.
     *
     * Attempts deletion via HMS SDK and calls [complete] with `true` on success, `false` otherwise.
     *
     * @param context Context required for HMS operations.
     * @param complete Callback with the result of the operation.
     */
    override fun deleteToken(context: Context, complete: (Boolean) -> Unit) {
        try {
            val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID)

            HmsInstanceId.getInstance(context).deleteToken(appId, TOKEN_SCOPE)
            complete(true)
        } catch (_: Exception) {
            complete(false)
        }
    }
}

