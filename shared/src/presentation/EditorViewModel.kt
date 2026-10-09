package org.andrew.jpad.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.ui.components.EngineState
import org.andrew.jpad.ui.components.QueryTabItem
import org.andrew.jpad.ui.components.ScriptLanguageMode

data class EditorUiState(
    val code: String = DEFAULT_CODE,
    val languageMode: ScriptLanguageMode = ScriptLanguageMode.JAVA_STATEMENTS,
    val engineState: EngineState = EngineState.READY,
    val statusMessage: String = "Ready",
    val executionTimeMs: Long? = null,
    val consoleOutput: String = "",
    val dumpResult: DumpValue? = DEFAULT_DUMP_SAMPLE,
    val selectedTabId: String = "1",
    val tabs: List<QueryTabItem> = listOf(
        QueryTabItem(id = "1", title = "Scratchpad.java", isModified = false),
        QueryTabItem(id = "2", title = "DatabaseQuery.java", isModified = false)
    ),
    val cursorPosition: Pair<Int, Int>? = Pair(1, 1),
    val memoryUsageMb: Long? = 54
) {
    val isRunning: Boolean get() = engineState == EngineState.COMPILING || engineState == EngineState.EXECUTING
}

private const val DEFAULT_CODE = """// JPad - LINQPad for Java & JVM
record Developer(String name, String role, int experienceYears, boolean isRemote) {}

var team = List.of(
    new Developer("Andrew", "Lead Architect", 8, true),
    new Developer("Elena", "Senior Backend Engineer", 6, true),
    new Developer("Lucas", "Compiler & JVM Specialist", 10, false)
);

dump(team, "Active Engineering Team");
System.out.println("Execution finished. Team size: " + team.size());
"""

private val DEFAULT_DUMP_SAMPLE = DumpValue.Table(
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

class EditorViewModel(
    private val executionEngine: ExecutionEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null

    fun onCodeChanged(newCode: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.selectedTabId) tab.copy(isModified = true) else tab
            }
            state.copy(code = newCode, tabs = updatedTabs)
        }
    }

    fun onLanguageModeChanged(mode: ScriptLanguageMode) {
        _uiState.update { it.copy(languageMode = mode) }
    }

    fun onTabSelected(tabId: String) {
        _uiState.update { it.copy(selectedTabId = tabId) }
    }

    fun onTabClosed(tabId: String) {
        _uiState.update { state ->
            val remainingTabs = state.tabs.filterNot { it.id == tabId }
            val nextSelected = if (state.selectedTabId == tabId) {
                remainingTabs.firstOrNull()?.id ?: ""
            } else {
                state.selectedTabId
            }
            state.copy(tabs = remainingTabs, selectedTabId = nextSelected)
        }
    }

    fun onNewTab() {
        _uiState.update { state ->
            val newId = (state.tabs.size + 1).toString()
            val newTab = QueryTabItem(id = newId, title = "Query$newId.java", isModified = false)
            state.copy(tabs = state.tabs + newTab, selectedTabId = newId, code = "")
        }
    }

    fun onCursorPositionChanged(line: Int, col: Int) {
        _uiState.update { it.copy(cursorPosition = Pair(line, col)) }
    }

    fun onExecute() {
        if (_uiState.value.isRunning) return

        _uiState.update {
            it.copy(
                engineState = EngineState.EXECUTING,
                statusMessage = "Compiling & executing...",
                consoleOutput = "",
                executionTimeMs = null
            )
        }

        executionJob = viewModelScope.launch(dispatcher) {
            val codeToRun = _uiState.value.code
            val response = executionEngine.execute(codeToRun)
            if (!isActive) return@launch

            if (response.isSuccess) {
                _uiState.update {
                    it.copy(
                        engineState = EngineState.SUCCESS,
                        statusMessage = "Execution completed successfully",
                        consoleOutput = response.output,
                        dumpResult = response.dumps.lastOrNull() ?: it.dumpResult,
                        executionTimeMs = response.durationMs
                    )
                }
            } else {
                val errorDetails = if (response.error.isNotBlank()) response.error else response.output
                _uiState.update {
                    it.copy(
                        engineState = EngineState.ERROR,
                        statusMessage = "Execution failed",
                        consoleOutput = errorDetails,
                        executionTimeMs = response.durationMs
                    )
                }
            }
        }
    }

    fun onStop() {
        executionJob?.cancel()
        executionJob = null
        executionEngine.cancel()
        _uiState.update {
            it.copy(
                engineState = EngineState.READY,
                statusMessage = "Execution canceled by user"
            )
        }
    }
}
