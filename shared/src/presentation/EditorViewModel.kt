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
import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.ui.components.BackgroundTask
import org.andrew.jpad.ui.components.EngineState
import org.andrew.jpad.ui.components.QueryTabItem
import org.andrew.jpad.ui.components.ScriptLanguageMode
import org.andrew.jpad.ui.components.sidebar.FileExplorerItem
import org.andrew.jpad.ui.components.sidebar.OpenScriptItem
import org.andrew.jpad.ui.components.sidebar.buildVisibleFileTree
import java.io.File

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
    val diagnostics: List<CompilationDiagnostic> = emptyList(),
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
    val targetCursorPosition: Pair<Int, Int>? = null,
    val memoryUsageMb: Long? = 54,
    val isSidebarVisible: Boolean = true,
    val workspaceDirectory: String = File(System.getProperty("user.home"), ".jpad/queries").absolutePath,
    val expandedDirectories: Set<String> = emptySet(),
    val isCreatingFile: Boolean = false,
    val explorerItems: List<FileExplorerItem> = emptyList(),
    val backgroundTasks: List<BackgroundTask> = emptyList()
) {
    val activeBackgroundTask: BackgroundTask?
        get() = backgroundTasks.firstOrNull()

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
    val diagnostics: List<CompilationDiagnostic> get() = activeTab.diagnostics
    val cursorPosition: Pair<Int, Int>? get() = activeTab.cursorPosition
    val isRunning: Boolean get() = engineState == EngineState.COMPILING || engineState == EngineState.EXECUTING

    val tabItems: List<QueryTabItem>
        get() = tabs.map { QueryTabItem(id = it.id, title = it.title, isModified = it.isModified) }

    val openScripts: List<OpenScriptItem>
        get() = tabs.map { OpenScriptItem(id = it.id, title = it.title, isModified = it.isModified, isSelected = it.id == selectedTabId) }
}

