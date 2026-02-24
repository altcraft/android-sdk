@file:Suppress("SpellCheckingInspection")

package test.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.CATS
import com.altcraft.sdk.data.Constants.CATS_ACTIVE
import com.altcraft.sdk.data.Constants.CATS_NAME
import com.altcraft.sdk.data.Constants.CATS_STEADY
import com.altcraft.sdk.data.Constants.CATS_TITLE
import com.altcraft.sdk.data.Constants.FIELDS
import com.altcraft.sdk.data.Constants.NEW_PROVIDER
import com.altcraft.sdk.data.Constants.NEW_TOKEN
import com.altcraft.sdk.data.Constants.OLD_PROVIDER
import com.altcraft.sdk.data.Constants.OLD_TOKEN
import com.altcraft.sdk.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.data.Constants.PROVIDER
import com.altcraft.sdk.data.Constants.REPLACE
import com.altcraft.sdk.data.Constants.SKIP_TRIGGERS
import com.altcraft.sdk.data.Constants.SMID
import com.altcraft.sdk.data.Constants.STATUS
import com.altcraft.sdk.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.data.Constants.TIME
import com.altcraft.sdk.data.DataClasses.CategoryData
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.DataClasses.SubscribeRequestData
import com.altcraft.sdk.data.DataClasses.TokenUpdateRequestData
import com.altcraft.sdk.data.DataClasses.UnSuspendRequestData
import com.altcraft.sdk.json.JsonFactory
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JsonFactoryTest
 *
 * Positive scenarios:
 * - test_1: createSubscribeJson builds full payload (time, subscription, cats, profile_fields, flags).
 * - test_2: createPushEventJson builds minimal payload (time, smid).
 * - test_3: createTokenUpdateJson includes old/new token and provider.
 * - test_4: createUnSuspendJson wraps subscription and sets replace=true.
 *
 * Edge scenarios:
 * - test_5: createSubscribeJson handles null fields and empty categories (fields/profile_fields become JsonNull).
 * - test_6: createTokenUpdateJson handles null old fields (old_* become JsonNull).
 */
class JsonFactoryTest {

    private fun subscribeReq(
        provider: String = "android-firebase",
        token: String = "tkn",
        status: String = "subscribed",
        time: Long = 123456L,
        profileFields: JsonElement? = null,
        fields: JsonElement? = null,
        cats: List<CategoryData>? = listOf(
            CategoryData("news", "News", true, true),
            CategoryData("sports", "Sports", false, true)
        ),
        replace: Boolean? = true,
        skip: Boolean? = false,
    ) = SubscribeRequestData(
        url = "https://api.example.com/subscribe",
        requestId = "rid-1",
        time = time,
        rToken = "rT",
        authHeader = "Bearer X",
        matchingMode = "device",
        provider = provider,
        deviceToken = token,
        status = status,
        sync = null,
        profileFields = profileFields,
        fields = fields,
        cats = cats,
        replace = replace,
        skipTriggers = skip
    )

    private fun pushReq(uid: String, time: Long) = PushEventRequestData(
        url = "https://api.example.com/event",
        requestId = "req-$uid",
        time = time,
        type = "opened",
        uid = uid,
        authHeader = "Bearer X",
        matchingMode = "device"
    )

    private fun updateReq(
        oldT: String? = "old-t",
        oldP: String? = "android-firebase",
        newT: String = "new-t",
        newP: String = "android-huawei",
    ) = TokenUpdateRequestData(
        url = "https://api.example.com/update",
        requestId = "rid-2",
        oldToken = oldT,
        newToken = newT,
        oldProvider = oldP,
        newProvider = newP,
        authHeader = "Bearer X",
        sync = false
    )

    private fun unSuspendReq(
        provider: String = "android-firebase",
        token: String = "TT",
    ) = UnSuspendRequestData(
        url = "https://api.example.com/unsuspend",
        requestId = "rid-3",
        provider = provider,
        token = token,
        authHeader = "Bearer X",
        matchingMode = "device"
    )

