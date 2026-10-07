package org.andrew.jpad.ui.components.grid

sealed interface DumpValue {
    data class Primitive(val rawValue: Any?, val typeName: String? = null) : DumpValue
    data class KeyValueList(val title: String, val entries: List<Pair<String, DumpValue>>) : DumpValue
    data class Table(
        val typeName: String,
        val columns: List<String>,
        val rows: List<Map<String, DumpValue>>,
        val totalCount: Int = rows.size
    ) : DumpValue
    data class Text(val content: String, val isError: Boolean = false) : DumpValue
}
