package org.andrew.jpad.core.database

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseServiceTest {

    private lateinit var tempDir: File
    private lateinit var dbService: DatabaseService
    private lateinit var sampleConfig: DatabaseConnectionConfig

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("jpad_db_test").toFile()
        dbService = DatabaseService(tempDir)
        sampleConfig = dbService.getOrCreateSampleDatabase()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initializes sample SQLite database with customers and orders tables`() {
        val testResult = dbService.testConnection(sampleConfig)
        assertTrue(testResult.isSuccess, "Sample database connection should succeed")

        val schema = dbService.getSchema(sampleConfig)
        val tableNames = schema.map { it.name.lowercase() }

        assertTrue(tableNames.contains("customers"), "Expected customers table")
        assertTrue(tableNames.contains("orders"), "Expected orders table")

        val customersTable = schema.first { it.name.equals("customers", ignoreCase = true) }
        val columnNames = customersTable.columns.map { it.name.lowercase() }
        assertTrue(columnNames.contains("id"))
        assertTrue(columnNames.contains("name"))
        assertTrue(columnNames.contains("email"))
        assertTrue(columnNames.contains("country"))
    }

    @Test
    fun `executes SELECT query and maps to DumpValue Table`() {
        val result = dbService.executeQuery(sampleConfig, "SELECT * FROM customers ORDER BY id ASC")
        assertTrue(result.isSuccess)
        assertNotNull(result.table)
        assertEquals(5, result.table.rows.size)
        assertEquals(listOf("id", "name", "email", "country", "created_at"), result.table.columns)

        val firstRow = result.table.rows[0]
        assertEquals(1, (firstRow["id"] as? org.andrew.jpad.core.dump.DumpValue.Primitive)?.rawValue)
        assertEquals("Ada Lovelace", (firstRow["name"] as? org.andrew.jpad.core.dump.DumpValue.Primitive)?.rawValue)
    }

    @Test
    fun `executes JOIN query between customers and orders`() {
        val sql = """
            SELECT c.name, o.product, o.amount
            FROM customers c
            JOIN orders o ON c.id = o.customer_id
            WHERE c.country = 'UK'
        """.trimIndent()

        val result = dbService.executeQuery(sampleConfig, sql)
        assertTrue(result.isSuccess)
        assertNotNull(result.table)
        assertEquals(2, result.table.rows.size)
        assertTrue(result.table.columns.contains("name"))
        assertTrue(result.table.columns.contains("product"))
        assertTrue(result.table.columns.contains("amount"))
    }

    @Test
    fun `returns error cleanly on invalid SQL syntax`() {
        val result = dbService.executeQuery(sampleConfig, "SELECT FROM NON_EXISTENT_TABLE WHERE ;;")
        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.isNotBlank())
    }

    @Test
    fun `executes INSERT and UPDATE statements returning affected count`() {
        val insertSql = "INSERT INTO customers (id, name, email) VALUES (99, 'Test User', 'test@user.com')"
        val insertResult = dbService.executeQuery(sampleConfig, insertSql)
        assertTrue(insertResult.isSuccess)
        assertEquals(1, insertResult.affectedRows)

        val updateSql = "UPDATE customers SET country = 'Mars' WHERE id = 99"
        val updateResult = dbService.executeQuery(sampleConfig, updateSql)
        assertTrue(updateResult.isSuccess)
        assertEquals(1, updateResult.affectedRows)
    }
}
