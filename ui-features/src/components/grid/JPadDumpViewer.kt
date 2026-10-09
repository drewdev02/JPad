package org.andrew.jpad.ui.components.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.ui.theme.JPadTheme

@Composable
fun JPadDumpViewer(
    value: DumpValue,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    when (value) {
        is DumpValue.Primitive -> DumpPrimitiveCell(value)
        is DumpValue.Text -> DumpTextCell(value)
        is DumpValue.KeyValueList -> DumpKeyValueView(value, modifier, depth)
        is DumpValue.Table -> DumpTableView(value, modifier, depth)
    }
}

@Composable
fun JPadMultiDumpViewer(
    dumps: List<DumpValue>,
    modifier: Modifier = Modifier
) {
    if (dumps.isEmpty()) {
        Text(
            text = "// No dump results available for this query.",
            style = JPadTheme.typography.uiSmall.copy(color = JPadTheme.colors.textMuted),
            modifier = modifier.padding(8.dp)
        )
        return
    }

    val scrollState = rememberScrollState()

    if (dumps.size == 1) {
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            JPadDumpViewer(value = dumps.first(), modifier = Modifier.fillMaxWidth())
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        dumps.forEachIndexed { index, dump ->
            val dumpTitle = when (dump) {
                is DumpValue.Table -> "Table: ${dump.typeName} (${dump.totalCount} items)"
                is DumpValue.KeyValueList -> "Object: ${dump.title} (${dump.entries.size} fields)"
                is DumpValue.Primitive -> "Primitive (${dump.typeName ?: dump.rawValue?.let { it::class.simpleName } ?: "null"}): ${dump.rawValue}"
                is DumpValue.Text -> "Text Output"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JPadTheme.colors.backgroundPanel, RoundedCornerShape(4.dp))
                    .border(1.dp, JPadTheme.colors.borderSubtle, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(JPadTheme.colors.accentPrimary, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Dump #${index + 1}",
                            style = JPadTheme.typography.uiSmall.copy(
                                color = JPadTheme.colors.textOnAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = dumpTitle,
                        style = JPadTheme.typography.uiSmall.copy(
                            color = JPadTheme.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                JPadDumpViewer(value = dump, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun DumpPrimitiveCell(
    value: DumpValue.Primitive,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    val raw = value.rawValue

    val (text, color, style) = when (raw) {
        null -> Triple("null", colors.syntaxNull, FontStyle.Italic)
        is String -> Triple("\"$raw\"", colors.syntaxString, FontStyle.Normal)
        is Number -> Triple(raw.toString(), colors.syntaxNumber, FontStyle.Normal)
        is Boolean -> Triple(raw.toString(), colors.syntaxBoolean, FontStyle.Normal)
        else -> Triple(raw.toString(), colors.textPrimary, FontStyle.Normal)
    }

    Text(
        text = text,
        style = JPadTheme.typography.gridCell.copy(
            color = color,
            fontStyle = style
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun DumpTextCell(
    value: DumpValue.Text,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    Text(
        text = value.content,
        style = JPadTheme.typography.code.copy(
            color = if (value.isError) colors.errorRed else colors.textPrimary
        ),
        modifier = modifier.padding(6.dp)
    )
}

@Composable
fun DumpExportButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(colors.backgroundPanelHeader)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = JPadTheme.typography.uiSmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        )
    }
}

private fun calculateColumnWidths(columns: List<String>, rows: List<Map<String, DumpValue>>): Map<String, Dp> {
    val charWidthDp = 8.5f
    val cellPaddingDp = 24f

    return columns.associateWith { colName ->
        var maxChars = colName.length
        rows.forEach { rowMap ->
            val valueChars = when (val cellVal = rowMap[colName]) {
                null -> 4
                is DumpValue.Primitive -> when (val raw = cellVal.rawValue) {
                    null -> 4
                    is String -> raw.length + 2
                    else -> raw.toString().length
                }
                is DumpValue.Table -> cellVal.typeName.length + 12
                is DumpValue.KeyValueList -> cellVal.title.length + 6
                is DumpValue.Text -> cellVal.content.take(60).length
            }
            if (valueChars > maxChars) {
                maxChars = valueChars
            }
        }
        val computedDp = (maxChars * charWidthDp + cellPaddingDp).dp
        maxOf(90.dp, minOf(computedDp, 500.dp))
    }
}

@Composable
fun DumpTableView(
    table: DumpValue.Table,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    var isExpanded by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf<DumpColumnSort?>(null) }
    var copiedFeedback by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val colors = JPadTheme.colors
    val headerBg = if (depth == 0) colors.accentHeaderTable else colors.accentHeaderNested

    // Filter and sort rows
    val filteredRows = remember(table.rows, searchQuery) {
        DumpTableExporter.filterRows(table.rows, searchQuery)
    }
    val displayRows = remember(filteredRows, currentSort) {
        DumpTableExporter.sortRows(filteredRows, currentSort)
    }

    val columnWidths = remember(table.columns, displayRows) {
        calculateColumnWidths(table.columns, displayRows)
    }
    val rowIndexWidth = remember(displayRows.size) {
        val digits = maxOf(1, displayRows.size.toString().length)
        maxOf(36.dp, (digits * 9 + 20).dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(3.dp))
    ) {
        // LINQPad Signature Blue Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JPadTheme.dimensions.gridHeaderHeight)
                .background(headerBg)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isExpanded) "▼" else "▲",
                style = JPadTheme.typography.gridHeader.copy(color = colors.textOnAccent)
            )
            Text(
                text = "${table.typeName} (${table.totalCount} items)",
                style = JPadTheme.typography.gridHeader.copy(color = colors.textOnAccent)
            )
        }

        if (isExpanded) {
            // Interactive Table Toolbar: Search, Counters, Export (CSV/JSON/TSV)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPanel)
                    .border(0.5.dp, colors.borderSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Search Input Box
                Row(
                    modifier = Modifier
                        .width(180.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.backgroundInput)
                        .border(
                            1.dp,
                            if (searchQuery.isNotEmpty()) colors.borderFocus else colors.borderSubtle,
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍", style = JPadTheme.typography.uiSmall.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Filter rows...",
                                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted),
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                // Row Counter Badge
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "${displayRows.size} / ${table.rows.size} rows",
                        style = JPadTheme.typography.uiSmall.copy(
                            color = colors.accentPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                } else {
                    Text(
                        text = "${displayRows.size} rows",
                        style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary)
                    )
                }

                // Sorting Indicator & Reset
                if (currentSort != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.backgroundHover)
                            .clickable { currentSort = null }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sort: ${currentSort!!.column} ${if (currentSort!!.direction == DumpSortDirection.ASC) "▲" else "▼"}",
                            style = JPadTheme.typography.uiSmall.copy(color = colors.syntaxKeyword, fontSize = 11.sp)
                        )
                        Text(
                            text = "✕",
                            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 10.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Copied to Clipboard Notification
                AnimatedVisibility(
                    visible = copiedFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (copiedFeedback != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.runSuccess.copy(alpha = 0.2f))
                                .border(1.dp, colors.runSuccess, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = copiedFeedback!!,
                                style = JPadTheme.typography.uiSmall.copy(
                                    color = colors.runSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Export Buttons
                DumpExportButton(
                    label = "CSV",
                    onClick = {
                        val csv = DumpTableExporter.toCsv(table.columns, displayRows)
                        DumpTableExporter.copyToClipboard(csv)
                        copiedFeedback = "Copied CSV! ✓"
                        coroutineScope.launch {
                            delay(2000)
                            copiedFeedback = null
                        }
                    }
                )

                DumpExportButton(
                    label = "JSON",
                    onClick = {
                        val json = DumpTableExporter.toJson(table.columns, displayRows)
                        DumpTableExporter.copyToClipboard(json)
                        copiedFeedback = "Copied JSON! ✓"
                        coroutineScope.launch {
                            delay(2000)
                            copiedFeedback = null
                        }
                    }
                )

                DumpExportButton(
                    label = "TSV",
                    onClick = {
                        val tsv = DumpTableExporter.toTsv(table.columns, displayRows)
                        DumpTableExporter.copyToClipboard(tsv)
                        copiedFeedback = "Copied TSV! ✓"
                        coroutineScope.launch {
                            delay(2000)
                            copiedFeedback = null
                        }
                    }
                )
            }

            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundSurface)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column {
                    // Column Headers (with Sort Support)
                    Row(
                        modifier = Modifier
                            .background(colors.backgroundPanelHeader)
                            .border(0.5.dp, colors.gridLine),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Row index column header
                        Box(
                            modifier = Modifier
                                .width(rowIndexWidth)
                                .height(JPadTheme.dimensions.gridHeaderHeight)
                                .border(0.5.dp, colors.gridLine)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#",
                                style = JPadTheme.typography.uiSmall.copy(
                                    color = colors.textMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        table.columns.forEach { colName ->
                            val colWidth = columnWidths[colName] ?: 100.dp
                            val isSorted = currentSort?.column == colName
                            val sortIcon = if (isSorted) {
                                if (currentSort!!.direction == DumpSortDirection.ASC) " ▲" else " ▼"
                            } else ""

                            Box(
                                modifier = Modifier
                                    .width(colWidth)
                                    .height(JPadTheme.dimensions.gridHeaderHeight)
                                    .background(if (isSorted) colors.backgroundSelected.copy(alpha = 0.35f) else colors.backgroundPanelHeader)
                                    .border(0.5.dp, colors.gridLine)
                                    .clickable {
                                        currentSort = when {
                                            currentSort?.column == colName && currentSort?.direction == DumpSortDirection.ASC ->
                                                DumpColumnSort(colName, DumpSortDirection.DESC)
                                            currentSort?.column == colName && currentSort?.direction == DumpSortDirection.DESC ->
                                                null
                                            else ->
                                                DumpColumnSort(colName, DumpSortDirection.ASC)
                                        }
                                    }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = colName,
                                        style = JPadTheme.typography.uiSmall.copy(
                                            color = if (isSorted) colors.accentPrimary else colors.textPrimary,
                                            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (sortIcon.isNotEmpty()) {
                                        Text(
                                            text = sortIcon,
                                            style = JPadTheme.typography.uiSmall.copy(
                                                color = colors.accentPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Table Rows
                    if (displayRows.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "No rows match the filter \"$searchQuery\"",
                                style = JPadTheme.typography.uiSmall.copy(
                                    color = colors.textMuted,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    } else {
                        displayRows.forEachIndexed { index, rowMap ->
                            val rowBg = if (index % 2 == 0) colors.backgroundSurface else colors.backgroundPanel.copy(alpha = 0.5f)

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .border(0.5.dp, colors.gridLine),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Row number
                                Box(
                                    modifier = Modifier
                                        .width(rowIndexWidth)
                                        .defaultMinSize(minHeight = JPadTheme.dimensions.gridRowHeight)
                                        .border(0.5.dp, colors.gridLine)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = index.toString(),
                                        style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted)
                                    )
                                }

                                // Columns cells
                                table.columns.forEach { colName ->
                                    val cellVal = rowMap[colName] ?: DumpValue.Primitive(null)
                                    val colWidth = columnWidths[colName] ?: 100.dp
                                    Box(
                                        modifier = Modifier
                                            .width(colWidth)
                                            .defaultMinSize(minHeight = JPadTheme.dimensions.gridRowHeight)
                                            .border(0.5.dp, colors.gridLine),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        JPadDumpViewer(value = cellVal, depth = depth + 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DumpKeyValueView(
    kv: DumpValue.KeyValueList,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    var isExpanded by remember { mutableStateOf(true) }
    val colors = JPadTheme.colors
    val headerBg = if (depth == 0) colors.accentHeaderTable else colors.accentHeaderNested

    val maxKeyWidth = remember(kv.entries) {
        val charWidthDp = 8.5f
        val paddingDp = 20f
        val maxKeyChars = kv.entries.maxOfOrNull { it.first.length } ?: 10
        maxOf(110.dp, minOf((maxKeyChars * charWidthDp + paddingDp).dp, 300.dp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(3.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JPadTheme.dimensions.gridHeaderHeight)
                .background(headerBg)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isExpanded) "▼" else "▲",
                style = JPadTheme.typography.gridHeader.copy(color = colors.textOnAccent)
            )
            Text(
                text = kv.title,
                style = JPadTheme.typography.gridHeader.copy(color = colors.textOnAccent)
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundSurface)
            ) {
                kv.entries.forEachIndexed { index, (key, value) ->
                    val rowBg = if (index % 2 == 0) colors.backgroundSurface else colors.backgroundPanel.copy(alpha = 0.5f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .border(0.5.dp, colors.gridLine)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key,
                            style = JPadTheme.typography.uiSmall.copy(
                                color = colors.syntaxType,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.width(maxKeyWidth)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            JPadDumpViewer(value = value, depth = depth + 1)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun JPadDumpViewerPreview() {
    JPadTheme {
        Column(
            modifier = Modifier
                .background(JPadTheme.colors.backgroundWindow)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sampleTable = DumpValue.Table(
                typeName = "List<User>",
                columns = listOf("id", "name", "email", "isActive", "score"),
                rows = listOf(
                    mapOf(
                        "id" to DumpValue.Primitive(101),
                        "name" to DumpValue.Primitive("Ada Lovelace"),
                        "email" to DumpValue.Primitive("ada@jpad.io"),
                        "isActive" to DumpValue.Primitive(true),
                        "score" to DumpValue.Primitive(98.5)
                    ),
                    mapOf(
                        "id" to DumpValue.Primitive(102),
                        "name" to DumpValue.Primitive("James Gosling"),
                        "email" to DumpValue.Primitive(null),
                        "isActive" to DumpValue.Primitive(false),
                        "score" to DumpValue.Primitive(89.0)
                    )
                )
            )

            val sampleKv = DumpValue.KeyValueList(
                title = "Configuration (Properties)",
                entries = listOf(
                    "java.version" to DumpValue.Primitive("21.0.2"),
                    "os.name" to DumpValue.Primitive("Mac OS X"),
                    "jpad.mode" to DumpValue.Primitive("In-Memory JVM")
                )
            )

            Text("Dump Table Sample:", style = JPadTheme.typography.uiSmall.copy(color = JPadTheme.colors.textSecondary))
            JPadDumpViewer(value = sampleTable, modifier = Modifier.fillMaxWidth())

            Text("Dump Key-Value Sample:", style = JPadTheme.typography.uiSmall.copy(color = JPadTheme.colors.textSecondary))
            JPadDumpViewer(value = sampleKv, modifier = Modifier.fillMaxWidth())
        }
    }
}
