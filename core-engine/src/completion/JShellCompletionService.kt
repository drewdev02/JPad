package org.andrew.jpad.core.completion

import jdk.jshell.JShell
import jdk.jshell.SourceCodeAnalysis
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JShellCompletionService(
    private val liveTemplateContributor: LiveTemplateContributor = LiveTemplateContributor()
) : CompletionService, AutoCloseable {

    private val lock = Any()
    private var jshell: JShell? = null
    private val addedClasspaths = mutableSetOf<String>()

    private val defaultImports = listOf(
        "import java.util.*;",
        "import java.util.stream.*;",
        "import java.util.concurrent.*;",
        "import java.time.*;",
        "import java.util.function.*;",
        "import static org.andrew.jpad.runtime.JPad.dump;"
    )

    private fun getOrCreateJShell(): JShell {
        val current = jshell
        if (current != null) return current

        val out = PrintStream(ByteArrayOutputStream(), true, Charsets.UTF_8)
        val js = JShell.builder()
            .out(out)
            .err(out)
            .build()

        for (imp in defaultImports) {
            try {
                js.eval(imp)
            } catch (_: Exception) {}
        }

        jshell = js
        return js
    }

    override fun getCompletions(
        code: String,
        cursorOffset: Int,
        classpath: List<File>
    ): CompletionResult {
        synchronized(lock) {
            val js = getOrCreateJShell()

            // Update classpath if new JARs are provided
            for (jar in classpath) {
                if (jar.exists() && addedClasspaths.add(jar.absolutePath)) {
                    try {
                        js.addToClasspath(jar.absolutePath)
                    } catch (_: Exception) {}
                }
            }

            val safeOffset = cursorOffset.coerceIn(0, code.length)
            val anchor = IntArray(1)
            val analysis = js.sourceCodeAnalysis()

            val rawSuggestions = try {
                analysis.completionSuggestions(code, safeOffset, anchor)
            } catch (_: Throwable) {
                emptyList<SourceCodeAnalysis.Suggestion>()
            }

            val anchorOffset = anchor[0].coerceIn(0, safeOffset)
            val prefix = if (safeOffset > anchorOffset) code.substring(anchorOffset, safeOffset) else ""

            val items = mutableListOf<LookupItem>()

            // 1. Live Templates (IntelliJ style: sout, main, dump, fori, etc.)
            if (prefix.isNotBlank()) {
                items.addAll(liveTemplateContributor.getTemplates(prefix))
            }

            // 2. Semantic JDK suggestions from SourceCodeAnalysis
            val seenSignatures = mutableSetOf<String>()
            for (suggestion in rawSuggestions) {
                val fullText = suggestion.continuation()
                if (fullText.isBlank()) continue

                val item = parseSuggestion(fullText, suggestion.matchesType())
                val dedupKey = "${item.kind}:${item.lookupString}"
                if (seenSignatures.add(dedupKey)) {
                    items.add(item)
                }
            }

            // Sort: smart matches first, then prefix match score, then alphabetical
            val sorted = items.sortedWith(
                compareByDescending<LookupItem> { it.isSmart }
                    .thenByDescending { it.lookupString.startsWith(prefix, ignoreCase = true) }
                    .thenBy { it.lookupString.lowercase() }
            )

            return CompletionResult(
                anchor = anchorOffset,
                prefix = prefix,
                items = sorted
            )
        }
    }

    private fun parseSuggestion(fullText: String, matchesType: Boolean): LookupItem {
        val trimmed = fullText.trim()

        return when {
            trimmed.endsWith("(") -> {
                val methodName = trimmed.removeSuffix("(")
                LookupItem(
                    lookupString = methodName,
                    tailText = "(...)",
                    typeText = "method",
                    kind = LookupElementKind.METHOD,
                    insertText = "$methodName()",
                    cursorDelta = -1, // inside parentheses
                    isSmart = matchesType
                )
            }
            trimmed.endsWith("()") -> {
                val methodName = trimmed.removeSuffix("()")
                LookupItem(
                    lookupString = methodName,
                    tailText = "()",
                    typeText = "method",
                    kind = LookupElementKind.METHOD,
                    insertText = "$methodName()",
                    cursorDelta = 0,
                    isSmart = matchesType
                )
            }
            trimmed.startsWith("import ") -> {
                val pkg = trimmed.removePrefix("import ").removeSuffix(";")
                LookupItem(
                    lookupString = pkg,
                    tailText = " (import)",
                    typeText = "package",
                    kind = LookupElementKind.CLASS,
                    insertText = trimmed,
                    isSmart = false
                )
            }
            trimmed.firstOrNull()?.isUpperCase() == true -> {
                LookupItem(
                    lookupString = trimmed,
                    tailText = "",
                    typeText = "class",
                    kind = LookupElementKind.CLASS,
                    insertText = trimmed,
                    isSmart = matchesType
                )
            }
            isJavaKeyword(trimmed) -> {
                LookupItem(
                    lookupString = trimmed,
                    tailText = "",
                    typeText = "keyword",
                    kind = LookupElementKind.KEYWORD,
                    insertText = trimmed,
                    isSmart = false
                )
            }
            else -> {
                LookupItem(
                    lookupString = trimmed,
                    tailText = "",
                    typeText = "member",
                    kind = LookupElementKind.FIELD,
                    insertText = trimmed,
                    isSmart = matchesType
                )
            }
        }
    }

    private fun isJavaKeyword(word: String): Boolean = word in JAVA_KEYWORDS

    override fun close() {
        synchronized(lock) {
            try {
                jshell?.close()
            } catch (_: Exception) {}
            jshell = null
            addedClasspaths.clear()
        }
    }

    companion object {
        private val JAVA_KEYWORDS = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "permits", "private", "protected", "public", "record", "return",
            "sealed", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "var", "void", "volatile",
            "while", "yield"
        )
    }
}
