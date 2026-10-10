package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

enum class ScriptLanguageMode(val displayName: String) {
    JAVA_STATEMENTS("Java Statement(s)"),
    JAVA_PROGRAM("Java Program / Class"),
    JAVA_EXPRESSION("Java Expression"),
    SQL("SQL Query")
}

@Composable
fun JPadToolbar(
    languageMode: ScriptLanguageMode,
    onLanguageModeChange: (ScriptLanguageMode) -> Unit,
    onRunClick: () -> Unit,
    onStopClick: () -> Unit,
    isRunning: Boolean,
    onSaveClick: (() -> Unit)? = null,
    onOpenClick: (() -> Unit)? = null,
    onNewQueryClick: (() -> Unit)? = null,
    isSidebarVisible: Boolean = true,
    onToggleSidebar: (() -> Unit)? = null,
    onToggleHistory: (() -> Unit)? = null,
    onOpenCommandPalette: (() -> Unit)? = null,
    onShowShortcuts: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JPadTheme.dimensions.toolbarHeight)
            .background(colors.backgroundPanel)
            .border(1.dp, colors.borderSubtle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Run button (Play)
        JPadToolbarButton(
            onClick = onRunClick,
            text = "Run",
            shortcut = "F5",
            style = JPadButtonStyle.RUN,
            enabled = !isRunning,
            icon = {
                Text("▶", style = JPadTheme.typography.uiSmall.copy(color = colors.runSuccess))
            }
        )

        // Stop button
        JPadToolbarButton(
            onClick = onStopClick,
            text = "Stop",
            shortcut = "Shift+F5",
            style = JPadButtonStyle.STOP,
            enabled = isRunning,
            icon = {
                Text("■", style = JPadTheme.typography.uiSmall.copy(color = colors.stopRed))
            }
        )

        onOpenClick?.let { onOpen ->
            JPadToolbarButton(
                onClick = onOpen,
                text = "Open",
                shortcut = "Ctrl+O",
                style = JPadButtonStyle.DEFAULT,
                icon = {
                    Text("📂", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        onSaveClick?.let { onSave ->
            JPadToolbarButton(
                onClick = onSave,
                text = "Save",
                shortcut = "Ctrl+S",
                style = JPadButtonStyle.DEFAULT,
                icon = {
                    Text("💾", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        onNewQueryClick?.let { onNew ->
            JPadToolbarButton(
                onClick = onNew,
                text = "New",
                shortcut = "Ctrl+N",
                style = JPadButtonStyle.DEFAULT,
                icon = {
                    Text("+", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        onToggleSidebar?.let { onToggle ->
            JPadToolbarButton(
                onClick = onToggle,
                text = "Explorer",
                shortcut = "Ctrl+B",
                style = if (isSidebarVisible) JPadButtonStyle.DEFAULT else JPadButtonStyle.GHOST,
                icon = {
                    Text(if (isSidebarVisible) "◧" else "◻", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        onToggleHistory?.let { onHist ->
            JPadToolbarButton(
                onClick = onHist,
                text = "History",
                shortcut = "Ctrl+H",
                style = JPadButtonStyle.DEFAULT,
                icon = {
                    Text("🕒", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        onOpenCommandPalette?.let { onPalette ->
            JPadToolbarButton(
                onClick = onPalette,
                text = "Commands",
                shortcut = "Cmd+K",
                style = JPadButtonStyle.DEFAULT,
                icon = {
                    Text("⌘", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        // Separator
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(colors.borderStrong)
        )

        // Language Mode Dropdown
        val languageOptions = listOf(
            DropdownOption(ScriptLanguageMode.JAVA_STATEMENTS, ScriptLanguageMode.JAVA_STATEMENTS.displayName),
            DropdownOption(ScriptLanguageMode.JAVA_PROGRAM, ScriptLanguageMode.JAVA_PROGRAM.displayName),
            DropdownOption(ScriptLanguageMode.JAVA_EXPRESSION, ScriptLanguageMode.JAVA_EXPRESSION.displayName),
            DropdownOption(ScriptLanguageMode.SQL, ScriptLanguageMode.SQL.displayName)
        )

        JPadDropdown(
            options = languageOptions,
            selectedOption = languageMode,
            onOptionSelected = onLanguageModeChange,
            labelPrefix = "Language:"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Right side info / action buttons (Samples, Shortcuts)
        onShowShortcuts?.let { onShortcuts ->
            JPadToolbarButton(
                onClick = onShortcuts,
                text = "Shortcuts",
                shortcut = "F1",
                style = JPadButtonStyle.GHOST,
                icon = {
                    Text("⌨️", style = JPadTheme.typography.uiSmall)
                }
            )
        }

        JPadToolbarButton(
            onClick = { /* Help */ },
            text = "Samples",
            style = JPadButtonStyle.GHOST
        )
    }
}

@Preview
@Composable
private fun JPadToolbarPreview() {
    JPadTheme {
        var mode by remember { mutableStateOf(ScriptLanguageMode.JAVA_STATEMENTS) }
        var isRunning by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .background(JPadTheme.colors.backgroundWindow)
                .padding(12.dp)
        ) {
            JPadToolbar(
                languageMode = mode,
                onLanguageModeChange = { mode = it },
                onRunClick = { isRunning = true },
                onStopClick = { isRunning = false },
                isRunning = isRunning
            )
        }
    }
}
