@file:Suppress("SpellCheckingInspection")

package test.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.app.NotificationManagerCompat
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.data.Collector
import com.altcraft.sdk.sdk_events.EventList.pushIsPosted
import com.altcraft.sdk.sdk_events.Events
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.PushPresenter
import com.altcraft.sdk.additional.SubFunction
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
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
 * Utility:
 * - test_5: createNotification(context, channelId, body, icon) returns a Notification instance.
 */
@RunWith(AndroidJUnit4::class)
class PushPresenterTest {

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
        mockkObject(SubFunction)
        mockkObject(Collector)
        mockkObject(PushChannel)
        mockkObject(Events)
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
        color: Int = Color.BLACK
    ): DataClasses.NotificationData {
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(Intent.ACTION_VIEW), PendingIntent.FLAG_IMMUTABLE
        )
        return DataClasses.NotificationData(
            uid = "uid-1",
            title = title,
            body = body,
            icon = android.R.drawable.stat_notify_chat,
            messageId = messageId,
            channelInfo = channelId to channelDescr,
            smallImg = null,
            largeImage = null,
            color = color,
            pendingIntent = pi,
            buttons = emptyList()
        )
    }

    /** - test_1: showPush() permission denied → no notify, error logged. */
    @Test
    fun test_1_showPush_permissionDenied_noNotify() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns false
        every { Events.error(any(), any()) } returns DataClasses.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("showPush"), any()) }
    }

    /** - test_2: showPush() returns no notify and logs error when getNotificationData() == null. */
    @Test
    fun test_2_showPush_dataNull_noNotify() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns true
        coEvery { Collector.getNotificationData(ctx, any()) } returns null
        every { Events.error(any(), any()) } returns DataClasses.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("showPush"), any()) }
    }

    /** - test_3: showPush() attempts channel create but no notify when channel not created; logs error. */
    @Test
    fun test_3_showPush_channelNotCreated_noNotify() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns true
        val data = sampleNotificationData()
        coEvery { Collector.getNotificationData(ctx, any()) } returns data
        every { PushChannel.versionsSupportChannels } returns true
        every { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) } just Runs
        every { PushChannel.isChannelCreated(ctx, data.channelInfo) } returns false
        every { Events.error(any(), any()) } returns DataClasses.Error("showPush")
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 1) { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) }
        verify(exactly = 0) { nm.notify(any(), any()) }
        verify(atLeast = 1) { Events.error(eq("showPush"), any()) }
    }

    /** - test_4: showPush() notifies and emits SDK event when permission granted and channel ready. */
    @Test
    fun test_4_showPush_success_notifies_and_emitsEvent() = runBlocking {
        every { SubFunction.checkingNotificationPermission(ctx) } returns true
        val data = sampleNotificationData(messageId = 777)
        coEvery { Collector.getNotificationData(ctx, any()) } returns data
        every { PushChannel.versionsSupportChannels } returns true
        every { PushChannel.selectAndCreateChannel(ctx, data.channelInfo) } just Runs
        every { PushChannel.isChannelCreated(ctx, data.channelInfo) } returns true
        val nm = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(ctx) } returns nm
        every { Events.event(any(), any(), any()) } returns DataClasses.Event("showPush")

        PushPresenter.showPush(ctx, mapOf("k" to "v"))

        verify(exactly = 1) { nm.notify(eq(777), any()) }
        verify(exactly = 1) { Events.event(eq("showPush"), eq(pushIsPosted), any()) }
    }

    /** - test_5: createNotification(...) returns non-null Notification instance. */
    @Test
    fun test_5_createNotification_foregroundHelper_notNull() {
        val n = PushPresenter.createNotification(
            context = ctx,
            channelId = "fg_channel",
            body = "Loading…",
            icon = android.R.drawable.stat_notify_chat
        )
        Assert.assertNotNull("Expected non-null Notification", n)
    }
}