    /** - test_1: createSubscribeJson builds full payload. */
    @Test
    fun createSubscribeJson_buildsFullPayload() {
        val pf = buildJsonObject { put("age", 25) }
        val cf = buildJsonObject { put("vip", true) }

        val req = subscribeReq(profileFields = pf, fields = cf, time = 123456L)
        val obj = JsonFactory.createSubscribeJson(req)

        assertEquals(req.time / 1000, obj[TIME]!!.jsonPrimitive.long)

        val sub = obj[SUBSCRIPTION]!!.jsonObject
        assertEquals(req.deviceToken, sub[SUBSCRIPTION_ID]!!.jsonPrimitive.content)
        assertEquals(req.provider, sub[PROVIDER]!!.jsonPrimitive.content)
        assertEquals(req.status, sub[STATUS]!!.jsonPrimitive.content)
        assertEquals(true, sub[FIELDS]!!.jsonObject["vip"]!!.jsonPrimitive.boolean)

        val cats = sub[CATS]!!.jsonArray
        assertEquals(2, cats.size)

        val c0 = cats[0].jsonObject
        assertEquals("news", c0[CATS_NAME]!!.jsonPrimitive.content)
        assertEquals("News", c0[CATS_TITLE]!!.jsonPrimitive.content)
        assertEquals(true, c0[CATS_STEADY]!!.jsonPrimitive.boolean)
        assertEquals(true, c0[CATS_ACTIVE]!!.jsonPrimitive.boolean)

        assertEquals(25, obj[PROFILE_FIELDS]!!.jsonObject["age"]!!.jsonPrimitive.int)
        assertEquals(true, obj[REPLACE]!!.jsonPrimitive.boolean)
        assertEquals(false, obj[SKIP_TRIGGERS]!!.jsonPrimitive.boolean)
    }

    /** - test_2: createPushEventJson builds minimal payload. */
    @Test
    fun createPushEventJson_minimalPayload() {
        val req = pushReq(uid = "E999", time = 1999L)
        val obj = JsonFactory.createPushEventJson(req) // JsonObject

        assertEquals(req.time / 1000, obj[TIME]!!.jsonPrimitive.long)
        assertEquals(req.uid, obj[SMID]!!.jsonPrimitive.content)
        assertEquals(2, obj.size)
    }

    /** - test_3: createTokenUpdateJson includes old/new token and provider. */
    @Test
    fun createTokenUpdateJson_includesOldNewValues() {
        val req = updateReq(
            oldT = "old-t",
            oldP = "android-firebase",
            newT = "new-t",
            newP = "android-huawei"
        )
        val obj = JsonFactory.createTokenUpdateJson(req) // JsonObject

        assertEquals("old-t", obj[OLD_TOKEN]!!.jsonPrimitive.content)
        assertEquals("android-firebase", obj[OLD_PROVIDER]!!.jsonPrimitive.content)
        assertEquals("new-t", obj[NEW_TOKEN]!!.jsonPrimitive.content)
        assertEquals("android-huawei", obj[NEW_PROVIDER]!!.jsonPrimitive.content)
    }

    /** - test_4: createUnSuspendJson wraps subscription and sets replace=true. */
    @Test
    fun createUnSuspendJson_wrapsSubscription_andSetsReplaceTrue() {
        val req = unSuspendReq(provider = "android-firebase", token = "TT")
        val obj = JsonFactory.createUnSuspendJson(req) // JsonObject
        val sub = obj[SUBSCRIPTION]!!.jsonObject

        assertEquals("TT", sub[SUBSCRIPTION_ID]!!.jsonPrimitive.content)
        assertEquals("android-firebase", sub[PROVIDER]!!.jsonPrimitive.content)
        assertEquals(true, obj[REPLACE]!!.jsonPrimitive.boolean)
    }

    /** - test_5: createSubscribeJson handles null fields and empty categories. */
    @Test
    fun createSubscribeJson_handlesNullsAndEmptyCats() {
        val obj = JsonFactory.createSubscribeJson(
            subscribeReq(
                profileFields = null,
                fields = null,
                cats = emptyList(),
                replace = null,
                skip = null
            )
        )

        assertTrue(obj[PROFILE_FIELDS] is JsonNull)

        val sub = obj[SUBSCRIPTION]!!.jsonObject
        assertTrue(sub[FIELDS] is JsonNull)
        assertTrue(sub[CATS]!!.jsonArray.isEmpty())

        assertEquals(false, obj[REPLACE]!!.jsonPrimitive.boolean)
        assertEquals(false, obj[SKIP_TRIGGERS]!!.jsonPrimitive.boolean)
    }

    /** - test_6: createTokenUpdateJson handles null old fields. */
    @Test
    fun createTokenUpdateJson_handlesNullOldFields() {
        val req = updateReq(oldT = null, oldP = null)
        val obj = JsonFactory.createTokenUpdateJson(req) // JsonObject

        assertTrue(obj[OLD_TOKEN] is JsonNull)
        assertTrue(obj[OLD_PROVIDER] is JsonNull)
        assertEquals(req.newToken, obj[NEW_TOKEN]!!.jsonPrimitive.content)
        assertEquals(req.newProvider, obj[NEW_PROVIDER]!!.jsonPrimitive.content)
    }
}
