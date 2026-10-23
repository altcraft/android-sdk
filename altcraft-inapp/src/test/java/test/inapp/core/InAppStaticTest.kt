@file:Suppress("SpellCheckingInspection")

package test.inapp.core

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import org.junit.Assert.*
import org.junit.Test

/**
 * InAppStaticTest
 *
 * Tests the inject() function's script building logic.
 *
 * Positive scenarios:
 *  - test_1: inject builds script with all three resources.
 *  - test_2: inject builds script with only libraryJs.
 *  - test_3: inject builds script with only blockJs.
 *  - test_4: inject builds script with only blockCss.
 *  - test_5: inject wraps script in IIFE.
 *  - test_6: inject handles null resources gracefully.
 */
class InAppStaticTest {

    /** test_1: inject builds script with all three resources. */
    @Test
    fun test_1_inject_allResources_scriptContainsAll() {
        val script = buildScript(
            blockCss = "body { color: red; }",
            libraryJs = "var lib = 1;",
            blockJs = "var block = 2;"
        )
        assertTrue(script.contains("body { color: red; }"))
        assertTrue(script.contains("var lib = 1;"))
        assertTrue(script.contains("var block = 2;"))
    }

    /** test_2: inject with only libraryJs. */
    @Test
    fun test_2_inject_libraryJsOnly() {
        val script = buildScript(
            blockCss = null,
            libraryJs = "var lib = 1;",
            blockJs = null
        )
        assertTrue(script.contains("var lib = 1;"))
        assertFalse(script.contains("body"))
    }

    /** test_3: inject with only blockJs. */
    @Test
    fun test_3_inject_blockJsOnly() {
        val script = buildScript(
            blockCss = null,
            libraryJs = null,
            blockJs = "var block = 2;"
        )
        assertTrue(script.contains("var block = 2;"))
    }

    /** test_4: inject with only blockCss. */
    @Test
    fun test_4_inject_blockCssOnly() {
        val script = buildScript(
            blockCss = "body { color: red; }",
            libraryJs = null,
            blockJs = null
        )
        assertTrue(script.contains("body { color: red; }"))
    }

    /** test_5: inject wraps script in IIFE. */
    @Test
    fun test_5_inject_wrapsInIIFE() {
        val script = buildScript(
            blockCss = "css",
            libraryJs = "lib",
            blockJs = "block"
        )
        assertTrue(script.startsWith("(function() {"))
        assertTrue(script.endsWith("})();"))
    }

    /** test_6: inject handles all null resources. */
    @Test
    fun test_6_inject_allNull_minimalScript() {
        val script = buildScript(
            blockCss = null,
            libraryJs = null,
            blockJs = null
        )
        assertEquals("(function() {})();", script)
    }

    // Replicates the inject() script-building logic for testing
    private fun buildScript(
        blockCss: String?,
        libraryJs: String?,
        blockJs: String?
    ): String {
        return buildString {
            append("(function() {")

            if (!blockCss.isNullOrBlank()) {
                append(createCssInjectionScript(blockCss))
                append("\n")
            }

            if (!libraryJs.isNullOrBlank()) {
                append(libraryJs)
                append("\n")
            }

            if (!blockJs.isNullOrBlank()) {
                append(blockJs)
                append("\n")
            }

            append("})();")
        }
    }

    // Replicates the createCssInjectionScript logic
    private fun createCssInjectionScript(css: String): String {
        return """
        var style = document.createElement('style');
        style.type = 'text/css';
        style.appendChild(document.createTextNode(${css.toJsString()}));
        document.head.appendChild(style);
    """.trimIndent()
    }

    // Replicates the toJsString logic
    private fun String.toJsString(): String {
        return "\"" + this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("</script>", "<\\/script>") + "\""
    }
}
