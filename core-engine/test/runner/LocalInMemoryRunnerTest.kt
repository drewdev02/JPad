package org.andrew.jpad.core.runner

import org.andrew.jpad.core.di.coreEngineModule
import org.koin.dsl.koinApplication
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.parser.SnippetWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalInMemoryRunnerTest {

    private val compiler = InMemoryJavaCompiler()
    private val wrapper = SnippetWrapper()
    private val runner = koinApplication { modules(coreEngineModule) }.koin.get<LocalInMemoryRunner>()

    @Test
    fun `executes statements snippet and captures stdout output`() {
        val snippet = """
            System.out.println("Hello from JPad Local Runner!");
        """.trimIndent()

        val parsed = wrapper.wrap(snippet, className = "TestSnippet")
        val compilationResult = compiler.compile(listOf(parsed.generatedSource))

        assertTrue(compilationResult.isSuccess, "Compilation should succeed")

        val execution = runner.run(compilationResult.compiledClasses, parsed.fullClassName)

        assertEquals(0, execution.exitCode)
        assertNull(execution.throwable)
        assertTrue(execution.stdout.contains("Hello from JPad Local Runner!"), "Stdout should capture printed line")
    }

    @Test
    fun `captures thrown exceptions during execution`() {
        val snippet = """
            throw new IllegalArgumentException("Test exception message");
        """.trimIndent()

        val parsed = wrapper.wrap(snippet, className = "ExceptionSnippet")
        val compilationResult = compiler.compile(listOf(parsed.generatedSource))

        assertTrue(compilationResult.isSuccess)

        val execution = runner.run(compilationResult.compiledClasses, parsed.fullClassName)

        assertEquals(1, execution.exitCode)
        assertTrue(execution.throwable is IllegalArgumentException || execution.throwable?.cause is IllegalArgumentException)
        assertTrue(execution.stderr.contains("Test exception message") || execution.throwable.message?.contains("Test exception message") == true)
    }
}
