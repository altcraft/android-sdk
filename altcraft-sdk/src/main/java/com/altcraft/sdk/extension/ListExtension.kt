package com.altcraft.sdk.extension

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Convenience extensions for assembling multipart/form-data with OkHttp.
 *
 * Provides helpers to append text and JSON fields to a mutable list of [MultipartBody.Part],
 * applying consistent media types and avoiding repetitive boilerplate.
 */
object ListExtension {

    private val TEXT = "text/plain; charset=utf-8".toMediaType()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Appends a simple text form field using `text/plain; charset=utf-8`.
     *
     * The part is created with a null filename, so it is treated as a regular form field
     * (not a file upload).
     *
     * @receiver the target list of multipart parts
     * @param name form field name
     * @param value text value to send
     */
    internal fun MutableList<MultipartBody.Part>.addTextPart(name: String, value: String?) {
        value?.let {
            add(MultipartBody.Part.createFormData(name, null, value.toRequestBody(TEXT)))
        }
    }

    /**
     * Appends a JSON form field using `application/json; charset=utf-8` when [jsonStr] is not null.
     *
     * The JSON string is sent as-is without validation or parsing. If you require strict JSON,
     * validate/serialize before calling this function.
     *
     * @receiver the target list of multipart parts
     * @param name form field name
     * @param jsonStr raw JSON string; if null, the part is skipped
     */
    internal fun MutableList<MultipartBody.Part>.addJsonPart(name: String, jsonStr: String?) {
        jsonStr?.let {
            add(MultipartBody.Part.createFormData(name, null, it.toRequestBody(JSON)))
        }
    }
}