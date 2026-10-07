package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

enum class EngineState {
    READY,
    COMPILING,
    EXECUTING,
    SUCCESS,
    ERROR
}

@Composable
fun JPadStatusBar(
    state: EngineState,
    statusMessage: String,
    executionTimeMs: Long? = null,
    cursorPosition: Pair<Int, Int>? = null, // line, col
    memoryUsageMb: Long? = null,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    val statusDotColor = when (state) {
        EngineState.READY -> colors.textMuted
        EngineState.COMPILING -> colors.warningAmber
        EngineState.EXECUTING -> colors.accentPrimary
        EngineState.SUCCESS -> colors.runSuccess
        EngineState.ERROR -> colors.errorRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JPadTheme.dimensions.statusBarHeight)
            .background(colors.backgroundPanel)
            .border(1.dp, colors.borderSubtle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status indicator dot + message
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Text(
                text = statusMessage,
                style = JPadTheme.typography.uiSmall.copy(color = colors.textPrimary)
            )
        }

        if (executionTimeMs != null) {
            Text(
                text = "⏱ ${(executionTimeMs / 1000.0).toString().take(5)}s",
                style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (cursorPosition != null) {
            Text(
                text = "Ln ${cursorPosition.first}, Col ${cursorPosition.second}",
                style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary)
            )
        }

        if (memoryUsageMb != null) {
            Text(
                text = "RAM: ${memoryUsageMb} MB",
                style = JPadTheme.typography.uiSmall.copy(color = colors.textSecondary)
            )
        }

        Text(
            text = "UTF-8",
            style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted)
        )
    }
}

@Preview
@Composable
private fun JPadStatusBarPreview() {
    JPadTheme {
        Column(
            modifier = Modifier
                .background(JPadTheme.colors.backgroundWindow)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JPadStatusBar(
                state = EngineState.READY,
                statusMessage = "Ready",
                cursorPosition = Pair(1, 1),
                memoryUsageMb = 42
            )
            JPadStatusBar(
                state = EngineState.COMPILING,
                statusMessage = "Compiling snippet in memory...",
                cursorPosition = Pair(12, 8),
                memoryUsageMb = 64
            )
            JPadStatusBar(
                state = EngineState.EXECUTING,
                statusMessage = "Executing isolated worker process...",
                cursorPosition = Pair(12, 8),
                memoryUsageMb = 98
            )
            JPadStatusBar(
                state = EngineState.SUCCESS,
                statusMessage = "Query completed successfully",
                executionTimeMs = 38,
                cursorPosition = Pair(15, 2),
                memoryUsageMb = 75
            )
            JPadStatusBar(
                state = EngineState.ERROR,
                statusMessage = "Compilation error: cannot find symbol",
                cursorPosition = Pair(8, 21),
                memoryUsageMb = 72
            )
        }
    }
}
