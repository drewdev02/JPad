package org.andrew.jpad.history

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager
import java.sql.ResultSet

interface ExecutionHistoryRepository {
    fun record(snapshot: ExecutionSnapshot)
    fun getAll(limit: Int = 100): List<ExecutionSnapshot>
    fun getById(id: String): ExecutionSnapshot?
    fun delete(id: String)
    fun clear()
}

class SqliteExecutionHistoryRepository(
    private val dbPath: Path = Paths.get(System.getProperty("user.home"), ".jpad", "history.db")
) : ExecutionHistoryRepository {

    private val jdbcUrl: String = "jdbc:sqlite:${dbPath.toAbsolutePath()}"

    init {
        try {
            if (dbPath.parent != null) {
                Files.createDirectories(dbPath.parent)
            }
            initSchema()
        } catch (e: Exception) {
            System.err.println("Failed to initialize SqliteExecutionHistoryRepository: ${e.message}")
        }
    }

    private fun getConnection() = DriverManager.getConnection(jdbcUrl)

    private fun initSchema() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS execution_history (
                        id TEXT PRIMARY KEY,
                        timestamp INTEGER NOT NULL,
                        tab_title TEXT NOT NULL,
                        language_mode TEXT NOT NULL,
                        code TEXT NOT NULL,
                        status TEXT NOT NULL,
                        duration_ms INTEGER NOT NULL,
                        console_output TEXT,
                        dump_count INTEGER DEFAULT 0
                    );
                    CREATE INDEX IF NOT EXISTS idx_history_timestamp ON execution_history(timestamp DESC);
                    """.trimIndent()
                )
            }
        }
    }

    override fun record(snapshot: ExecutionSnapshot) {
        try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    """
                    INSERT INTO execution_history (id, timestamp, tab_title, language_mode, code, status, duration_ms, console_output, dump_count)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, snapshot.id)
                    stmt.setLong(2, snapshot.timestamp)
                    stmt.setString(3, snapshot.tabTitle)
                    stmt.setString(4, snapshot.languageMode)
                    stmt.setString(5, snapshot.code)
                    stmt.setString(6, snapshot.status)
                    stmt.setLong(7, snapshot.durationMs)
                    stmt.setString(8, snapshot.consoleOutput)
                    stmt.setInt(9, snapshot.dumpCount)
                    stmt.executeUpdate()
                }

                // Keep maximum 200 items in history
                conn.createStatement().use { stmt ->
                    stmt.execute(
                        """
                        DELETE FROM execution_history WHERE id NOT IN (
                            SELECT id FROM execution_history ORDER BY timestamp DESC LIMIT 200
                        )
                        """.trimIndent()
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Error recording execution snapshot: ${e.message}")
        }
    }

    override fun getAll(limit: Int): List<ExecutionSnapshot> {
        val list = mutableListOf<ExecutionSnapshot>()
        try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, timestamp, tab_title, language_mode, code, status, duration_ms, console_output, dump_count FROM execution_history ORDER BY timestamp DESC LIMIT ?"
                ).use { stmt ->
                    stmt.setInt(1, limit)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(mapRow(rs))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching execution history: ${e.message}")
        }
        return list
    }

    override fun getById(id: String): ExecutionSnapshot? {
        try {
            getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, timestamp, tab_title, language_mode, code, status, duration_ms, console_output, dump_count FROM execution_history WHERE id = ?"
                ).use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return mapRow(rs)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching execution snapshot by id: ${e.message}")
        }
        return null
    }

    override fun delete(id: String) {
        try {
            getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM execution_history WHERE id = ?").use { stmt ->
                    stmt.setString(1, id)
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            System.err.println("Error deleting execution snapshot: ${e.message}")
        }
    }

    override fun clear() {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("DELETE FROM execution_history")
                }
            }
        } catch (e: Exception) {
            System.err.println("Error clearing execution history: ${e.message}")
        }
    }

    private fun mapRow(rs: ResultSet): ExecutionSnapshot {
        return ExecutionSnapshot(
            id = rs.getString("id"),
            timestamp = rs.getLong("timestamp"),
            tabTitle = rs.getString("tab_title"),
            languageMode = rs.getString("language_mode"),
            code = rs.getString("code"),
            status = rs.getString("status"),
            durationMs = rs.getLong("duration_ms"),
            consoleOutput = rs.getString("console_output") ?: "",
            dumpCount = rs.getInt("dump_count")
        )
    }
}
