package org.andrew.jpad.core.parser

import org.andrew.jpad.core.compiler.JavaSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnippetWrapperTest {

    private val wrapper = SnippetWrapper()

    @Test
    fun `wraps statements into a runnable class with main method`() {
        val userCode = """
            import java.util.List;
            
            List<String> items = List.of("A", "B");
            System.out.println(items.size());
        """.trimIndent()

        val parsed = wrapper.wrap(userCode, className = "JPadScript")

        assertEquals(SnippetMode.STATEMENTS, parsed.mode)
        assertEquals("JPadScript", parsed.className)
        assertTrue(parsed.generatedSource.code.contains("import java.util.List;"))
        assertTrue(parsed.generatedSource.code.contains("public class JPadScript"))
        assertTrue(parsed.generatedSource.code.contains("public static void main(String[] args) throws Throwable"))
        assertTrue(parsed.generatedSource.code.contains("List<String> items = List.of(\"A\", \"B\");"))
    }

    @Test
    fun `detects full class program without wrapping into another class`() {
        val userCode = """
            package my.custom.pkg;
            
            public class CustomApp {
                public static void main(String[] args) {
                    System.out.println("Custom");
                }
            }
        """.trimIndent()

        val parsed = wrapper.wrap(userCode)

        assertEquals(SnippetMode.PROGRAM, parsed.mode)
        assertEquals("CustomApp", parsed.className)
        assertEquals("my.custom.pkg.CustomApp", parsed.fullClassName)
        assertEquals(userCode, parsed.generatedSource.code)
    }
}
