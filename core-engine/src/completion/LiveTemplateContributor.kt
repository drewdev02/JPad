package org.andrew.jpad.core.completion

data class LiveTemplate(
    val key: String,
    val description: String,
    val templateText: String,
    val cursorDelta: Int = 0 // offset from end of inserted text to place cursor
)

class LiveTemplateContributor {

    private val templates = listOf(
        LiveTemplate(
            key = "sout",
            description = "System.out.println()",
            templateText = "System.out.println();",
            cursorDelta = -2 // inside parentheses
        ),
        LiveTemplate(
            key = "serr",
            description = "System.err.println()",
            templateText = "System.err.println();",
            cursorDelta = -2
        ),
        LiveTemplate(
            key = "dump",
            description = "JPad dump visualizer",
            templateText = "dump();",
            cursorDelta = -2
        ),
        LiveTemplate(
            key = "main",
            description = "main method declaration",
            templateText = "public static void main(String[] args) {\n    \n}",
            cursorDelta = -3 // inside body
        ),
        LiveTemplate(
            key = "psvm",
            description = "main method declaration",
            templateText = "public static void main(String[] args) {\n    \n}",
            cursorDelta = -3
        ),
        LiveTemplate(
            key = "fori",
            description = "Indexed for loop",
            templateText = "for (int i = 0; i < ; i++) {\n    \n}",
            cursorDelta = -14
        ),
        LiveTemplate(
            key = "record",
            description = "Record declaration",
            templateText = "record Item() {}",
            cursorDelta = -5
        ),
        LiveTemplate(
            key = "deps",
            description = "Maven dependency directive",
            templateText = "// @deps ",
            cursorDelta = 0
        ),
        LiveTemplate(
            key = "cache",
            description = "JPad interactive cache",
            templateText = "cache(\"key\", () -> );",
            cursorDelta = -2
        ),
        LiveTemplate(
            key = "bench",
            description = "JPad micro-benchmark",
            templateText = "benchmark(\"Task\", () -> {\n    \n});",
            cursorDelta = -5
        ),
        LiveTemplate(
            key = "query",
            description = "JPad SQLite query",
            templateText = "query(\"SELECT * FROM \");",
            cursorDelta = -3
        ),
        LiveTemplate(
            key = "tables",
            description = "JPad database table list",
            templateText = "dump(tables(), \"Database Tables\");",
            cursorDelta = 0
        )
    )

    fun getTemplates(prefix: String): List<LookupItem> {
        val lowerPrefix = prefix.lowercase().trim()
        if (lowerPrefix.isEmpty()) return emptyList()

        return templates
            .filter { it.key.startsWith(lowerPrefix) || lowerPrefix.startsWith(it.key) }
            .map { tmpl ->
                LookupItem(
                    lookupString = tmpl.key,
                    tailText = " — ${tmpl.description}",
                    typeText = "snippet",
                    kind = LookupElementKind.SNIPPET,
                    insertText = tmpl.templateText,
                    cursorDelta = tmpl.cursorDelta,
                    isSmart = true
                )
            }
    }
}
