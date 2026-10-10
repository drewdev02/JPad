package org.andrew.jpad.ui.components.palette

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.ui.theme.JPadTheme

data class ShortcutEntry(
    val action: String,
    val shortcut: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JPadShortcutsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    val shortcuts = listOf(
        ShortcutEntry("Run Script / Query", "F5", "Execution"),
        ShortcutEntry("Stop Execution", "Shift + F5", "Execution"),
        ShortcutEntry("Open Command Palette", "Ctrl+K / Cmd+K", "General"),
        ShortcutEntry("Open Command Palette (Alt)", "Ctrl+Shift+P / Cmd+Shift+P", "General"),
        ShortcutEntry("Trigger Code Autocompletion", "Ctrl + Space", "Editor"),
        ShortcutEntry("New Query Tab", "Ctrl+N / Cmd+N", "Tabs & Files"),
        ShortcutEntry("Close Current Tab", "Ctrl+W / Cmd+W", "Tabs & Files"),
        ShortcutEntry("Save Current File", "Ctrl+S / Cmd+S", "Tabs & Files"),
        ShortcutEntry("Save As...", "Ctrl+Shift+S / Cmd+Shift+S", "Tabs & Files"),
        ShortcutEntry("Open File...", "Ctrl+O / Cmd+O", "Tabs & Files"),
        ShortcutEntry("Toggle Sidebar Explorer", "Ctrl+B / Cmd+B", "Panels"),
        ShortcutEntry("Toggle Execution History", "Ctrl+H / Cmd+H", "Panels"),
        ShortcutEntry("Close Dialogs / Dismiss", "Esc", "General")
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth(0.55f)
            .height(520.dp)
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
                Text(
                    text = "⌨️ Keyboard Shortcuts Cheat Sheet",
                    style = JPadTheme.typography.uiTitle.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "✕",
                    style = JPadTheme.typography.uiTitle.copy(color = colors.textMuted),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            // Shortcuts table
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val grouped = shortcuts.groupBy { it.category }
                grouped.forEach { (category, entries) ->
                    item {
                        Text(
                            text = category.uppercase(),
                            style = JPadTheme.typography.uiSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentPrimary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(entries) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.backgroundPanel.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.action,
                                style = JPadTheme.typography.uiBody.copy(fontSize = 12.sp)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.backgroundPanelHeader)
                                    .border(0.5.dp, colors.borderStrong, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.shortcut,
                                    style = JPadTheme.typography.codeSmall.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
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
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("Close", style = JPadTheme.typography.uiSmall.copy(fontSize = 11.sp))
                }
            }
        }
    }
}
