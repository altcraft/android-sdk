package com.altcraft.sdk.push.presenter

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import io.mockk.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import com.altcraft.sdk.push.data.Constants.allSignal
import com.altcraft.sdk.push.data.Constants.onlySound
import com.altcraft.sdk.push.data.Constants.soundless
import com.altcraft.sdk.push.data.room.entity.PushConfigEntity
import com.altcraft.sdk.push.system.notification.PushChannel
import com.altcraft.sdk.push.system.receiver.PushData

/**
 * PushChannelInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: getChannelInfo() builds proper name/description for
 *    (a) null pushData → soundless, (b) vibration=true → allSignal,
 *    (c) vibration=false → onlySound; respects custom config.
 *  - test_2: selectAndCreateChannel() creates channel with correct
 *    importance & vibration for allSignal.
 *  - test_3: selectAndCreateChannel() creates channel with correct
 *    importance & no vibration for onlySound.
 *  - test_4: selectAndCreateChannel() creates channel with correct
 *    importance, no sound & no vibration for soundless.
 *  - test_5: isChannelCreated() returns true when manager has the
 *    channel; false when not.
 *
 * Notes:
 *  - NotificationManager is mocked via Context.getSystemService().
 *  - Channel-creation tests are suppressed below Android O (API 26).
 */
@RunWith(AndroidJUnit4::class)
class PushChannelInstrumentedTest {

    private lateinit var appContext: Context
    private lateinit var mockContext: Context
    private lateinit var nm: NotificationManager

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        mockContext = mockk(relaxed = true)
        nm = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns nm
        every { mockContext.getSystemService(NotificationManager::class.java) } returns nm
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** - test_1: getChannelInfo() returns soundless when pushData is null. */
    @Test
    fun getChannelInfo_returns_soundless_when_pushData_is_null() {
        val config: PushConfigEntity? = null
        val info = PushChannel.getChannelInfo(config, null)
        assertThat(info.first, `is`("_${soundless.first}"))
        assertThat(info.second, `is`("_${soundless.second}"))
    }

    /** - test_2: getChannelInfo() respects custom config for allSignal. */
    @Test
    fun getChannelInfo_respects_custom_config_for_allSignal() {
        val cfg = PushConfigEntity(
            id = 0,
            icon = null,
            providerPriority = emptyList(),
            receiverPackages = emptyList(),
            pushChannelName = "MyChannel",
            pushChannelDescription = "Custom description"
        )
        val pd = PushData(mapOf("_soundless" to "false", "_vibration" to "true"))

        val info = PushChannel.getChannelInfo(cfg, pd)

        val expectedName = "MyChannel_${allSignal.first}"
        val expectedDescr = "Custom description_${allSignal.second}"
        assertThat(info.first, `is`(expectedName))
        assertThat(info.second, `is`(expectedDescr))
    }

    /** - test_3: getChannelInfo() returns onlySound when vibration=false and not soundless. */
    @Test
    fun getChannelInfo_onlySound_when_vibration_false_and_not_soundless() {
        val cfg: PushConfigEntity? = null
        val pd = PushData(mapOf("_soundless" to "false", "_vibration" to "false"))

        val info = PushChannel.getChannelInfo(cfg, pd)

        assertThat(info.first, `is`("_${onlySound.first}"))
        assertThat(info.second, `is`("_${onlySound.second}"))
    }

    /** - test_4: creates allSignal channel (HIGH, vibration ON, pattern set). */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_allSignal_channel_with_vibration() {
        val info = "id_${allSignal.first}" to allSignal.second
        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_HIGH))
        assertThat(ch.shouldVibrate(), `is`(true))
        assertThat(ch.vibrationPattern!!.isNotEmpty(), `is`(true))
    }

    /** - test_5: creates onlySound channel (DEFAULT, vibration OFF). */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_onlySound_channel_without_vibration() {
        val info = "id_${onlySound.first}" to onlySound.second
        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_DEFAULT))
        assertThat(ch.shouldVibrate(), `is`(false))
        assertThat(ch.vibrationPattern == null, `is`(true))
    }

    /** - test_6: creates soundless channel (LOW, no sound, vibration OFF). */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_soundless_channel_without_sound_and_vibration() {
        val info = "id_${soundless.first}" to soundless.second
        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_LOW))
        assertThat(ch.shouldVibrate(), `is`(false))
        assertThat(ch.vibrationPattern == null, `is`(true))
        assertThat(ch.sound == null, `is`(true))
    }

    /** - test_7: isChannelCreated() returns true when manager has the channel. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun isChannelCreated_returns_true_if_manager_has_channel() {
        val id = "channel_${allSignal.first}"
        val info = id to "desc"
        val existing = NotificationChannel(id, id, NotificationManager.IMPORTANCE_DEFAULT)
        every { nm.getNotificationChannel(id) } returns existing

        val ok = PushChannel.isChannelCreated(mockContext, info)
        assertThat(ok, `is`(true))
    }

    /** - test_8: isChannelCreated() returns false when manager misses the channel. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun isChannelCreated_returns_false_if_manager_missing_channel() {
        val id = "missing_${soundless.first}"
        val info = id to "desc"
        every { nm.getNotificationChannel(id) } returns null

        val ok = PushChannel.isChannelCreated(mockContext, info)
        assertThat(ok, `is`(false))
    }
}
