package org.andrew.jpad.ui.components.grid

import org.andrew.jpad.core.dump.DumpValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DumpTableExporterTest {

    private val sampleColumns = listOf("id", "name", "role", "salary", "isRemote")
    private val sampleRows = listOf(
        mapOf(
            "id" to DumpValue.Primitive(1),
            "name" to DumpValue.Primitive("Alice"),
            "role" to DumpValue.Primitive("Tech Lead"),
            "salary" to DumpValue.Primitive(120000),
            "isRemote" to DumpValue.Primitive(true)
        ),
        mapOf(
            "id" to DumpValue.Primitive(2),
            "name" to DumpValue.Primitive("Bob Smith, Jr."),
            "role" to DumpValue.Primitive("Backend Engineer"),
            "salary" to DumpValue.Primitive(95000),
            "isRemote" to DumpValue.Primitive(false)
        ),
        mapOf(
            "id" to DumpValue.Primitive(3),
            "name" to DumpValue.Primitive("Charlie \"Chuck\""),
            "role" to DumpValue.Primitive("Frontend Dev"),
            "salary" to DumpValue.Primitive(98000),
            "isRemote" to DumpValue.Primitive(true)
        )
    )

    @Test
    fun `exports table rows to valid CSV with proper quoting`() {
        val csv = DumpTableExporter.toCsv(sampleColumns, sampleRows)
        val lines = csv.lines().filter { it.isNotBlank() }

        assertEquals(4, lines.size) // header + 3 rows
        assertEquals("id,name,role,salary,isRemote", lines[0])

        // First row: Alice
        assertEquals("1,Alice,Tech Lead,120000,true", lines[1])

        // Second row contains comma: Bob Smith, Jr. -> should be quoted
        assertTrue(lines[2].contains("\"Bob Smith, Jr.\""))

        // Third row contains quotes: Charlie "Chuck" -> should be escaped as ""Chuck""
        assertTrue(lines[3].contains("\"Charlie \"\"Chuck\"\"\""))
    }

    @Test
    fun `exports table rows to valid JSON`() {
        val json = DumpTableExporter.toJson(sampleColumns, sampleRows)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"name\": \"Alice\""))
        assertTrue(json.contains("\"salary\": 120000"))
        assertTrue(json.contains("\"isRemote\": true"))
        assertTrue(json.contains("\"Bob Smith, Jr.\""))
    }

    @Test
    fun `exports table rows to valid TSV`() {
        val tsv = DumpTableExporter.toTsv(sampleColumns, sampleRows)
        val lines = tsv.lines().filter { it.isNotBlank() }

        assertEquals(4, lines.size)
        assertEquals("id\tname\trole\tsalary\tisRemote", lines[0])
        assertEquals("1\tAlice\tTech Lead\t120000\ttrue", lines[1])
    }

    @Test
    fun `exports table rows to formatted ASCII table`() {
        val ascii = DumpTableExporter.toAsciiTable(listOf("id", "name"), sampleRows)
        val lines = ascii.lines().filter { it.isNotBlank() }

        assertTrue(lines.size >= 4) // header, separator, 3 rows
        assertTrue(lines[0].contains("id"))
        assertTrue(lines[0].contains("name"))
        assertTrue(lines[1].contains("-+-"))
        assertTrue(lines[2].contains("Alice"))
    }

    @Test
    fun `filters rows case-insensitively and with multi-token search`() {
        // Empty query returns all
        assertEquals(3, DumpTableExporter.filterRows(sampleRows, "").size)
        assertEquals(3, DumpTableExporter.filterRows(sampleRows, "   ").size)

        // Single word matching name
        val aliceResult = DumpTableExporter.filterRows(sampleRows, "alice")
        assertEquals(1, aliceResult.size)
        assertEquals("Alice", (aliceResult[0]["name"] as DumpValue.Primitive).rawValue)

        // Matching role
        val frontendResult = DumpTableExporter.filterRows(sampleRows, "Frontend")
        assertEquals(1, frontendResult.size)

        // Multi-token: "Bob Backend" matches row 2
        val multiResult = DumpTableExporter.filterRows(sampleRows, "Bob Backend")
        assertEquals(1, multiResult.size)

        // Non-matching query
        val noMatch = DumpTableExporter.filterRows(sampleRows, "Developer XYZ")
        assertEquals(0, noMatch.size)
    }

    @Test
    fun `sorts rows numerically and alphabetically ASC and DESC`() {
        // Sort by salary ASC
        val sortSalaryAsc = DumpTableExporter.sortRows(
            sampleRows,
            DumpColumnSort("salary", DumpSortDirection.ASC)
        )
        val salariesAsc = sortSalaryAsc.map { (it["salary"] as DumpValue.Primitive).rawValue as Int }
        assertEquals(listOf(95000, 98000, 120000), salariesAsc)

        // Sort by salary DESC
        val sortSalaryDesc = DumpTableExporter.sortRows(
            sampleRows,
            DumpColumnSort("salary", DumpSortDirection.DESC)
        )
        val salariesDesc = sortSalaryDesc.map { (it["salary"] as DumpValue.Primitive).rawValue as Int }
        assertEquals(listOf(120000, 98000, 95000), salariesDesc)

        // Sort by name ASC
        val sortNameAsc = DumpTableExporter.sortRows(
            sampleRows,
            DumpColumnSort("name", DumpSortDirection.ASC)
        )
        val namesAsc = sortNameAsc.map { (it["name"] as DumpValue.Primitive).rawValue as String }
        assertEquals("Alice", namesAsc[0])
        assertTrue(namesAsc[1].startsWith("Bob"))
        assertTrue(namesAsc[2].startsWith("Charlie"))
    }

    @Test
    fun `sorting places null values last`() {
        val rowsWithNull = listOf(
            mapOf("val" to DumpValue.Primitive("Zebra")),
            mapOf("val" to DumpValue.Primitive(null)),
            mapOf("val" to DumpValue.Primitive("Apple"))
        )

        val sortedAsc = DumpTableExporter.sortRows(
            rowsWithNull,
            DumpColumnSort("val", DumpSortDirection.ASC)
        )
        val values = sortedAsc.map { (it["val"] as DumpValue.Primitive).rawValue }
        assertEquals("Apple", values[0])
        assertEquals("Zebra", values[1])
        assertEquals(null, values[2])
    }
}
