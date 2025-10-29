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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NotificationExtensionInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: addActions attaches actions to builder.
 *  - test_2: applyBigPictureStyle applies style if image available.
 *
 * Negative scenarios:
 *  - test_3: applyBigPictureStyle ignores null image.
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
        every { getIntent(any(), any(), any(), any()) } returns pendingIntent
    }

    /** test_1: addActions attaches buttons */
    @Test
    fun test_1_addActions_addsButtons() {
        val buttons = listOf(
            DataClasses.ButtonStructure("label1", "link1"),
            DataClasses.ButtonStructure("label2", "link2")
        )

        builder.addActions(ctx, 1, buttons, "uid1")
        val notification = builder.build()

        assertEquals(2, notification.actions.size)
        assertEquals("label1", notification.actions[0].title)
        assertEquals("label2", notification.actions[1].title)
        assertNotNull(notification.actions[0].actionIntent)
    }

    /** test_2: applyBigPictureStyle with image */
    @Test
    fun test_2_applyBigPictureStyle_withImage() {
        val bmp = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        val data = DataClasses.NotificationData(
            uid = "u",
            title = "t",
            body = "b",
            icon = 0,
            messageId = 1,
            channelInfo = "ch" to "d",
            smallImg = null,
            largeImage = bmp,
            color = 0,
            pendingIntent = pendingIntent,
            buttons = null
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
            messageId = 1,
            channelInfo = "ch" to "d",
            smallImg = null,
            largeImage = null,
            color = 0,
            pendingIntent = pendingIntent,
            buttons = null
        )

        builder.applyBigPictureStyle(data)
        val notification = builder.build()
        assertNotNull(notification)
    }
}
