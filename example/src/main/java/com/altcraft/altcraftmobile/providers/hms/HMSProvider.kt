package com.altcraft.altcraftmobile.providers.hms

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.interfaces.HMSInterface
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

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
    override suspend fun getToken(context: Context): String? {
        return try {
            val availability =
                HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context)

            if (availability != ConnectionResult.SUCCESS) return null

            val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID)

            HmsInstanceId.getInstance(context).getToken(appId, TOKEN_SCOPE)
        } catch (_: Exception) {
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
    override suspend fun deleteToken(context: Context, complete: (Boolean) -> Unit) {
        try {
            val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID)

            HmsInstanceId.getInstance(context).deleteToken(appId, TOKEN_SCOPE)
            complete(true)
        } catch (_: Exception) {
            complete(false)
        }
    }
}

