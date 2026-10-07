package org.andrew.jpad.core

import org.andrew.jpad.core.compiler.DiagnosticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionEngineTest {

    private val engine = ExecutionEngine()

    @Test
    fun `executes raw statements end to end and returns formatted output`() {
        val script = """
            int a = 10;
            int b = 20;
            System.out.println("Result: " + (a + b));
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess)
        assertTrue(response.output.contains("Result: 30"))
        assertEquals("", response.error)
        assertTrue(response.diagnostics.isEmpty())
    }

    @Test
    fun `returns compilation error diagnostics when code is invalid`() {
        val script = "int a = ;"

        val response = engine.execute(script)

        assertFalse(response.isSuccess)
        assertTrue(response.diagnostics.any { it.kind == DiagnosticKind.ERROR })
        assertTrue(response.error.contains("Compilation failed"))
    }
}
