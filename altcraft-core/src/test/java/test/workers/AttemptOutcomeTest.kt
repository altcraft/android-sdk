@file:Suppress("SpellCheckingInspection")

package test.workers

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.work.ListenableWorker
import com.altcraft.sdk.data.Constants.MAX_RETRY_COUNT
import com.altcraft.sdk.workers.onetime.Request.attemptOutcome
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AttemptOutcomeTest
 *
 * Verifies the bounded retry budget of one-time workers.
 *
 * Positive scenarios:
 *  - test_1: retries while the budget is not exhausted.
 *  - test_2: stops immediately when shouldStop holds.
 *
 * Negative scenarios:
 *  - test_3: stops once the retry budget is exhausted.
 */
class AttemptOutcomeTest {

    /** test_1: retries while the budget is not exhausted. */
    @Test
    fun retries_withinBudget() {
        val first = attemptOutcome(0) { false }

        assertTrue(first is ListenableWorker.Result.Retry)

        val lastAllowed = attemptOutcome(MAX_RETRY_COUNT) { false }

        assertTrue(lastAllowed is ListenableWorker.Result.Retry)
    }

    /** test_2: stops immediately when shouldStop holds. */
    @Test
    fun stops_whenShouldStop() {
        val result = attemptOutcome(0) { true }

        assertTrue(result is ListenableWorker.Result.Success)
    }

    /** test_3: stops once the retry budget is exhausted. */
    @Test
    fun stops_whenBudgetExhausted() {
        val result = attemptOutcome(MAX_RETRY_COUNT + 1) { false }

        assertTrue(result is ListenableWorker.Result.Success)
    }
}
