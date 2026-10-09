package org.andrew.jpad.ui.components.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.ui.theme.JPadTheme

@Composable
fun JPadProblemsViewer(
    diagnostics: List<CompilationDiagnostic>,
    onSelectProblem: (line: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    if (diagnostics.isEmpty()) {
        Box(
            modifier = modifier.padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "✓ No problems detected in the current query.",
                style = JPadTheme.typography.uiSmall.copy(color = colors.runSuccess)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(diagnostics) { diag ->
            val isError = diag.kind == DiagnosticKind.ERROR
            val icon = if (isError) "ⓧ" else "⚠"
            val badgeColor = if (isError) colors.errorRed else colors.warningAmber

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPanel, RoundedCornerShape(4.dp))
                    .clickable { onSelectProblem(diag.line.toInt(), diag.column.toInt()) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    style = JPadTheme.typography.uiSmall.copy(
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ln ${diag.line}, Col ${diag.column}",
                        style = JPadTheme.typography.code.copy(
                            color = badgeColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = diag.message,
                    style = JPadTheme.typography.uiSmall.copy(
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
