package com.altcraft.sdk.network

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.network.json.Converter.json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Configures and provides the Retrofit API client.
 */
internal object NetworkClient {

    /**
     * Retrofit converter that returns `null` for empty response bodies.
     */
    internal class NullOnEmptyConverterFactory : Converter.Factory() {

        /**
         * Returns a converter that handles empty response bodies as null.
         */
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter { if (it.contentLength() == 0L) null else delegate.convert(it) }
        }
    }

    /**
     * Configured OkHttpClient with timeouts and logging.
     */
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    /** Process-wide Retrofit API instance. Built once on first access. */
    internal val api: Api by lazy { buildApi() }

    /**
     * Creates and returns a configured Retrofit API instance.
     */
    private fun buildApi(): Api {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://altcraft.com/")
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(client)
            .build()
            .create(Api::class.java)
    }
}