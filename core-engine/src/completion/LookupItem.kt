package org.andrew.jpad.core.completion

enum class LookupElementKind {
    METHOD,
    FIELD,
    CLASS,
    INTERFACE,
    KEYWORD,
    VARIABLE,
    SNIPPET,
    DIRECTIVE,
    TABLE,
    COLUMN,
    SQL_FUNCTION
}

data class LookupItem(
    val lookupString: String,
    val tailText: String = "",
    val typeText: String = "",
    val kind: LookupElementKind = LookupElementKind.METHOD,
    val insertText: String = lookupString,
    val cursorDelta: Int = 0,
    val isSmart: Boolean = false
)

data class CompletionResult(
    val anchor: Int,
    val prefix: String,
    val items: List<LookupItem>
)

interface CompletionService {
    fun getCompletions(
        code: String,
        cursorOffset: Int,
        classpath: List<java.io.File> = emptyList()
    ): CompletionResult
}
