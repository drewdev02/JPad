package org.andrew.jpad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.presentation.EditorUiState
import org.andrew.jpad.presentation.EditorViewModel
import org.andrew.jpad.ui.components.EngineState
import org.andrew.jpad.ui.components.JPadQueryTabBar
import org.andrew.jpad.ui.components.JPadStatusBar
import org.andrew.jpad.ui.components.JPadToolbar
import org.andrew.jpad.ui.components.JPadWorkspaceLayout
import org.andrew.jpad.ui.components.ScriptLanguageMode
import org.andrew.jpad.ui.components.editor.JPadCodeEditor
import org.andrew.jpad.ui.components.grid.JPadMultiDumpViewer
import org.andrew.jpad.ui.theme.JPadTheme
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.koin.compose.viewmodel.koinViewModel

@Composable
@DevelopmentEntryPoint
fun App(
    viewModel: EditorViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    AppContent(
        state = state,
        onCodeChanged = viewModel::onCodeChanged,
        onCursorPositionChanged = viewModel::onCursorPositionChanged,
        onLanguageModeChanged = viewModel::onLanguageModeChanged,
        onTabSelected = viewModel::onTabSelected,
        onTabClosed = viewModel::onTabClosed,
        onNewTab = viewModel::onNewTab,
        onExecute = viewModel::onExecute,
        onStop = viewModel::onStop,
        onSave = { viewModel.saveCurrentTab() }
    )
}

enum class ResultViewTab {
    RESULTS_DUMP,
    CONSOLE_OUTPUT
}

@Composable
fun AppContent(
    state: EditorUiState,
    onCodeChanged: (String) -> Unit,
    onCursorPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onLanguageModeChanged: (ScriptLanguageMode) -> Unit,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    onExecute: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit = {}
) {
    var activeResultTab by remember { mutableStateOf(ResultViewTab.CONSOLE_OUTPUT) }

    // Auto-switch to Results tab if dumps were generated
    LaunchedEffect(state.dumpResults) {
        if (state.dumpResults.isNotEmpty()) {
            activeResultTab = ResultViewTab.RESULTS_DUMP
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val isModifierDown = event.isCtrlPressed || event.isMetaPressed

                when {
                    event.key == Key.F5 && event.isShiftPressed -> {
                        if (state.isRunning) {
                            onStop()
                            true
                        } else false
                    }
                    event.key == Key.F5 || (isModifierDown && event.key == Key.Enter) -> {
                        if (!state.isRunning) {
                            onExecute()
                            true
                        } else false
                    }
                    event.key == Key.Escape && state.isRunning -> {
                        onStop()
                        true
                    }
                    isModifierDown && event.key == Key.N -> {
                        onNewTab()
                        true
                    }
                    isModifierDown && event.key == Key.W -> {
                        onTabClosed(state.selectedTabId)
                        true
                    }
                    isModifierDown && event.key == Key.S -> {
                        onSave()
                        true
                    }
                    else -> false
                }
            }
    ) {
        JPadTheme {
            JPadWorkspaceLayout(
                toolbar = {
                    JPadToolbar(
                        languageMode = state.languageMode,
                        onLanguageModeChange = onLanguageModeChanged,
                        onRunClick = onExecute,
                        onStopClick = onStop,
                        isRunning = state.isRunning,
                        onSaveClick = onSave,
                        onNewQueryClick = onNewTab
                    )
                },
                queryTabBar = {
                    JPadQueryTabBar(
                        tabs = state.tabItems,
                        selectedTabId = state.selectedTabId,
                        onTabSelected = onTabSelected,
                        onTabClosed = onTabClosed,
                        onNewTab = onNewTab
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
                        state.tabs.forEach { tab ->
                            val isSelected = tab.id == state.selectedTabId
                            val color = if (isSelected) JPadTheme.colors.accentPrimary else JPadTheme.colors.textSecondary
                            Text(
                                text = "  📄 ${tab.title}",
                                style = JPadTheme.typography.uiSmall.copy(color = color),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTabSelected(tab.id) }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                },
                editor = {
                    key(state.selectedTabId) {
                        JPadCodeEditor(
                            value = state.code,
                            onValueChange = onCodeChanged,
                            onCursorPositionChange = onCursorPositionChanged,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                resultsPanel = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(JPadTheme.colors.backgroundSurface)
                    ) {
                        // Result tabs (Dump vs Console)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(JPadTheme.colors.backgroundPanel)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Console Output",
                                style = JPadTheme.typography.uiSmall.copy(
                                    color = if (activeResultTab == ResultViewTab.CONSOLE_OUTPUT) JPadTheme.colors.accentPrimary else JPadTheme.colors.textSecondary,
                                    fontWeight = if (activeResultTab == ResultViewTab.CONSOLE_OUTPUT) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier
                                    .clickable { activeResultTab = ResultViewTab.CONSOLE_OUTPUT }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                            val dumpCount = state.dumpResults.size
                            val dumpTabTitle = if (dumpCount > 0) "Results (Dump View) [$dumpCount]" else "Results (Dump View)"
                            Text(
                                text = dumpTabTitle,
                                style = JPadTheme.typography.uiSmall.copy(
                                    color = if (activeResultTab == ResultViewTab.RESULTS_DUMP) JPadTheme.colors.accentPrimary else JPadTheme.colors.textSecondary,
                                    fontWeight = if (activeResultTab == ResultViewTab.RESULTS_DUMP) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier
                                    .clickable { activeResultTab = ResultViewTab.RESULTS_DUMP }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            when (activeResultTab) {
                                ResultViewTab.CONSOLE_OUTPUT -> {
                                    val scrollState = rememberScrollState()
                                    val isError = state.engineState == EngineState.ERROR
                                    val textColor = if (isError) JPadTheme.colors.errorRed else JPadTheme.colors.textPrimary
                                    val displayText = state.consoleOutput.ifEmpty {
                                        if (state.isRunning) "Executing JVM process..." else "// Output console is empty. Click 'Execute' (F5) to run."
                                    }

                                    Text(
                                        text = displayText,
                                        style = JPadTheme.typography.code.copy(
                                            color = if (state.consoleOutput.isEmpty()) JPadTheme.colors.textMuted else textColor,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                    )
                                }
                                ResultViewTab.RESULTS_DUMP -> {
                                    JPadMultiDumpViewer(
                                        dumps = state.dumpResults,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                },
                statusBar = {
                    JPadStatusBar(
                        state = state.engineState,
                        statusMessage = state.statusMessage,
                        executionTimeMs = state.executionTimeMs,
                        cursorPosition = state.cursorPosition,
                        memoryUsageMb = state.memoryUsageMb
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    AppContent(
        state = EditorUiState(),
        onCodeChanged = {},
        onLanguageModeChanged = {},
        onTabSelected = {},
        onTabClosed = {},
        onNewTab = {},
        onExecute = {},
        onStop = {},
        onSave = {}
    )
}
