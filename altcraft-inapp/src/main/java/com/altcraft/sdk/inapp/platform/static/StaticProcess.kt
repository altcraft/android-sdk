package com.altcraft.sdk.inapp.platform.static

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.webkit.WebView
import com.altcraft.sdk.inapp.util.Log.error
import com.altcraft.sdk.inapp.data.Constants.IN_APP_STATIC_O_WORK_TAG
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.room.entity.InAppStaticEntity
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.util.SubFunction.time
import com.altcraft.sdk.workers.onetime.Request.hasNewRequest
import com.altcraft.sdk.workers.onetime.Request.hasRunningWorker
import com.altcraft.sdk.workers.onetime.Request.startWorker
import com.altcraft.sdk.inapp.util.Log.retry
import com.altcraft.sdk.inapp.platform.static.request.StaticRequest.inAppStaticRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles loading, caching, and retry logic for in-app static resources.
 *
 * Downloads JS/CSS required for rendering in-app messages, caches them in memory
 * and database for 24 hours, and schedules retries via WorkManager on failure.
 *
 * SDK can operate without these resources, but in-app messages will not be displayed
 * until they are successfully loaded.
 */
internal object StaticProcess {

    /** Prevents concurrent execution of this command flow. */
    private val mutex = Mutex()

    /** Prevents concurrent static bundle downloads. */
    private val downloadMutex = Mutex()

    /** Function name used for logging and error reporting. */
    private const val FUNCTION = "inAppStatic"

    /** In-memory cache of loaded in-app static resources. */
    var static: InAppStaticEntity? = null

    /**
     * Returns cached or downloaded in-app static resources.
     *
     * Static resources are loaded from memory first, then from database cache,
     * and finally from the network. If loading fails, a retry worker is started.
     *
     * @param context Application context.
     * @return available [InAppStaticEntity], or null if static resources could not be loaded.
     */
    suspend fun loadStatic(context: Context): InAppStaticEntity? = mutex.withLock {
        val env: Environment = Environment.create(context)
        val bundles = static ?: getValidStaticBundle(env) ?: downloadStaticBundle(
            context, env
        )
        if (bundles == null && !hasRunningWorker(context, IN_APP_STATIC_O_WORK_TAG))

            startWorker<StaticWorker>(context, IN_APP_STATIC_O_WORK_TAG)
        static = bundles
        return static
    }

    /**
     * Checks whether downloading in-app static resources should be retried.
     *
     * @param context Application context.
     * @param workerId Optional worker identifier.
     * @return `true` if a retry should be performed; `false` otherwise.
     * @throws CancellationException if the coroutine is canceled.
     */
    suspend fun isRetry(context: Context, workerId: UUID? = null) = try {
        mutex.withLock {
            logic(context, workerId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        retry("isRetry :: $FUNCTION", e)
        true
    }

    /**
     * Performs retry decision logic for in-app static resources.
     *
     * A retry is required when static resources are still missing,
     * the download attempt failed, and there is no newer worker request.
     *
     * @param context Application context.
     * @param id Current worker identifier.
     * @return `true` if WorkManager should retry; `false` otherwise.
     */
    suspend fun logic(context: Context, id: UUID?): Boolean {
        val tag = IN_APP_STATIC_O_WORK_TAG
        val environment = Environment.create(context)

        return static == null && downloadStaticBundle(context, environment) == null
                && !hasNewRequest(
            context, tag, id
        )
    }

    /**
     * Returns a valid static bundle if it exists and is not expired.
     *
     * If the bundle is missing or older than 24 hours, returns null.
     * Expired bundles are removed from the database.
     *
     * @param environment SDK environment with database access.
     * @return valid [InAppStaticEntity], or null if missing or expired.
     */
    private suspend fun getValidStaticBundle(
        environment: Environment
    ): InAppStaticEntity? {
        val entity = environment.room.dao.static.get() ?: return null

        val isExpired = time() - entity.createdAt >= 24 * 60 * 60 * 1000L

        return if (isExpired) {
            environment.room.dao.static.delete()
            null
        } else {
            entity
        }
    }

    /**
     * Downloads in-app static resources from configured static endpoints.
     *
     * Loads the common in-app library JS and block-specific CSS/JS,
     * saves them to the database, and updates the in-memory cache.
     *
     * @param context Application context.
     * @param environment SDK environment with configuration and database access.
     * @return downloaded [InAppStaticEntity], or null if downloading failed.
     */
    private suspend fun downloadStaticBundle(
        context: Context,
        environment: Environment
    ): InAppStaticEntity? = downloadMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val data = inAppStaticRequest(
                    context
                ) ?: return@withContext null

                val bundle = InAppStaticEntity(
                    id = 0,
                    createdAt = time(),
                    blockJs = data.blockJs,
                    blockCss = data.blockCss,
                    inAppLibraryJs = data.inAppLibraryJs,
                )

                environment.room.dao.static.insert(bundle)
                static = bundle
                static
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error("downloadStaticBundle", e)
                null
            }
        }
    }

    /**
     * Injects static in-app resources into the provided [WebView].
     *
     * Builds and evaluates a JavaScript wrapper that adds cached CSS,
     * library JS, and block JS to the loaded in-app page.
     *
     * @param webView WebView where static resources should be injected.
     * @param staticBundle Static resources to inject.
     * @param onComplete Callback invoked after JavaScript evaluation is completed.
     */
    fun inject(
        webView: WebView,
        staticBundle: InAppModel.InAppStatic,
        onComplete: (() -> Unit)? = null
    ) {
        val libraryJs = staticBundle.inAppLibraryJs
        val blockJs = staticBundle.blockJs
        val blockCss = staticBundle.blockCss

        val script = buildString {
            append("(function() {")

            if (!blockCss.isNullOrBlank()) {
                append(createCssInjectionScript(blockCss))
                append("\n")
            }

            if (!libraryJs.isNullOrBlank()) {
                append(libraryJs)
                append("\n")
            }

            if (!blockJs.isNullOrBlank()) {
                append(blockJs)
                append("\n")
            }

            append("})();")
        }

        webView.evaluateJavascript(script) {
            onComplete?.invoke()
        }
    }

    /**
     * Creates JavaScript code that injects CSS into the document head.
     *
     * @param css Raw CSS content.
     * @return JavaScript snippet for CSS injection.
     */
    private fun createCssInjectionScript(css: String): String {
        return """
        var style = document.createElement('style');
        style.type = 'text/css';
        style.appendChild(document.createTextNode(${css.toJsString()}));
        document.head.appendChild(style);
    """.trimIndent()
    }

    /**
     * Escapes a string so it can be safely embedded into JavaScript code.
     *
     * @return Escaped JavaScript string literal.
     */
    private fun String.toJsString(): String {
        return "\"" + this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("</script>", "<\\/script>") + "\""
    }
}