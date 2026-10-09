package org.andrew.jpad.ui.components.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.history.ExecutionSnapshot
import org.andrew.jpad.ui.theme.JPadTheme
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JPadHistoryPanel(
    history: List<ExecutionSnapshot>,
    selectedSnapshotId: String?,
    onSelectSnapshot: (ExecutionSnapshot) -> Unit,
    onRestoreSnapshot: (ExecutionSnapshot) -> Unit,
    onCompareDiff: (ExecutionSnapshot) -> Unit,
    onDeleteSnapshot: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val filteredList = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                    it.languageMode.contains(searchQuery, ignoreCase = true) ||
                    it.tabTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSidebar)
    ) {
        // Search & Filter header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                placeholder = {
                    Text(
                        "Filter history...",
                        style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 11.sp)
                    )
                },
                textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary, fontSize = 11.sp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.backgroundInput,
                    unfocusedContainerColor = colors.backgroundInput,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✕",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted),
                    modifier = Modifier
                        .clickable { searchQuery = "" }
                        .padding(4.dp)
                )
            }
        }

        // Action Subheader: Count + Clear All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(colors.backgroundPanelHeader)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RUNS (${filteredList.size})",
                style = JPadTheme.typography.uiSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            )

            if (history.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    style = JPadTheme.typography.uiSmall.copy(
                        color = colors.textMuted,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable { onClearHistory() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (history.isEmpty()) "No execution history yet.\nRun code to see past snapshots." else "No matching snapshots.",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                items(filteredList, key = { it.id }) { snapshot ->
                    val isSelected = snapshot.id == selectedSnapshotId
                    val timeFormatted = remember(snapshot.timestamp) {
                        dateFormat.format(Date(snapshot.timestamp))
                    }
                    val firstLine = remember(snapshot.code) {
                        snapshot.code.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "(empty)"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) colors.backgroundSelected.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable { onSelectSnapshot(snapshot) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        // Title row: Status Dot, Mode badge, Time, Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Status dot
                                val dotColor = if (snapshot.status == "SUCCESS") colors.runSuccess else colors.errorRed
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                // Mode badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colors.backgroundPanelHeader)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = snapshot.languageMode.take(6),
                                        style = JPadTheme.typography.codeSmall.copy(
                                            fontSize = 9.sp,
                                            color = colors.syntaxKeyword,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                // Time
                                Text(
                                    text = timeFormatted,
                                    style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary, fontSize = 11.sp)
                                )
                            }

                            // Duration
                            Text(
                                text = "${snapshot.durationMs}ms",
                                style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 10.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Code snippet preview
                        Text(
                            text = firstLine,
                            style = JPadTheme.typography.codeSmall.copy(
                                color = colors.textPrimary,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // If expanded/selected, show detailed actions
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Restore
                                ActionButton(
                                    label = "↶ Restore",
                                    tooltip = "Restore code and mode to current editor tab",
                                    color = colors.runGreen
                                ) {
                                    onRestoreSnapshot(snapshot)
                                }

                                // Compare Diff
                                ActionButton(
                                    label = "🔍 Diff",
                                    tooltip = "Compare differences with current editor code",
                                    color = colors.accentPrimary
                                ) {
                                    onCompareDiff(snapshot)
                                }

                                // Copy
                                ActionButton(
                                    label = "📋 Copy",
                                    tooltip = "Copy snippet to clipboard",
                                    color = colors.textSecondary
                                ) {
                                    try {
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                            StringSelection(snapshot.code),
                                            null
                                        )
                                    } catch (_: Exception) {}
                                }

                                // Delete
                                ActionButton(
                                    label = "🗑️",
                                    tooltip = "Delete snapshot",
                                    color = colors.errorRed
                                ) {
                                    onDeleteSnapshot(snapshot.id)
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
private fun ActionButton(
    label: String,
    tooltip: String,
    color: Color,
    onClick: () -> Unit
) {
    val colors = JPadTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(colors.backgroundPanelHeader)
            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = JPadTheme.typography.uiSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        )
    }
}
