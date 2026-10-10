package org.andrew.jpad.core.completion

import org.andrew.jpad.core.database.DatabaseColumnMeta
import org.andrew.jpad.core.database.DatabaseTableMeta
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlCompletionServiceTest {

    private lateinit var service: SqlCompletionService
    private val sampleSchema = listOf(
        DatabaseTableMeta(
            name = "customers",
            columns = listOf(
                DatabaseColumnMeta("id", "INTEGER", isNullable = false, isPrimaryKey = true),
                DatabaseColumnMeta("name", "TEXT", isNullable = false),
                DatabaseColumnMeta("email", "TEXT", isNullable = true),
                DatabaseColumnMeta("country", "TEXT", isNullable = true)
            )
        ),
        DatabaseTableMeta(
            name = "orders",
            columns = listOf(
                DatabaseColumnMeta("id", "INTEGER", isNullable = false, isPrimaryKey = true),
                DatabaseColumnMeta("customer_id", "INTEGER", isNullable = false),
                DatabaseColumnMeta("product", "TEXT", isNullable = false),
                DatabaseColumnMeta("amount", "REAL", isNullable = true)
            )
        )
    )

    @BeforeTest
    fun setUp() {
        service = SqlCompletionService { sampleSchema }
    }

    @Test
    fun `suggests tables after FROM clause`() {
        val sql = "SELECT * FROM "
        val result = service.getCompletions(sql, sql.length)

        val tableNames = result.items.filter { it.kind == LookupElementKind.TABLE }.map { it.lookupString }
        assertTrue("customers" in tableNames, "Expected customers in table completions")
        assertTrue("orders" in tableNames, "Expected orders in table completions")
    }

    @Test
    fun `suggests tables after JOIN clause with prefix`() {
        val sql = "SELECT * FROM customers c JOIN ord"
        val result = service.getCompletions(sql, sql.length)

        assertEquals("ord", result.prefix)
        val ordersItem = result.items.firstOrNull { it.lookupString == "orders" }
        assertNotNull(ordersItem)
        assertEquals(LookupElementKind.TABLE, ordersItem.kind)
    }

    @Test
    fun `suggests columns qualified by table alias after dot`() {
        val sql = "SELECT c.em FROM customers c"
        // Cursor right after "em"
        val cursorOffset = "SELECT c.em".length
        val result = service.getCompletions(sql, cursorOffset)

        assertEquals("em", result.prefix)
        assertEquals("SELECT c.".length, result.anchor)

        val emailItem = result.items.firstOrNull { it.lookupString == "email" }
        assertNotNull(emailItem, "Expected 'email' column when qualifying with 'c.'")
        assertEquals(LookupElementKind.COLUMN, emailItem.kind)
    }

    @Test
    fun `suggests columns of tables in query after SELECT`() {
        val sql = "SELECT  FROM customers"
        val cursorOffset = "SELECT ".length
        val result = service.getCompletions(sql, cursorOffset)

        val columns = result.items.filter { it.kind == LookupElementKind.COLUMN }.map { it.lookupString }
        assertTrue("name" in columns, "Expected 'name' column in SELECT clause")
        assertTrue("email" in columns, "Expected 'email' column in SELECT clause")
    }

    @Test
    fun `suggests smart join condition after ON clause`() {
        val sql = "SELECT * FROM customers c JOIN orders o ON "
        val result = service.getCompletions(sql, sql.length)

        val smartOn = result.items.firstOrNull { it.isSmart }
        assertNotNull(smartOn, "Expected smart join condition after ON")
        assertTrue(
            smartOn.lookupString == "o.customer_id = c.id" || smartOn.lookupString == "c.id = o.customer_id",
            "Smart join condition should match customer_id with id, got: ${smartOn.lookupString}"
        )
    }

    @Test
    fun `suggests SQL snippets like sel`() {
        val sql = "sel"
        val result = service.getCompletions(sql, sql.length)

        val snippet = result.items.firstOrNull { it.lookupString == "sel" }
        assertNotNull(snippet, "Expected 'sel' live template")
        assertEquals("SELECT * FROM ", snippet.insertText)
        assertEquals(LookupElementKind.SNIPPET, snippet.kind)
    }

    @Test
    fun `suggests SQL functions like COUNT and SUM`() {
        val sql = "SELECT cou"
        val result = service.getCompletions(sql, sql.length)

        val countItem = result.items.firstOrNull { it.lookupString.startsWith("COUNT") }
        assertNotNull(countItem, "Expected COUNT function in SELECT clause")
        assertEquals(LookupElementKind.SQL_FUNCTION, countItem.kind)
    }
}
