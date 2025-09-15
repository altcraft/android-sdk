package test.json

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants.SUBSCRIPTION_ID
import com.altcraft.sdk.data.Constants.PROFILE_FIELDS
import com.altcraft.sdk.data.Constants.SUBSCRIPTION
import com.altcraft.sdk.data.Constants.PROVIDER
import com.altcraft.sdk.data.Constants.FIELDS
import com.altcraft.sdk.data.Constants.CATS
import com.altcraft.sdk.data.Constants.STATUS
import com.altcraft.sdk.data.Constants.TIME
import com.altcraft.sdk.data.Constants.REPLACE
import com.altcraft.sdk.data.Constants.SKIP_TRIGGERS
import com.altcraft.sdk.data.Constants.CATS_NAME
import com.altcraft.sdk.data.Constants.CATS_ACTIVE
import com.altcraft.sdk.data.Constants.CATS_TITLE
import com.altcraft.sdk.data.Constants.CATS_STEADY
import com.altcraft.sdk.data.Constants.SMID
import com.altcraft.sdk.data.Constants.OLD_TOKEN
import com.altcraft.sdk.data.Constants.OLD_PROVIDER
import com.altcraft.sdk.data.Constants.NEW_TOKEN
import com.altcraft.sdk.data.Constants.NEW_PROVIDER
import com.altcraft.sdk.data.DataClasses.CategoryData
import com.altcraft.sdk.data.DataClasses.PushEventRequestData
import com.altcraft.sdk.data.DataClasses.SubscribeRequestData
import com.altcraft.sdk.data.DataClasses.UnSuspendRequestData
import com.altcraft.sdk.data.DataClasses.UpdateRequestData
import com.altcraft.sdk.json.JsonFactory
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/**
 * JsonFactoryUnitTest
 *
 * Positive:
 *  - test_1: createSubscribeJson builds full payload (fields, cats, booleans).
 *  - test_2: createPushEventJson builds minimal payload (time + smid).
 *  - test_3: createUpdateJson includes old/new token/provider.
 *  - test_4: createUnSuspendJson wraps subscription and sets replace=true.
 *
 * Edge:
 *  - test_5: createSubscribeJson handles null profile/fields and empty cats.
 *  - test_6: createUpdateJson handles null oldToken/oldProvider (new* must be non-null).
 *
 * Notes:
 *  - Pure JVM unit tests (no Android runtime).
 *  - Uses kotlinx.serialization JsonObject.
 */
class JsonFactoryTest {

    // ---------- Helpers ----------

