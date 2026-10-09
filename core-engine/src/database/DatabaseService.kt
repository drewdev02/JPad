package org.andrew.jpad.core.database

import org.andrew.jpad.core.dump.DumpValue
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import kotlin.system.measureTimeMillis

class DatabaseService(
    private val databasesDir: File = File(System.getProperty("user.home"), ".jpad/databases")
) {

    init {
        try {
            if (!databasesDir.exists()) {
                databasesDir.mkdirs()
            }
        } catch (_: Exception) {}
    }

    /**
     * Initializes a sample SQLite database with sample tables and rows if not present.
     */
    fun getOrCreateSampleDatabase(): DatabaseConnectionConfig {
        databasesDir.mkdirs()
        val sampleDbFile = File(databasesDir, "sample.db")
        val isFirstTime = !sampleDbFile.exists() || sampleDbFile.length() == 0L

        val config = DatabaseConnectionConfig(
            id = "sample-sqlite",
            name = "Sample SQLite (Built-in)",
            type = DatabaseDriverType.SQLITE,
            jdbcUrl = "jdbc:sqlite:${sampleDbFile.absolutePath}",
            isSample = true
        )

        if (isFirstTime) {
            seedSampleDatabase(config)
        }

        return config
    }

    fun seedSampleDatabase(config: DatabaseConnectionConfig) {
        try {
            getConnection(config).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS customers (
                            id INTEGER PRIMARY KEY,
                            name TEXT NOT NULL,
                            email TEXT UNIQUE,
                            country TEXT,
                            created_at TEXT
                        );
                    """.trimIndent())

                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS orders (
                            id INTEGER PRIMARY KEY,
                            customer_id INTEGER,
                            product TEXT NOT NULL,
                            amount REAL,
                            status TEXT,
                            FOREIGN KEY (customer_id) REFERENCES customers(id)
                        );
                    """.trimIndent())

                    // Insert sample customers if empty
                    val rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")
                    val count = if (rs.next()) rs.getInt(1) else 0
                    rs.close()

                    if (count == 0) {
                        stmt.execute("""
                            INSERT INTO customers VALUES
                            (1, 'Ada Lovelace', 'ada@jpad.io', 'UK', '2026-01-15'),
                            (2, 'James Gosling', 'james@jpad.io', 'Canada', '2026-02-20'),
                            (3, 'Grace Hopper', 'grace@jpad.io', 'USA', '2026-03-10'),
                            (4, 'Linus Torvalds', 'linus@jpad.io', 'Finland', '2026-04-05'),
                            (5, 'Guido van Rossum', 'guido@jpad.io', 'Netherlands', '2026-05-12');
                        """.trimIndent())

                        stmt.execute("""
                            INSERT INTO orders VALUES
                            (101, 1, 'JPad Pro Developer License', 49.99, 'COMPLETED'),
                            (102, 2, 'JVM Performance Handbook', 29.50, 'COMPLETED'),
                            (103, 1, 'Cloud Scratchpad Add-on', 19.00, 'PENDING'),
                            (104, 3, 'Compiler Optimization Guide', 39.99, 'COMPLETED'),
                            (105, 4, 'Kernel Tuning Toolkit', 99.00, 'COMPLETED');
                        """.trimIndent())
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun getConnection(config: DatabaseConnectionConfig): Connection {
        return if (config.username.isNotBlank() || config.password.isNotBlank()) {
            DriverManager.getConnection(config.jdbcUrl, config.username, config.password)
        } else {
            DriverManager.getConnection(config.jdbcUrl)
        }
    }

    fun testConnection(config: DatabaseConnectionConfig): Result<Unit> {
        return try {
            getConnection(config).use { conn ->
                if (conn.isValid(5)) {
                    Result.success(Unit)
                } else {
                    Result.failure(SQLException("Connection test returned invalid status"))
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun getSchema(config: DatabaseConnectionConfig): List<DatabaseTableMeta> {
        val result = mutableListOf<DatabaseTableMeta>()
        try {
            getConnection(config).use { conn ->
                val metaData = conn.metaData

                val tableRs = metaData.getTables(null, null, "%", arrayOf("TABLE", "VIEW"))
                while (tableRs.next()) {
                    val tableName = tableRs.getString("TABLE_NAME") ?: continue
                    val tableType = tableRs.getString("TABLE_TYPE") ?: "TABLE"

                    // Ignore SQLite internal tables
                    if (tableName.startsWith("sqlite_")) continue

                    // Get primary keys
                    val primaryKeys = mutableSetOf<String>()
                    try {
                        val pkRs = metaData.getPrimaryKeys(null, null, tableName)
                        while (pkRs.next()) {
                            pkRs.getString("COLUMN_NAME")?.let { primaryKeys.add(it) }
                        }
                        pkRs.close()
                    } catch (_: Exception) {}

                    // Get columns
                    val columns = mutableListOf<DatabaseColumnMeta>()
                    val colRs = metaData.getColumns(null, null, tableName, "%")
                    while (colRs.next()) {
                        val colName = colRs.getString("COLUMN_NAME") ?: continue
                        val typeName = colRs.getString("TYPE_NAME") ?: "TEXT"
                        val nullable = colRs.getInt("NULLABLE") != 0
                        columns.add(
                            DatabaseColumnMeta(
                                name = colName,
                                typeName = typeName,
                                isNullable = nullable,
                                isPrimaryKey = primaryKeys.contains(colName)
                            )
                        )
                    }
                    colRs.close()

                    result.add(
                        DatabaseTableMeta(
                            name = tableName,
                            type = tableType,
                            columns = columns
                        )
                    )
                }
                tableRs.close()
            }
        } catch (_: Exception) {}
        return result
    }

    fun executeQuery(
        config: DatabaseConnectionConfig,
        sql: String,
        maxRows: Int = 1000
    ): DatabaseQueryResult {
        var durationMs = 0L
        val trimmed = sql.trim()
        if (trimmed.isEmpty()) {
            return DatabaseQueryResult(isSuccess = true, affectedRows = 0)
        }

        return try {
            var tableResult: DumpValue.Table? = null
            var affectedCount: Int? = null

            durationMs = measureTimeMillis {
                getConnection(config).use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.maxRows = maxRows
                        val hasResultSet = stmt.execute(trimmed)

                        if (hasResultSet) {
                            stmt.resultSet.use { rs ->
                                val rsMeta = rs.metaData
                                val colCount = rsMeta.columnCount
                                val columns = (1..colCount).map { i ->
                                    val label = rsMeta.getColumnLabel(i)
                                    if (!label.isNullOrBlank()) label else rsMeta.getColumnName(i)
                                }

                                val rows = mutableListOf<Map<String, DumpValue>>()
                                while (rs.next()) {
                                    val rowMap = mutableMapOf<String, DumpValue>()
                                    for (i in 1..colCount) {
                                        val colName = columns[i - 1]
                                        val obj = rs.getObject(i)
                                        rowMap[colName] = DumpValue.Primitive(
                                            rawValue = obj,
                                            typeName = obj?.javaClass?.simpleName
                                        )
                                    }
                                    rows.add(rowMap)
                                }

                                tableResult = DumpValue.Table(
                                    typeName = "SQL Query Result",
                                    columns = columns,
                                    rows = rows,
                                    totalCount = rows.size
                                )
                            }
                        } else {
                            affectedCount = stmt.updateCount
                        }
                    }
                }
            }

            DatabaseQueryResult(
                isSuccess = true,
                table = tableResult,
                affectedRows = affectedCount,
                executionTimeMs = durationMs
            )
        } catch (e: Throwable) {
            DatabaseQueryResult(
                isSuccess = false,
                errorMessage = e.message ?: e.toString(),
                executionTimeMs = durationMs
            )
        }
    }
}
