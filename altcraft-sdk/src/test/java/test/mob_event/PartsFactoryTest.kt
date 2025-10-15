package test.mob_event

// Created by Andrey Pogodin.
// Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.ALTCRAFT_CLIENT_ID
import com.altcraft.sdk.data.Constants.MATCHING_MOB
import com.altcraft.sdk.data.Constants.MATCHING_TYPE
import com.altcraft.sdk.data.Constants.MOB_EVENT_NAME
import com.altcraft.sdk.data.Constants.PAYLOAD
import com.altcraft.sdk.data.Constants.PROFILE_FIELDS_MOB
import com.altcraft.sdk.data.Constants.SMID_MOB
import com.altcraft.sdk.data.Constants.SUBSCRIPTION_MOB
import com.altcraft.sdk.data.Constants.TIME_MOB
import com.altcraft.sdk.data.Constants.TIME_ZONE
import com.altcraft.sdk.data.Constants.UTM_CAMPAIGN
import com.altcraft.sdk.data.Constants.UTM_CONTENT
import com.altcraft.sdk.data.Constants.UTM_KEYWORD
import com.altcraft.sdk.data.Constants.UTM_MEDIUM
import com.altcraft.sdk.data.Constants.UTM_SOURCE
import com.altcraft.sdk.data.Constants.UTM_TEMP
import com.altcraft.sdk.data.room.MobileEventEntity
import com.altcraft.sdk.mob_events.PartsFactory
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

/**
 * PartsFactoryTest (updated for the new PartsFactory contract)
 *
 * Positive scenarios:
 * - test_1: createMobileEventParts builds 16 parts (11 text + 5 JSON) when all values are present.
 * - test_2: with all optional JSONs and utmTags = null → only 5 text parts remain (tz, t, aci, wn, mm).
 * - test_3: order is stable: [tz, t, aci, wn, mm, cn, cc, ck, cm, cs, ct, wd, mi, ma, sn, pf].
 *
 * Notes:
 * - Pure JVM unit tests (no Android runtime).
 * - RequestBody content is read via okio.Buffer in UTF-8.
 * - TIME_MOB = entity.time / 1000 (seconds) serialized as a string.
 * - UTM values are parsed from entity.utmTags (JSON) into DataClasses.UTM; null values are skipped.
 */

private val TEXT_CT = "text/plain; charset=utf-8".toMediaType()
private val JSON_CT = "application/json; charset=utf-8".toMediaType()

class PartsFactoryTest {

    /** Helper to read a part body as UTF-8 text. */
    private fun bodyUtf8(part: okhttp3.MultipartBody.Part): String =
        Buffer().apply { part.body.writeTo(this) }.readUtf8()

