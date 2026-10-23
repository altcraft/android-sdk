package com.altcraft.sdk.coordination

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.
//

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Executes suspend operations with up to five concurrent tasks
 * and returns their results wrapped in [Result].
 *
 * @param block Suspend operation applied to each element.
 * @return Results in the same order as the input elements.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun <T> Iterable<T>.mapSuspendCatching(
    block: suspend (T) -> Unit
): List<Result<Unit>> = coroutineScope {
    val semaphore = Semaphore(5)

    map { item ->
        async {
            semaphore.withPermit {
                try {
                    Result.success(block(item))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }.awaitAll()
}