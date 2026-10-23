package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration as WMConfiguration
import androidx.work.testing.WorkManagerTestInitHelper
import com.altcraft.sdk.util.SubFunction
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.system.presentation.DelayTask
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import com.altcraft.sdk.inapp.system.presentation.Presenter
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerEvent
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * DelayTaskInstrumentedTest
 *
 * Tests the delayTask() function which requires Context and Presenter.
 *
 * Positive scenarios:
 * - test_1: delayTask with zero delay presents immediately.
 * - test_2: delayTask with positive delay does not throw.
 * - test_3: getTriggerDelay returns correct delay from matching event.
 * - test_4: getTriggerDelay returns 0 when no matching event.
 * - test_5: delayTask resets matched trigger delay before present.
 *
 * Negative scenarios:
 * - test_6: delayTask with Presenter exception does not throw caller.
 * - test_7: delayTask does not increment display counters before present.
 */
@RunWith(AndroidJUnit4::class)
class DelayTaskInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val wmConfig = WMConfiguration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            wmConfig
        )

        MockKAnnotations.init(
            this,
            relaxUnitFun = true
        )

        mockkObject(SubFunction)
        every { SubFunction.isOnline(any()) } returns true
        every { SubFunction.isAppInForegrounded() } returns false
        every { SubFunction.time() } returns System.currentTimeMillis()

        mockkObject(Events)
        every { Events.error(any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.event(any(), any()) } answers {
            SDKEventModel.Event(function = firstArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.event(any(), any(), any()) } answers {
            SDKEventModel.Event(function = firstArg())
        }

        mockkObject(Presenter)

        mockkObject(InAppVariables)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: delayTask with zero delay presents immediately. */
    @Test
    fun test_1_delayTask_zero_delay_presents_immediately() = runBlocking {
        val trigger = makeTrigger(
            name = "open_page",
            type = TriggerType.DEFAULT
        )

        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 0
                )
            ),
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        DelayTask.delayTask(
            context = context,
            inApp = inApp,
            trigger = trigger,
            delay = 0
        )

        delay(500)

        coVerify {
            Presenter.present(
                context,
                any(),
                delayTask = true,
                trigger = trigger
            )
        }
    }

    /** test_2: delayTask with positive delay does not throw. */
    @Test
    fun test_2_delayTask_positive_delay_does_not_throw() = runBlocking {
        val trigger = makeTrigger(
            name = "open_page",
            type = TriggerType.DEFAULT
        )

        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 1
                )
            ),
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        try {
            DelayTask.delayTask(
                context = context,
                inApp = inApp,
                trigger = trigger,
                delay = 1
            )

            delay(1500)
        } catch (e: Exception) {
            fail("delayTask should not throw: ${e.message}")
        }
    }

    /** test_3: getTriggerDelay returns correct delay from matching event. */
    @Test
    fun test_3_getTriggerDelay_matching_event_returns_delay() {
        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 5
                ),
                TriggerEvent(
                    type = "custom",
                    name = "btn",
                    delay = 10
                )
            ),
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        assertThat(
            DelayTask.getTriggerDelay(inApp),
            `is`(5)
        )
    }

    /** test_4: getTriggerDelay returns 0 when no matching event. */
    @Test
    fun test_4_getTriggerDelay_no_match_returns_zero() {
        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 5
                )
            ),
            actionTriggerName = "unknown",
            actionTriggerType = TriggerType.CUSTOM
        )

        assertThat(
            DelayTask.getTriggerDelay(inApp),
            `is`(0)
        )
    }

    /** test_5: delayTask resets matched trigger delay before present. */
    @Test
    fun test_5_delayTask_resets_matched_trigger_delay_before_present() = runBlocking {
        val trigger = makeTrigger(
            name = "open_page",
            type = TriggerType.DEFAULT
        )

        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 10
                ),
                TriggerEvent(
                    type = "custom",
                    name = "button_click",
                    delay = 20
                )
            ),
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        DelayTask.delayTask(
            context = context,
            inApp = inApp,
            trigger = trigger,
            delay = 0
        )

        delay(500)

        coVerify {
            Presenter.present(
                context,
                match { presentedInApp ->
                    val matchedDelay = presentedInApp.options.triggerEvents
                        .first { it.name == "open_page" }
                        .delay

                    val unmatchedDelay = presentedInApp.options.triggerEvents
                        .first { it.name == "button_click" }
                        .delay

                    matchedDelay == 0 && unmatchedDelay == 20
                },
                delayTask = true,
                trigger = trigger
            )
        }
    }

    /** test_6: delayTask with Presenter exception does not throw caller. */
    @Test
    fun test_6_delayTask_presenter_exception_does_not_throw_caller() {
        coEvery {
            Presenter.present(
                any(),
                any(),
                delayTask = any(),
                trigger = any()
            )
        } throws RuntimeException("present error")

        val trigger = makeTrigger(
            name = "open_page",
            type = TriggerType.DEFAULT
        )

        val inApp = makeInApp(
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        try {
            DelayTask.delayTask(
                context = context,
                inApp = inApp,
                trigger = trigger,
                delay = 0
            )

            Thread.sleep(500)
        } catch (e: Exception) {
            fail("delayTask should not throw caller: ${e.message}")
        }
    }

    /** test_7: delayTask does not increment display counters before present. */
    @Test
    fun test_7_delayTask_does_not_increment_counters_before_present() = runBlocking {
        val trigger = makeTrigger(
            name = "open_page",
            type = TriggerType.DEFAULT
        )

        val inApp = makeInApp(
            triggerEvents = listOf(
                TriggerEvent(
                    type = "default",
                    name = "open_page",
                    delay = 1
                )
            ),
            actionTriggerName = "open_page",
            actionTriggerType = TriggerType.DEFAULT
        )

        DelayTask.delayTask(
            context = context,
            inApp = inApp,
            trigger = trigger,
            delay = 1
        )

        delay(1500)

        coVerify(exactly = 0) { InAppVariables.openBySessionInc() }
        coVerify(exactly = 0) { InAppVariables.triggerBySessionInc(any()) }
    }

    private fun makeTrigger(
        name: String,
        type: TriggerType
    ): InAppModel.InAppTrigger {
        return InAppModel.InAppTrigger(
            name = name,
            type = type
        )
    }

    private fun makeInApp(
        triggerEvents: List<TriggerEvent> = emptyList(),
        actionTriggerName: String = "open_page",
        actionTriggerType: TriggerType = TriggerType.DEFAULT
    ): InApp {
        return InApp(
            companyId = 1,
            smid = "s1",
            type = InAppContentType.CUSTOM,
            contentUrl = "",
            checksum = "",
            options = PlacementOptions(
                endTime = 0,
                triggerEvents = triggerEvents
            ),
            deadline = 0,
            lastCompanyUpdate = 0,
            actionTriggerType = actionTriggerType,
            actionTriggerName = actionTriggerName
        )
    }
}
