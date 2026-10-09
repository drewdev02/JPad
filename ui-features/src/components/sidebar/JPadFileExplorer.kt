package org.andrew.jpad.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.ui.theme.JPadTheme
import java.io.File

data class FileExplorerItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val extension: String = if (file.isDirectory) "" else file.extension.lowercase(),
    val depth: Int = 0,
    val isExpanded: Boolean = false,
    val isSelected: Boolean = false,
    val isOpenInTab: Boolean = false
)

data class OpenScriptItem(
    val id: String,
    val title: String,
    val isModified: Boolean = false,
    val isSelected: Boolean = false
)

fun buildVisibleFileTree(
    root: File,
    expanded: Set<String>,
    activeFilePath: String? = null,
    openFilePaths: Set<String> = emptySet(),
    depth: Int = 0
): List<FileExplorerItem> {
    if (!root.exists() || !root.isDirectory) return emptyList()

    val children = root.listFiles()?.filterNot { it.name.startsWith(".") } ?: return emptyList()
    val sorted = children.sortedWith(
        compareBy<File> { !it.isDirectory }
            .thenBy { it.name.lowercase() }
    )

    val result = mutableListOf<FileExplorerItem>()
    for (child in sorted) {
        val isExpanded = child.isDirectory && expanded.contains(child.absolutePath)
        val isSelected = child.absolutePath == activeFilePath
        val isOpenInTab = openFilePaths.contains(child.absolutePath)

        result.add(
            FileExplorerItem(
                file = child,
                name = child.name,
                isDirectory = child.isDirectory,
                extension = if (child.isDirectory) "" else child.extension.lowercase(),
                depth = depth,
                isExpanded = isExpanded,
                isSelected = isSelected,
                isOpenInTab = isOpenInTab
            )
        )

        if (isExpanded) {
            result.addAll(
                buildVisibleFileTree(
                    root = child,
                    expanded = expanded,
                    activeFilePath = activeFilePath,
                    openFilePaths = openFilePaths,
                    depth = depth + 1
                )
            )
        }
    }
    return result
}

