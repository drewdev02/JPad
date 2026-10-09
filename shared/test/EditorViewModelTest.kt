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
}
