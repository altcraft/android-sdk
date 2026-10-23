@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * InAppVariablesTest
 *
 * Positive scenarios:
 *  - test_1: getCurrentPlacement returns null initially.
 *  - test_2: setCurrentPlacement updates the value.
 *  - test_3: setCurrentPlacement with null clears the placement.
 *  - test_4: getOpenBySession starts at 0 and increments with openBySessionInc.
 *  - test_5: getTriggerCount returns 0 for unknown triggers.
 *  - test_6: triggerBySessionInc increments counters for DEFAULT and CUSTOM triggers.
 *  - test_7: reset clears all state.
 */
class InAppVariablesTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(InAppProcTrigger)
        coEvery { InAppProcTrigger.trigger(any(), any()) } returns mockk(relaxed = true)
        InAppVariables.reset()
    }

    @After
    fun tearDown() {
        unmockkAll()
        InAppVariables.reset()
    }

    /** test_1: getCurrentPlacement returns null initially. */
    @Test
    fun test_1_placement_initialIsNull() {
        assertNull(InAppVariables.getCurrentScreen())
    }

    /** test_2: setCurrentPlacement updates placement value. */
    @Test
    fun test_2_placement_setAndGet() {
        InAppVariables.setCurrentScreen(context, "home_screen")
        assertEquals("home_screen", InAppVariables.getCurrentScreen())
    }

    /** test_3: setCurrentPlacement with null clears it. */
    @Test
    fun test_3_placement_setNull_clears() {
        InAppVariables.setCurrentScreen(context, "some_screen")
        InAppVariables.setCurrentScreen(context, null)
        assertNull(InAppVariables.getCurrentScreen())
    }

    /** test_4: openBySession starts at 0 and increments. */
    @Test
    fun test_4_openBySession_increment() {
        assertEquals(0, InAppVariables.getOpenBySession())
        InAppVariables.openBySessionInc()
        assertEquals(1, InAppVariables.getOpenBySession())
        InAppVariables.openBySessionInc()
        assertEquals(2, InAppVariables.getOpenBySession())
    }

    /** test_5: getTriggerCount returns 0 for unknown triggers of both types. */
    @Test
    fun test_5_triggerCount_unknown_returnsZero() {
        assertEquals(0, InAppVariables.getTriggerCount(TriggerType.DEFAULT, "unknown"))
        assertEquals(0, InAppVariables.getTriggerCount(TriggerType.CUSTOM, "unknown"))
    }

    /** test_6: triggerBySessionInc increments DEFAULT and CUSTOM trigger counters independently. */
    @Test
    fun test_6_triggerBySessionInc_incrementsIndependently() {
        val defaultTrigger = trigger(TriggerType.DEFAULT, "open_app")
        val customTrigger = trigger(TriggerType.CUSTOM, "button_click")

        InAppVariables.triggerBySessionInc(defaultTrigger)
        InAppVariables.triggerBySessionInc(defaultTrigger)

        assertEquals(2, InAppVariables.getTriggerCount(TriggerType.DEFAULT, "open_app"))
        assertEquals(0, InAppVariables.getTriggerCount(TriggerType.CUSTOM, "open_app"))

        InAppVariables.triggerBySessionInc(customTrigger)

        assertEquals(1, InAppVariables.getTriggerCount(TriggerType.CUSTOM, "button_click"))
        assertEquals(0, InAppVariables.getTriggerCount(TriggerType.DEFAULT, "button_click"))
    }

    /** test_7: reset clears placement, open count, and trigger counters. */
    @Test
    fun test_7_reset_clearsAllState() {
        InAppVariables.setCurrentScreen(context, "screen")
        InAppVariables.openBySessionInc()
        InAppVariables.triggerBySessionInc(trigger(TriggerType.DEFAULT, "evt"))

        InAppVariables.reset()

        assertNull(InAppVariables.getCurrentScreen())
        assertEquals(0, InAppVariables.getOpenBySession())
        assertEquals(0, InAppVariables.getTriggerCount(TriggerType.DEFAULT, "evt"))
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun trigger(type: TriggerType, name: String) = InAppTrigger(
        name = name,
        type = type
    )
}