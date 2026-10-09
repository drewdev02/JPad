package org.andrew.jpad.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.andrew.jpad.core.database.DatabaseConnectionConfig
import org.andrew.jpad.core.database.DatabaseDriverType
import org.andrew.jpad.core.database.DatabaseTableMeta
import org.andrew.jpad.ui.theme.JPadTheme

@Composable
fun JPadDatabaseExplorer(
    connections: List<DatabaseConnectionConfig>,
    schemas: Map<String, List<DatabaseTableMeta>>,
    selectedConnectionId: String?,
    onSelectConnection: (String) -> Unit,
    onQueryTable: (connectionId: String, tableName: String) -> Unit,
    onAddConnection: (DatabaseConnectionConfig) -> Unit,
    onRefresh: () -> Unit,
    onTestConnection: (DatabaseConnectionConfig) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedConnections by remember { mutableStateOf(setOf("sample-sqlite")) }
    var expandedTables by remember { mutableStateOf(emptySet<String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSidebar)
    ) {
        // Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(colors.backgroundPanelHeader)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CONNECTIONS",
                style = JPadTheme.typography.uiSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Add Connection
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+ New",
                        style = JPadTheme.typography.uiSmall.copy(
                            color = colors.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Refresh
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable { onRefresh() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⟳",
                        style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary)
                    )
                }
            }
        }

        // Connections & Tables List
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 4.dp)
        ) {
            if (connections.isEmpty()) {
                Text(
                    text = "No database connections configured.",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted),
                    modifier = Modifier.padding(12.dp)
                )
            }

            connections.forEach { conn ->
                val isConnExpanded = expandedConnections.contains(conn.id)
                val isSelected = selectedConnectionId == conn.id
                val tableList = schemas[conn.id] ?: emptyList()

                // Connection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(if (isSelected) colors.backgroundSelected.copy(alpha = 0.35f) else colors.backgroundSidebar)
                        .clickable {
                            onSelectConnection(conn.id)
                            expandedConnections = if (isConnExpanded) {
                                expandedConnections - conn.id
                            } else {
                                expandedConnections + conn.id
                            }
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isConnExpanded) "▼" else "▶",
                        style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 9.sp)
                    )
                    Text(
                        text = "⚡",
                        style = JPadTheme.typography.uiSmall.copy(fontSize = 11.sp)
                    )
                    Text(
                        text = conn.name,
                        style = JPadTheme.typography.uiSmall.copy(
                            color = if (isSelected) colors.accentPrimary else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tables under connection
                if (isConnExpanded) {
                    if (tableList.isEmpty()) {
                        Text(
                            text = "No tables found or not connected",
                            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 10.sp),
                            modifier = Modifier.padding(start = 28.dp, top = 2.dp, bottom = 2.dp)
                        )
                    } else {
                        tableList.forEach { table ->
                            val tableKey = "${conn.id}_${table.name}"
                            val isTableExpanded = expandedTables.contains(tableKey)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clickable {
                                        expandedTables = if (isTableExpanded) {
                                            expandedTables - tableKey
                                        } else {
                                            expandedTables + tableKey
                                        }
                                    }
                                    .padding(start = 20.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isTableExpanded) "▼" else "▶",
                                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 8.sp)
                                )
                                Text(
                                    text = "📋",
                                    style = JPadTheme.typography.uiSmall.copy(fontSize = 10.sp)
                                )
                                Text(
                                    text = table.name,
                                    style = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(colors.backgroundPanelHeader)
                                        .clickable { onQueryTable(conn.id, table.name) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Query",
                                        style = JPadTheme.typography.uiSmall.copy(
                                            color = colors.accentPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Columns under table
                            if (isTableExpanded) {
                                table.columns.forEach { col ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .padding(start = 36.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (col.isPrimaryKey) {
                                            Text(
                                                text = "🔑",
                                                style = JPadTheme.typography.uiSmall.copy(fontSize = 9.sp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.textMuted)
                                            )
                                        }
                                        Text(
                                            text = col.name,
                                            style = JPadTheme.typography.uiSmall.copy(
                                                color = if (col.isPrimaryKey) colors.accentPrimary else colors.textSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = col.typeName,
                                            style = JPadTheme.typography.uiSmall.copy(
                                                color = colors.textMuted,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddConnectionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newConn ->
                onAddConnection(newConn)
                showAddDialog = false
            },
            onTest = onTestConnection
        )
    }
}

@Composable
fun AddConnectionDialog(
    onDismiss: () -> Unit,
    onSave: (DatabaseConnectionConfig) -> Unit,
    onTest: (DatabaseConnectionConfig) -> Result<Unit>
) {
    val colors = JPadTheme.colors
    var name by remember { mutableStateOf("New Database Connection") }
    var selectedType by remember { mutableStateOf(DatabaseDriverType.SQLITE) }
    var jdbcUrl by remember { mutableStateOf("jdbc:sqlite:") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.backgroundPanel)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add Database Connection",
                    style = JPadTheme.typography.uiBody.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Name field
                Column {
                    Text("Connection Name", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(colors.backgroundInput, RoundedCornerShape(4.dp))
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                // Driver selector
                Column {
                    Text("Database Driver", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DatabaseDriverType.SQLITE, DatabaseDriverType.H2, DatabaseDriverType.POSTGRESQL).forEach { type ->
                            val isSel = selectedType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) colors.accentPrimary else colors.backgroundInput)
                                    .clickable {
                                        selectedType = type
                                        jdbcUrl = when (type) {
                                            DatabaseDriverType.SQLITE -> "jdbc:sqlite:test.db"
                                            DatabaseDriverType.H2 -> "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                                            DatabaseDriverType.POSTGRESQL -> "jdbc:postgresql://localhost:5432/mydb"
                                            else -> "jdbc:"
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = type.displayName,
                                    style = JPadTheme.typography.uiSmall.copy(
                                        color = if (isSel) colors.textOnAccent else colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // JDBC URL field
                Column {
                    Text("JDBC URL", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = jdbcUrl,
                        onValueChange = { jdbcUrl = it },
                        singleLine = true,
                        textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(colors.backgroundInput, RoundedCornerShape(4.dp))
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                // Username & Password
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("User", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                        Spacer(Modifier.height(4.dp))
                        BasicTextField(
                            value = username,
                            onValueChange = { username = it },
                            singleLine = true,
                            textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accentPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(colors.backgroundInput, RoundedCornerShape(4.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Password", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                        Spacer(Modifier.height(4.dp))
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it },
                            singleLine = true,
                            textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accentPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(colors.backgroundInput, RoundedCornerShape(4.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                if (testMessage != null) {
                    Text(
                        text = testMessage!!,
                        style = JPadTheme.typography.uiSmall.copy(
                            color = if (isTestSuccess) colors.runSuccess else colors.errorRed
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Actions: Test, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.backgroundHover)
                            .clickable {
                                val cfg = DatabaseConnectionConfig(
                                    id = System.currentTimeMillis().toString(),
                                    name = name,
                                    type = selectedType,
                                    jdbcUrl = jdbcUrl,
                                    username = username,
                                    password = password
                                )
                                val res = onTest(cfg)
                                if (res.isSuccess) {
                                    isTestSuccess = true
                                    testMessage = "Connection successful!"
                                } else {
                                    isTestSuccess = false
                                    testMessage = "Failed: ${res.exceptionOrNull()?.message}"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Test", style = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary))
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.backgroundHover)
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Cancel", style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary))
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accentPrimary)
                            .clickable {
                                val cfg = DatabaseConnectionConfig(
                                    id = System.currentTimeMillis().toString(),
                                    name = name,
                                    type = selectedType,
                                    jdbcUrl = jdbcUrl,
                                    username = username,
                                    password = password
                                )
                                onSave(cfg)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Save & Connect", style = JPadTheme.typography.uiSmall.copy(color = colors.textOnAccent, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
