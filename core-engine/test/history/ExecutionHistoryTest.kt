package org.andrew.jpad.history

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecutionHistoryTest {

    @Test
    fun `test TextDiffUtil with identical code`() {
        val code = "int a = 10;\nSystem.out.println(a);"
        val diff = TextDiffUtil.computeDiff(code, code)

        assertEquals(2, diff.lines.size)
        assertEquals(2, diff.unchangedCount)
        assertEquals(0, diff.addedCount)
        assertEquals(0, diff.removedCount)
        assertTrue(diff.lines.all { it.type == DiffLineType.UNCHANGED })
    }

    @Test
    fun `test TextDiffUtil with added and removed lines`() {
        val oldCode = "int a = 10;\nint b = 20;\nint c = a + b;"
        val newCode = "int a = 10;\nint b = 30;\nint d = 40;\nint c = a + b;"

        val diff = TextDiffUtil.computeDiff(oldCode, newCode)

        assertTrue(diff.addedCount >= 1)
        assertTrue(diff.removedCount >= 1)
        assertTrue(diff.unchangedCount >= 2)
    }

    @Test
    fun `test TextDiffUtil with empty strings`() {
        val diff = TextDiffUtil.computeDiff("", "val x = 1;")
        assertEquals(1, diff.addedCount)
        assertEquals(0, diff.removedCount)
        assertEquals(0, diff.unchangedCount)

        val diffEmpty = TextDiffUtil.computeDiff("", "")
        assertEquals(0, diffEmpty.lines.size)
    }

    @Test
    fun `test SqliteExecutionHistoryRepository CRUD operations`() {
        val tempDb = Files.createTempFile("jpad_history_test", ".db")
        try {
            val repo = SqliteExecutionHistoryRepository(tempDb)

            val snapshot1 = ExecutionSnapshot(
                id = "snap-1",
                timestamp = 1000L,
                tabTitle = "Tab 1",
                languageMode = "JAVA_STATEMENTS",
                code = "System.out.println(\"Hello 1\");",
                status = "SUCCESS",
                durationMs = 25L,
                consoleOutput = "Hello 1\n",
                dumpCount = 0
            )

            val snapshot2 = ExecutionSnapshot(
                id = "snap-2",
                timestamp = 2000L,
                tabTitle = "Tab 1",
                languageMode = "SQL",
                code = "SELECT * FROM customers;",
                status = "SUCCESS",
                durationMs = 15L,
                consoleOutput = "3 rows retrieved",
                dumpCount = 1
            )

            repo.record(snapshot1)
            repo.record(snapshot2)

            val all = repo.getAll()
            assertEquals(2, all.size)
            // Descending order of timestamp: snap-2 first, then snap-1
            assertEquals("snap-2", all[0].id)
            assertEquals("snap-1", all[1].id)

            val fetched = repo.getById("snap-2")
            assertNotNull(fetched)
            assertEquals("SELECT * FROM customers;", fetched.code)
            assertEquals("SQL", fetched.languageMode)

            repo.delete("snap-1")
            assertEquals(1, repo.getAll().size)
            assertNull(repo.getById("snap-1"))

            repo.clear()
            assertEquals(0, repo.getAll().size)
        } finally {
            Files.deleteIfExists(tempDb)
        }
    }
}
