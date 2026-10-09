package org.andrew.jpad.core.completion

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JShellCompletionServiceTest {

    private lateinit var service: JShellCompletionService

    @BeforeTest
    fun setUp() {
        service = JShellCompletionService()
    }

    @AfterTest
    fun tearDown() {
        service.close()
    }

    @Test
    fun `provides live templates when typing matching snippet prefixes`() {
        val result = service.getCompletions("sou", 3)
        val soutTemplate = result.items.firstOrNull { it.lookupString == "sout" }
        assertTrue(soutTemplate != null, "Expected sout template in completion items")
        assertEquals(LookupElementKind.SNIPPET, soutTemplate?.kind)
        assertEquals("System.out.println();", soutTemplate?.insertText)
        assertEquals(-2, soutTemplate?.cursorDelta)
    }

    @Test
    fun `provides semantic completions for Java standard library methods`() {
        val code = "System.out.pr"
        val result = service.getCompletions(code, code.length)

        assertEquals(11, result.anchor) // Start of "pr"
        assertEquals("pr", result.prefix)

        val printlnItem = result.items.firstOrNull { it.lookupString == "println" }
        assertTrue(printlnItem != null, "Expected println in completion items")
        assertEquals(LookupElementKind.METHOD, printlnItem?.kind)
        assertEquals("println()", printlnItem?.insertText)
        assertEquals(-1, printlnItem?.cursorDelta)
    }

    @Test
    fun `provides type inference completions across multiple statements`() {
        val code = "String message = \"hello\";\nmessage.l"
        val result = service.getCompletions(code, code.length)

        val lengthItem = result.items.firstOrNull { it.lookupString == "length" }
        assertTrue(lengthItem != null, "Expected length() in completion items for String variable")
        assertEquals(LookupElementKind.METHOD, lengthItem?.kind)
    }

    @Test
    fun `provides JPad dump function in suggestions`() {
        val code = "dum"
        val result = service.getCompletions(code, code.length)

        val dumpItem = result.items.firstOrNull { it.lookupString == "dump" }
        assertTrue(dumpItem != null, "Expected dump in completion items")
    }

    @Test
    fun `handles empty or invalid offset gracefully`() {
        val emptyResult = service.getCompletions("", 0)
        assertTrue(emptyResult.anchor == 0)

        val outOfBoundsResult = service.getCompletions("test", 999)
        assertTrue(outOfBoundsResult.anchor <= 4)
    }
}
