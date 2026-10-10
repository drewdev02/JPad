package org.andrew.jpad.ui.components.palette

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandPaletteSearchTest {

    private val actions = listOf(
        CommandPaletteAction(
            id = "exec_run",
            title = "Run Script / Query",
            subtitle = "Execute the active script",
            category = CommandCategory.EXECUTION,
            shortcut = "F5",
            onExecute = {}
        ),
        CommandPaletteAction(
            id = "exec_stop",
            title = "Stop Execution",
            subtitle = "Terminate JVM process",
            category = CommandCategory.EXECUTION,
            shortcut = "Shift+F5",
            onExecute = {}
        ),
        CommandPaletteAction(
            id = "file_new",
            title = "New Query Tab",
            subtitle = "Create blank tab",
            category = CommandCategory.FILE,
            shortcut = "Ctrl+N",
            onExecute = {}
        ),
        CommandPaletteAction(
            id = "view_history",
            title = "Toggle History",
            subtitle = "Show execution timeline",
            category = CommandCategory.HISTORY,
            shortcut = "Ctrl+H",
            onExecute = {}
        )
    )

    @Test
    fun `returns all actions when query is empty and category is ALL`() {
        val results = CommandPaletteSearch.filter(actions, "", CommandCategory.ALL)
        assertEquals(actions.size, results.size)
    }

    @Test
    fun `filters by category`() {
        val results = CommandPaletteSearch.filter(actions, "", CommandCategory.EXECUTION)
        assertEquals(2, results.size)
        assertTrue(results.all { it.category == CommandCategory.EXECUTION })
    }

    @Test
    fun `filters by search query case-insensitively`() {
        val results = CommandPaletteSearch.filter(actions, "run", CommandCategory.ALL)
        assertEquals(1, results.size)
        assertEquals("exec_run", results.first().id)
    }

    @Test
    fun `matches on shortcut and subtitle`() {
        val resultsByShortcut = CommandPaletteSearch.filter(actions, "F5", CommandCategory.ALL)
        assertEquals(2, resultsByShortcut.size)

        val resultsBySubtitle = CommandPaletteSearch.filter(actions, "timeline", CommandCategory.ALL)
        assertEquals(1, resultsBySubtitle.size)
        assertEquals("view_history", resultsBySubtitle.first().id)
    }

    @Test
    fun `filters by both category and query`() {
        val results = CommandPaletteSearch.filter(actions, "Stop", CommandCategory.EXECUTION)
        assertEquals(1, results.size)
        assertEquals("exec_stop", results.first().id)
    }
}
