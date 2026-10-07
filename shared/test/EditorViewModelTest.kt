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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val executionEngine = ExecutionEngine()
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
        assertNotNull(state.dumpResult)
        assertIs<DumpValue.Table>(state.dumpResult)
        assertEquals(3, state.dumpResult.rows.size)
    }

    @Test
    fun `onCursorPositionChanged updates cursor coordinates in uiState`() {
        viewModel.onCursorPositionChanged(14, 28)
        assertEquals(Pair(14, 28), viewModel.uiState.value.cursorPosition)
    }
}
