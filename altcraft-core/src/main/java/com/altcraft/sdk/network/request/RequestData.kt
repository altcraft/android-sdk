package com.altcraft.sdk.network.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo
import com.altcraft.sdk.events.SDKEvent
import com.altcraft.sdk.util.log.Log.MODULE_PREFIX
import kotlinx.serialization.KSerializer

/**
 * Describes an SDK request and its response.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface RequestData<R> {

    /** Network transport data. */
    val requestModel: GenericRequest

    /** Request name used in logs. */
    val requestName: String

    /** Event emitted after successful execution. */
    val successEvent: SDKEvent

    /** Event emitted after failed execution. */
    val failedEvent: SDKEvent

    /** Data included in SDK events. */
    val eventData: Map<String, Any?>

    /** Data included in logs. */
    val loggerData: Map<String, Any?>

    /** Module log prefix; defaults to core. */
    val moduleLogPrefix: String get() = MODULE_PREFIX

    /** Serializer used to decode the response. */
    val responseSerializer: KSerializer<R>
}