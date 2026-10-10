package org.andrew.jpad.core.completion

import org.andrew.jpad.core.database.DatabaseColumnMeta
import org.andrew.jpad.core.database.DatabaseTableMeta
import java.io.File

class SqlCompletionService(
    private var schemaProvider: () -> List<DatabaseTableMeta> = { emptyList() }
) : CompletionService {

    fun setSchemaProvider(provider: () -> List<DatabaseTableMeta>) {
        schemaProvider = provider
    }

    override fun getCompletions(
        code: String,
        cursorOffset: Int,
        classpath: List<File>
    ): CompletionResult {
        return getCompletionsForSchema(code, cursorOffset, schemaProvider())
    }

    fun getCompletionsForSchema(
        code: String,
        cursorOffset: Int,
        schema: List<DatabaseTableMeta>
    ): CompletionResult {
        val safeOffset = cursorOffset.coerceIn(0, code.length)
        val textBeforeCursor = code.substring(0, safeOffset)

        // 1. Calculate prefix and anchor
        val (prefix, anchor, isDotQualified, qualifier) = extractPrefixAndQualifier(textBeforeCursor, safeOffset)

        val items = mutableListOf<LookupItem>()

        // 2. Extract tables and aliases declared in current query
        val queryTables = extractQueryTables(textBeforeCursor, schema)

        // 3. If dot-qualified (e.g. "c." or "customers.")
        if (isDotQualified && qualifier.isNotBlank()) {
            val matchedTable = queryTables.firstOrNull { it.alias.equals(qualifier, ignoreCase = true) }?.table
                ?: schema.firstOrNull { it.name.equals(qualifier, ignoreCase = true) }

            if (matchedTable != null) {
                matchedTable.columns.forEach { col ->
                    items.add(createColumnLookupItem(col, matchedTable.name, isQualified = true))
                }
            } else {
                // If qualifier not found directly, search all tables for possible match
                schema.forEach { table ->
                    table.columns.forEach { col ->
                        items.add(createColumnLookupItem(col, table.name, isQualified = false))
                    }
                }
            }
        } else {
            // 4. Contextual completions based on previous SQL clause
            val previousClause = detectPreviousClause(textBeforeCursor)

            when (previousClause) {
                SqlClause.FROM, SqlClause.JOIN, SqlClause.INTO, SqlClause.UPDATE -> {
                    // Prioritize Tables
                    schema.forEach { table ->
                        items.add(createTableLookupItem(table))
                    }
                }
                SqlClause.ON -> {
                    // Smart Join suggestions & columns
                    val smartJoinConditions = generateSmartJoinConditions(queryTables)
                    items.addAll(smartJoinConditions)

                    // Add columns of query tables
                    queryTables.forEach { qt ->
                        qt.table.columns.forEach { col ->
                            items.add(createColumnLookupItem(col, qt.alias.ifBlank { qt.table.name }, isQualified = false))
                        }
                    }
                }
                SqlClause.SELECT, SqlClause.WHERE, SqlClause.ORDER_BY, SqlClause.GROUP_BY, SqlClause.HAVING, SqlClause.SET -> {
                    // Columns of tables in query
                    if (queryTables.isNotEmpty()) {
                        queryTables.forEach { qt ->
                            qt.table.columns.forEach { col ->
                                items.add(createColumnLookupItem(col, qt.alias.ifBlank { qt.table.name }, isQualified = false))
                            }
                        }
                    } else {
                        // If no table mentioned yet, offer all available schema columns
                        schema.forEach { table ->
                            table.columns.forEach { col ->
                                items.add(createColumnLookupItem(col, table.name, isQualified = false))
                            }
                        }
                    }

                    // Built-in SQL Functions
                    items.addAll(SQL_FUNCTIONS)
                }
                SqlClause.OTHER -> {
                    // Start of query or general context: offer snippets, keywords, tables
                    schema.forEach { table ->
                        items.add(createTableLookupItem(table))
                    }
                    items.addAll(SQL_FUNCTIONS)
                }
            }

            // Always provide Live Templates / Snippets
            items.addAll(SQL_SNIPPETS)

            // Always provide standard SQL Keywords
            items.addAll(SQL_KEYWORDS)
        }

        // 5. Filter & sort by prefix
        val filtered = filterAndSortItems(items, prefix)

        return CompletionResult(
            anchor = anchor,
            prefix = prefix,
            items = filtered
        )
    }

    private data class PrefixInfo(
        val prefix: String,
        val anchor: Int,
        val isDotQualified: Boolean,
        val qualifier: String
    )

    private fun extractPrefixAndQualifier(textBeforeCursor: String, offset: Int): PrefixInfo {
        if (textBeforeCursor.isEmpty()) {
            return PrefixInfo("", offset, false, "")
        }

        // Scan backwards for current word
        var i = textBeforeCursor.length - 1
        while (i >= 0 && (textBeforeCursor[i].isLetterOrDigit() || textBeforeCursor[i] == '_')) {
            i--
        }

        val prefixStart = i + 1
        val prefix = textBeforeCursor.substring(prefixStart)

        // Check if preceding character is a dot '.'
        if (i >= 0 && textBeforeCursor[i] == '.') {
            var qEnd = i
            var qStart = qEnd - 1
            while (qStart >= 0 && (textBeforeCursor[qStart].isLetterOrDigit() || textBeforeCursor[qStart] == '_')) {
                qStart--
            }
            val qualifier = textBeforeCursor.substring(qStart + 1, qEnd).trim()
            return PrefixInfo(prefix, prefixStart, true, qualifier)
        }

        return PrefixInfo(prefix, prefixStart, false, "")
    }

    private enum class SqlClause {
        SELECT,
        FROM,
        JOIN,
        ON,
        WHERE,
        GROUP_BY,
        ORDER_BY,
        HAVING,
        INTO,
        UPDATE,
        SET,
        OTHER
    }

    private fun detectPreviousClause(text: String): SqlClause {
        // Strip string literals and line comments
        val clean = text
            .replace(Regex("'[^']*'"), "''")
            .replace(Regex("--.*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")

        val tokens = clean.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return SqlClause.OTHER

        for (idx in tokens.indices.reversed()) {
            val tok = tokens[idx].uppercase()
            when (tok) {
                "FROM" -> return SqlClause.FROM
                "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS" -> return SqlClause.JOIN
                "ON" -> return SqlClause.ON
                "WHERE" -> return SqlClause.WHERE
                "BY" -> {
                    if (idx > 0 && tokens[idx - 1].uppercase() == "ORDER") return SqlClause.ORDER_BY
                    if (idx > 0 && tokens[idx - 1].uppercase() == "GROUP") return SqlClause.GROUP_BY
                }
                "HAVING" -> return SqlClause.HAVING
                "SELECT" -> return SqlClause.SELECT
                "INTO" -> return SqlClause.INTO
                "UPDATE" -> return SqlClause.UPDATE
                "SET" -> return SqlClause.SET
            }
        }
        return SqlClause.OTHER
    }

    data class QueryTableBinding(
        val table: DatabaseTableMeta,
        val alias: String
    )

    private fun extractQueryTables(sql: String, schema: List<DatabaseTableMeta>): List<QueryTableBinding> {
        val bindings = mutableListOf<QueryTableBinding>()
        val clean = sql
            .replace(Regex("'[^']*'"), "''")
            .replace(Regex("--.*"), "")

        // Regex for: (FROM|JOIN)\s+([a-zA-Z0-9_]+)(?:\s+(?:AS\s+)?([a-zA-Z0-9_]+))?
        val tableRegex = Regex("(?i)\\b(FROM|JOIN)\\s+([a-zA-Z0-9_]+)(?:\\s+(?:AS\\s+)?([a-zA-Z0-9_]+))?")
        tableRegex.findAll(clean).forEach { match ->
            val tableName = match.groupValues[2]
            val potentialAlias = match.groupValues.getOrNull(3).orEmpty()

            // Match table in schema
            val schemaTable = schema.firstOrNull { it.name.equals(tableName, ignoreCase = true) }
                ?: DatabaseTableMeta(name = tableName)

            val alias = if (potentialAlias.isNotBlank() && !isSqlKeyword(potentialAlias)) {
                potentialAlias
            } else {
                tableName
            }

            bindings.add(QueryTableBinding(schemaTable, alias))
        }
        return bindings
    }

    private fun generateSmartJoinConditions(queryTables: List<QueryTableBinding>): List<LookupItem> {
        if (queryTables.size < 2) return emptyList()
        val items = mutableListOf<LookupItem>()

        val lastJoined = queryTables.last()
        val priorTables = queryTables.dropLast(1)

        for (prior in priorTables) {
            // Find foreign key relationships or name patterns (e.g. customer_id == id)
            val priorBase = prior.table.name.trimEnd('s')
            val joinedBase = lastJoined.table.name.trimEnd('s')
            for (col1 in lastJoined.table.columns) {
                for (col2 in prior.table.columns) {
                    val isFkMatch = (col1.name.equals("${prior.table.name}_id", ignoreCase = true) && col2.name.equals("id", ignoreCase = true)) ||
                            (col1.name.equals("${priorBase}_id", ignoreCase = true) && col2.name.equals("id", ignoreCase = true)) ||
                            (col2.name.equals("${lastJoined.table.name}_id", ignoreCase = true) && col1.name.equals("id", ignoreCase = true)) ||
                            (col2.name.equals("${joinedBase}_id", ignoreCase = true) && col1.name.equals("id", ignoreCase = true)) ||
                            (col1.name.equals(col2.name, ignoreCase = true) && col1.name.endsWith("_id", ignoreCase = true))

                    if (isFkMatch) {
                        val cond = "${lastJoined.alias}.${col1.name} = ${prior.alias}.${col2.name}"
                        items.add(
                            LookupItem(
                                lookupString = cond,
                                tailText = "(smart join condition)",
                                typeText = "Smart ON",
                                kind = LookupElementKind.KEYWORD,
                                insertText = cond,
                                isSmart = true
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    private fun createTableLookupItem(table: DatabaseTableMeta): LookupItem {
        val colCount = table.columns.size
        return LookupItem(
            lookupString = table.name,
            tailText = if (colCount > 0) "($colCount cols)" else "",
            typeText = table.type,
            kind = LookupElementKind.TABLE,
            insertText = table.name,
            isSmart = false
        )
    }

    private fun createColumnLookupItem(
        col: DatabaseColumnMeta,
        sourceTitle: String,
        isQualified: Boolean
    ): LookupItem {
        val typeBadge = if (col.isPrimaryKey) "${col.typeName} [PK]" else col.typeName
        val tail = if (isQualified) "(column)" else "(from $sourceTitle)"
        return LookupItem(
            lookupString = col.name,
            tailText = tail,
            typeText = typeBadge,
            kind = LookupElementKind.COLUMN,
            insertText = col.name,
            isSmart = false
        )
    }

    private fun isSqlKeyword(word: String): Boolean {
        val upper = word.uppercase()
        return upper in SQL_KEYWORD_STRINGS || upper in setOf("WHERE", "ON", "AND", "OR", "GROUP", "ORDER", "LIMIT", "LEFT", "RIGHT", "INNER", "JOIN")
    }

    private fun filterAndSortItems(items: List<LookupItem>, prefix: String): List<LookupItem> {
        val unique = items.distinctBy { it.kind to it.lookupString }
        if (prefix.isBlank()) {
            return unique.sortedWith(
                compareByDescending<LookupItem> { it.isSmart }
                    .thenBy { kindRank(it.kind) }
                    .thenBy { it.lookupString }
            )
        }

        val lowerPrefix = prefix.lowercase()
        return unique
            .filter { it.lookupString.lowercase().contains(lowerPrefix) }
            .sortedWith(
                compareByDescending<LookupItem> { it.isSmart }
                    .thenByDescending { it.lookupString.lowercase().startsWith(lowerPrefix) }
                    .thenBy { kindRank(it.kind) }
                    .thenBy { it.lookupString.length }
                    .thenBy { it.lookupString }
            )
    }

    private fun kindRank(kind: LookupElementKind): Int = when (kind) {
        LookupElementKind.TABLE -> 1
        LookupElementKind.COLUMN -> 2
        LookupElementKind.SQL_FUNCTION -> 3
        LookupElementKind.SNIPPET -> 4
        LookupElementKind.KEYWORD -> 5
        else -> 6
    }

    companion object {
        private val SQL_KEYWORD_STRINGS = setOf(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN",
            "CROSS JOIN", "ON", "AND", "OR", "NOT", "GROUP BY", "ORDER BY", "HAVING",
            "LIMIT", "OFFSET", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
            "DISTINCT", "AS", "CASE", "WHEN", "THEN", "ELSE", "END", "IN", "BETWEEN",
            "LIKE", "IS NULL", "IS NOT NULL", "EXISTS", "UNION", "UNION ALL", "ASC", "DESC"
        )

        private val SQL_KEYWORDS = SQL_KEYWORD_STRINGS.map { kw ->
            LookupItem(
                lookupString = kw,
                tailText = "",
                typeText = "keyword",
                kind = LookupElementKind.KEYWORD,
                insertText = kw,
                isSmart = false
            )
        }

        private val SQL_FUNCTIONS = listOf(
            LookupItem("COUNT(*)", "", "BIGINT", LookupElementKind.SQL_FUNCTION, "COUNT(*)"),
            LookupItem("COUNT()", "(expr)", "BIGINT", LookupElementKind.SQL_FUNCTION, "COUNT()", cursorDelta = -1),
            LookupItem("SUM()", "(expr)", "NUMERIC", LookupElementKind.SQL_FUNCTION, "SUM()", cursorDelta = -1),
            LookupItem("AVG()", "(expr)", "NUMERIC", LookupElementKind.SQL_FUNCTION, "AVG()", cursorDelta = -1),
            LookupItem("MIN()", "(expr)", "T", LookupElementKind.SQL_FUNCTION, "MIN()", cursorDelta = -1),
            LookupItem("MAX()", "(expr)", "T", LookupElementKind.SQL_FUNCTION, "MAX()", cursorDelta = -1),
            LookupItem("COALESCE()", "(a, b)", "T", LookupElementKind.SQL_FUNCTION, "COALESCE()", cursorDelta = -1),
            LookupItem("LOWER()", "(str)", "TEXT", LookupElementKind.SQL_FUNCTION, "LOWER()", cursorDelta = -1),
            LookupItem("UPPER()", "(str)", "TEXT", LookupElementKind.SQL_FUNCTION, "UPPER()", cursorDelta = -1),
            LookupItem("TRIM()", "(str)", "TEXT", LookupElementKind.SQL_FUNCTION, "TRIM()", cursorDelta = -1),
            LookupItem("LENGTH()", "(str)", "INT", LookupElementKind.SQL_FUNCTION, "LENGTH()", cursorDelta = -1)
        )

        private val SQL_SNIPPETS = listOf(
            LookupItem("sel", "SELECT * FROM ...", "snippet", LookupElementKind.SNIPPET, "SELECT * FROM "),
            LookupItem("sc", "SELECT COUNT(*) FROM ...", "snippet", LookupElementKind.SNIPPET, "SELECT COUNT(*) FROM "),
            LookupItem("ins", "INSERT INTO ... VALUES ...", "snippet", LookupElementKind.SNIPPET, "INSERT INTO "),
            LookupItem("upd", "UPDATE ... SET ...", "snippet", LookupElementKind.SNIPPET, "UPDATE "),
            LookupItem("del", "DELETE FROM ...", "snippet", LookupElementKind.SNIPPET, "DELETE FROM "),
            LookupItem("wh", "WHERE ...", "snippet", LookupElementKind.SNIPPET, "WHERE "),
            LookupItem("ob", "ORDER BY ...", "snippet", LookupElementKind.SNIPPET, "ORDER BY "),
            LookupItem("gb", "GROUP BY ...", "snippet", LookupElementKind.SNIPPET, "GROUP BY ")
        )
    }
}