class EditorViewModel(
    private val executionEngine: ExecutionEngine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null

    init {
        try {
            val dir = File(_uiState.value.workspaceDirectory)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (_: Exception) {}
        refreshWorkspace()
    }

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
            state.copy(tabs = updatedTabs, targetCursorPosition = null)
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
            _uiState.update { state ->
                val next = state.copy(selectedTabId = tabId)
                next.copy(explorerItems = updateExplorerSelection(next))
            }
        }
    }

    fun onTabClosed(tabId: String) {
        _uiState.update { state ->
            val remainingTabs = state.tabs.filterNot { it.id == tabId }
            val nextState = if (remainingTabs.isEmpty()) {
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
            nextState.copy(explorerItems = updateExplorerSelection(nextState))
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
            val next = state.copy(tabs = state.tabs + newTab, selectedTabId = newId)
            next.copy(explorerItems = updateExplorerSelection(next))
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

    fun updateBackgroundTask(id: String, title: String, details: String? = null, progress: Float? = null) {
        _uiState.update { state ->
            val existingIndex = state.backgroundTasks.indexOfFirst { it.id == id }
            val task = BackgroundTask(id = id, title = title, details = details, progress = progress)
            val updated = if (existingIndex >= 0) {
                state.backgroundTasks.toMutableList().also { it[existingIndex] = task }
            } else {
                state.backgroundTasks + task
            }
            state.copy(backgroundTasks = updated)
        }
    }

    fun removeBackgroundTask(id: String) {
        _uiState.update { state ->
            state.copy(backgroundTasks = state.backgroundTasks.filterNot { it.id == id })
        }
    }

    fun resolveDependencies(tabId: String = _uiState.value.selectedTabId) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        val taskId = "deps-$tabId"
        viewModelScope.launch(dispatcher) {
            val coords = executionEngine.extractDependencies(tab.code)
            if (coords.isEmpty()) return@launch
            try {
                updateBackgroundTask(taskId, "Resolving ${coords.size} dependencies...", progress = 0f)
                val res = executionEngine.resolveDependencies(coords) { desc, prog ->
                    updateBackgroundTask(taskId, desc, progress = prog)
                }
                if (res.isSuccess) {
                    updateBackgroundTask(taskId, "Dependencies resolved (${res.classpathJars.size} JARs)", progress = 1.0f)
                    kotlinx.coroutines.delay(1000)
                } else {
                    updateBackgroundTask(taskId, "Failed to resolve dependencies", progress = null)
                    kotlinx.coroutines.delay(2000)
                }
            } finally {
                removeBackgroundTask(taskId)
            }
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
            val taskId = "exec-$targetTabId"
            val response = try {
                executionEngine.execute(codeToRun) { desc, prog ->
                    updateBackgroundTask(
                        id = taskId,
                        title = desc,
                        progress = prog
                    )
                }
            } finally {
                removeBackgroundTask(taskId)
            }
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
                                diagnostics = response.diagnostics,
                                executionTimeMs = response.durationMs
                            )
                        } else {
                            val errorDetails = if (response.error.isNotBlank()) response.error else response.output
                            tab.copy(
                                engineState = EngineState.ERROR,
                                statusMessage = "Execution failed",
                                consoleOutput = errorDetails,
                                dumpResults = if (response.dumps.isNotEmpty()) response.dumps else tab.dumpResults,
                                diagnostics = response.diagnostics,
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
        removeBackgroundTask("exec-$currentSelectedId")
        removeBackgroundTask("deps-$currentSelectedId")
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
            val dir = File(_uiState.value.workspaceDirectory).also { it.mkdirs() }
            File(dir, active.title).absolutePath
        }

        return try {
            val file = File(targetPath)
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
                val nextState = state.copy(tabs = updatedTabs)
                nextState.copy(
                    explorerItems = buildVisibleFileTree(
                        root = File(nextState.workspaceDirectory),
                        expanded = nextState.expandedDirectories,
                        activeFilePath = nextState.activeTab.filePath,
                        openFilePaths = nextState.tabs.mapNotNull { it.filePath }.toSet()
                    )
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openFile(filePath: String, content: String? = null): Boolean {
        val file = File(filePath)
        val text = content ?: try {
            if (file.exists()) file.readText() else return false
        } catch (_: Exception) {
            return false
        }

        _uiState.update { state ->
            val existingTab = state.tabs.firstOrNull { it.filePath == file.absolutePath }
            val nextState = if (existingTab != null) {
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
            nextState.copy(explorerItems = updateExplorerSelection(nextState))
        }
        return true
    }

    private fun updateExplorerSelection(state: EditorUiState): List<FileExplorerItem> {
        val activePath = state.activeTab.filePath
        val openPaths = state.tabs.mapNotNull { it.filePath }.toSet()
        return state.explorerItems.map {
            it.copy(
                isSelected = it.file.absolutePath == activePath,
                isOpenInTab = openPaths.contains(it.file.absolutePath)
            )
        }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(isSidebarVisible = !it.isSidebarVisible) }
    }

    fun setSidebarVisible(visible: Boolean) {
        _uiState.update { it.copy(isSidebarVisible = visible) }
    }

    fun setWorkspaceDirectory(path: String) {
        try {
            val dir = File(path)
            if (!dir.exists()) dir.mkdirs()
        } catch (_: Exception) {}
        _uiState.update { state ->
            val updatedState = state.copy(
                workspaceDirectory = path,
                expandedDirectories = emptySet()
            )
            val items = buildVisibleFileTree(
                root = File(path),
                expanded = emptySet(),
                activeFilePath = updatedState.activeTab.filePath,
                openFilePaths = updatedState.tabs.mapNotNull { it.filePath }.toSet()
            )
            updatedState.copy(explorerItems = items)
        }
    }

    fun refreshWorkspace() {
        _uiState.update { state ->
            val items = buildVisibleFileTree(
                root = File(state.workspaceDirectory),
                expanded = state.expandedDirectories,
                activeFilePath = state.activeTab.filePath,
                openFilePaths = state.tabs.mapNotNull { it.filePath }.toSet()
            )
            state.copy(explorerItems = items)
        }
    }

    fun toggleDirectoryExpanded(path: String) {
        _uiState.update { state ->
            val updatedExpanded = if (state.expandedDirectories.contains(path)) {
                state.expandedDirectories - path
            } else {
                state.expandedDirectories + path
            }
            val items = buildVisibleFileTree(
                root = File(state.workspaceDirectory),
                expanded = updatedExpanded,
                activeFilePath = state.activeTab.filePath,
                openFilePaths = state.tabs.mapNotNull { it.filePath }.toSet()
            )
            state.copy(
                expandedDirectories = updatedExpanded,
                explorerItems = items
            )
        }
    }

    fun startCreatingFile() {
        _uiState.update { it.copy(isCreatingFile = true) }
    }

    fun cancelCreatingFile() {
        _uiState.update { it.copy(isCreatingFile = false) }
    }

    fun createFileInWorkspace(fileName: String) {
        val trimmed = fileName.trim()
        val nameWithExt = if (trimmed.contains('.')) trimmed else "$trimmed.java"
        val workspaceDir = File(_uiState.value.workspaceDirectory).also { it.mkdirs() }
        val targetFile = File(workspaceDir, nameWithExt)

        if (!targetFile.exists()) {
            val template = """// $nameWithExt
System.out.println("Hello from $nameWithExt!");
"""
            try {
                targetFile.writeText(template)
            } catch (_: Exception) {}
        }

        openFile(targetFile.absolutePath)
        _uiState.update { it.copy(isCreatingFile = false) }
        refreshWorkspace()
    }

    fun onNavigateToPosition(line: Int, col: Int) {
        _uiState.update { it.copy(targetCursorPosition = Pair(line, col)) }
    }

    fun onOpenFileDialog() {
        val chosen = showNativeOpenFileDialog() ?: return
        openFile(chosen)
    }

    fun onSaveAsDialog() {
        val active = _uiState.value.activeTab
        val chosen = showNativeSaveFileDialog(defaultName = active.title) ?: return
        saveCurrentTab(chosen)
    }

    fun onOpenFolderDialog() {
        val chosen = showNativeOpenFolderDialog() ?: return
        setWorkspaceDirectory(chosen)
    }
}

internal fun showNativeOpenFileDialog(title: String = "Open Java Scratchpad"): String? {
    return try {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.endsWith(".java") || name.endsWith(".jpad") }
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        java.io.File(dir, file).absolutePath
    } catch (_: Throwable) {
        null
    }
}

internal fun showNativeSaveFileDialog(defaultName: String = "Scratchpad.java", title: String = "Save Scratchpad As"): String? {
    return try {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.SAVE)
        dialog.file = defaultName
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        java.io.File(dir, file).absolutePath
    } catch (_: Throwable) {
        null
    }
}

internal fun showNativeOpenFolderDialog(title: String = "Select Working Directory"): String? {
    return try {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        val chosen = java.io.File(dir, file)
        if (chosen.isDirectory) chosen.absolutePath else chosen.parentFile?.absolutePath ?: chosen.absolutePath
    } catch (_: Throwable) {
        try {
            val chooser = javax.swing.JFileChooser().apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = title
            }
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.absolutePath
            } else null
        } catch (_: Throwable) {
            null
        }
    } finally {
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
    }
}
