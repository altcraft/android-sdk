@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.inapp.infra.Environment
import com.altcraft.sdk.inapp.data.Preferenses
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InApp
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppContentType
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.InAppTrigger
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.PlacementOptions
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.Policy
import com.altcraft.sdk.inapp.data.models.InAppModels.InAppModel.TriggerType
import com.altcraft.sdk.inapp.data.room.dao.InAppDao
import com.altcraft.sdk.inapp.system.policy.Check.CommonPolicy
import com.altcraft.sdk.inapp.system.policy.Check.Deadline
import com.altcraft.sdk.inapp.system.policy.Check.PersonalPolicy
import com.altcraft.sdk.inapp.system.policy.Check.SubStatusCheck
import com.altcraft.sdk.inapp.system.policy.Check.Version
import com.altcraft.sdk.inapp.platform.subscription.subscribe.SubscriptionProcess
import com.altcraft.sdk.inapp.system.presentation.InAppVariables
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CheckPolicyTest
 *
 * Positive scenarios:
 *  - test_1: CommonPolicy.canShow() returns true when policy is null.
 *  - test_2: CommonPolicy.canShow() returns true when trigger limit is not exceeded.
 *  - test_4: CommonPolicy.canShow() returns true when session limit is not exceeded.
 *  - test_6: CommonPolicy.canShow() returns true when minimum interval has passed.
 *  - test_8: PersonalPolicy.canShow() returns true when impressionsLimit is zero.
 *  - test_9: PersonalPolicy.canShow() returns true when open count is below impressionsLimit.
 *  - test_11: Deadline.canShow() returns true when deadline is zero.
 *  - test_12: Deadline.canShow() returns true when deadline is in the future.
 *  - test_14: Version.canShow() returns true when appVersions is empty.
 *  - test_15: Version.canShow() returns true when app version matches condition.
 *  - test_17: SubStatusCheck.canShow() returns true for anonymous in-app.
 *  - test_18: SubStatusCheck.canShow() returns true when user is subscribed.
 *
 * Negative scenarios:
 *  - test_3: CommonPolicy.canShow() returns false when trigger limit is exceeded.
 *  - test_5: CommonPolicy.canShow() returns false when session limit is exceeded.
 *  - test_7: CommonPolicy.canShow() returns false when minimum interval has not passed.
 *  - test_10: PersonalPolicy.canShow() returns false when open count reaches impressionsLimit.
 *  - test_13: Deadline.canShow() returns false when deadline is in the past.
 *  - test_16: Version.canShow() returns false when app version does not match condition.
 *  - test_19: SubStatusCheck.canShow() returns false when user is not subscribed.
 */
class CheckPolicyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        InAppVariables.reset()
    }

    @After
    fun tearDown() {
        unmockkAll()
        InAppVariables.reset()
    }

    private fun trigger(
        type: TriggerType = TriggerType.DEFAULT,
        name: String = "open"
    ): InAppTrigger {
        return InAppTrigger(
            type = type,
            name = name
        )
    }

    /** test_1: CommonPolicy.canShow() returns true when policy is null. */
    @Test
    fun test_1_commonPolicy_nullPolicy_returnsTrue() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers { 0L }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = null),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_2: CommonPolicy.canShow() returns true when trigger limit is not exceeded. */
    @Test
    fun test_2_commonPolicy_triggerLimitNotExceeded_returnsTrue() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers { 0L }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(limitByTrigger = 5)),
            deadline = 0, lastCompanyUpdate = 0,
            actionTriggerType = TriggerType.DEFAULT, actionTriggerName = "open"
        )

        assertTrue(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_3: CommonPolicy.canShow() returns false when trigger limit is exceeded. */
    @Test
    fun test_3_commonPolicy_triggerLimitExceeded_returnsFalse() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers { 0L }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(limitByTrigger = 1)),
            deadline = 0, lastCompanyUpdate = 0,
            actionTriggerType = TriggerType.DEFAULT, actionTriggerName = "open"
        )

        val trigger = trigger()

        InAppVariables.triggerBySessionInc(trigger)

        assertFalse(CommonPolicy.canShow(context, inApp, trigger))
    }

    /** test_4: CommonPolicy.canShow() returns true when session limit is not exceeded. */
    @Test
    fun test_4_commonPolicy_sessionLimitNotExceeded_returnsTrue() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers { 0L }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(limitBySession = 3)),
            deadline = 0, lastCompanyUpdate = 0
        )

        InAppVariables.openBySessionInc()
        InAppVariables.openBySessionInc()

        assertTrue(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_5: CommonPolicy.canShow() returns false when session limit is exceeded. */
    @Test
    fun test_5_commonPolicy_sessionLimitExceeded_returnsFalse() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers { 0L }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(limitBySession = 2)),
            deadline = 0, lastCompanyUpdate = 0
        )

        InAppVariables.openBySessionInc()
        InAppVariables.openBySessionInc()

        assertFalse(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_6: CommonPolicy.canShow() returns true when minimum interval has passed. */
    @Test
    fun test_6_commonPolicy_minIntervalPassed_returnsTrue() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers {
            System.currentTimeMillis() - 100_000
        }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(minIntervalSec = 10)),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_7: CommonPolicy.canShow() returns false when minimum interval has not passed. */
    @Test
    fun test_7_commonPolicy_minIntervalNotPassed_returnsFalse() = runTest {
        mockkObject(Preferenses)
        coEvery { Preferenses.getInAppLastOpenTime(any()) } coAnswers {
            System.currentTimeMillis()
        }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, policy = Policy(minIntervalSec = 60)),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertFalse(CommonPolicy.canShow(context, inApp, trigger()))
    }

    /** test_8: PersonalPolicy.canShow() returns true when impressionsLimit is zero. */
    @Test
    fun test_8_personalPolicy_zeroLimit_returnsTrue() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        val db = mockk<com.altcraft.sdk.inapp.data.room.DB>(relaxed = true)
        val dao = mockk<InAppDao>(relaxed = true)
        every { Environment.create(any()) } returns env
        every { env.room } returns db
        every { db.dao.inApp } returns dao
        coEvery { dao.getOpenCount(any()) } coAnswers { 5 }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 0),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(PersonalPolicy.canShow(context, inApp))
    }

    /** test_9: PersonalPolicy.canShow() returns true when open count is below impressionsLimit. */
    @Test
    fun test_9_personalPolicy_belowLimit_returnsTrue() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        val db = mockk<com.altcraft.sdk.inapp.data.room.DB>(relaxed = true)
        val dao = mockk<InAppDao>(relaxed = true)
        every { Environment.create(any()) } returns env
        every { env.room } returns db
        every { db.dao.inApp } returns dao
        coEvery { dao.getOpenCount("s1") } coAnswers { 3 }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 5),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(PersonalPolicy.canShow(context, inApp))
    }

    /** test_10: PersonalPolicy.canShow() returns false when open count reaches impressionsLimit. */
    @Test
    fun test_10_personalPolicy_atLimit_returnsFalse() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        val db = mockk<com.altcraft.sdk.inapp.data.room.DB>(relaxed = true)
        val dao = mockk<InAppDao>(relaxed = true)
        every { Environment.create(any()) } returns env
        every { env.room } returns db
        every { db.dao.inApp } returns dao
        coEvery { dao.getOpenCount("s1") } coAnswers { 5 }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, impressionsLimit = 5),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertFalse(PersonalPolicy.canShow(context, inApp))
    }

    /** test_11: Deadline.canShow() returns true when deadline is zero. */
    @Test
    fun test_11_deadline_zero_returnsTrue() = runTest {
        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(Deadline.canShow(context, inApp))
    }

    /** test_12: Deadline.canShow() returns true when deadline is in the future. */
    @Test
    fun test_12_deadline_future_returnsTrue() = runTest {
        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = System.currentTimeMillis() + 100_000),
            deadline = System.currentTimeMillis() + 100_000, lastCompanyUpdate = 0
        )

        assertTrue(Deadline.canShow(context, inApp))
    }

    /** test_13: Deadline.canShow() returns false when deadline is in the past. */
    @Test
    fun test_13_deadline_past_returnsFalse() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        val db = mockk<com.altcraft.sdk.inapp.data.room.DB>(relaxed = true)
        val dao = mockk<InAppDao>(relaxed = true)
        every { Environment.create(any()) } returns env
        every { env.room } returns db
        every { db.dao.inApp } returns dao

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = System.currentTimeMillis() + 100_000),
            deadline = System.currentTimeMillis() - 100_000, lastCompanyUpdate = 0
        )

        assertFalse(Deadline.canShow(context, inApp))
    }

    /** test_14: Version.canShow() returns true when appVersions is empty. */
    @Test
    fun test_14_version_emptyVersions_returnsTrue() = runTest {
        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, appVersions = emptyList()),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(Version.canShow(context, inApp))
    }

    /** test_15: Version.canShow() returns true when app version matches condition. */
    @Test
    fun test_15_version_matching_returnsTrue() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { Environment.create(any()) } returns env
        coEvery { env.appVersion() } returns "1.2.3"

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(
                endTime = 0,
                appVersions = listOf("1.2.3"),
                appVersionConditionRaw = "equal"
            ),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(Version.canShow(context, inApp))
    }

    /** test_16: Version.canShow() returns false when app version does not match condition. */
    @Test
    fun test_16_version_nonMatching_returnsFalse() = runTest {
        mockkObject(Environment.Companion)
        val env = mockk<Environment>(relaxed = true)
        every { Environment.create(any()) } returns env
        coEvery { env.appVersion() } returns "1.0.0"

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(
                endTime = 0,
                appVersions = listOf("2.0.0"),
                appVersionConditionRaw = "equal"
            ),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertFalse(Version.canShow(context, inApp))
    }

    /** test_17: SubStatusCheck.canShow() returns true for anonymous in-app. */
    @Test
    fun test_17_subStatus_anonymous_returnsTrue() = runTest {
        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, isAnonim = true),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(SubStatusCheck.canShow(context, inApp))
    }

    /** test_18: SubStatusCheck.canShow() returns true when user is subscribed. */
    @Test
    fun test_18_subStatus_subscribed_returnsTrue() = runTest {
        mockkObject(SubscriptionProcess)
        coEvery { SubscriptionProcess.getInAppSubscriptionStatus(any()) } coAnswers { true }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, isAnonim = false),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertTrue(SubStatusCheck.canShow(context, inApp))
    }

    /** test_19: SubStatusCheck.canShow() returns false when user is not subscribed. */
    @Test
    fun test_19_subStatus_notSubscribed_returnsFalse() = runTest {
        mockkObject(SubscriptionProcess)
        coEvery { SubscriptionProcess.getInAppSubscriptionStatus(any()) } coAnswers { false }

        val inApp = InApp(
            companyId = 1, smid = "s1", type = InAppContentType.CUSTOM,
            contentUrl = "", checksum = "",
            options = PlacementOptions(endTime = 0, isAnonim = false),
            deadline = 0, lastCompanyUpdate = 0
        )

        assertFalse(SubStatusCheck.canShow(context, inApp))
    }
}