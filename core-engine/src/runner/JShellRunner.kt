package org.andrew.jpad.core.runner

import jdk.jshell.Diag
import jdk.jshell.JShell
import jdk.jshell.Snippet
import jdk.jshell.SnippetEvent
import jdk.jshell.SourceCodeAnalysis
import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.runtime.JPad
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class JShellRunner(
    private val objectDumper: ObjectDumper
) : SnippetRunner {

    private val defaultImports = listOf(
        "import java.util.*;",
        "import java.util.stream.*;",
        "import java.util.concurrent.*;",
        "import java.time.*;",
        "import java.util.function.*;",
        "import org.andrew.jpad.runtime.JPad;",
        "import static org.andrew.jpad.runtime.JPad.dump;"
    )

    override fun execute(code: String): ExecutionOutput {
        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()
        val outPrintStream = PrintStream(stdoutStream, true, Charsets.UTF_8)
        val errPrintStream = PrintStream(stderrStream, true, Charsets.UTF_8)

        val originalOut = System.out
        val originalErr = System.err

        val dumps = java.util.Collections.synchronizedList(mutableListOf<DumpValue>())
        val diagnostics = mutableListOf<CompilationDiagnostic>()
        var exitCode = 0
        var runtimeThrowable: Throwable? = null

        val builder = JShell.builder()
            .out(outPrintStream)
            .err(errPrintStream)

        // Try local execution engine if available for direct in-memory reflection & dump capture
        val jshell = try {
            builder.executionEngine("local").build()
        } catch (e: Throwable) {
            builder.build()
        }

        try {
            System.setOut(outPrintStream)
            System.setErr(errPrintStream)

            // Add current classpath so JPad runtime and dependencies are available
            val classPath = System.getProperty("java.class.path")
            if (classPath != null) {
                for (path in classPath.split(File.pathSeparator)) {
                    if (path.isNotBlank()) {
                        jshell.addToClasspath(path)
                    }
                }
            }

            // Set up dump consumer
            JPad.setDumpConsumer { obj, title ->
                dumps.add(objectDumper.dump(obj, title))
            }

            // Pre-load default imports into session
            for (importStmt in defaultImports) {
                jshell.eval(importStmt)
            }

            // Analyze and execute user snippets one by one
            var remaining = code.trim()
            val sourceAnalysis = jshell.sourceCodeAnalysis()

            while (remaining.isNotBlank()) {
                val completionInfo = sourceAnalysis.analyzeCompletion(remaining)
                val snippetSource = completionInfo.source()

                if (snippetSource.isNullOrBlank()) {
                    break
                }

                val events: List<SnippetEvent> = jshell.eval(snippetSource)

                for (event in events) {
                    val snippet = event.snippet()
                    val status = event.status()

                    // Collect diagnostics on rejected snippets
                    if (status == Snippet.Status.REJECTED) {
                        exitCode = 1
                        val snippetDiags = jshell.diagnostics(snippet)
                        for (diag in snippetDiags) {
                            diagnostics.add(
                                CompilationDiagnostic(
                                    line = diag.position,
                                    column = 1,
                                    message = diag.getMessage(null),
                                    kind = if (diag.isError) DiagnosticKind.ERROR else DiagnosticKind.WARNING
                                )
                            )
                        }
                    }

                    // Check for runtime exceptions during evaluation
                    val exception = event.exception()
                    if (exception != null) {
                        exitCode = 1
                        runtimeThrowable = exception
                        errPrintStream.println("Exception: ${exception.message ?: exception.toString()}")
                        exception.printStackTrace(errPrintStream)
                    }
                }

                if (exitCode != 0 && diagnostics.isNotEmpty()) {
                    break
                }

                remaining = completionInfo.remaining()?.trim() ?: ""
            }

        } catch (t: Throwable) {
            exitCode = 1
            runtimeThrowable = t
            t.printStackTrace(errPrintStream)
        } finally {
            JPad.setDumpConsumer(null)
            System.setOut(originalOut)
            System.setErr(originalErr)
            outPrintStream.flush()
            errPrintStream.flush()
            try {
                jshell.close()
            } catch (_: Throwable) {}
        }

        return ExecutionOutput(
            stdout = stdoutStream.toString(Charsets.UTF_8),
            stderr = stderrStream.toString(Charsets.UTF_8),
            exitCode = exitCode,
            throwable = runtimeThrowable,
            dumps = dumps,
            diagnostics = diagnostics
        )
    }
}
