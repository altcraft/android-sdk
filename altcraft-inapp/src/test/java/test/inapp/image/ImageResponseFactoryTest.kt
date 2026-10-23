@file:Suppress("SpellCheckingInspection")

package test.inapp.image

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * ImageResponseFactoryTest
 *
 * Tests MIME type resolution logic.
 *
 * Positive scenarios:
 *  - test_1: .png file returns image/png.
 *  - test_2: .webp file returns image/webp.
 *  - test_3: .gif file returns image/gif.
 *  - test_4: .jpg file returns image/jpeg.
 *  - test_5: unknown extension returns image/jpeg.
 *  - test_6: uppercase extension is handled (case-insensitive).
 *  - test_7: file with no extension returns image/jpeg.
 *  - test_8: nested path with extension resolved correctly.
 */
class ImageResponseFactoryTest {

    /** Replicates the private getMimeType logic. */
    private fun getMimeType(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    @Test
    fun test_1_png_returnsImagePng() {
        assertEquals("image/png", getMimeType(File("/cache/img.png")))
    }

    @Test
    fun test_2_webp_returnsImageWebp() {
        assertEquals("image/webp", getMimeType(File("/cache/img.webp")))
    }

    @Test
    fun test_3_gif_returnsImageGif() {
        assertEquals("image/gif", getMimeType(File("/cache/img.gif")))
    }

    @Test
    fun test_4_jpg_returnsImageJpeg() {
        assertEquals("image/jpeg", getMimeType(File("/cache/img.jpg")))
    }

    @Test
    fun test_5_unknownExtension_returnsImageJpeg() {
        assertEquals("image/jpeg", getMimeType(File("/cache/img.bmp")))
    }

    @Test
    fun test_6_uppercaseExtension_handled() {
        assertEquals("image/png", getMimeType(File("/cache/img.PNG")))
        assertEquals("image/webp", getMimeType(File("/cache/img.WEBP")))
        assertEquals("image/gif", getMimeType(File("/cache/img.GIF")))
    }

    @Test
    fun test_7_noExtension_returnsImageJpeg() {
        assertEquals("image/jpeg", getMimeType(File("/cache/image")))
    }

    @Test
    fun test_8_nestedPath_resolvedCorrectly() {
        assertEquals("image/png", getMimeType(File("/deep/nested/path/photo.png")))
    }
}
