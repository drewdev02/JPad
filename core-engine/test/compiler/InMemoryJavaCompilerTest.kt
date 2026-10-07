package org.andrew.jpad.core.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryJavaCompilerTest {

    private val compiler: JavaCompilerService = InMemoryJavaCompiler()

    @Test
    fun `compiles valid simple java class successfully`() {
        val source = JavaSource(
            className = "HelloWorld",
            code = """
                public class HelloWorld {
                    public static String getGreeting() {
                        return "Hello from JPad!";
                    }
                }
            """.trimIndent()
        )

        val result = compiler.compile(listOf(source))

        assertTrue(result.isSuccess, "Compilation should succeed")
        assertTrue(result.compiledClasses.containsKey("HelloWorld"), "Should contain compiled HelloWorld bytecode")
        assertTrue(result.compiledClasses["HelloWorld"]!!.isNotEmpty(), "Bytecode should not be empty")
        assertTrue(result.diagnostics.none { it.kind == DiagnosticKind.ERROR }, "There should be no error diagnostics")
    }

    @Test
    fun `captures syntax errors with line and column information`() {
        val invalidSource = JavaSource(
            className = "BrokenClass",
            code = """
                public class BrokenClass {
                    public static void test() {
                        int x = "incompatible type";
                    }
                }
            """.trimIndent()
        )

        val result = compiler.compile(listOf(invalidSource))

        assertFalse(result.isSuccess, "Compilation should fail")
        val errors = result.diagnostics.filter { it.kind == DiagnosticKind.ERROR }
        assertTrue(errors.isNotEmpty(), "Should contain error diagnostics")
        assertEquals(3, errors.first().line, "Error should point to line 3")
    }
}
