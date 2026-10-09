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

data class QueryTabSession(
    val id: String,
    val title: String,
    val code: String = "",
    val languageMode: ScriptLanguageMode = ScriptLanguageMode.JAVA_STATEMENTS,
    val isModified: Boolean = false,
    val filePath: String? = null,
    val cursorPosition: Pair<Int, Int>? = Pair(1, 1),
    val consoleOutput: String = "",
    val dumpResults: List<DumpValue> = emptyList(),
    val executionTimeMs: Long? = null,
    val engineState: EngineState = EngineState.READY,
    val statusMessage: String = "Ready"
)

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

private val DEFAULT_TABS = listOf(
    QueryTabSession(
        id = "1",
        title = "Scratchpad.java",
        code = DEFAULT_CODE,
        dumpResults = listOf(DEFAULT_DUMP_SAMPLE),
        isModified = false
    ),
    QueryTabSession(
        id = "2",
        title = "DatabaseQuery.java",
        code = """// Database scratchpad
System.out.println("Connecting to database...");
""",
        dumpResults = emptyList(),
        isModified = false
    )
)

data class EditorUiState(
    val tabs: List<QueryTabSession> = DEFAULT_TABS,
    val selectedTabId: String = "1",
    val memoryUsageMb: Long? = 54
) {
    val activeTab: QueryTabSession
        get() = tabs.firstOrNull { it.id == selectedTabId } ?: tabs.firstOrNull() ?: QueryTabSession("1", "Scratchpad.java")

    val code: String get() = activeTab.code
    val languageMode: ScriptLanguageMode get() = activeTab.languageMode
    val engineState: EngineState get() = activeTab.engineState
    val statusMessage: String get() = activeTab.statusMessage
    val executionTimeMs: Long? get() = activeTab.executionTimeMs
    val consoleOutput: String get() = activeTab.consoleOutput
    val dumpResult: DumpValue? get() = activeTab.dumpResults.lastOrNull()
    val dumpResults: List<DumpValue> get() = activeTab.dumpResults
    val cursorPosition: Pair<Int, Int>? get() = activeTab.cursorPosition
    val isRunning: Boolean get() = engineState == EngineState.COMPILING || engineState == EngineState.EXECUTING

    val tabItems: List<QueryTabItem>
        get() = tabs.map { QueryTabItem(id = it.id, title = it.title, isModified = it.isModified) }
}

