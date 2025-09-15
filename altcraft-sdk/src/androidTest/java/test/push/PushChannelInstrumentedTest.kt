package test.push

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

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

// SDK imports
import com.altcraft.sdk.data.Constants.allSignal
import com.altcraft.sdk.data.Constants.onlySound
import com.altcraft.sdk.data.Constants.soundless
import com.altcraft.sdk.data.room.ConfigurationEntity
import com.altcraft.sdk.push.PushChannel
import com.altcraft.sdk.push.PushData

/**
 * PushChannelInstrumentedTest
 *
 * Positive scenarios:
 *  - test_1: getChannelInfo() — builds proper name/description for (a) null pushData (soundless),
 *            (b) vibration = true (allSignal), (c) vibration = false (onlySound), including custom config.
 *  - test_2: selectAndCreateChannel() — creates channel with correct importance & vibration for "allSignal".
 *  - test_3: selectAndCreateChannel() — creates channel with correct importance & no vibration for "onlySound".
 *  - test_4: selectAndCreateChannel() — creates channel with correct importance, no sound & no vibration for "soundless".
 *  - test_5: isChannelCreated() returns true when manager has the channel; false when not.
 *
 * Notes:
 *  - Instrumented Android tests; we mock NotificationManager and inject it via Context.getSystemService(...).
 *  - Channel-creation tests are suppressed below Android O (API 26), since notification channels are O+ only.
 *  - We verify vibration flags & importance; for “soundless” we also assert sound is null.
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

        // PushChannel.createChannel(context, channel) uses the string system service:
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns nm
        // PushChannel.isChannelCreated(context, info) uses the type overload:
        every { mockContext.getSystemService(NotificationManager::class.java) } returns nm
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------
    // getChannelInfo() tests
    // -------------------------

    /** Null pushData -> soundless; name/descr are base values if config is null. */
    @Test
    fun getChannelInfo_returns_soundless_when_pushData_is_null() {
        val config: ConfigurationEntity? = null

        val info = PushChannel.getChannelInfo(config, null)

        assertThat(info.first, `is`(soundless.first))
        assertThat(info.second, `is`(soundless.second))
    }

    /** pushData.vibration = true -> allSignal; custom config name/descr are appended/formatted. */
    @Test
    fun getChannelInfo_respects_custom_config_for_allSignal() {
        val cfg = ConfigurationEntity(
            id = 0,
            icon = null,
            apiUrl = "https://api.example.com",
            rToken = "rt",
            appInfo = null,
            usingService = false,
            serviceMessage = null,
            pushReceiverModules = null,
            providerPriorityList = null,
            pushChannelName = "MyChannel",
            pushChannelDescription = "Custom description"
        )

        // PushData requires Map<String, String>
        val pd = PushData(
            mapOf(
                "_soundless" to "false",
                "_vibration" to "true"
            )
        )

        val info = PushChannel.getChannelInfo(cfg, pd)

        // Expected: name = "<name>_<mode>", description = "<descr> (<descText>)."
        val expectedName = "${cfg.pushChannelName}_${allSignal.first}"
        val expectedDescr = "${cfg.pushChannelDescription} (${allSignal.second})."

        assertThat(info.first, `is`(expectedName))
        assertThat(info.second, `is`(expectedDescr))
    }

    /** pushData.vibration = false & not soundless -> onlySound */
    @Test
    fun getChannelInfo_onlySound_when_vibration_false_and_not_soundless() {
        val cfg: ConfigurationEntity? = null
        val pd = PushData(
            mapOf(
                "_soundless" to "false",
                "_vibration" to "false"
            )
        )

        val info = PushChannel.getChannelInfo(cfg, pd)

        assertThat(info.first, `is`(onlySound.first))
        assertThat(info.second, `is`(onlySound.second))
    }

    // -----------------------------------------
    // selectAndCreateChannel() — O+ only tests
    // -----------------------------------------

    /**
     * Creates a HIGH importance channel with vibration pattern when mode is allSignal.
     * Verifies: importance, vibration enabled, pattern set.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_allSignal_channel_with_vibration() {
        val info = "${allSignal.first}_id" to allSignal.second

        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        // id & name are both info.first by implementation
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_HIGH))
        // Vibration should be ON and pattern set
        assertThat(ch.shouldVibrate(), `is`(true))
        // pattern set in code: longArrayOf(0, 250, 250, 250)
        assertThat(ch.vibrationPattern!!.isNotEmpty(), `is`(true))
    }

    /**
     * Creates a DEFAULT importance channel with NO vibration when mode is onlySound.
     * Verifies: importance, vibration disabled.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_onlySound_channel_without_vibration() {
        val info = "${onlySound.first}_id" to onlySound.second

        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_DEFAULT))
        // Vibration disabled
        assertThat(ch.shouldVibrate(), `is`(false))
        assertThat(ch.vibrationPattern == null, `is`(true))
    }

    /**
     * Creates a LOW importance channel with NO sound and NO vibration when mode is soundless.
     * Verifies: importance, vibration disabled, and sound is null.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun selectAndCreateChannel_creates_soundless_channel_without_sound_and_vibration() {
        val info = "${soundless.first}_id" to soundless.second

        val slot = slot<NotificationChannel>()
        every { nm.createNotificationChannel(capture(slot)) } just Runs

        PushChannel.selectAndCreateChannel(mockContext, info)

        val ch = slot.captured
        assertThat(ch.id, `is`(info.first))
        assertThat(ch.name.toString(), `is`(info.first))
        assertThat(ch.importance, `is`(NotificationManager.IMPORTANCE_LOW))
        // Vibration disabled
        assertThat(ch.shouldVibrate(), `is`(false))
        assertThat(ch.vibrationPattern == null, `is`(true))
        // Sound explicitly set to null in implementation for soundless mode
        assertThat(ch.sound == null, `is`(true))
    }

    // -------------------------
    // isChannelCreated() tests
    // -------------------------

    /** Returns true when manager has a channel with given id. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun isChannelCreated_returns_true_if_manager_has_channel() {
        val id = "any_id_${allSignal.first}"
        val info = id to "desc"
        val existing = NotificationChannel(id, id, NotificationManager.IMPORTANCE_DEFAULT)

        every { nm.getNotificationChannel(id) } returns existing

        val ok = PushChannel.isChannelCreated(mockContext, info)
        assertThat(ok, `is`(true))
    }

    /** Returns false when manager does not have a channel with given id. */
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
