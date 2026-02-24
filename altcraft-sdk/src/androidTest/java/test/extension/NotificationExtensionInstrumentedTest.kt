package test.extension

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.PendingIntent
import android.content.Context
import android.content.Intent as AndroidIntent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.DataClasses
import com.altcraft.sdk.extension.NotificationExtension.addActions
import com.altcraft.sdk.extension.NotificationExtension.applyBigPictureStyle
import com.altcraft.sdk.push.action.Intent.getIntent
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NotificationExtensionInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: addActions attaches actions to builder and creates PendingIntents per button.
 *  - test_2: applyBigPictureStyle applies style if image is available (no crash).
 *
 * Negative scenarios:
 *  - test_3: applyBigPictureStyle ignores null image (no crash).
 *
 * Notes:
 *  - Runs on Android (instrumented).
 *  - Intent.getIntent is mocked to return a non-null PendingIntent.
 */
@RunWith(AndroidJUnit4::class)
class NotificationExtensionInstrumentedTest {

    private lateinit var ctx: Context
    private lateinit var builder: NotificationCompat.Builder
    private lateinit var pendingIntent: PendingIntent

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        builder = NotificationCompat.Builder(ctx, "test-channel")

        val intent = AndroidIntent(ctx, javaClass)
        pendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mockkObject(com.altcraft.sdk.push.action.Intent)

        every { getIntent(any(), any(), any(), any(), any()) } returns pendingIntent
    }

    /** test_1: addActions attaches actions for each button and wires PendingIntents */
    @Test
    fun test_1_addActions_addsButtons_andWiresPendingIntents() {
        val buttons = listOf(
            DataClasses.ButtonStructure(label = "label1", link = "link1"),
            DataClasses.ButtonStructure(label = "label2", link = "link2")
        )

        val data = DataClasses.NotificationData(
            uid = "uid1",
            title = "title",
            body = "body",
            icon = 0,
            smallImg = null,
            largeImage = null,
            color = 0,
            url = "main-url",
            extra = "extra-1",
            messageId = 777,
            buttons = buttons,
            channelInfo = "ch" to "d"
        )

        builder.addActions(ctx, data)
        val notification = builder.build()

        assertEquals(2, notification.actions.size)
        assertEquals("label1", notification.actions[0].title)
        assertEquals("label2", notification.actions[1].title)
        assertNotNull(notification.actions[0].actionIntent)
        assertNotNull(notification.actions[1].actionIntent)

        verify(exactly = 1) { getIntent(ctx, 777, "link1", "uid1", "extra-1") }
        verify(exactly = 1) { getIntent(ctx, 777, "link2", "uid1", "extra-1") }
    }

    /** test_2: applyBigPictureStyle with image → no crash */
    @Test
    fun test_2_applyBigPictureStyle_withImage() {
        val bmp = android.graphics.Bitmap.createBitmap(
            10,
            10,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        val data = DataClasses.NotificationData(
            uid = "u",
            title = "t",
            body = "b",
            icon = 0,
            smallImg = null,
            largeImage = bmp,
            color = 0,
            url = "url",
            extra = "",
            messageId = 1,
            buttons = null,
            channelInfo = "ch" to "d"
        )

        builder.applyBigPictureStyle(data)
        val notification = builder.build()
        assertNotNull(notification)
    }

    /** test_3: applyBigPictureStyle with null image → no crash */
    @Test
    fun test_3_applyBigPictureStyle_withNullImage() {
        val data = DataClasses.NotificationData(
            uid = "u",
            title = "t",
            body = "b",
            icon = 0,
            smallImg = null,
            largeImage = null,
            color = 0,
            url = "url",
            extra = "",
            messageId = 1,
            buttons = null,
            channelInfo = "ch" to "d"
        )

        builder.applyBigPictureStyle(data)
        val notification = builder.build()
        assertNotNull(notification)
    }
}
