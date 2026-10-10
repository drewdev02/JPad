package org.andrew.jpad.ui.components.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.andrew.jpad.ui.theme.JPadColorScheme

object SqlSyntaxHighlighter {

    private val KEYWORDS = setOf(
        "select", "from", "where", "join", "inner", "left", "right", "full", "cross",
        "on", "group", "order", "by", "having", "limit", "offset", "insert", "into",
        "values", "update", "set", "delete", "create", "table", "drop", "alter",
        "view", "index", "primary", "foreign", "key", "references", "not", "null",
        "and", "or", "as", "distinct", "union", "all", "in", "between", "like",
        "is", "exists", "case", "when", "then", "else", "end", "asc", "desc",
        "default", "check", "unique", "constraint", "with"
    )

    private val TYPES = setOf(
        "int", "integer", "bigint", "smallint", "tinyint", "varchar", "char",
        "text", "real", "float", "double", "decimal", "numeric", "boolean",
        "date", "time", "timestamp", "blob", "json"
    )

    private val FUNCTIONS = setOf(
        "count", "sum", "avg", "min", "max", "coalesce", "lower", "upper",
        "trim", "length", "now", "round", "abs", "concat"
    )

    private val BOOLEANS = setOf("true", "false")
    private const val NULL = "null"

    fun highlight(code: String, colors: JPadColorScheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val len = code.length
        var i = 0

        while (i < len) {
            val c = code[i]

            // 1. Line comment --
            if (c == '-' && i + 1 < len && code[i + 1] == '-') {
                val start = i
                i += 2
                while (i < len && code[i] != '\n') {
                    i++
                }
                builder.addStyle(
                    SpanStyle(color = colors.syntaxComment, fontStyle = FontStyle.Italic),
                    start,
                    i
                )
                continue
            }

            // 2. Block comment /* ... */
            if (c == '/' && i + 1 < len && code[i + 1] == '*') {
                val start = i
                i += 2
                while (i < len && !(code[i - 1] == '*' && code[i] == '/')) {
                    i++
                }
                if (i < len) i++ // include trailing slash
                builder.addStyle(
                    SpanStyle(color = colors.syntaxComment, fontStyle = FontStyle.Italic),
                    start,
                    i
                )
                continue
            }

            // 3. String literal '...'
            if (c == '\'') {
                val start = i
                i++
                while (i < len) {
                    if (code[i] == '\'' && i + 1 < len && code[i + 1] == '\'') {
                        i += 2 // Escaped single quote in SQL ''
                    } else if (code[i] == '\'') {
                        i++
                        break
                    } else {
                        i++
                    }
                }
                builder.addStyle(SpanStyle(color = colors.syntaxString), start, i)
                continue
            }

            // 4. Double-quoted or backtick identifier "..." / `...`
            if (c == '"' || c == '`') {
                val quote = c
                val start = i
                i++
                while (i < len && code[i] != quote) {
                    i++
                }
                if (i < len) i++
                builder.addStyle(SpanStyle(color = colors.textPrimary), start, i)
                continue
            }

            // 5. Number literal
            if (c.isDigit() || (c == '.' && i + 1 < len && code[i + 1].isDigit())) {
                val start = i
                while (i < len && (code[i].isDigit() || code[i] == '.')) {
                    i++
                }
                builder.addStyle(SpanStyle(color = colors.syntaxNumber), start, i)
                continue
            }

            // 6. Words (Keywords, Types, Functions, Booleans, Null)
            if (isIdentifierStart(c)) {
                val start = i
                while (i < len && isIdentifierPart(code[i])) {
                    i++
                }
                val word = code.substring(start, i)
                val lower = word.lowercase()

                when {
                    lower in KEYWORDS -> builder.addStyle(
                        SpanStyle(color = colors.syntaxKeyword, fontWeight = FontWeight.Bold),
                        start,
                        i
                    )
                    lower in TYPES -> builder.addStyle(
                        SpanStyle(color = colors.syntaxType, fontWeight = FontWeight.Medium),
                        start,
                        i
                    )
                    lower in FUNCTIONS -> builder.addStyle(
                        SpanStyle(color = colors.accentPrimary, fontWeight = FontWeight.SemiBold),
                        start,
                        i
                    )
                    lower in BOOLEANS -> builder.addStyle(
                        SpanStyle(color = colors.syntaxBoolean, fontWeight = FontWeight.Bold),
                        start,
                        i
                    )
                    lower == NULL -> builder.addStyle(
                        SpanStyle(color = colors.syntaxNull, fontStyle = FontStyle.Italic),
                        start,
                        i
                    )
                }
                continue
            }

            i++
        }

        return builder.toAnnotatedString()
    }

    private fun isIdentifierStart(c: Char): Boolean =
        c.isLetter() || c == '_' || c == '$'

    private fun isIdentifierPart(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '$'
}

class SqlVisualTransformation(
    private val colors: JPadColorScheme
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = SqlSyntaxHighlighter.highlight(text.text, colors),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
