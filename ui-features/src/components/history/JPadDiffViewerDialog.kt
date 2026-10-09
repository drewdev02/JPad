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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.history.DiffLineType
import org.andrew.jpad.history.ExecutionSnapshot
import org.andrew.jpad.history.TextDiffUtil
import org.andrew.jpad.ui.theme.JPadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JPadDiffViewerDialog(
    snapshot: ExecutionSnapshot,
    currentCode: String,
    onRestore: (ExecutionSnapshot) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = JPadTheme.colors
    val diff = remember(snapshot.code, currentCode) {
        TextDiffUtil.computeDiff(snapshot.code, currentCode)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(560.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.backgroundWindow)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(colors.backgroundPanelHeader)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Diff: Snapshot vs Current Editor",
                        style = JPadTheme.typography.uiTitle.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "+${diff.addedCount} added, -${diff.removedCount} removed, ${diff.unchangedCount} unchanged",
                        style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 11.sp)
                    )
                }

                Text(
                    text = "✕",
                    style = JPadTheme.typography.uiTitle.copy(color = colors.textMuted),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            // Legend bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(colors.backgroundPanel)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(12.dp).height(12.dp).background(colors.errorRed.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Historical Only (-)", style = JPadTheme.typography.uiSmall.copy(fontSize = 10.sp, color = colors.textMuted))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(12.dp).height(12.dp).background(colors.runSuccess.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Current Editor Only (+)", style = JPadTheme.typography.uiSmall.copy(fontSize = 10.sp, color = colors.textMuted))
                }
            }

            // Diff Lines List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.backgroundEditor)
            ) {
                items(diff.lines) { line ->
                    val bgColor = when (line.type) {
                        DiffLineType.ADDED -> colors.runSuccess.copy(alpha = 0.15f)
                        DiffLineType.REMOVED -> colors.errorRed.copy(alpha = 0.15f)
                        DiffLineType.UNCHANGED -> Color.Transparent
                    }
                    val prefix = when (line.type) {
                        DiffLineType.ADDED -> "+"
                        DiffLineType.REMOVED -> "-"
                        DiffLineType.UNCHANGED -> " "
                    }
                    val prefixColor = when (line.type) {
                        DiffLineType.ADDED -> colors.runSuccess
                        DiffLineType.REMOVED -> colors.errorRed
                        DiffLineType.UNCHANGED -> colors.textMuted
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Old Line Number
                        Text(
                            text = line.oldLineNumber?.toString() ?: "",
                            style = JPadTheme.typography.codeSmall.copy(
                                color = colors.textMuted,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.width(32.dp)
                        )
                        // New Line Number
                        Text(
                            text = line.newLineNumber?.toString() ?: "",
                            style = JPadTheme.typography.codeSmall.copy(
                                color = colors.textMuted,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.width(32.dp)
                        )
                        // Prefix (+ / - / space)
                        Text(
                            text = prefix,
                            style = JPadTheme.typography.codeSmall.copy(
                                color = prefixColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.width(14.dp)
                        )
                        // Line content
                        Text(
                            text = line.text,
                            style = JPadTheme.typography.code.copy(
                                color = colors.textPrimary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.backgroundPanelHeader)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.backgroundPanel)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Close", style = JPadTheme.typography.uiBody.copy(fontSize = 12.sp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.accentPrimary)
                        .clickable {
                            onRestore(snapshot)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "↶ Restore Snapshot",
                        style = JPadTheme.typography.uiBody.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
