package org.andrew.jpad.ui.components.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

private fun calculateColumnWidths(table: DumpValue.Table): Map<String, Dp> {
    val charWidthDp = 8.5f
    val cellPaddingDp = 24f

    return table.columns.associateWith { colName ->
        var maxChars = colName.length
        table.rows.forEach { rowMap ->
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
    val colors = JPadTheme.colors
    val headerBg = if (depth == 0) colors.accentHeaderTable else colors.accentHeaderNested

    val columnWidths = remember(table) { calculateColumnWidths(table) }
    val rowIndexWidth = remember(table.rows.size) {
        val digits = maxOf(1, table.rows.size.toString().length)
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
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundSurface)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column {
                    // Column Headers
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
                            Box(
                                modifier = Modifier
                                    .width(colWidth)
                                    .height(JPadTheme.dimensions.gridHeaderHeight)
                                    .border(0.5.dp, colors.gridLine)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = colName,
                                    style = JPadTheme.typography.uiSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Table Rows
                    table.rows.forEachIndexed { index, rowMap ->
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
