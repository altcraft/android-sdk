package com.altcraft.sdk.push.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Error
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.RetryError
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.events.SDKEvent

/**
 * Push-module facade over [Events].
 *
 * Prefixes every SDK log emitted by the push module with the module tag
 * and delegates to [Events], which remains the single sink for logging
 * and subscriber event dispatch.
 */
internal object Log {

    /** Module tag added before the function name in the log line. */
    const val MODULE_PREFIX = "[altcraft-push] "

    /**
     * Emits an SDK event with the push-module tag.
     */
    fun event(
        function: String,
        event: SDKEvent,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): Event = Events.event(
        "$MODULE_PREFIX$function", event, eventValue, logValue
    )

    /**
     * Emits a non-retryable error with the push-module tag.
     */
    fun error(
        function: String,
        error: Any?,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): Error = Events.error(
        "$MODULE_PREFIX$function", error, eventValue, logValue
    )

    /**
     * Emits a retryable error with the push-module tag.
     */
    fun retry(
        function: String,
        error: Any?,
        eventValue: Map<String, Any?>? = null,
        logValue: Map<String, Any?>? = null,
    ): RetryError = Events.retry(
        "$MODULE_PREFIX$function", error, eventValue, logValue
    )
}
