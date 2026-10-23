@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerEvent
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.system.presentation.DelayTask
import com.altcraft.sdk.inapp.system.presentation.PresentationGate
import com.altcraft.sdk.inapp.system.presentation.Presenter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DelayTaskTest
 *
 * Positive scenarios:
 *  - test_1: getTriggerDelay returns delay from matching trigger event.
 *  - test_2: getTriggerDelay returns 0 when no trigger events match.
 *  - test_4: delayTask presents the campaign when the epoch is unchanged.
 *  - test_6: invalidate increments the presentation epoch.
 *
 * Negative scenarios:
 *  - test_3: getTriggerDelay returns 0 when trigger events list is empty.
 *  - test_5: delayTask discards the campaign when the epoch was invalidated during the delay.
 */
class DelayTaskTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** test_1: getTriggerDelay returns delay from matching trigger. */
    @Test
    fun test_1_getTriggerDelay_matchingTrigger_returnsDelay() {
        val inApp = inAppWithTrigger(
            triggerName = "open_page",
            triggerType = TriggerType.DEFAULT,
            events = listOf(
                TriggerEvent(type = "default", name = "open_page", delay = 5),
                TriggerEvent(type = "custom", name = "other", delay = 10)
            )
        )
        assertEquals(5, DelayTask.getTriggerDelay(inApp))
    }

    /** test_2: getTriggerDelay returns 0 when no trigger events match. */
    @Test
    fun test_2_getTriggerDelay_noMatchingTrigger_returnsZero() {
        val inApp = inAppWithTrigger(
            triggerName = "unknown_event",
            triggerType = TriggerType.DEFAULT,
            events = listOf(
                TriggerEvent(type = "default", name = "open_page", delay = 5)
            )
        )
        assertEquals(0, DelayTask.getTriggerDelay(inApp))
    }

    /** test_3: getTriggerDelay returns 0 with an empty trigger events list. */
    @Test
    fun test_3_getTriggerDelay_emptyEvents_returnsZero() {
        val inApp = inAppWithTrigger(
            triggerName = "open_page",
            triggerType = TriggerType.DEFAULT,
            events = emptyList()
        )
        assertEquals(0, DelayTask.getTriggerDelay(inApp))
    }

    /** test_4: delayTask presents the campaign when the epoch is unchanged. */
    @Test
    fun test_4_delayTask_epochUnchanged_presents() {
        mockkObject(Presenter)
        coEvery { Presenter.present(any(), any(), any(), any()) } returns Unit

        DelayTask.delayTask(
            mockk<Context>(),
            inAppWithTrigger(
                triggerName = "open_page",
                triggerType = TriggerType.DEFAULT,
                events = listOf(
                    TriggerEvent(type = "default", name = "open_page", delay = 1)
                )
            ),
            InAppTrigger(name = "open_page", type = TriggerType.DEFAULT),
            1
        )

        Thread.sleep(1500)

        coVerify(exactly = 1) {
            Presenter.present(any(), any(), any(), any())
        }
    }

    /** test_5: delayTask discards the campaign when the epoch was invalidated during the delay. */
    @Test
    fun test_5_delayTask_epochInvalidated_skipsPresentation() {
        mockkObject(Presenter)
        coEvery { Presenter.present(any(), any(), any(), any()) } returns Unit

        DelayTask.delayTask(
            mockk<Context>(),
            inAppWithTrigger(
                triggerName = "open_page",
                triggerType = TriggerType.DEFAULT,
                events = listOf(
                    TriggerEvent(type = "default", name = "open_page", delay = 1)
                )
            ),
            InAppTrigger(name = "open_page", type = TriggerType.DEFAULT),
            1
        )

        PresentationGate.invalidate()

        Thread.sleep(1500)

        coVerify(exactly = 0) {
            Presenter.present(any(), any(), any(), any())
        }
    }

    /** test_6: invalidate increments the presentation epoch. */
    @Test
    fun test_6_invalidate_incrementsEpoch() {
        val before = PresentationGate.epoch

        PresentationGate.invalidate()

        assertEquals(before + 1, PresentationGate.epoch)
    }

    // ── helper ─────────────────────────────────────────────────────────────

    private fun inAppWithTrigger(
        triggerName: String,
        triggerType: TriggerType,
        events: List<TriggerEvent>
    ): InApp {
        return InApp(
            companyId = 1,
            smid = "smid-1",
            type = InAppContentType.CUSTOM,
            contentUrl = "",
            checksum = "",
            options = PlacementOptions(
                endTime = 0, triggerEvents = events
            ),
            deadline = 0,
            lastCompanyUpdate = 0,
            actionTriggerName = triggerName,
            actionTriggerType = triggerType
        )
    }
}
