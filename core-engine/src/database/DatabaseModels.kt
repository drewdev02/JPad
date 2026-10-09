package org.andrew.jpad.core.database

import org.andrew.jpad.core.dump.DumpValue
import java.io.Serializable

enum class DatabaseDriverType(val displayName: String, val defaultUrl: String) {
    SQLITE("SQLite", "jdbc:sqlite:{path}"),
    H2("H2 (In-Memory)", "jdbc:h2:mem:jpad;DB_CLOSE_DELAY=-1"),
    POSTGRESQL("PostgreSQL", "jdbc:postgresql://localhost:5432/postgres"),
    MYSQL("MySQL", "jdbc:mysql://localhost:3306/test"),
    GENERIC("Generic JDBC", "jdbc:...")
}

data class DatabaseColumnMeta(
    val name: String,
    val typeName: String,
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false
) : Serializable

data class DatabaseTableMeta(
    val name: String,
    val type: String = "TABLE",
    val columns: List<DatabaseColumnMeta> = emptyList(),
    val rowCount: Long? = null
) : Serializable

data class DatabaseConnectionConfig(
    val id: String,
    val name: String,
    val type: DatabaseDriverType,
    val jdbcUrl: String,
    val username: String = "",
    val password: String = "",
    val isSample: Boolean = false
) : Serializable

data class DatabaseQueryResult(
    val isSuccess: Boolean,
    val table: DumpValue.Table? = null,
    val affectedRows: Int? = null,
    val executionTimeMs: Long = 0,
    val errorMessage: String? = null
) : Serializable
