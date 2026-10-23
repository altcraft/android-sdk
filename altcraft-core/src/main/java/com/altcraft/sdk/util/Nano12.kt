package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.data.Constants
import java.security.SecureRandom

/**
 * Generates 12-character random IDs using a secure random source.
 */
internal object Nano12 {
    private const val SIZE = 12
    private const val MASK = 63
    private val random = SecureRandom()
    private val alphabet = Constants.ALPHABET.toCharArray()

    /**
     * Returns a random 12-character string from the configured alphabet.
     */
    fun getNano12(): String {
        val out = CharArray(SIZE)
        val bytes = ByteArray(SIZE)
        random.nextBytes(bytes)
        for (i in 0 until SIZE) { out[i] = alphabet[bytes[i].toInt() and MASK] }
        return String(out)
    }
}