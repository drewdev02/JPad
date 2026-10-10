package org.andrew.jpad.ui.components.editor

import org.andrew.jpad.ui.theme.JPadColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlSyntaxHighlighterTest {

    private val colors = JPadColorScheme()

    @Test
    fun `highlights sql keywords case-insensitively`() {
        val code = "SELECT id, name FROM customers WHERE id = 1 ORDER BY id DESC"
        val annotated = SqlSyntaxHighlighter.highlight(code, colors)

        val keywordSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxKeyword }
        assertTrue(keywordSpans.isNotEmpty())

        val words = keywordSpans.map { code.substring(it.start, it.end).uppercase() }
        assertTrue("SELECT" in words)
        assertTrue("FROM" in words)
        assertTrue("WHERE" in words)
        assertTrue("ORDER" in words)
        assertTrue("BY" in words)
        assertTrue("DESC" in words)
    }

    @Test
    fun `highlights sql comments and ignores keywords inside`() {
        val code = "-- SELECT * FROM ignored\nSELECT 1;"
        val annotated = SqlSyntaxHighlighter.highlight(code, colors)

        val commentSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxComment }
        assertEquals(1, commentSpans.size)
        assertEquals("-- SELECT * FROM ignored", code.substring(commentSpans[0].start, commentSpans[0].end))
    }

    @Test
    fun `highlights sql string literals and handles escaped quotes`() {
        val code = "SELECT 'O''Reilly' AS author;"
        val annotated = SqlSyntaxHighlighter.highlight(code, colors)

        val stringSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxString }
        assertEquals(1, stringSpans.size)
        assertEquals("'O''Reilly'", code.substring(stringSpans[0].start, stringSpans[0].end))
    }

    @Test
    fun `highlights sql functions and types`() {
        val code = "SELECT COUNT(id), CAST(amount AS NUMERIC) FROM orders;"
        val annotated = SqlSyntaxHighlighter.highlight(code, colors)

        val funcSpans = annotated.spanStyles.filter { it.item.color == colors.accentPrimary }
        assertTrue(funcSpans.any { code.substring(it.start, it.end).equals("COUNT", ignoreCase = true) })

        val typeSpans = annotated.spanStyles.filter { it.item.color == colors.syntaxType }
        assertTrue(typeSpans.any { code.substring(it.start, it.end).equals("NUMERIC", ignoreCase = true) })
    }
}