@Composable
fun JPadFileExplorer(
    workspaceDirectory: String,
    items: List<FileExplorerItem>,
    openScripts: List<OpenScriptItem> = emptyList(),
    isCreatingFile: Boolean = false,
    onFileSelected: (String) -> Unit,
    onScriptSelected: (String) -> Unit = {},
    onScriptClosed: (String) -> Unit = {},
    onToggleDirectory: (String) -> Unit,
    onOpenFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onNewFileClick: () -> Unit,
    onConfirmCreateFile: (String) -> Unit,
    onCancelCreateFile: () -> Unit,
    onCollapseSidebar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    var isOpenScriptsExpanded by remember { mutableStateOf(true) }
    var isWorkspaceExpanded by remember { mutableStateOf(true) }

    val workspaceFolder = remember(workspaceDirectory) { File(workspaceDirectory) }
    val workspaceFolderName = workspaceFolder.name.ifBlank { "Workspace" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSidebar)
    ) {
        // Top Header
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
                text = "EXPLORER",
                style = JPadTheme.typography.uiSmall.copy(
                    color = colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SidebarIconButton(
                    icon = "+",
                    tooltip = "New Script",
                    onClick = onNewFileClick
                )
                SidebarIconButton(
                    icon = "📂",
                    tooltip = "Open Folder",
                    onClick = onOpenFolderClick
                )
                SidebarIconButton(
                    icon = "🔄",
                    tooltip = "Refresh",
                    onClick = onRefreshClick
                )
                if (onCollapseSidebar != null) {
                    SidebarIconButton(
                        icon = "◀",
                        tooltip = "Hide Sidebar",
                        onClick = onCollapseSidebar
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Section 1: OPEN SCRIPTS
            if (openScripts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "OPEN SCRIPTS",
                        badge = openScripts.size.toString(),
                        isExpanded = isOpenScriptsExpanded,
                        onToggle = { isOpenScriptsExpanded = !isOpenScriptsExpanded }
                    )
                }

                if (isOpenScriptsExpanded) {
                    items(openScripts, key = { "tab_${it.id}" }) { script ->
                        OpenScriptRow(
                            item = script,
                            onClick = { onScriptSelected(script.id) },
                            onClose = { onScriptClosed(script.id) }
                        )
                    }
                }
            }

            // Section 2: WORKSPACE
            item {
                SectionHeader(
                    title = workspaceFolderName.uppercase(),
                    badge = if (items.isNotEmpty()) items.count { !it.isDirectory }.toString() else null,
                    isExpanded = isWorkspaceExpanded,
                    onToggle = { isWorkspaceExpanded = !isWorkspaceExpanded }
                )
            }

            if (isWorkspaceExpanded) {
                // Inline New File Input
                if (isCreatingFile) {
                    item {
                        InlineCreateFileInput(
                            onConfirm = onConfirmCreateFile,
                            onCancel = onCancelCreateFile
                        )
                    }
                }

                if (items.isEmpty() && !isCreatingFile) {
                    item {
                        EmptyWorkspaceView(
                            folderName = workspaceFolderName,
                            onNewFileClick = onNewFileClick,
                            onOpenFolderClick = onOpenFolderClick
                        )
                    }
                } else {
                    items(items, key = { it.file.absolutePath }) { item ->
                        FileTreeItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    onToggleDirectory(item.file.absolutePath)
                                } else {
                                    onFileSelected(item.file.absolutePath)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badge: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = JPadTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(colors.backgroundPanel.copy(alpha = 0.5f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isExpanded) "▾ " else "▸ ",
            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 9.sp)
        )
        Text(
            text = title,
            style = JPadTheme.typography.uiSmall.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                style = JPadTheme.typography.uiSmall.copy(
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun OpenScriptRow(
    item: OpenScriptItem,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = JPadTheme.colors
    val backgroundColor = if (item.isSelected) colors.backgroundSelected else colors.backgroundSidebar
    val textColor = if (item.isSelected) colors.textOnAccent else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (item.isModified) "● " else "📄 ",
            style = JPadTheme.typography.uiSmall.copy(
                color = if (item.isModified) colors.warningAmber else colors.accentPrimary,
                fontSize = 11.sp
            )
        )
        Text(
            text = item.title,
            style = JPadTheme.typography.uiSmall.copy(color = textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                style = JPadTheme.typography.uiSmall.copy(
                    color = colors.textMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
private fun FileTreeItemRow(
    item: FileExplorerItem,
    onClick: () -> Unit
) {
    val colors = JPadTheme.colors
    val backgroundColor = if (item.isSelected) colors.backgroundSelected else colors.backgroundSidebar
    val textColor = if (item.isSelected) colors.textOnAccent else colors.textPrimary
    val indentDp = (item.depth * 14 + 10).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(start = indentDp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isDirectory) {
            Text(
                text = if (item.isExpanded) "▾ " else "▸ ",
                style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 9.sp)
            )
            Text(
                text = if (item.isExpanded) "📂 " else "📁 ",
                style = JPadTheme.typography.uiSmall.copy(fontSize = 11.sp)
            )
        } else {
            Spacer(modifier = Modifier.width(10.dp))
            val icon = when (item.extension) {
                "java" -> "☕ "
                "jpad" -> "📝 "
                "sql" -> "🗄️ "
                "json", "xml", "yaml", "yml" -> "⚙️ "
                else -> "📄 "
            }
            Text(
                text = icon,
                style = JPadTheme.typography.uiSmall.copy(fontSize = 11.sp)
            )
        }

        Text(
            text = item.name,
            style = JPadTheme.typography.uiSmall.copy(color = textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (item.isOpenInTab && !item.isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.accentPrimary, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun InlineCreateFileInput(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val colors = JPadTheme.colors
    var text by remember { mutableStateOf("NewScript.java") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(colors.backgroundPanelHeader)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("☕ ", style = JPadTheme.typography.uiSmall.copy(fontSize = 11.sp))

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (text.isNotBlank()) onConfirm(text.trim()) else onCancel()
            }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .background(colors.backgroundInput, RoundedCornerShape(2.dp))
                .border(1.dp, colors.borderFocus, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .onKeyEvent { event ->
                    if (event.key == Key.Enter) {
                        if (text.isNotBlank()) onConfirm(text.trim()) else onCancel()
                        true
                    } else if (event.key == Key.Escape) {
                        onCancel()
                        true
                    } else false
                }
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "✔",
            style = JPadTheme.typography.uiSmall.copy(color = colors.runSuccess, fontSize = 11.sp),
            modifier = Modifier
                .clickable { if (text.isNotBlank()) onConfirm(text.trim()) else onCancel() }
                .padding(2.dp)
        )

        Text(
            text = "✖",
            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 10.sp),
            modifier = Modifier
                .clickable { onCancel() }
                .padding(2.dp)
        )
    }
}

@Composable
private fun EmptyWorkspaceView(
    folderName: String,
    onNewFileClick: () -> Unit,
    onOpenFolderClick: () -> Unit
) {
    val colors = JPadTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Folder is empty",
            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted)
        )

        Box(
            modifier = Modifier
                .background(colors.accentPrimary, RoundedCornerShape(4.dp))
                .clickable(onClick = onNewFileClick)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "+ Create Script",
                style = JPadTheme.typography.uiSmall.copy(color = colors.textOnAccent)
            )
        }

        Text(
            text = "Open another folder",
            style = JPadTheme.typography.uiSmall.copy(color = colors.syntaxKeyword),
            modifier = Modifier.clickable(onClick = onOpenFolderClick)
        )
    }
}

@Composable
private fun SidebarIconButton(
    icon: String,
    tooltip: String,
    onClick: () -> Unit
) {
    val colors = JPadTheme.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(colors.backgroundPanelHeader)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            style = JPadTheme.typography.uiSmall.copy(
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        )
    }
}
