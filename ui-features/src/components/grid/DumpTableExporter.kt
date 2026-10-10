package org.andrew.jpad.ui.components.grid

import org.andrew.jpad.core.dump.DumpValue
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

enum class DumpSortDirection {
    ASC,
    DESC
}

data class DumpColumnSort(
    val column: String,
    val direction: DumpSortDirection
)

object DumpTableExporter {

    fun extractCellString(value: DumpValue?): String {
        return when (value) {
            null -> ""
            is DumpValue.Primitive -> value.rawValue?.toString() ?: ""
            is DumpValue.Text -> value.content
            is DumpValue.KeyValueList -> "{${value.title}: ${value.entries.size} fields}"
            is DumpValue.Table -> "[Table: ${value.typeName} (${value.rows.size})]"
        }
    }

    fun toAsciiTable(columns: List<String>, rows: List<Map<String, DumpValue>>): String {
        if (columns.isEmpty()) return ""
        val colWidths = columns.associateWith { col ->
            maxOf(col.length, rows.maxOfOrNull { extractCellString(it[col]).length } ?: 0)
        }
        val sb = StringBuilder()
        val header = columns.joinToString(" | ") { col -> col.padEnd(colWidths[col] ?: col.length) }
        val separator = columns.joinToString("-+-") { col -> "-".repeat(colWidths[col] ?: col.length) }
        sb.append(header).append("\n").append(separator).append("\n")
        for (row in rows) {
            val line = columns.joinToString(" | ") { col ->
                extractCellString(row[col]).padEnd(colWidths[col] ?: col.length)
            }
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun toCsv(columns: List<String>, rows: List<Map<String, DumpValue>>): String {
        val sb = StringBuilder()
        sb.append(columns.joinToString(",") { escapeCsv(it) }).append("\n")
        for (row in rows) {
            val line = columns.joinToString(",") { col ->
                escapeCsv(extractCellString(row[col]))
            }
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun toTsv(columns: List<String>, rows: List<Map<String, DumpValue>>): String {
        val sb = StringBuilder()
        sb.append(columns.joinToString("\t")).append("\n")
        for (row in rows) {
            val line = columns.joinToString("\t") { col ->
                extractCellString(row[col])
                    .replace("\t", " ")
                    .replace("\n", " ")
                    .replace("\r", "")
            }
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun toJson(columns: List<String>, rows: List<Map<String, DumpValue>>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        rows.forEachIndexed { rIdx, row ->
            sb.append("  {\n")
            columns.forEachIndexed { cIdx, col ->
                val cellVal = row[col]
                sb.append("    \"").append(escapeJson(col)).append("\": ")
                sb.append(cellToJsonValue(cellVal))
                if (cIdx < columns.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("  }")
            if (rIdx < rows.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun cellToJsonValue(value: DumpValue?): String {
        return when (value) {
            null -> "null"
            is DumpValue.Primitive -> {
                when (val raw = value.rawValue) {
                    null -> "null"
                    is Number, is Boolean -> raw.toString()
                    else -> "\"${escapeJson(raw.toString())}\""
                }
            }
            is DumpValue.Text -> "\"${escapeJson(value.content)}\""
            is DumpValue.KeyValueList -> {
                val entries = value.entries.joinToString(", ") { (k, v) ->
                    "\"${escapeJson(k)}\": ${cellToJsonValue(v)}"
                }
                "{ $entries }"
            }
            is DumpValue.Table -> {
                toJson(value.columns, value.rows)
            }
        }
    }

    private fun escapeCsv(str: String): String {
        return if (str.contains(',') || str.contains('"') || str.contains('\n') || str.contains('\r')) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    private fun escapeJson(str: String): String {
        return buildString {
            for (ch in str) {
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (ch.code < 0x20) {
                            append(String.format("\\u%04x", ch.code))
                        } else {
                            append(ch)
                        }
                    }
                }
            }
        }
    }

    fun filterRows(
        rows: List<Map<String, DumpValue>>,
        query: String
    ): List<Map<String, DumpValue>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return rows

        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        return rows.filter { row ->
            tokens.all { token ->
                row.values.any { cellValue ->
                    extractCellString(cellValue).contains(token, ignoreCase = true)
                }
            }
        }
    }

    fun sortRows(
        rows: List<Map<String, DumpValue>>,
        sort: DumpColumnSort?
    ): List<Map<String, DumpValue>> {
        if (sort == null) return rows

        return rows.sortedWith { rowA, rowB ->
            val valA = rowA[sort.column]
            val valB = rowB[sort.column]
            val cmp = compareDumpValues(valA, valB)
            if (sort.direction == DumpSortDirection.ASC) cmp else -cmp
        }
    }

    fun compareDumpValues(a: DumpValue?, b: DumpValue?): Int {
        if (a == null && b == null) return 0
        if (a == null) return 1
        if (b == null) return -1

        if (a is DumpValue.Primitive && b is DumpValue.Primitive) {
            val rawA = a.rawValue
            val rawB = b.rawValue
            if (rawA == null && rawB == null) return 0
            if (rawA == null) return 1
            if (rawB == null) return -1

            if (rawA is Number && rawB is Number) {
                return rawA.toDouble().compareTo(rawB.toDouble())
            }
            if (rawA is Boolean && rawB is Boolean) {
                return rawA.compareTo(rawB)
            }
            return rawA.toString().compareTo(rawB.toString(), ignoreCase = true)
        }

        val strA = extractCellString(a)
        val strB = extractCellString(b)
        return strA.compareTo(strB, ignoreCase = true)
    }

    fun copyToClipboard(text: String): Boolean {
        return try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
