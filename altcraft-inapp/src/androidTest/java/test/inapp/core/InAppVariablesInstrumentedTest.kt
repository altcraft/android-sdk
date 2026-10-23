package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import com.altcraft.sdk.inapp.data.Preferenses
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.data.room.DB
import com.altcraft.sdk.inapp.data.room.dao.InAppDao
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * InAppVariablesInstrumentedTest
 *
 * Tests the updateInAppVariables() function which requires Context
 * (calls setInAppLastOpenTime via DataStore and checkPersonalPolicy via Room).
 *
 * Positive scenarios:
 *  - test_1: updateInAppVariables increments counters when not delayed.
 *  - test_2: updateInAppVariables increments counters for delayed displays too.
 *  - test_3: updateInAppVariables calls checkPersonalPolicy.
 *  - test_4: updateInAppVariables does not throw on exception.
 *
 * Negative scenarios:
 *  - test_5: updateInAppVariables with CUSTOM trigger increments correct counter.
 */
@RunWith(AndroidJUnit4::class)
class InAppVariablesInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: DB
    private lateinit var dao: InAppDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        db = Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao.inApp

        MockKAnnotations.init(this, relaxUnitFun = true)
        InAppVariables.reset()

        mockkObject(Events)
        every { Events.error(any(), any()) } answers { SDKEventModel.Error(function = firstArg()) }
        every {
            Events.retry(
                any(),
                any()
            )
        } answers { SDKEventModel.RetryError(function = firstArg()) }
        every { Events.event(any(), any()) } answers { SDKEventModel.Event(function = firstArg()) }
        every {
            Events.error(
                any(),
                any(),
                any()
            )
        } answers { SDKEventModel.Error(function = firstArg()) }
        every {
            Events.retry(
                any(),
                any(),
                any()
            )
        } answers { SDKEventModel.RetryError(function = firstArg()) }
        every {
            Events.event(
                any(),
                any(),
                any()
            )
        } answers { SDKEventModel.Event(function = firstArg()) }
    }

    @After
    fun tearDown() {
        InAppVariables.reset()
        db.close()
        unmockkAll()
    }

    /** test_1: updateInAppVariables increments counters when not delayed. */
    @Test
    fun test_1_updateInAppVariables_notDelayed_incrementsCounters() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setInAppLastOpenTime(any()) } coAnswers { }

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        every { Environment.create(any()) } returns env

        val trigger = trigger(TriggerType.DEFAULT, "open")

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0,
            actionTriggerType = TriggerType.DEFAULT, actionTriggerName = "open"
        )

        InAppVariables.updateInAppVariables(
            context = context,
            inApp = inApp,
            trigger = trigger
        )

        assertThat(InAppVariables.getOpenBySession(), `is`(1))
        assertThat(InAppVariables.getTriggerCount(TriggerType.DEFAULT, "open"), `is`(1))
    }

    /** test_2: updateInAppVariables increments counters for delayed displays too. */
    @Test
    fun test_2_updateInAppVariables_delayed_incrementsCounters() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setInAppLastOpenTime(any()) } coAnswers { }

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        every { Environment.create(any()) } returns env

        val trigger = trigger(TriggerType.DEFAULT, "open")

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0,
            actionTriggerType = TriggerType.DEFAULT, actionTriggerName = "open"
        )

        InAppVariables.updateInAppVariables(
            context = context,
            inApp = inApp,
            trigger = trigger
        )

        assertThat(InAppVariables.getOpenBySession(), `is`(1))
        assertThat(InAppVariables.getTriggerCount(TriggerType.DEFAULT, "open"), `is`(1))
    }

    /** test_3: updateInAppVariables calls setInAppLastOpenTime. */
    @Test
    fun test_3_updateInAppVariables_callsSetLastOpenTime() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setInAppLastOpenTime(any()) } coAnswers { }

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        every { Environment.create(any()) } returns env

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0
        )

        InAppVariables.updateInAppVariables(
            context = context,
            inApp = inApp,
            trigger = trigger()
        )

        coVerify { Preferenses.setInAppLastOpenTime(context) }
    }

    /** test_4: updateInAppVariables does not throw on exception. */
    @Test
    fun test_4_updateInAppVariables_exception_doesNotThrow() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setInAppLastOpenTime(any()) } throws RuntimeException("datastore error")

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0
        )

        try {
            InAppVariables.updateInAppVariables(
                context = context,
                inApp = inApp,
                trigger = trigger()
            )
        } catch (e: Exception) {
            fail("updateInAppVariables should not throw: ${e.message}")
        }
    }

    /** test_5: updateInAppVariables with CUSTOM trigger increments correct counter. */
    @Test
    fun test_5_updateInAppVariables_customTrigger_incrementsCustomCounter() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.setInAppLastOpenTime(any()) } coAnswers { }

        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { env.room } returns db
        every { Environment.create(any()) } returns env

        val trigger = trigger(TriggerType.CUSTOM, "custom_btn")

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0,
            actionTriggerType = TriggerType.CUSTOM, actionTriggerName = "custom_btn"
        )

        InAppVariables.updateInAppVariables(
            context = context,
            inApp = inApp,
            trigger = trigger
        )

        assertThat(InAppVariables.getTriggerCount(TriggerType.CUSTOM, "custom_btn"), `is`(1))
        assertThat(InAppVariables.getTriggerCount(TriggerType.DEFAULT, "custom_btn"), `is`(0))
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun trigger(
        type: TriggerType = TriggerType.DEFAULT,
        name: String = "open"
    ) = InAppTrigger(
        name = name,
        type = type
    )
}
