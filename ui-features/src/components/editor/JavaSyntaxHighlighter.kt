package org.andrew.jpad.ui.components.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.andrew.jpad.ui.theme.JPadColorScheme

object JavaSyntaxHighlighter {

    private val KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "non-sealed", "package", "permits", "private", "protected", "public", "record", "return",
        "sealed", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
        "throws", "transient", "try", "void", "volatile", "while", "yield", "var"
    )

    private val BOOLEANS = setOf("true", "false")
    private const val NULL = "null"

    fun highlight(code: String, colors: JPadColorScheme): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        val len = code.length
        var i = 0

        while (i < len) {
            val c = code[i]

            // 1. Line comment //
            if (c == '/' && i + 1 < len && code[i + 1] == '/') {
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

            // 3. String literal "..." or text block """..."""
            if (c == '"') {
                val start = i
                val isTriple = (i + 2 < len && code[i + 1] == '"' && code[i + 2] == '"')
                if (isTriple) {
                    i += 3
                    while (i + 2 < len && !(code[i] == '"' && code[i + 1] == '"' && code[i + 2] == '"')) {
                        i++
                    }
                    if (i + 2 < len) i += 3 else i = len
                } else {
                    i++
                    while (i < len && code[i] != '"' && code[i] != '\n') {
                        if (code[i] == '\\' && i + 1 < len) {
                            i += 2
                        } else {
                            i++
                        }
                    }
                    if (i < len && code[i] == '"') {
                        i++
                    }
                }
                builder.addStyle(SpanStyle(color = colors.syntaxString), start, i)
                continue
            }

            // 4. Character literal '...'
            if (c == '\'') {
                val start = i
                i++
                while (i < len && code[i] != '\'' && code[i] != '\n') {
                    if (code[i] == '\\' && i + 1 < len) {
                        i += 2
                    } else {
                        i++
                    }
                }
                if (i < len && code[i] == '\'') {
                    i++
                }
                builder.addStyle(SpanStyle(color = colors.syntaxString), start, i)
                continue
            }

            // 5. Annotation @Identifier
            if (c == '@' && i + 1 < len && isIdentifierStart(code[i + 1])) {
                val start = i
                i++
                while (i < len && isIdentifierPart(code[i])) {
                    i++
                }
                builder.addStyle(SpanStyle(color = colors.syntaxKeyword), start, i)
                continue
            }

            // 6. Number literal
            if (c.isDigit() || (c == '.' && i + 1 < len && code[i + 1].isDigit())) {
                val start = i
                if (c == '0' && i + 1 < len && (code[i + 1] == 'x' || code[i + 1] == 'X' || code[i + 1] == 'b' || code[i + 1] == 'B')) {
                    i += 2
                    while (i < len && (code[i].isLetterOrDigit() || code[i] == '_')) {
                        i++
                    }
                } else {
                    while (i < len && (code[i].isDigit() || code[i] == '.' || code[i] == '_' || code[i] in "eEfFdDlL")) {
                        i++
                    }
                }
                builder.addStyle(SpanStyle(color = colors.syntaxNumber), start, i)
                continue
            }

            // 7. Words (Identifiers, Keywords, Types, Booleans, Null)
            if (isIdentifierStart(c)) {
                val start = i
                while (i < len && isIdentifierPart(code[i])) {
                    i++
                }
                val word = code.substring(start, i)
                when {
                    word in KEYWORDS -> builder.addStyle(
                        SpanStyle(color = colors.syntaxKeyword, fontWeight = FontWeight.Bold),
                        start,
                        i
                    )
                    word in BOOLEANS -> builder.addStyle(
                        SpanStyle(color = colors.syntaxBoolean, fontWeight = FontWeight.Bold),
                        start,
                        i
                    )
                    word == NULL -> builder.addStyle(
                        SpanStyle(color = colors.syntaxNull, fontStyle = FontStyle.Italic),
                        start,
                        i
                    )
                    word[0].isUpperCase() -> builder.addStyle(
                        SpanStyle(color = colors.syntaxType),
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

class JavaVisualTransformation(
    private val colors: JPadColorScheme
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = JavaSyntaxHighlighter.highlight(text.text, colors),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