class EditorViewModel(
    private val executionEngine: ExecutionEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null

    fun onCodeChanged(newCode: String) {
        val currentSelectedId = _uiState.value.selectedTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == currentSelectedId) {
                    tab.copy(code = newCode, isModified = true)
                } else {
                    tab
                }
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onLanguageModeChanged(mode: ScriptLanguageMode) {
        val currentSelectedId = _uiState.value.selectedTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == currentSelectedId) tab.copy(languageMode = mode) else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onTabSelected(tabId: String) {
        if (_uiState.value.tabs.any { it.id == tabId }) {
            _uiState.update { it.copy(selectedTabId = tabId) }
        }
    }

    fun onTabClosed(tabId: String) {
        _uiState.update { state ->
            val remainingTabs = state.tabs.filterNot { it.id == tabId }
            if (remainingTabs.isEmpty()) {
                val newId = System.currentTimeMillis().toString()
                val defaultTab = QueryTabSession(id = newId, title = "Query1.java", code = "")
                state.copy(tabs = listOf(defaultTab), selectedTabId = newId)
            } else {
                val nextSelected = if (state.selectedTabId == tabId) {
                    val closedIndex = state.tabs.indexOfFirst { it.id == tabId }
                    val targetIndex = (closedIndex - 1).coerceAtLeast(0).coerceAtMost(remainingTabs.size - 1)
                    remainingTabs[targetIndex].id
                } else {
                    state.selectedTabId
                }
                state.copy(tabs = remainingTabs, selectedTabId = nextSelected)
            }
        }
    }

    fun onNewTab(title: String? = null, initialCode: String = "") {
        _uiState.update { state ->
            val existingNumbers = state.tabs.mapNotNull {
                Regex("""Query(\d+)\.java""").find(it.title)?.groupValues?.get(1)?.toIntOrNull()
            }
            val nextNumber = (existingNumbers.maxOrNull() ?: state.tabs.size) + 1
            val newId = System.currentTimeMillis().toString()
            val newTitle = title ?: "Query$nextNumber.java"
            val newTab = QueryTabSession(
                id = newId,
                title = newTitle,
                code = initialCode,
                isModified = false
            )
            state.copy(tabs = state.tabs + newTab, selectedTabId = newId)
        }
    }

    fun onCursorPositionChanged(line: Int, col: Int) {
        val currentSelectedId = _uiState.value.selectedTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == currentSelectedId) tab.copy(cursorPosition = Pair(line, col)) else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onExecute() {
        val active = _uiState.value.activeTab
        if (_uiState.value.isRunning) return

        val targetTabId = active.id
        _uiState.update { state ->
            val updated = state.tabs.map { tab ->
                if (tab.id == targetTabId) {
                    tab.copy(
                        engineState = EngineState.EXECUTING,
                        statusMessage = "Compiling & executing...",
                        consoleOutput = "",
                        executionTimeMs = null
                    )
                } else tab
            }
            state.copy(tabs = updated)
        }

        executionJob = viewModelScope.launch(dispatcher) {
            val codeToRun = active.code
            val response = executionEngine.execute(codeToRun)
            if (!isActive) return@launch

            _uiState.update { state ->
                val updated = state.tabs.map { tab ->
                    if (tab.id == targetTabId) {
                        if (response.isSuccess) {
                            tab.copy(
                                engineState = EngineState.SUCCESS,
                                statusMessage = "Execution completed successfully",
                                consoleOutput = response.output,
                                dumpResults = if (response.dumps.isNotEmpty()) response.dumps else tab.dumpResults,
                                executionTimeMs = response.durationMs
                            )
                        } else {
                            val errorDetails = if (response.error.isNotBlank()) response.error else response.output
                            tab.copy(
                                engineState = EngineState.ERROR,
                                statusMessage = "Execution failed",
                                consoleOutput = errorDetails,
                                executionTimeMs = response.durationMs
                            )
                        }
                    } else tab
                }
                state.copy(tabs = updated)
            }
        }
    }

    fun onStop() {
        executionJob?.cancel()
        executionJob = null
        executionEngine.cancel()
        val currentSelectedId = _uiState.value.selectedTabId
        _uiState.update { state ->
            val updated = state.tabs.map { tab ->
                if (tab.id == currentSelectedId && (tab.engineState == EngineState.EXECUTING || tab.engineState == EngineState.COMPILING)) {
                    tab.copy(
                        engineState = EngineState.READY,
                        statusMessage = "Execution canceled by user"
                    )
                } else tab
            }
            state.copy(tabs = updated)
        }
    }

    fun saveCurrentTab(destinationPath: String? = null): Boolean {
        val active = _uiState.value.activeTab
        val targetPath = destinationPath ?: active.filePath ?: run {
            val dir = java.io.File(System.getProperty("user.home"), ".jpad/queries").also { it.mkdirs() }
            java.io.File(dir, active.title).absolutePath
        }

        return try {
            val file = java.io.File(targetPath)
            file.parentFile?.mkdirs()
            file.writeText(active.code)
            _uiState.update { state ->
                val updatedTabs = state.tabs.map { tab ->
                    if (tab.id == active.id) {
                        tab.copy(
                            isModified = false,
                            filePath = file.absolutePath,
                            title = file.name
                        )
                    } else tab
                }
                state.copy(tabs = updatedTabs)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openFile(filePath: String, content: String? = null): Boolean {
        val file = java.io.File(filePath)
        val text = content ?: try {
            if (file.exists()) file.readText() else return false
        } catch (_: Exception) {
            return false
        }

        _uiState.update { state ->
            val existingTab = state.tabs.firstOrNull { it.filePath == file.absolutePath }
            if (existingTab != null) {
                state.copy(selectedTabId = existingTab.id)
            } else {
                val newId = System.currentTimeMillis().toString()
                val newTab = QueryTabSession(
                    id = newId,
                    title = file.name,
                    code = text,
                    filePath = file.absolutePath,
                    isModified = false
                )
                state.copy(tabs = state.tabs + newTab, selectedTabId = newId)
            }
        }
        return true
    }
}
