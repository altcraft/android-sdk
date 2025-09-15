package com.altcraft.altcraftmobile.providers.rustore

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.util.Log
import ru.rustore.sdk.pushclient.common.logger.Logger

/**
 *A class designed to get RuStore logs of different levels.
 */
class DefaultLogger(
private val tag: String? = null,
) : Logger {
    override fun verbose(message: String, throwable: Throwable?) {
        Log.v(tag, message, throwable)
    }

    override fun debug(message: String, throwable: Throwable?) {
        Log.d(tag, message, throwable)
    }

    override fun info(message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun createLogger(tag: String): Logger {
        return DefaultLogger(tag)
    }
}