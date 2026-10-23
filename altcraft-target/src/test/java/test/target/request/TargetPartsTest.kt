@file:Suppress("SpellCheckingInspection")

package test.target.request

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.target.data.models.TargetModels.UTM
import com.altcraft.sdk.target.data.room.Entity
import com.altcraft.sdk.target.platform.target.request.TargetParts.createRequestParts
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel
import com.altcraft.sdk.events.Events
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TargetPartsTest
 *
 * Verifies the multipart part builder used for target requests.
 *
 * Positive scenarios:
 *  - Fully-populated entity produces exactly 16 parts in the documented order.
 *  - Optional parts are skipped when the corresponding values are null.
 *
 * Resilience:
 *  - Malformed UTM JSON degrades to parts without UTM instead of throwing.
 */
class TargetPartsTest {

    private companion object {
        const val EXPECTED_PARTS = "A fully populated entity must produce exactly 16 parts"
        const val EXPECTED_ORDER =
            "Parts must follow the fixed order tz,t,aci,wn,mm,cn,cc,ck,cm,cs,ct,wd,mi,ma,sn,pf"
    }

    private val utmJson = Json.encodeToString(
        UTM(
            campaign = "campaign",
            content = "content",
            keyword = "keyword",
            medium = "medium",
            source = "source",
            temp = "temp"
        )
    )

    @Before
    fun setUp() {
        mockkObject(Events)
        every { Events.error(any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.error(any(), any(), any(), any()) } answers {
            SDKEventModel.Error(function = firstArg())
        }
        every { Events.retry(any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
        every { Events.retry(any(), any(), any()) } answers {
            SDKEventModel.RetryError(function = firstArg())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun fullEntity() = Entity(
        userTag = "user-123",
        timeZone = 180,
        sid = "pixel-1",
        altcraftClientID = "client-id",
        eventName = "onAppOpen",
        payload = """{"a":1}""",
        matching = """{"m":1}""",
        matchingType = "1",
        profileFields = """{"p":1}""",
        subscription = """{"s":1}""",
        sendMessageId = "mid-1",
        utmTags = utmJson
    )

    private fun partName(part: MultipartBody.Part): String? {
        return part.headers?.get("Content-Disposition")
            ?.let { Regex("name\\s*=\\s*\"([^\"]*)\"").find(it)?.groupValues?.get(1) }
    }

    /** A fully populated entity produces 16 parts in the documented order. */
    @Test
    fun createRequestParts_fullEntity_produces16PartsInOrder() {
        val parts = createRequestParts(fullEntity())

        assertTrue(EXPECTED_PARTS, parts != null)
        val names = parts!!.map(::partName)
        val expected = listOf(
            "tz", "t", "aci", "wn", "mm",
            "cn", "cc", "ck", "cm", "cs", "ct",
            "wd", "mi", "ma", "sn", "pf"
        )

        assertEquals(EXPECTED_ORDER, expected, names)
    }

    /** Optional parts (UTM, matching type, JSON payloads) are skipped when null. */
    @Test
    fun createRequestParts_minimalEntity_skipsOptionalParts() {
        val parts = createRequestParts(
            Entity(
                userTag = "user-123",
                timeZone = 180,
                sid = "pixel-1",
                altcraftClientID = null,
                eventName = "onAppOpen",
                payload = null,
                matching = null,
                matchingType = null,
                profileFields = null,
                subscription = null,
                sendMessageId = null,
                utmTags = null
            )
        )

        assertTrue(EXPECTED_PARTS, parts != null)
        // Only mandatory parts survive: tz, t and wn.
        assertEquals(listOf("tz", "t", "wn"), parts!!.map(::partName))
    }

    /** Malformed UTM JSON degrades gracefully: UTM parts are omitted, no throw. */
    @Test
    fun createRequestParts_invalidUtmJson_degradesWithoutUtmParts() {
        val parts = createRequestParts(
            Entity(
                userTag = "user-123",
                timeZone = 180,
                sid = "pixel-1",
                altcraftClientID = "client-id",
                eventName = "onAppOpen",
                payload = null,
                matching = null,
                matchingType = null,
                profileFields = null,
                subscription = null,
                sendMessageId = null,
                utmTags = "not-a-json"
            )
        )

        assertTrue(EXPECTED_PARTS, parts != null)
        val names = parts!!.map(::partName)
        assertTrue("UTM parts must be omitted when UTM JSON is malformed", "cn" !in names)
        // Mandatory tz, t, aci, wn survive; the six UTM parts are dropped.
        assertEquals(listOf("tz", "t", "aci", "wn"), names.filter { it in listOf("cn", "cc", "ck", "cm", "cs", "ct", "tz", "t", "aci", "wn") })
    }
}
