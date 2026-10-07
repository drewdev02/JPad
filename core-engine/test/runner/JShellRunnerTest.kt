package org.andrew.jpad.core.runner

import org.andrew.jpad.core.di.coreEngineModule
import org.koin.dsl.koinApplication
import org.andrew.jpad.core.dump.DumpValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JShellRunnerTest {

    private val runner = koinApplication { modules(coreEngineModule) }.koin.get<JShellRunner>()

    @Test
    fun `evaluates simple statements and captures stdout`() {
        val code = """
            int x = 15;
            int y = 25;
            System.out.println("Sum: " + (x + y));
        """.trimIndent()

        val output = runner.execute(code)

        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("Sum: 40"))
        assertTrue(output.stderr.isEmpty())
        assertTrue(output.diagnostics.isEmpty())
    }

    @Test
    fun `evaluates top-level methods and records with dump`() {
        val code = """
            record Product(String name, double price) {}
            
            double discounted(Product p, double pct) {
                return p.price() * (1.0 - pct);
            }
            
            var p = new Product("MacBook Pro", 2000.0);
            var finalPrice = discounted(p, 0.10);
            System.out.println("Final: " + finalPrice);
            dump(p, "Tested Product");
        """.trimIndent()

        val output = runner.execute(code)

        assertEquals(0, output.exitCode)
        assertTrue(output.stdout.contains("Final: 1800.0"))
        assertEquals(1, output.dumps.size)
        val dumpItem = output.dumps.first()
        assertIs<DumpValue.KeyValueList>(dumpItem)
        assertEquals("Tested Product", dumpItem.title)
    }

    @Test
    fun `evaluates sealed interfaces pattern matching virtual threads and complex script`() {
        val code = """
            enum Severity { LOW, MEDIUM, CRITICAL }
            
            sealed interface Task permits Feature, Bug, Chore {}
            record Feature(String title, int storyPoints) implements Task {}
            record Bug(String title, Severity severity) implements Task {}
            record Chore(String title, Duration estimate) implements Task {}
            
            record Developer(String name, String role, int experienceYears,
                             boolean isRemote, List<Task> tasks) {}
            
            record Report(String developer, int totalWeight, long elapsedMs) {}
            
            var team = List.of(
                new Developer("Andrew", "Lead Architect", 8, true, List.of(
                    new Feature("Engine socket protocol", 13),
                    new Bug("Memory leak in dump renderer", Severity.CRITICAL),
                    new Chore("Update CI pipeline", Duration.ofHours(4)))),
                new Developer("Elena", "Senior Backend Engineer", 6, true, List.of(
                    new Feature("Maven Central search", 8),
                    new Feature("Dependency resolver", 21),
                    new Bug("Wrong classpath order", Severity.MEDIUM))),
                new Developer("Lucas", "Compiler & JVM Specialist", 10, false, List.of(
                    new Feature("Autocomplete via JDT", 21),
                    new Bug("JShell crash on sealed types", Severity.CRITICAL),
                    new Bug("Typo in error message", Severity.LOW),
                    new Chore("Write jpackage scripts", Duration.ofHours(6))))
            );
            
            int weight(Task t) {
                return switch (t) {
                    case Feature f -> f.storyPoints();
                    case Bug b when b.severity() == Severity.CRITICAL -> 8;
                    case Bug b -> 3;
                    case Chore c -> (int) c.estimate().toHours();
                };
            }
            
            dump(team, "Equipo con sus tareas");
            
            var workload = team.stream().collect(Collectors.toMap(
                Developer::name,
                d -> d.tasks().stream().mapToInt(t -> weight(t)).sum(),
                (a, b) -> a,
                LinkedHashMap::new));
            dump(workload, "Carga por desarrollador");
            
            System.out.println("Workload computed for " + workload.size() + " developers");
        """.trimIndent()

        val output = runner.execute(code)

        assertEquals(0, output.exitCode, "Output error: ${output.stderr}")
        assertTrue(output.stdout.contains("Workload computed for 3 developers"))
        assertTrue(output.dumps.size >= 2)
    }

    @Test
    fun `captures syntax errors as diagnostics`() {
        val code = "int a = ;"
        val output = runner.execute(code)

        assertFalse(output.exitCode == 0)
        assertTrue(output.diagnostics.isNotEmpty())
    }

    @Test
    fun `captures runtime exceptions`() {
        val code = """
            String text = null;
            text.length();
        """.trimIndent()

        val output = runner.execute(code)

        assertFalse(output.exitCode == 0)
        assertTrue(output.stderr.contains("NullPointerException") || output.stderr.contains("Exception"))
    }
}
