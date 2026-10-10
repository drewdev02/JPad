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

    private val mainMethodRegex = Regex("""public\s+static\s+void\s+main\s*\(\s*String\s*\[\s*\]""")
    private val classDeclarationRegex = Regex("""(?:public\s+)?(?:final\s+)?(?:abstract\s+)?(?:class|interface|enum|record)\s+([A-Za-z0-9_]+)""")
    private val packageRegex = Regex("""^\s*package\s+([A-Za-z0-9_.]+)\s*;""", RegexOption.MULTILINE)
    private val importRegex = Regex("""^\s*import\s+([A-Za-z0-9_.*]+)\s*;""", RegexOption.MULTILINE)

    fun wrap(code: String, className: String = "JPadScript"): ParsedSnippet {
        val trimmed = code.trim()

        val packageMatch = packageRegex.find(trimmed)
        val packageName = packageMatch?.groupValues?.get(1)

        val hasMainMethod = mainMethodRegex.containsMatchIn(trimmed)
        val classMatch = classDeclarationRegex.find(trimmed)

        if (hasMainMethod && classMatch != null) {
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
        val defaultImports = listOf(
            "import java.util.*;",
            "import java.util.stream.*;",
            "import java.util.function.*;",
            "import java.time.*;",
            "import org.andrew.jpad.runtime.JPad;",
            "import static org.andrew.jpad.runtime.JPad.dump;",
            "import static org.andrew.jpad.runtime.JPad.cache;",
            "import static org.andrew.jpad.runtime.JPad.benchmark;",
            "import static org.andrew.jpad.runtime.JPad.query;"
        )

        val defaultImportBlock = defaultImports.joinToString("\n") + "\n"
        val userImportBlock = if (imports.isNotEmpty()) imports.joinToString("\n") + "\n\n" else "\n"

        val wrappedCode = buildString {
            append(defaultImportBlock)
            append(userImportBlock)
            appendLine("public class $className {")
            appendLine("    public static void main(String[] args) throws Throwable {")
            bodyCode.lines().forEach { bodyLine ->
                appendLine("        $bodyLine")
            }
            appendLine("    }")
            appendLine("}")
        }

        val lineOffset = defaultImports.size + (if (imports.isNotEmpty()) imports.size + 1 else 1) + 2

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
