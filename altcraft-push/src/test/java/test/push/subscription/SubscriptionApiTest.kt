@file:Suppress("SpellCheckingInspection")

package test.push.subscription

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.CategoryData
import com.altcraft.sdk.push.data.Constants
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionAPI
import com.altcraft.sdk.push.platform.subscription.subscribe.SubscriptionProcess
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * SubscriptionApiTest
 *
 * Verifies that the public subscription facade forwards
 * subscribe/suspend/unsubscribe requests to [SubscriptionProcess].
 */
class SubscriptionApiTest {

    private val context: Context = mockk(relaxed = true)
    private val cats = listOf(
        CategoryData(name = "news", active = true)
    )

    @Before
    fun setUp() {
        mockkObject(SubscriptionProcess)
        every { SubscriptionProcess.pushSubscribe(any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** subscribe forwards the SUBSCRIBED status with sync=true. */
    @Test
    fun subscribe_delegatesToPushSubscribe() {
        SubscriptionAPI.subscribe(
            context = context,
            sync = true,
            profileFields = mapOf("a" to 1),
            customFields = mapOf("c" to "x"),
            cats = cats,
            replace = true,
            skipTriggers = false
        )

        verify(exactly = 1) {
            SubscriptionProcess.pushSubscribe(
                context,
                status = Constants.SUBSCRIBED,
                sync = true,
                profileFields = mapOf("a" to 1),
                customFields = mapOf("c" to "x"),
                cats = cats,
                replace = true,
                skipTriggers = false
            )
        }
    }

    /** suspend forwards the SUSPENDED status regardless of sync mapping. */
    @Test
    fun suspend_delegatesToPushSubscribe() {
        SubscriptionAPI.suspend(context = context, sync = true)

        verify(exactly = 1) {
            SubscriptionProcess.pushSubscribe(
                context,
                status = Constants.SUSPENDED,
                sync = true,
                profileFields = null,
                customFields = null,
                cats = null,
                replace = null,
                skipTriggers = null
            )
        }
    }

    /** unsubscribe forwards the UNSUBSCRIBED status with sync=false. */
    @Test
    fun unsubscribe_delegatesToPushSubscribe() {
        SubscriptionAPI.unsubscribe(context = context, sync = false)

        verify(exactly = 1) {
            SubscriptionProcess.pushSubscribe(
                context,
                status = Constants.UNSUBSCRIBED,
                sync = false,
                profileFields = null,
                customFields = null,
                cats = null,
                replace = null,
                skipTriggers = null
            )
        }
    }
}