    private fun subscribeReq(
        provider: String = "android-firebase",
        token: String = "tkn",
        status: String = "subscribed",
        time: Long = 123456789L,
        profileFields: JsonElement? = null,
        fields: JsonElement? = null,
        cats: List<CategoryData>? = listOf(
            CategoryData(name = "news", title = "News", steady = true,  active = true),
            CategoryData(name = "sports", title = "Sports", steady = false, active = true),
        ),
        replace: Boolean? = true,
        skip: Boolean? = false,
        rToken: String? = "rT",
        auth: String = "Bearer X",
        matching: String = "device"
    ) = SubscribeRequestData(
        url = "https://api.example.com/subscribe",
        time = time,
        rToken = rToken,
        uid = "uid-1",
        authHeader = auth,
        matchingMode = matching,
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

    private fun pushReq(
        uid: String = "ev-1",
        time: Long = 42L,
        type: String = "opened",
        auth: String = "Bearer X",
        matching: String = "device"
    ) = PushEventRequestData(
        url = "https://api.example.com/event",
        time = time,
        type = type,
        uid = uid,
        authHeader = auth,
        matchingMode = matching
    )

    private fun updateReq(
        oldT: String? = "old-t",
        oldP: String? = "android-firebase",
        newT: String = "new-t",
        newP: String = "android-huawei",
        auth: String = "Bearer X"
    ) = UpdateRequestData(
        url = "https://api.example.com/update",
        uid = "uid-2",
        oldToken = oldT,
        newToken = newT,
        oldProvider = oldP,
        newProvider = newP,
        authHeader = auth
    )

    private fun unSuspendReq(
        provider: String = "android-firebase",
        token: String = "tkn",
        auth: String = "Bearer X",
        matching: String = "device"
    ) = UnSuspendRequestData(
        url = "https://api.example.com/unsuspend",
        uid = "uid-3",
        provider = provider,
        token = token,
        authHeader = auth,
        matchingMode = matching
    )

    // ---------- Tests ----------

    /** Verifies subscribe JSON structure with fields/cats/booleans */
    @Test
    fun createSubscribeJson_buildsFullPayload() {
        val pf = buildJsonObject { put("age", JsonPrimitive(25)) }
        val cf = buildJsonObject { put("vip", JsonPrimitive(true)) }

        val json = JsonFactory.createSubscribeJson(
            subscribeReq(profileFields = pf, fields = cf)
        )

        val obj = json.jsonObject
        assertEquals(123456789L, obj[TIME]?.jsonPrimitive?.long)
        assertEquals("tkn", obj[SUBSCRIPTION_ID]?.jsonPrimitive?.content)

        val sub = obj[SUBSCRIPTION]?.jsonObject!!
        assertEquals("tkn", sub[SUBSCRIPTION_ID]?.jsonPrimitive?.content)
        assertEquals("android-firebase", sub[PROVIDER]?.jsonPrimitive?.content)
        assertEquals("subscribed", sub[STATUS]?.jsonPrimitive?.content)
        assertEquals(true, sub[FIELDS]?.jsonObject?.get("vip")?.jsonPrimitive?.boolean)

        val cats = sub[CATS]?.jsonArray!!
        assertEquals(2, cats.size)
        val c0 = cats[0].jsonObject
        assertEquals("news", c0[CATS_NAME]?.jsonPrimitive?.content)
        assertEquals("News", c0[CATS_TITLE]?.jsonPrimitive?.content)
        assertEquals(true,  c0[CATS_STEADY]?.jsonPrimitive?.boolean)
        assertEquals(true,  c0[CATS_ACTIVE]?.jsonPrimitive?.boolean)

        assertEquals(25, obj[PROFILE_FIELDS]?.jsonObject?.get("age")?.jsonPrimitive?.int)
        assertEquals(true,  obj[REPLACE]?.jsonPrimitive?.boolean)
        assertEquals(false, obj[SKIP_TRIGGERS]?.jsonPrimitive?.boolean)
    }

    /** Verifies minimal push event JSON (TIME + SMID) */
    @Test
    fun createPushEventJson_minimalPayload() {
        val json = JsonFactory.createPushEventJson(pushReq(uid = "E123", time = 999L))
        val obj = json.jsonObject

        assertEquals(999L, obj[TIME]?.jsonPrimitive?.long)
        assertEquals("E123", obj[SMID]?.jsonPrimitive?.content)
        assertEquals(2, obj.size)
    }

    /** Verifies update JSON contains old/new token/provider */
    @Test
    fun createUpdateJson_includesOldNewValues() {
        val json = JsonFactory.createUpdateJson(
            updateReq(oldT = "ot", oldP = "android-firebase", newT = "nt", newP = "android-huawei")
        )
        val obj = json.jsonObject

        assertEquals("ot", obj[OLD_TOKEN]?.jsonPrimitive?.content)
        assertEquals("android-firebase", obj[OLD_PROVIDER]?.jsonPrimitive?.content)
        assertEquals("nt", obj[NEW_TOKEN]?.jsonPrimitive?.content)
        assertEquals("android-huawei", obj[NEW_PROVIDER]?.jsonPrimitive?.content)
    }

    /** Verifies unSuspend JSON wraps subscription and sets replace=true */
    @Test
    fun createUnSuspendJson_wrapsSubscription_andSetsReplaceTrue() {
        val json = JsonFactory.createUnSuspendJson(unSuspendReq(provider = "android-firebase", token = "TT"))
        val obj = json.jsonObject

        val sub = obj[SUBSCRIPTION]?.jsonObject!!
        assertEquals("TT", sub[SUBSCRIPTION_ID]?.jsonPrimitive?.content)
        assertEquals("android-firebase", sub[PROVIDER]?.jsonPrimitive?.content)

        assertEquals(true, obj[REPLACE]?.jsonPrimitive?.boolean)
    }

    /** Verifies subscribe JSON with null profile/fields and empty cats */
    @Test
    fun createSubscribeJson_handlesNullsAndEmptyCats() {
        val json = JsonFactory.createSubscribeJson(
            subscribeReq(
                profileFields = null,
                fields = null,
                cats = emptyList(),
                replace = null,
                skip = null
            )
        )

        val obj = json.jsonObject
        assertTrue(obj[PROFILE_FIELDS] is JsonNull)

        val sub = obj[SUBSCRIPTION]?.jsonObject!!
        assertTrue(sub[FIELDS] is JsonNull)

        val cats = sub[CATS]?.jsonArray!!
        assertTrue(cats.isEmpty())

        assertEquals(false, obj[REPLACE]?.jsonPrimitive?.boolean)
        assertEquals(false, obj[SKIP_TRIGGERS]?.jsonPrimitive?.boolean)
    }

    /** Verifies update JSON handles nullable oldToken/oldProvider (new* must be non-null by model) */
    @Test
    fun createUpdateJson_handlesNullOldFields() {
        val json = JsonFactory.createUpdateJson(
            updateReq(oldT = null, oldP = null, newT = "nt", newP = "android-huawei")
        )
        val obj = json.jsonObject

        assertTrue(obj[OLD_TOKEN]    is JsonNull)
        assertTrue(obj[OLD_PROVIDER] is JsonNull)
        assertEquals("nt", obj[NEW_TOKEN]?.jsonPrimitive?.content)
        assertEquals("android-huawei", obj[NEW_PROVIDER]?.jsonPrimitive?.content)
    }
}
