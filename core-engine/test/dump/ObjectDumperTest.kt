package org.andrew.jpad.core.dump

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ObjectDumperTest {

    private val dumper = ObjectDumper()

    @Test
    fun `dump null returns Primitive with null`() {
        val result = dumper.dump(null)
        assertIs<DumpValue.Primitive>(result)
        assertEquals(null, result.rawValue)
    }

    @Test
    fun `dump primitive types returns Primitive with value and type`() {
        val intResult = dumper.dump(42)
        assertIs<DumpValue.Primitive>(intResult)
        assertEquals(42, intResult.rawValue)
        assertEquals("Integer", intResult.typeName)

        val strResult = dumper.dump("Hello")
        assertIs<DumpValue.Primitive>(strResult)
        assertEquals("Hello", strResult.rawValue)
        assertEquals("String", strResult.typeName)

        val boolResult = dumper.dump(true)
        assertIs<DumpValue.Primitive>(boolResult)
        assertEquals(true, boolResult.rawValue)
    }

    @Test
    fun `dump primitive collection returns Table with Value column`() {
        val numbers = listOf(10, 20, 30)
        val result = dumper.dump(numbers)
        assertIs<DumpValue.Table>(result)
        assertEquals(listOf("Value"), result.columns)
        assertEquals(3, result.rows.size)
        assertEquals(3, result.totalCount)
        assertEquals(10, (result.rows[0]["Value"] as DumpValue.Primitive).rawValue)
        assertEquals(20, (result.rows[1]["Value"] as DumpValue.Primitive).rawValue)
    }

    @Test
    fun `dump map returns Table with Key and Value columns`() {
        val map = mapOf("es" to "Spanish", "en" to "English")
        val result = dumper.dump(map)
        assertIs<DumpValue.Table>(result)
        assertEquals(listOf("Key", "Value"), result.columns)
        assertEquals(2, result.rows.size)
        assertEquals("es", (result.rows[0]["Key"] as DumpValue.Primitive).rawValue)
        assertEquals("Spanish", (result.rows[0]["Value"] as DumpValue.Primitive).rawValue)
    }

    data class Person(val name: String, val age: Int, val isDev: Boolean)

    @Test
    fun `dump list of objects returns multi-column Table`() {
        val people = listOf(
            Person("Andrew", 30, true),
            Person("Elena", 28, false)
        )
        val result = dumper.dump(people)
        assertIs<DumpValue.Table>(result)
        assertTrue(result.columns.contains("name"))
        assertTrue(result.columns.contains("age"))
        assertTrue(result.columns.contains("isDev"))
        assertEquals(2, result.rows.size)

        val row0 = result.rows[0]
        assertEquals("Andrew", (row0["name"] as DumpValue.Primitive).rawValue)
        assertEquals(30, (row0["age"] as DumpValue.Primitive).rawValue)
        assertEquals(true, (row0["isDev"] as DumpValue.Primitive).rawValue)
    }

    @Test
    fun `dump single object returns KeyValueList or Table`() {
        val person = Person("Lucas", 35, true)
        val result = dumper.dump(person)
        assertIs<DumpValue.KeyValueList>(result)
        val entriesMap = result.entries.toMap()
        assertEquals("Lucas", (entriesMap["name"] as DumpValue.Primitive).rawValue)
        assertEquals(35, (entriesMap["age"] as DumpValue.Primitive).rawValue)
        assertEquals(true, (entriesMap["isDev"] as DumpValue.Primitive).rawValue)
    }

    class CyclicNode(val name: String, var next: CyclicNode? = null)

    @Test
    fun `dump cyclic structure stops and does not cause StackOverflowError`() {
        val nodeA = CyclicNode("A")
        val nodeB = CyclicNode("B")
        nodeA.next = nodeB
        nodeB.next = nodeA

        val result = dumper.dump(nodeA)
        assertIs<DumpValue.KeyValueList>(result)
    }
}
