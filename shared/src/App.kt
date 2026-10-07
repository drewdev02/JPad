package org.andrew.jpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.components.EngineState
import org.andrew.jpad.ui.components.JPadQueryTabBar
import org.andrew.jpad.ui.components.JPadStatusBar
import org.andrew.jpad.ui.components.JPadToolbar
import org.andrew.jpad.ui.components.JPadWorkspaceLayout
import org.andrew.jpad.ui.components.QueryTabItem
import org.andrew.jpad.ui.components.ScriptLanguageMode
import org.andrew.jpad.ui.components.grid.DumpValue
import org.andrew.jpad.ui.components.grid.JPadDumpViewer
import org.andrew.jpad.ui.theme.JPadTheme
import org.jetbrains.compose.reload.DevelopmentEntryPoint

@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    JPadTheme {
        var languageMode by remember { mutableStateOf(ScriptLanguageMode.JAVA_STATEMENTS) }
        var isRunning by remember { mutableStateOf(false) }
        var selectedTabId by remember { mutableStateOf("1") }

        var codeText by remember {
            mutableStateOf(
                """
                // JPad - LINQPad for Java & JVM
                record Developer(String name, String role, int experienceYears, boolean isRemote) {}
                
                var team = List.of(
                    new Developer("Andrew", "Lead Architect", 8, true),
                    new Developer("Elena", "Senior Backend Engineer", 6, true),
                    new Developer("Lucas", "Compiler & JVM Specialist", 10, false)
                );
                
                team.dump();
                """.trimIndent()
            )
        }

        val tabs = listOf(
            QueryTabItem(id = "1", title = "Scratchpad.java", isModified = true),
            QueryTabItem(id = "2", title = "DatabaseQuery.java", isModified = false)
        )

        val sampleDumpTable = DumpValue.Table(
            typeName = "List<Developer>",
            columns = listOf("name", "role", "experienceYears", "isRemote"),
            rows = listOf(
                mapOf(
                    "name" to DumpValue.Primitive("Andrew"),
                    "role" to DumpValue.Primitive("Lead Architect"),
                    "experienceYears" to DumpValue.Primitive(8),
                    "isRemote" to DumpValue.Primitive(true)
                ),
                mapOf(
                    "name" to DumpValue.Primitive("Elena"),
                    "role" to DumpValue.Primitive("Senior Backend Engineer"),
                    "experienceYears" to DumpValue.Primitive(6),
                    "isRemote" to DumpValue.Primitive(true)
                ),
                mapOf(
                    "name" to DumpValue.Primitive("Lucas"),
                    "role" to DumpValue.Primitive("Compiler & JVM Specialist"),
                    "experienceYears" to DumpValue.Primitive(10),
                    "isRemote" to DumpValue.Primitive(false)
                )
            )
        )

        JPadWorkspaceLayout(
            toolbar = {
                JPadToolbar(
                    languageMode = languageMode,
                    onLanguageModeChange = { languageMode = it },
                    onRunClick = { isRunning = true },
                    onStopClick = { isRunning = false },
                    isRunning = isRunning
                )
            },
            queryTabBar = {
                JPadQueryTabBar(
                    tabs = tabs,
                    selectedTabId = selectedTabId,
                    onTabSelected = { selectedTabId = it },
                    onTabClosed = {},
                    onNewTab = {}
                )
            },
            sidebar = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "EXPLORER",
                        style = JPadTheme.typography.uiSmall.copy(
                            color = JPadTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "📁 My Queries",
                        style = JPadTheme.typography.uiBody.copy(color = JPadTheme.colors.textPrimary),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "  📄 Scratchpad.java",
                        style = JPadTheme.typography.uiSmall.copy(color = JPadTheme.colors.accentPrimary),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "  📄 DatabaseQuery.java",
                        style = JPadTheme.typography.uiSmall.copy(color = JPadTheme.colors.textSecondary),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            },
            editor = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JPadTheme.colors.backgroundEditor)
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        textStyle = JPadTheme.typography.code,
                        cursorBrush = SolidColor(JPadTheme.colors.accentPrimary),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            resultsPanel = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "RESULTS (Dump View)",
                        style = JPadTheme.typography.uiSmall.copy(
                            color = JPadTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    JPadDumpViewer(
                        value = sampleDumpTable,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            statusBar = {
                JPadStatusBar(
                    state = if (isRunning) EngineState.EXECUTING else EngineState.SUCCESS,
                    statusMessage = if (isRunning) "Executing JVM script..." else "Query completed successfully",
                    executionTimeMs = 27,
                    cursorPosition = Pair(11, 14),
                    memoryUsageMb = 84
                )
            }
        )
    }
}
