package org.andrew.jpad.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.ui.components.EngineState
import org.andrew.jpad.ui.components.ScriptLanguageMode
import org.andrew.jpad.core.di.coreEngineModule
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val executionEngine = koinApplication { modules(coreEngineModule) }.koin.get<ExecutionEngine>()
    private lateinit var viewModel: EditorViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditorViewModel(
            executionEngine = executionEngine,
            dispatcher = testDispatcher
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is ready and has initial code and tabs`() {
        val state = viewModel.uiState.value
        assertEquals(EngineState.READY, state.engineState)
        assertEquals(ScriptLanguageMode.JAVA_STATEMENTS, state.languageMode)
        assertTrue(state.code.isNotEmpty())
        assertTrue(state.tabs.isNotEmpty())
    }

    @Test
    fun `onCodeChanged updates code and marks active tab modified`() {
        viewModel.onCodeChanged("System.out.println(123);")
        val state = viewModel.uiState.value
        assertEquals("System.out.println(123);", state.code)
        val activeTab = state.tabs.first { it.id == state.selectedTabId }
        assertTrue(activeTab.isModified)
    }

    @Test
    fun `onLanguageModeChanged updates mode`() {
        viewModel.onLanguageModeChanged(ScriptLanguageMode.JAVA_PROGRAM)
        assertEquals(ScriptLanguageMode.JAVA_PROGRAM, viewModel.uiState.value.languageMode)
    }

    @Test
    fun `onExecute runs code successfully and updates output and state`() = runTest(testDispatcher) {
        viewModel.onCodeChanged("""System.out.println("Hello from ViewModel");""")
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.SUCCESS, state.engineState)
        assertEquals("Hello from ViewModel\n", state.consoleOutput)
        assertNotNull(state.executionTimeMs)
    }

    @Test
    fun `onExecute with compilation error reports failure state and error diagnostic`() = runTest(testDispatcher) {
        viewModel.onCodeChanged("""int x = "not an int";""")
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.ERROR, state.engineState)
        assertTrue(state.consoleOutput.contains("Compilation failed") || state.statusMessage.contains("failed"))
    }

    @Test
    fun `onExecute with dump calls updates dumpResult in uiState`() = runTest(testDispatcher) {
        viewModel.onCodeChanged("""
            var list = List.of(1, 2, 3);
            dump(list);
        """.trimIndent())
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.SUCCESS, state.engineState)
        val dump = state.dumpResult
        assertNotNull(dump)
        assertIs<DumpValue.Table>(dump)
        assertEquals(3, dump.rows.size)
    }

    @Test
    fun `onCursorPositionChanged updates cursor coordinates in uiState`() {
        viewModel.onCursorPositionChanged(14, 28)
        assertEquals(Pair(14, 28), viewModel.uiState.value.cursorPosition)
    }

    @Test
    fun `switching tabs preserves independent code and cursor states`() {
        // Tab 1 is active by default
        viewModel.onCodeChanged("String tab1Code = \"hello\";")
        viewModel.onCursorPositionChanged(2, 5)

        // Switch to Tab 2
        viewModel.onTabSelected("2")
        val tab2State = viewModel.uiState.value
        assertEquals("2", tab2State.selectedTabId)
        assertTrue(tab2State.code.contains("Database scratchpad"))

        // Modify Tab 2
        viewModel.onCodeChanged("String tab2Code = \"world\";")
        viewModel.onCursorPositionChanged(4, 10)

        // Switch back to Tab 1
        viewModel.onTabSelected("1")
        val tab1State = viewModel.uiState.value
        assertEquals("1", tab1State.selectedTabId)
        assertEquals("String tab1Code = \"hello\";", tab1State.code)
        assertEquals(Pair(2, 5), tab1State.cursorPosition)

        // Verify Tab 2 kept its code too
        val tab2 = tab1State.tabs.first { it.id == "2" }
        assertEquals("String tab2Code = \"world\";", tab2.code)
        assertEquals(Pair(4, 10), tab2.cursorPosition)
    }

    @Test
    fun `onNewTab adds a fresh tab and selects it`() {
        val initialCount = viewModel.uiState.value.tabs.size
        viewModel.onNewTab()
        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.tabs.size)
        assertEquals("", state.code)
        val active = state.activeTab
        assertTrue(active.title.startsWith("Query"))
    }

    @Test
    fun `onTabClosed selects adjacent tab or creates default tab if last closed`() {
        viewModel.onTabSelected("1")
        viewModel.onTabClosed("1")
        val stateAfterClose1 = viewModel.uiState.value
        assertEquals(1, stateAfterClose1.tabs.size)
        assertEquals("2", stateAfterClose1.selectedTabId)

        // Close the only remaining tab
        viewModel.onTabClosed("2")
        val stateAfterCloseAll = viewModel.uiState.value
        assertEquals(1, stateAfterCloseAll.tabs.size)
        assertEquals("Query1.java", stateAfterCloseAll.activeTab.title)
    }

    @Test
    fun `onExecute with multiple dump calls updates all dumpResults in uiState`() = runTest(testDispatcher) {
        viewModel.onCodeChanged("""
            var list = List.of(1, 2);
            var map = Map.of("key", "value");
            dump(list, "Numbers");
            dump(map, "Config");
        """.trimIndent())
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.SUCCESS, state.engineState)
        assertEquals(2, state.dumpResults.size)
        assertIs<DumpValue.Table>(state.dumpResults[0])
        assertIs<DumpValue.Table>(state.dumpResults[1])
    }

    @Test
    fun `saveCurrentTab and openFile work properly`() {
        val tempFile = java.io.File.createTempFile("jpad_test", ".java")
        tempFile.deleteOnExit()

        viewModel.onCodeChanged("int x = 42;")
        assertTrue(viewModel.uiState.value.activeTab.isModified)

        val saved = viewModel.saveCurrentTab(tempFile.absolutePath)
        assertTrue(saved)
        val stateAfterSave = viewModel.uiState.value.activeTab
        assertFalse(stateAfterSave.isModified)
        assertEquals(tempFile.absolutePath, stateAfterSave.filePath)
        assertEquals("int x = 42;", tempFile.readText())

        // Test openFile
        val anotherFile = java.io.File.createTempFile("jpad_test_open", ".java")
        anotherFile.deleteOnExit()
        anotherFile.writeText("int openedValue = 99;")

        val opened = viewModel.openFile(anotherFile.absolutePath)
        assertTrue(opened)
        val activeAfterOpen = viewModel.uiState.value.activeTab
        assertEquals("int openedValue = 99;", activeAfterOpen.code)
        assertEquals(anotherFile.name, activeAfterOpen.title)
    }

    @Test
    fun `onExecute with compilation error populates diagnostics in uiState`() = runTest(testDispatcher) {
        viewModel.onCodeChanged("""int a = "type mismatch";""")
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.ERROR, state.engineState)
        assertTrue(state.diagnostics.isNotEmpty(), "Expected compilation diagnostics")
        val diag = state.diagnostics.first()
        assertTrue(diag.message.isNotBlank())
    }

    @Test
    fun `onNavigateToPosition sets targetCursorPosition and onCodeChanged clears it`() {
        viewModel.onNavigateToPosition(5, 12)
        assertEquals(Pair(5, 12), viewModel.uiState.value.targetCursorPosition)

        viewModel.onCodeChanged("String s = \"reset\";")
        assertEquals(null, viewModel.uiState.value.targetCursorPosition)
    }

    @Test
    fun `toggleSidebar and setSidebarVisible update visibility`() {
        assertTrue(viewModel.uiState.value.isSidebarVisible)

        viewModel.toggleSidebar()
        assertFalse(viewModel.uiState.value.isSidebarVisible)

        viewModel.toggleSidebar()
        assertTrue(viewModel.uiState.value.isSidebarVisible)

        viewModel.setSidebarVisible(false)
        assertFalse(viewModel.uiState.value.isSidebarVisible)
    }

    @Test
    fun `workspace directory scanning and file creation in workspace`() {
        val tempDir = java.nio.file.Files.createTempDirectory("jpad_workspace_test").toFile()
        try {
            viewModel.setWorkspaceDirectory(tempDir.absolutePath)
            assertEquals(tempDir.absolutePath, viewModel.uiState.value.workspaceDirectory)
            assertTrue(viewModel.uiState.value.explorerItems.isEmpty())

            viewModel.startCreatingFile()
            assertTrue(viewModel.uiState.value.isCreatingFile)
            viewModel.cancelCreatingFile()
            assertFalse(viewModel.uiState.value.isCreatingFile)

            viewModel.createFileInWorkspace("CustomScript.java")
            assertFalse(viewModel.uiState.value.isCreatingFile)

            val createdFile = java.io.File(tempDir, "CustomScript.java")
            assertTrue(createdFile.exists())
            assertTrue(createdFile.readText().contains("CustomScript.java"))

            // Verify active tab is the new file
            val activeTab = viewModel.uiState.value.activeTab
            assertEquals("CustomScript.java", activeTab.title)
            assertEquals(createdFile.absolutePath, activeTab.filePath)

            // Verify it's present in explorerItems
            val items = viewModel.uiState.value.explorerItems
            assertEquals(1, items.size)
            assertEquals("CustomScript.java", items[0].name)
            assertTrue(items[0].isSelected)
            assertTrue(items[0].isOpenInTab)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `expanding and collapsing subdirectories in file tree`() {
        val tempDir = java.nio.file.Files.createTempDirectory("jpad_tree_test").toFile()
        try {
            val subDir = java.io.File(tempDir, "subdir").also { it.mkdir() }
            val nestedFile = java.io.File(subDir, "Nested.java").also { it.writeText("int nested = 1;") }

            viewModel.setWorkspaceDirectory(tempDir.absolutePath)

            // Initially collapsed: only subdir is visible
            val initialItems = viewModel.uiState.value.explorerItems
            assertEquals(1, initialItems.size)
            assertEquals("subdir", initialItems[0].name)
            assertTrue(initialItems[0].isDirectory)
            assertFalse(initialItems[0].isExpanded)

            // Expand subdir
            viewModel.toggleDirectoryExpanded(subDir.absolutePath)
            val expandedItems = viewModel.uiState.value.explorerItems
            assertEquals(2, expandedItems.size)
            assertEquals("subdir", expandedItems[0].name)
            assertTrue(expandedItems[0].isExpanded)
            assertEquals("Nested.java", expandedItems[1].name)
            assertEquals(1, expandedItems[1].depth)

            // Collapse subdir
            viewModel.toggleDirectoryExpanded(subDir.absolutePath)
            val collapsedItems = viewModel.uiState.value.explorerItems
            assertEquals(1, collapsedItems.size)
            assertFalse(collapsedItems[0].isExpanded)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `background task lifecycle and progress updates are tracked in uiState`() {
        assertTrue(viewModel.uiState.value.backgroundTasks.isEmpty())
        assertNull(viewModel.uiState.value.activeBackgroundTask)

        // Add task 1 with indeterminate progress
        viewModel.updateBackgroundTask(
            id = "task-dl-1",
            title = "Downloading com.google.code.gson:gson:2.10.1...",
            progress = null
        )
        val stateWithOneTask = viewModel.uiState.value
        assertEquals(1, stateWithOneTask.backgroundTasks.size)
        assertEquals("task-dl-1", stateWithOneTask.activeBackgroundTask?.id)
        assertEquals("Downloading com.google.code.gson:gson:2.10.1...", stateWithOneTask.activeBackgroundTask?.title)
        assertNull(stateWithOneTask.activeBackgroundTask?.progress)
        assertTrue(stateWithOneTask.activeBackgroundTask?.isIndeterminate == true)

        // Update task 1 progress to 50%
        viewModel.updateBackgroundTask(
            id = "task-dl-1",
            title = "Downloading gson-2.10.1.jar (140 / 283 KB)...",
            progress = 0.5f
        )
        val stateUpdated = viewModel.uiState.value
        assertEquals(1, stateUpdated.backgroundTasks.size)
        assertEquals(0.5f, stateUpdated.activeBackgroundTask?.progress)
        assertFalse(stateUpdated.activeBackgroundTask?.isIndeterminate == true)

        // Add second task
        viewModel.updateBackgroundTask(
            id = "task-dl-2",
            title = "Resolving POM dependencies...",
            progress = null
        )
        val stateWithTwoTasks = viewModel.uiState.value
        assertEquals(2, stateWithTwoTasks.backgroundTasks.size)
        assertEquals("task-dl-1", stateWithTwoTasks.activeBackgroundTask?.id)

        // Remove first task
        viewModel.removeBackgroundTask("task-dl-1")
        val stateAfterRemoveOne = viewModel.uiState.value
        assertEquals(1, stateAfterRemoveOne.backgroundTasks.size)
        assertEquals("task-dl-2", stateAfterRemoveOne.activeBackgroundTask?.id)

        // Remove remaining task
        viewModel.removeBackgroundTask("task-dl-2")
        val stateEmpty = viewModel.uiState.value
        assertTrue(stateEmpty.backgroundTasks.isEmpty())
        assertNull(stateEmpty.activeBackgroundTask)
    }

    @Test
    fun `initializes sample database connection and schemas`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.databaseConnections.isNotEmpty(), "Sample database connection should be initialized")
        val sampleConn = state.databaseConnections.first()
        assertEquals("sample-sqlite", sampleConn.id)
        assertEquals(sampleConn.id, state.selectedConnectionId)

        val schemas = state.databaseSchemas[sampleConn.id]
        assertNotNull(schemas)
        assertTrue(schemas.any { it.name.equals("customers", ignoreCase = true) })
        assertTrue(schemas.any { it.name.equals("orders", ignoreCase = true) })
    }

    @Test
    fun `onQueryDatabaseTable creates new SQL tab`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val initialTabsCount = viewModel.uiState.value.tabs.size

        viewModel.onQueryDatabaseTable("sample-sqlite", "customers")
        val state = viewModel.uiState.value
        assertEquals(initialTabsCount + 1, state.tabs.size)

        val active = state.activeTab
        assertEquals("customers.sql", active.title)
        assertEquals(ScriptLanguageMode.SQL, active.languageMode)
        assertTrue(active.code.contains("SELECT * FROM customers"))
    }

    @Test
    fun `onExecute with SQL query executes against database and populates dump results`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onQueryDatabaseTable("sample-sqlite", "customers")
        viewModel.onCodeChanged("SELECT id, name, country FROM customers WHERE country = 'USA';")
        viewModel.onExecute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EngineState.SUCCESS, state.engineState)
        assertEquals(1, state.dumpResults.size)
        val table = state.dumpResults[0]
        assertIs<DumpValue.Table>(table)
        assertEquals(listOf("id", "name", "country"), table.columns)
        assertEquals(1, table.rows.size)
        val firstRow = table.rows[0]
        assertEquals("Grace Hopper", (firstRow["name"] as? DumpValue.Primitive)?.rawValue)
    }
}