    /** test_1: all fields are present → 16 parts with correct headers/types/bodies. */
    @Test
    fun test_1_all_fields_present() {
        // Fixed timestamp to get deterministic TIME_MOB (seconds)
        val fixedMs = 1_700_000_000_000L

        // Full set of values, including matchingType and utmTags
        val utmJson = """{
          "campaign":"camp-1",
          "content":"cont-2",
          "keyword":"key-3",
          "medium":"med-4",
          "source":"src-5",
          "temp":"tmp-6"
        }""".trimIndent()

        val entity = MobileEventEntity(
            id = 0L,
            userTag = "tag-1",
            timeZone = 180,                    // +03:00 in minutes (domain-specific)
            time = fixedMs,
            sid = "pixel-42",
            altcraftClientID = "ac-123",
            eventName = "purchase",
            payload = """{"sum":9.99,"ok":true}""",
            matching = """{"m":"v"}""",
            profileFields = """{"age":30}""",
            subscription = """{"channel":"email"}""",
            sendMessageId = """"sm-777"""",    // JSON string by design (sent as application/json)
            retryCount = 0,
            maxRetryCount = 3,
            matchingType = "email",            // NEW required text part
            utmTags = utmJson                  // NEW source for UTM text parts
        )

        val parts = PartsFactory.createMobileEventParts(entity)
        assertNotNull(parts)
        parts!!

        // 11 text + 5 JSON = 16
        assertEquals(16, parts.size)

        // 1) TIME_ZONE (text)
        parts[0].also {
            assertEquals("""form-data; name="$TIME_ZONE"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("180", bodyUtf8(it))
        }
        // 2) TIME_MOB (text) = time/1000
        parts[1].also {
            assertEquals("""form-data; name="$TIME_MOB"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals((fixedMs / 1000).toString(), bodyUtf8(it))
        }
        // 3) ALTCRAFT_CLIENT_ID (text)
        parts[2].also {
            assertEquals("""form-data; name="$ALTCRAFT_CLIENT_ID"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("ac-123", bodyUtf8(it))
        }
        // 4) MOB_EVENT_NAME (text)
        parts[3].also {
            assertEquals("""form-data; name="$MOB_EVENT_NAME"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("purchase", bodyUtf8(it))
        }
        // 5) MATCHING_TYPE (text)
        parts[4].also {
            assertEquals("""form-data; name="$MATCHING_TYPE"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("email", bodyUtf8(it))
        }
        // 6) UTM_CAMPAIGN (text)
        parts[5].also {
            assertEquals("""form-data; name="$UTM_CAMPAIGN"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("camp-1", bodyUtf8(it))
        }
        // 7) UTM_CONTENT (text)
        parts[6].also {
            assertEquals("""form-data; name="$UTM_CONTENT"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("cont-2", bodyUtf8(it))
        }
        // 8) UTM_KEYWORD (text)
        parts[7].also {
            assertEquals("""form-data; name="$UTM_KEYWORD"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("key-3", bodyUtf8(it))
        }
        // 9) UTM_MEDIUM (text)
        parts[8].also {
            assertEquals("""form-data; name="$UTM_MEDIUM"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("med-4", bodyUtf8(it))
        }
        // 10) UTM_SOURCE (text)
        parts[9].also {
            assertEquals("""form-data; name="$UTM_SOURCE"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("src-5", bodyUtf8(it))
        }
        // 11) UTM_TEMP (text)
        parts[10].also {
            assertEquals("""form-data; name="$UTM_TEMP"""", it.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, it.body.contentType())
            assertEquals("tmp-6", bodyUtf8(it))
        }

        // 12) PAYLOAD (JSON)
        parts[11].also {
            assertEquals("""form-data; name="$PAYLOAD"""", it.headers?.get("Content-Disposition"))
            assertEquals(JSON_CT, it.body.contentType())
            assertEquals("""{"sum":9.99,"ok":true}""", bodyUtf8(it))
        }
        // 13) SMID_MOB (JSON)
        parts[12].also {
            assertEquals("""form-data; name="$SMID_MOB"""", it.headers?.get("Content-Disposition"))
            assertEquals(JSON_CT, it.body.contentType())
            assertEquals(""""sm-777"""", bodyUtf8(it))
        }
        // 14) MATCHING_MOB (JSON)
        parts[13].also {
            assertEquals("""form-data; name="$MATCHING_MOB"""", it.headers?.get("Content-Disposition"))
            assertEquals(JSON_CT, it.body.contentType())
            assertEquals("""{"m":"v"}""", bodyUtf8(it))
        }
        // 15) SUBSCRIPTION_MOB (JSON)
        parts[14].also {
            assertEquals("""form-data; name="$SUBSCRIPTION_MOB"""", it.headers?.get("Content-Disposition"))
            assertEquals(JSON_CT, it.body.contentType())
            assertEquals("""{"channel":"email"}""", bodyUtf8(it))
        }
        // 16) PROFILE_FIELDS_MOB (JSON)
        parts[15].also {
            assertEquals("""form-data; name="$PROFILE_FIELDS_MOB"""", it.headers?.get("Content-Disposition"))
            assertEquals(JSON_CT, it.body.contentType())
            assertEquals("""{"age":30}""", bodyUtf8(it))
        }
    }

    /** test_2: optional JSONs are null, utmTags = null → only 5 text parts (tz, t, aci, wn, mm). */
    @Test
    fun test_2_null_optionals_only_text_parts() {
        val fixedMs = 1_700_000_100_000L

        val entity = MobileEventEntity(
            id = 0L,
            userTag = "tag-2",
            timeZone = -120,
            time = fixedMs,
            sid = "px",
            altcraftClientID = "ac-0",
            eventName = "open",
            payload = null,
            matching = null,
            profileFields = null,
            subscription = null,
            sendMessageId = null,
            retryCount = 0,
            maxRetryCount = 3,
            matchingType = "none",
            utmTags = null
        )

        val parts = PartsFactory.createMobileEventParts(entity)
        assertNotNull(parts)
        parts!!

        // Only required text fields + matchingType remain: 5 parts
        assertEquals(5, parts.size)

        val expectedNames = listOf(
            TIME_ZONE, TIME_MOB, ALTCRAFT_CLIENT_ID, MOB_EVENT_NAME, MATCHING_TYPE
        )

        expectedNames.forEachIndexed { idx, name ->
            val p = parts[idx]
            assertEquals("""form-data; name="$name"""", p.headers?.get("Content-Disposition"))
            assertEquals(TEXT_CT, p.body.contentType())
        }

        // Bodies
        assertEquals("-120", bodyUtf8(parts[0]))
        assertEquals((fixedMs / 1000).toString(), bodyUtf8(parts[1]))
        assertEquals("ac-0", bodyUtf8(parts[2]))
        assertEquals("open", bodyUtf8(parts[3]))
        assertEquals("none", bodyUtf8(parts[4]))
    }

    /** test_3: parts order is updated and preserved (all fields present). */
    @Test
    fun test_3_order_is_preserved() {
        val entity = MobileEventEntity(
            id = 0L,
            userTag = "tag",
            timeZone = 0,
            time = 2_000_000_000_000L,
            sid = "px",
            altcraftClientID = "client",
            eventName = "evt",
            payload = """{"a":1}""",
            matching = """{"b":2}""",
            profileFields = """{"p":3}""",
            subscription = """{"s":4}""",
            sendMessageId = """"smid"""",
            retryCount = 0,
            maxRetryCount = 3,
            matchingType = "phone",
            utmTags = """{
              "campaign":"C",
              "content":"Ct",
              "keyword":"K",
              "medium":"M",
              "source":"S",
              "temp":"T"
            }""".trimIndent()
        )

        val parts = PartsFactory.createMobileEventParts(entity)!!
        val namesInOrder = listOf(
            TIME_ZONE,
            TIME_MOB,
            ALTCRAFT_CLIENT_ID,
            MOB_EVENT_NAME,
            MATCHING_TYPE,
            UTM_CAMPAIGN,
            UTM_CONTENT,
            UTM_KEYWORD,
            UTM_MEDIUM,
            UTM_SOURCE,
            UTM_TEMP,
            PAYLOAD,
            SMID_MOB,
            MATCHING_MOB,
            SUBSCRIPTION_MOB,
            PROFILE_FIELDS_MOB
        )

        assertEquals(16, parts.size)
        parts.forEachIndexed { idx, part ->
            val cd = part.headers?.get("Content-Disposition")
            assertEquals("""form-data; name="${namesInOrder[idx]}"""", cd)
        }
    }
}
