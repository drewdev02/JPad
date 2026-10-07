package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

@Composable
fun JPadWorkspaceLayout(
    toolbar: @Composable () -> Unit,
    queryTabBar: @Composable () -> Unit,
    sidebar: (@Composable () -> Unit)? = null,
    editor: @Composable () -> Unit,
    resultsPanel: @Composable () -> Unit,
    statusBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var sidebarWidthDp by remember { mutableStateOf(200.dp) }
    var resultsHeightDp by remember { mutableStateOf(240.dp) }

    val colors = JPadTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundWindow)
    ) {
        // Top Toolbar
        toolbar()

        // Main content area (Sidebar + Central editor/results)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Optional Sidebar
            if (sidebar != null) {
                Box(
                    modifier = Modifier
                        .width(sidebarWidthDp)
                        .fillMaxHeight()
                        .background(colors.backgroundSidebar)
                ) {
                    sidebar()
                }

                // Sidebar Vertical Splitter
                JPadVerticalSplitter(
                    onDelta = { delta ->
                        sidebarWidthDp = (sidebarWidthDp.value + delta * 0.5f).coerceIn(120f, 400f).dp
                    }
                )
            }

            // Central Area (Query Tabs, Editor, Horizontal Splitter, Results)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Query Tabs
                queryTabBar()

                // Code Editor Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colors.backgroundEditor)
                ) {
                    editor()
                }

                // Horizontal Splitter between Editor and Results
                JPadHorizontalSplitter(
                    onDelta = { delta ->
                        resultsHeightDp = (resultsHeightDp.value - delta * 0.5f).coerceIn(100f, 600f).dp
                    }
                )

                // Results Panel (.dump(), Console, Output)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(resultsHeightDp)
                        .background(colors.backgroundSurface)
                ) {
                    resultsPanel()
                }
            }
        }

        // Bottom Status Bar
        statusBar()
    }
}

@Preview
@Composable
private fun JPadWorkspaceLayoutPreview() {
    JPadTheme {
        Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
            JPadWorkspaceLayout(
                toolbar = {
                    JPadToolbar(
                        languageMode = ScriptLanguageMode.JAVA_STATEMENTS,
                        onLanguageModeChange = {},
                        onRunClick = {},
                        onStopClick = {},
                        isRunning = false
                    )
                },
                queryTabBar = {
                    JPadQueryTabBar(
                        tabs = listOf(QueryTabItem("1", "Scratchpad.java", isModified = true)),
                        selectedTabId = "1",
                        onTabSelected = {},
                        onTabClosed = {},
                        onNewTab = {}
                    )
                },
                sidebar = {
                    Text("Sidebar / Explorer", style = JPadTheme.typography.uiBody)
                },
                editor = {
                    Text("// Code editor here", style = JPadTheme.typography.code)
                },
                resultsPanel = {
                    Text("Results Panel", style = JPadTheme.typography.uiBody)
                },
                statusBar = {
                    JPadStatusBar(
                        state = EngineState.READY,
                        statusMessage = "Ready",
                        cursorPosition = Pair(1, 1),
                        memoryUsageMb = 48
                    )
                }
            )
        }
    }
}
