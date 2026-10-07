package org.andrew.jpad.ui.components.editor

import androidx.compose.ui.text.SpanStyle
import org.andrew.jpad.ui.theme.JPadColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaSyntaxHighlighterTest {

    private val colors = JPadColorScheme()

    @Test
    fun `highlights java keywords`() {
        val code = "public class MyService { void execute() { return; } }"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val keywordSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxKeyword }
        assertTrue(keywordSpans.isNotEmpty(), "Should highlight keywords")
        
        // "public", "class", "void", "return" should be keywords
        val highlightedWords = keywordSpans.map { code.substring(it.start, it.end) }
        assertTrue(highlightedWords.contains("public"))
        assertTrue(highlightedWords.contains("class"))
        assertTrue(highlightedWords.contains("void"))
        assertTrue(highlightedWords.contains("return"))
    }

    @Test
    fun `highlights string literals and escape characters`() {
        val code = "String msg = \"Hello \\\"World\\\"\";"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val stringSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxString }
        assertEquals(1, stringSpans.size)
        assertEquals("\"Hello \\\"World\\\"\"", code.substring(stringSpans[0].start, stringSpans[0].end))
    }

    @Test
    fun `highlights single line comments and ignores keywords inside`() {
        val code = "// public class Ignored\nint x = 10;"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val commentSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxComment }
        assertEquals(1, commentSpans.size)
        assertEquals("// public class Ignored", code.substring(commentSpans[0].start, commentSpans[0].end))

        // "public" inside the comment should NOT be marked as keyword
        val keywordSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxKeyword }
        val highlightedWords = keywordSpans.map { code.substring(it.start, it.end) }
        assertTrue(!highlightedWords.contains("class"))
    }

    @Test
    fun `highlights multi-line block comments`() {
        val code = "/* multi-line\n * class Comment\n */\nint a = 5;"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val commentSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxComment }
        assertEquals(1, commentSpans.size)
        assertTrue(code.substring(commentSpans[0].start, commentSpans[0].end).startsWith("/*"))
    }

    @Test
    fun `highlights numbers including hex and decimals`() {
        val code = "int a = 123; long b = 0xFFL; double c = 3.14159;"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val numberSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxNumber }
        assertTrue(numberSpans.size >= 3)
    }

    @Test
    fun `highlights types and classes`() {
        val code = "String name = \"test\"; List<Integer> numbers = null;"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val typeSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxType }
        val highlightedTypes = typeSpans.map { code.substring(it.start, it.end) }
        assertTrue(highlightedTypes.contains("String"))
        assertTrue(highlightedTypes.contains("List"))
        assertTrue(highlightedTypes.contains("Integer"))
    }

    @Test
    fun `highlights null and booleans`() {
        val code = "boolean flag = true; Object obj = null;"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val boolSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxBoolean }
        val nullSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxNull }
        
        assertTrue(boolSpans.any { code.substring(it.start, it.end) == "true" })
        assertTrue(nullSpans.any { code.substring(it.start, it.end) == "null" })
    }

    @Test
    fun `highlights annotations`() {
        val code = "@Override\npublic void run() {}"
        val annotated = JavaSyntaxHighlighter.highlight(code, colors)

        val keywordOrTypeSpans = annotated.spanStyles.filter { 
            it.item.color == colors.syntaxKeyword || it.item.color == colors.syntaxType 
        }
        val annotatedTokens = keywordOrTypeSpans.map { code.substring(it.start, it.end) }
        assertTrue(annotatedTokens.any { it == "@Override" || it == "Override" })
    }

    @Test
    fun `calculates cursor position correctly for single line and multi-line`() {
        val text = "public class Main {\n    int x = 10;\n}"
        
        // At start (offset 0) -> Ln 1, Col 1
        assertEquals(Pair(1, 1), calculateCursorPosition(text, 0))
        
        // After "public " (offset 7) -> Ln 1, Col 8
        assertEquals(Pair(1, 8), calculateCursorPosition(text, 7))
        
        // On second line after newline (offset 20) -> Ln 2, Col 1
        assertEquals(Pair(2, 1), calculateCursorPosition(text, 20))
        
        // On second line after "    int" (offset 27) -> Ln 2, Col 8
        assertEquals(Pair(2, 8), calculateCursorPosition(text, 27))
    }
}
