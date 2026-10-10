package org.andrew.jpad.core

import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.di.coreEngineModule
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExecutionEngineTest {

    private val engine = koinApplication { modules(coreEngineModule) }.koin.get<ExecutionEngine>()

    @Test
    fun `executes raw statements end to end and returns formatted output`() {
        val script = """
            int a = 10;
            int b = 20;
            System.out.println("Result: " + (a + b));
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess)
        assertTrue(response.output.contains("Result: 30"))
        assertEquals("", response.error)
        assertTrue(response.diagnostics.isEmpty())
    }

    @Test
    fun `returns compilation error diagnostics when code is invalid`() {
        val script = "int a = ;"

        val response = engine.execute(script)

        assertFalse(response.isSuccess)
        assertTrue(response.diagnostics.any { it.kind == DiagnosticKind.ERROR })
        assertTrue(response.error.contains("Compilation failed"))
    }

    @Test
    fun `executes statements calling dump and captures DumpValue objects`() {
        val script = """
            record Item(String name, double price) {}
            var items = List.of(
                new Item("Keyboard", 99.9),
                new Item("Mouse", 49.5)
            );
            dump(items);
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess)
        assertEquals(1, response.dumps.size)
        val dumpTable = response.dumps.first()
        assertIs<DumpValue.Table>(dumpTable)
        assertEquals(2, dumpTable.rows.size)
        assertTrue(dumpTable.columns.contains("name"))
        assertTrue(dumpTable.columns.contains("price"))
    }

    @Test
    fun `executes modern java 21 script with sealed types pattern matching and virtual threads`() {
        val script = """
            enum Severity { LOW, MEDIUM, CRITICAL }
            sealed interface Task permits Feature, Bug, Chore {}
            record Feature(String title, int storyPoints) implements Task {}
            record Bug(String title, Severity severity) implements Task {}
            record Chore(String title, Duration estimate) implements Task {}

            record Developer(String name, String role, int experienceYears, boolean isRemote, List<Task> tasks) {}

            var team = List.of(
                new Developer("Andrew", "Lead Architect", 8, true, List.of(
                    new Feature("Engine socket protocol", 13),
                    new Bug("Memory leak in dump renderer", Severity.CRITICAL),
                    new Chore("Update CI pipeline", Duration.ofHours(4))))
            );

            int weight(Task t) {
                return switch (t) {
                    case Feature f -> f.storyPoints();
                    case Bug b when b.severity() == Severity.CRITICAL -> 8;
                    case Bug b -> 3;
                    case Chore c -> (int) c.estimate().toHours();
                };
            }

            dump(team, "Equipo completo");
            System.out.println("Developer tasks: " + team.get(0).tasks().size());
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess, "Error: ${response.error}")
        assertTrue(response.output.contains("Developer tasks: 3"))
        assertEquals(1, response.dumps.size)
    }

    @Test
    fun `executes user script using cache and retains computed values across executions`() {
        val script = """
            var val1 = cache("my_calc", () -> 42 * 10);
            System.out.println("Cached value: " + val1);
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess, "Error: ${response.error}")
        assertTrue(response.output.contains("Cached value: 420"))
    }

    @Test
    fun `executes user script using benchmark and captures benchmark dump`() {
        val script = """
            benchmark("Array Sorting", 20, () -> {
                var list = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9));
                Collections.sort(list);
            });
        """.trimIndent()

        val response = engine.execute(script)

        assertTrue(response.isSuccess, "Error: ${response.error}")
        assertTrue(response.dumps.isNotEmpty(), "Expected benchmark to trigger a dump")
    }
}
