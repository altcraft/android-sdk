@file:Suppress("SpellCheckingInspection")

package com.altcraft.sdk.push.presenter

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.R
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.push.system.notification.NotificationBuilder
import com.altcraft.sdk.push.data.models.PushModels
import com.altcraft.sdk.push.events.ModuleEvents
import com.altcraft.sdk.push.system.notification.PushChannel
import com.altcraft.sdk.push.system.notification.PushPresenter
import com.altcraft.sdk.push.util.Utils
import com.altcraft.sdk.push.system.action.Intent as AltcraftIntent
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PushPresenterTest
 *
 * Positive scenarios:
 * - test_4: showPush() — permission granted and channel ready → publishes notification and emits SDK event.
 *
 * Negative scenarios:
 * - test_1: showPush() — permission denied → no notify, error logged.
 * - test_2: showPush() — getNotificationData() == null → no notify, error logged.
 * - test_3: showPush() — channel not created → tries to create channel, no notify, error logged.
 *
 */
@RunWith(AndroidJUnit4::class)
class PushPresenterInstrumentedTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.println(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0

        mockkStatic(NotificationManagerCompat::class)

        mockkObject(Utils)
        mockkObject(NotificationBuilder)
        mockkObject(PushChannel)
        mockkObject(Events)

        mockkObject(AltcraftIntent)
        every { AltcraftIntent.getIntent(any(), any(), any(), any(), any()) } returns
                android.app.PendingIntent.getActivity(
                    ctx,
                    0,
                    Intent(Intent.ACTION_VIEW),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun sampleNotificationData(
        messageId: Int = 42,
        channelId: String = "test_channel",
        channelDescr: String = "Test channel",
        title: String = "Title",
        body: String = "Body",
        color: Int = Color.BLACK,
        url: String = "https://example.com",
        extra: String = "extra"
    ): PushModels.NotificationData {
        return PushModels.NotificationData(
            uid = "uid-1",
            title = title,
            body = body,
            icon = R.drawable.stat_notify_chat,
            smallImg = null,
            largeImage = null,
            color = color,
            url = url,
            extra = extra,
            messageId = messageId,
            buttons = emptyList(),
            channelInfo = channelId to channelDescr
        )
    }

    /** - test_1: showPush() permission denied → no notify, error logged. */
    @Test
    fun test_1_showPush_permissionDenied_noNotify() = runBlocking {
        every { Utils.hasNotificationPermission(ctx) } returns false
        every { Events.error(any(), any()) } returns SDKEventModel.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("[altcraft-push] showPush"), any()) }
    }

    /** - test_2: showPush() returns no notify and logs error when getNotificationData() == null. */
    @Test
    fun test_2_showPush_dataNull_noNotify() = runBlocking {
        every { Utils.hasNotificationPermission(ctx) } returns true
        coEvery { NotificationBuilder.getNotificationData(ctx, any()) } returns null
        every { Events.error(any(), any()) } returns SDKEventModel.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("[altcraft-push] showPush"), any()) }
    }

    /** - test_3: showPush() attempts channel create but no notify when channel not created; logs error. */
    @Test
    fun test_3_showPush_channelNotCreated_noNotify() = runBlocking {
        every { Utils.hasNotificationPermission(ctx) } returns true
        val data = sampleNotificationData()
        coEvery { NotificationBuilder.getNotificationData(ctx, any()) } returns data

        every { PushChannel.channelsSupportedForCurrentVersion } returns true
        every { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) } just Runs
        every { PushChannel.isChannelCreated(ctx, data.channelInfo) } returns false

        every { Events.error(any(), any()) } returns SDKEventModel.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 1) { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) }
        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("[altcraft-push] showPush"), any()) }
    }

    /** - test_4: showPush() notifies and emits SDK event when permission granted and channel ready. */
    @Test
    fun test_4_showPush_success_notifies_and_emitsEvent() = runBlocking {
        every { Utils.hasNotificationPermission(ctx) } returns true
        val data =
            sampleNotificationData(messageId = 777, url = "https://example.org/x", extra = "e777")
        coEvery { NotificationBuilder.getNotificationData(ctx, any()) } returns data

        every { PushChannel.channelsSupportedForCurrentVersion } returns true
        every { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) } just Runs
        every { PushChannel.isChannelCreated(ctx, data.channelInfo) } returns true

        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        every { Events.event(any(), any(), any()) } returns SDKEventModel.Event("showPush")

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 1) { nm.notify(eq(777), any()) }

        verify(exactly = 1) {
            Events.event(
                eq("[altcraft-push] showPush"),
                eq(ModuleEvents.pushIsPosted),
                match { value ->
                    (value as? Map<*, *>)?.containsKey("push") == true
                }
            )
        }

        verify(exactly = 1) {
            AltcraftIntent.getIntent(ctx, 777, data.url, data.uid, data.extra)
        }
    }
}
