@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Availability
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TimePeriod
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TimePoint
import com.altcraft.sdk.inapp.system.policy.Check.Availability as AvailabilityCheck
import com.altcraft.sdk.inapp.system.policy.Check.Placement as PlacementCheck
import com.altcraft.sdk.inapp.system.presentation.InAppProcTrigger
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * CheckTest
 *
 * Positive scenarios:
 *  - test_1_availability: null availability → allowed.
 *  - test_2_availability: empty restriction lists → allowed.
 *  - test_3_availability: matching years → allowed.
 *  - test_5_availability: matching months → allowed.
 *  - test_6_availability: matching weekdays → allowed.
 *  - test_8_availability: matching time period → allowed.
 *  - test_10_availability: period crossing midnight → allowed for late night.
 *  - test_11_availability: period crossing midnight → allowed for early morning.
 *  - test_13_placement: empty placements → allowed.
 *  - test_14_placement: matching placement → allowed.
 *
 * Negative scenarios:
 *  - test_4_availability: non-matching years → denied.
 *  - test_7_availability: non-matching weekday → denied.
 *  - test_9_availability: outside time period → denied.
 *  - test_12_availability: period crossing midnight outside → denied.
 *  - test_15_placement: non-matching placement → denied.
 */
class CheckTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(InAppProcTrigger)
        coEvery { InAppProcTrigger.trigger(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        InAppVariables.reset()
    }

    // ── Availability ────────────────────────────────────────────────────

    /** test_1_availability: null availability → allowed. */
    @Test
    fun `availability null returns true`() {
        assertTrue(AvailabilityCheck.canShow(inApp(availability = null)))
    }

    /** test_2_availability: all empty lists → allowed. */
    @Test
    fun `availability empty lists returns true`() {
        val av = Availability()
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av)))
    }

    /** test_3_availability: matching year → allowed. */
    @Test
    fun `availability matching year returns true`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(years = listOf(2024), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_4_availability: non-matching year → denied. */
    @Test
    fun `availability non matching year returns false`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(years = listOf(1999), tz = "UTC")
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_5_availability: matching month → allowed. */
    @Test
    fun `availability matching month returns true`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(months = listOf(6), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_6_availability: matching weekday → allowed. */
    @Test
    fun `availability matching weekday returns true`() {
        // Mon 2024-01-01 12:00:00 UTC
        val mondayMillis = dateTimeMillis(2024, 1, 1, 12, 0)
        val av = Availability(wdays = listOf(1), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = mondayMillis))
    }

    /** test_7_availability: non-matching weekday → denied. */
    @Test
    fun `availability non matching weekday returns false`() {
        // Mon 2024-01-01 12:00:00 UTC
        val mondayMillis = dateTimeMillis(2024, 1, 1, 12, 0)
        val av = Availability(wdays = listOf(2), tz = "UTC")
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = mondayMillis))
    }

    /** test_8_availability: current time inside period → allowed. */
    @Test
    fun `availability inside time period returns true`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(10, 0), TimePoint(12, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 11, 0)
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_9_availability: current time outside period → denied. */
    @Test
    fun `availability outside time period returns false`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(10, 0), TimePoint(12, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 14, 0)
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_10_availability: period crossing midnight → allowed for late night. */
    @Test
    fun `availability period crossing midnight late night returns true`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(22, 0), TimePoint(2, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 23, 0)
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_11_availability: period crossing midnight → allowed for early morning. */
    @Test
    fun `availability period crossing midnight early morning returns true`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(22, 0), TimePoint(2, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 1, 0)
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_12_availability: period crossing midnight outside → denied. */
    @Test
    fun `availability period crossing midnight outside returns false`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(22, 0), TimePoint(2, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_13_availability: matching month day → allowed. */
    @Test
    fun `availability matching month day returns true`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(mdays = listOf(15), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_14_availability: non-matching month day → denied. */
    @Test
    fun `availability non matching month day returns false`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(mdays = listOf(16), tz = "UTC")
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_15_availability: first weekday order in month → allowed. */
    @Test
    fun `availability first weekday order returns true`() {
        // Mon 2024-01-01 is the first Monday of January.
        val millis = dateTimeMillis(2024, 1, 1, 12, 0)
        val av = Availability(wdays = listOf(1), wdayOrders = listOf(1), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_16_availability: wrong weekday order in month → denied. */
    @Test
    fun `availability wrong weekday order returns false`() {
        // Mon 2024-01-01 is the first Monday of January, not the second.
        val millis = dateTimeMillis(2024, 1, 1, 12, 0)
        val av = Availability(wdays = listOf(1), wdayOrders = listOf(2), tz = "UTC")
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_17_availability: last weekday order in month → allowed. */
    @Test
    fun `availability last weekday order returns true`() {
        // Mon 2024-01-29 is the last Monday of January.
        val millis = dateTimeMillis(2024, 1, 29, 12, 0)
        val av = Availability(wdays = listOf(1), wdayOrders = listOf(-1), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_18_availability: weekday order zero disables filter → allowed. */
    @Test
    fun `availability weekday order zero returns true`() {
        val millis = dateTimeMillis(2024, 1, 15, 12, 0)
        val av = Availability(wdayOrders = listOf(0), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_19_availability: unknown weekday order keeps backend compatibility → allowed. */
    @Test
    fun `availability unknown weekday order returns true`() {
        val millis = dateTimeMillis(2024, 1, 15, 12, 0)
        val av = Availability(wdayOrders = listOf(99), tz = "UTC")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_20_availability: period start boundary is inclusive → allowed. */
    @Test
    fun `availability period start boundary returns true`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(10, 0), TimePoint(12, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 10, 0)
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_21_availability: period end boundary is exclusive → denied. */
    @Test
    fun `availability period end boundary returns false`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(10, 0), TimePoint(12, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_22_availability: equal period boundaries mean whole day → allowed. */
    @Test
    fun `availability equal period boundaries returns true`() {
        val av = Availability(
            periods = listOf(TimePeriod(TimePoint(10, 0), TimePoint(10, 0))),
            tz = "UTC"
        )
        val millis = dateTimeMillis(2024, 6, 15, 23, 59)
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_23_availability: timezone is applied → allowed. */
    @Test
    fun `availability timezone applied returns true`() {
        // 2024-06-15 21:00 UTC = 2024-06-16 00:00 Europe/Moscow.
        val millis = dateTimeMillis(2024, 6, 15, 21, 0)
        val av = Availability(mdays = listOf(16), tz = "Europe/Moscow")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_24_availability: invalid timezone falls back safely → allowed. */
    @Test
    fun `availability invalid timezone returns true`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(years = listOf(2024), tz = "Invalid/Timezone")
        assertTrue(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    /** test_25_availability: one failed condition denies display → denied. */
    @Test
    fun `availability mixed conditions with failed month returns false`() {
        val millis = dateTimeMillis(2024, 6, 15, 12, 0)
        val av = Availability(
            years = listOf(2024),
            months = listOf(7),
            mdays = listOf(15),
            wdays = listOf(6),
            tz = "UTC"
        )
        assertFalse(AvailabilityCheck.canShow(inApp(availability = av), nowMillis = millis))
    }

    // ── Placement ───────────────────────────────────────────────────────

    /** test_26_placement: empty placements → allowed. */
    @Test
    fun `placement empty list returns true`() {
        InAppVariables.setCurrentScreen(context, "some_screen")
        val inAppWithEmptyPlacements = inApp(
            options = PlacementOptions(endTime = 0, placements = emptyList())
        )
        assertTrue(PlacementCheck.canShow(inAppWithEmptyPlacements))
    }

    /** test_27_placement: matching placement → allowed. */
    @Test
    fun `placement matching returns true`() {
        InAppVariables.setCurrentScreen(context, "home_banner")
        val inAppWithPlacement = inApp(
            options = PlacementOptions(endTime = 0, placements = listOf("home_banner"))
        )
        assertTrue(PlacementCheck.canShow(inAppWithPlacement))
    }

    /** test_28_placement: non-matching placement → denied. */
    @Test
    fun `placement non matching returns false`() {
        InAppVariables.setCurrentScreen(context, "profile")
        val inAppWithPlacement = inApp(
            options = PlacementOptions(endTime = 0, placements = listOf("home_banner"))
        )
        assertFalse(PlacementCheck.canShow(inAppWithPlacement))
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun inApp(
        availability: Availability? = Availability(),
        options: PlacementOptions = PlacementOptions(
            endTime = 0,
            policy = null,
            isAnonim = true,
            placements = emptyList(),
            appVersions = emptyList(),
            appVersionConditionRaw = null,
            priority = 0,
            triggerEvents = emptyList(),
            impressionsLimit = 0,
            availability = availability
        )
    ): InApp = InApp(
        companyId = 1,
        contentUrl = "",
        checksum = "",
        type = InAppContentType.CUSTOM,
        smid = "test-smid",
        options = options,
        deadline = 0,
        lastCompanyUpdate = 0
    )

    private fun dateTimeMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}