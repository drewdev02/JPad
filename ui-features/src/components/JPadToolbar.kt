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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

enum class ScriptLanguageMode(val displayName: String) {
    JAVA_STATEMENTS("Java Statement(s)"),
    JAVA_PROGRAM("Java Program / Class"),
    JAVA_EXPRESSION("Java Expression")
}

@Composable
fun JPadToolbar(
    languageMode: ScriptLanguageMode,
    onLanguageModeChange: (ScriptLanguageMode) -> Unit,
    onRunClick: () -> Unit,
    onStopClick: () -> Unit,
    isRunning: Boolean,
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
            DropdownOption(ScriptLanguageMode.JAVA_EXPRESSION, ScriptLanguageMode.JAVA_EXPRESSION.displayName)
        )

        JPadDropdown(
            options = languageOptions,
            selectedOption = languageMode,
            onOptionSelected = onLanguageModeChange,
            labelPrefix = "Language:"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Right side info / action buttons (Samples, Help)
        JPadToolbarButton(
            onClick = { /* Help */ },
            text = "Samples",
            style = JPadButtonStyle.GHOST
        )
    }
}
