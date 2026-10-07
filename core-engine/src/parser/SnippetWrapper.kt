package org.andrew.jpad.core.parser

import org.andrew.jpad.core.compiler.JavaSource

enum class SnippetMode {
    STATEMENTS,
    EXPRESSION,
    PROGRAM
}

data class ParsedSnippet(
    val mode: SnippetMode,
    val className: String,
    val packageName: String? = null,
    val fullClassName: String = if (packageName != null) "$packageName.$className" else className,
    val generatedSource: JavaSource,
    val userImports: List<String> = emptyList(),
    val lineOffset: Int = 0
)

class SnippetWrapper {

    private val classDeclarationRegex = Regex("""(?:public\s+)?(?:final\s+)?(?:abstract\s+)?(?:class|interface|enum|record)\s+([A-Za-z0-9_]+)""")
    private val packageRegex = Regex("""^\s*package\s+([A-Za-z0-9_.]+)\s*;""", RegexOption.MULTILINE)
    private val importRegex = Regex("""^\s*import\s+([A-Za-z0-9_.*]+)\s*;""", RegexOption.MULTILINE)

    fun wrap(code: String, className: String = "JPadScript"): ParsedSnippet {
        val trimmed = code.trim()

        val packageMatch = packageRegex.find(trimmed)
        val packageName = packageMatch?.groupValues?.get(1)

        val classMatch = classDeclarationRegex.find(trimmed)
        if (classMatch != null) {
            val detectedClassName = classMatch.groupValues[1]
            return ParsedSnippet(
                mode = SnippetMode.PROGRAM,
                className = detectedClassName,
                packageName = packageName,
                generatedSource = JavaSource(detectedClassName, trimmed),
                lineOffset = 0
            )
        }

        val imports = mutableListOf<String>()
        val nonImportLines = mutableListOf<String>()

        trimmed.lines().forEach { line ->
            val importMatch = importRegex.matchEntire(line.trim())
            if (importMatch != null) {
                imports.add(line.trim())
            } else {
                nonImportLines.add(line)
            }
        }

        val bodyCode = nonImportLines.joinToString("\n")
        val importBlock = if (imports.isNotEmpty()) imports.joinToString("\n") + "\n\n" else ""

        val wrappedCode = buildString {
            if (importBlock.isNotEmpty()) {
                append(importBlock)
            }
            appendLine("public class $className {")
            appendLine("    public static void main(String[] args) throws Throwable {")
            bodyCode.lines().forEach { bodyLine ->
                appendLine("        $bodyLine")
            }
            appendLine("    }")
            appendLine("}")
        }

        val lineOffset = (if (imports.isNotEmpty()) imports.size + 1 else 0) + 2

        return ParsedSnippet(
            mode = SnippetMode.STATEMENTS,
            className = className,
            packageName = null,
            generatedSource = JavaSource(className, wrappedCode),
            userImports = imports,
            lineOffset = lineOffset
        )
    }
}
