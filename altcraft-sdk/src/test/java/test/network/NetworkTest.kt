package test.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.sdk.network.Network
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type

/**
 * NullOnEmptyConverterFactoryTest
 *
 * Positive:
 *  - test_1: Empty body -> null
 *  - test_2: Non-empty body -> delegated to ScalarsConverterFactory
 */
class NullOnEmptyConverterFactoryTest {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://example.com/")
        .addConverterFactory(Network.NullOnEmptyConverterFactory())
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    private val stringType: Type = String::class.java
    private val plain = "text/plain".toMediaType()

    /** test_1: empty body -> null */
    @Test
    fun emptyBody_returnsNull() {
        val converter: Converter<ResponseBody, String> =
            retrofit.responseBodyConverter(stringType, emptyArray())

        val body: ResponseBody = ByteArray(0).toResponseBody(plain)
        val result: String? = converter.convert(body)

        assertNull(result)
    }

    /** test_2 Non-empty body -> delegated to ScalarsConverterFactory */
    @Test
    fun nonEmptyBody_delegatesToScalars() {
        val converter: Converter<ResponseBody, String> =
            retrofit.responseBodyConverter(stringType, emptyArray())

        val body: ResponseBody = "hello".toByteArray().toResponseBody(plain)
        val result: String? = converter.convert(body)

        assertEquals("hello", result)
    }
}

