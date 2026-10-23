package com.altcraft.altcraftmobile.providers.rustore
import android.util.Log
import com.altcraft.sdk.push.platform.token.provider.RustoreInterface
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.rustore.sdk.universalpush.RuStoreUniversalPushClient
import ru.rustore.sdk.universalpush.UNIVERSAL_RUSTORE_PROVIDER
import kotlin.coroutines.resume

/**
 * RuStore push provider implementation.
 *
 * Retrieves and deletes RuStore push tokens.
 */
class RuStoreProvider : RustoreInterface {

    private companion object {
        const val TAG = "rustore"
    }

    /**
     * Retrieves the current RuStore push token.
     *
     * @return Token or null if unavailable.
     */
    override suspend fun getToken(): String? {
        Log.d(TAG, "RuStoreProvider.getToken: START")

        return suspendCancellableCoroutine { continuation ->
            try {
                RuStoreUniversalPushClient.getTokens()
                    .addOnSuccessListener { tokens ->
                        Log.d(
                            TAG,
                            "RuStoreProvider.getToken: SUCCESS, " +
                                    "availableProviders=${tokens.keys}"
                        )

                        val token = tokens[UNIVERSAL_RUSTORE_PROVIDER]

                        Log.d(
                            TAG,
                            "RuStoreProvider.getToken: " +
                                    "provider=$UNIVERSAL_RUSTORE_PROVIDER, " +
                                    "tokenNull=${token == null}, " +
                                    "tokenEmpty=${token?.isEmpty()}, " +
                                    "tokenLength=${token?.length ?: 0}"
                        )

                        if (continuation.isActive) {
                            continuation.resume(token)
                        } else {
                            Log.d(
                                TAG,
                                "RuStoreProvider.getToken: " +
                                        "continuation inactive"
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            TAG,
                            "RuStoreProvider.getToken: FAILED",
                            exception
                        )

                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "RuStoreProvider.getToken: EXCEPTION",
                    e
                )

                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Deletes the current RuStore push token.
     *
     * @param complete Callback with deletion result.
     */
    override fun deleteToken(
        complete: (Boolean) -> Unit
    ) {
        Log.d(TAG, "RuStoreProvider.deleteToken: START")

        try {
            RuStoreUniversalPushClient.getTokens()
                .addOnSuccessListener { tokens ->
                    Log.d(
                        TAG,
                        "RuStoreProvider.deleteToken: " +
                                "availableProviders=${tokens.keys}"
                    )

                    val token = tokens[UNIVERSAL_RUSTORE_PROVIDER]

                    if (token == null) {
                        Log.d(
                            TAG,
                            "RuStoreProvider.deleteToken: " +
                                    "token not found, RESULT=false"
                        )

                        complete(false)
                        return@addOnSuccessListener
                    }

                    Log.d(
                        TAG,
                        "RuStoreProvider.deleteToken: " +
                                "token found, length=${token.length}"
                    )

                    RuStoreUniversalPushClient.deleteTokens(
                        mapOf(UNIVERSAL_RUSTORE_PROVIDER to token)
                    )
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "RuStoreProvider.deleteToken: SUCCESS"
                            )

                            complete(true)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                TAG,
                                "RuStoreProvider.deleteToken: FAILED",
                                exception
                            )

                            complete(false)
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e(
                        TAG,
                        "RuStoreProvider.deleteToken: " +
                                "getTokens FAILED",
                        exception
                    )

                    complete(false)
                }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "RuStoreProvider.deleteToken: EXCEPTION",
                e
            )

            complete(false)
        }
    }
}
