package com.altcraft.sdk.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.annotation.RestrictTo

/**
 * Utility for checking whether the current application version matches
 * at least one target version by the specified operator.
 *
 * Version parsing rules:
 * - text prefix before the first digit is ignored, for example `v1.2.3`;
 * - only the part before the first space is used;
 * - version parts are separated by dots;
 * - only the first three numeric parts are compared;
 * - everything after `-` inside a version part is ignored;
 * - parsing stops when a part cannot be parsed as a number;
 * - missing version parts are treated as zeroes;
 * - zero in the last raw version part is ignored.
 *
 * Examples:
 * - `v1.2.3` becomes `[1, 2, 3]`;
 * - `version 1.2.0` becomes `[1, 2]`;
 * - `v4.3.1.1.213361` becomes `[4, 3, 1]`;
 * - `1.2.3-beta` becomes `[1, 2, 3]`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AppVersionMatcher {

    /**
     * Supported version comparison operators.
     */
    enum class Operator(val value: String) {
        EQUAL("equal"),
        NOT_EQUAL("not_equal"),
        GREATER_THAN("greater_than"),
        LESS_THAN("less_than"),
        GREATER_EQUAL("greater_e"),
        LESS_EQUAL("less_e");

        companion object {
            /** Returns the operator matching the raw value, or `null` if unknown. */
            fun fromValue(value: String?): Operator? {
                return entries.firstOrNull {
                    it.value.equals(value, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Returns `true` when [appVersion] matches at least one version
     * from [appVersions] using the specified [operator].
     *
     * Example:
     *
     * ```kotlin
     * AppVersionMatcher.matches(
     *     appVersion = "v1.2.4",
     *     appVersions = listOf("v1.2.3"),
     *     operator = AppVersionMatcher.Operator.GREATER_THAN
     * )
     * ```
     *
     * In this example the result is `true`, because `1.2.4 > 1.2.3`.
     */
    fun matches(
        appVersion: String,
        appVersions: List<String>,
        operator: Operator
    ): Boolean {
        /** Splits the raw version string into numeric components. */
        fun parse(rawVersion: String): List<Int> {
            val firstDigitIndex = rawVersion.indexOfFirst { it.isDigit() }
            if (firstDigitIndex == -1) return emptyList()

            val raw = rawVersion
                .substring(firstDigitIndex)
                .substringBefore(" ")

            val result = mutableListOf<Int>()

            for ((index, part) in raw.split(".").withIndex()) {
                if (index >= 3) break

                val numericPart = part
                    .substringBefore("-")
                    .takeWhile { it.isDigit() }

                if (numericPart.isEmpty()) break

                val number = numericPart.toIntOrNull() ?: break

                if (index == raw.split(".").lastIndex && number == 0) break

                result.add(number)
            }

            return result
        }

        /** Compares two raw version strings component by component. */
        fun compare(leftRaw: String, rightRaw: String): Int {
            val left = parse(leftRaw).toMutableList()
            val right = parse(rightRaw).toMutableList()

            while (left.size < 3) left.add(0)
            while (right.size < 3) right.add(0)

            for (i in 0 until 3) {
                if (left[i] > right[i]) return 1
                if (left[i] < right[i]) return -1
            }

            return 0
        }

        return appVersions.any { version ->
            val result = compare(appVersion, version)

            when (operator) {
                Operator.EQUAL -> result == 0
                Operator.NOT_EQUAL -> result != 0
                Operator.GREATER_THAN -> result > 0
                Operator.LESS_THAN -> result < 0
                Operator.GREATER_EQUAL -> result >= 0
                Operator.LESS_EQUAL -> result <= 0
            }
        }
    }
}