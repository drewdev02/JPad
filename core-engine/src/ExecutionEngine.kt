package org.andrew.jpad.core

import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.parser.SnippetMode
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.CancellableRunner
import org.andrew.jpad.core.runner.ScriptRunner
import org.andrew.jpad.core.runner.SnippetRunner
import kotlin.system.measureTimeMillis

data class ExecutionResponse(
    val isSuccess: Boolean,
    val output: String,
    val error: String,
    val diagnostics: List<CompilationDiagnostic> = emptyList(),
    val durationMs: Long = 0,
    val dumps: List<DumpValue> = emptyList()
)

class ExecutionEngine(
    private val compiler: JavaCompilerService,
    private val wrapper: SnippetWrapper,
    private val runner: ScriptRunner,
    private val snippetRunner: SnippetRunner
) {

    /** Stops the code currently running, if any. */
    fun cancel() {
        (runner as? CancellableRunner)?.cancel()
        (snippetRunner as? CancellableRunner)?.cancel()
    }

    fun execute(userCode: String): ExecutionResponse {
        val parsed = wrapper.wrap(userCode)
        var durationMs = 0L

        // If it's a full Java class with main method, compile directly 1:1 without altering the code
        if (parsed.mode == SnippetMode.PROGRAM) {
            val compilationResult: org.andrew.jpad.core.compiler.CompilationResult
            val compilationTime = measureTimeMillis {
                compilationResult = compiler.compile(listOf(parsed.generatedSource))
            }
            durationMs += compilationTime

            if (!compilationResult.isSuccess) {
                val errorDiagnostics = compilationResult.diagnostics.filter { it.kind == DiagnosticKind.ERROR }
                val formattedErrors = errorDiagnostics.joinToString("\n") { diag ->
                    "Line ${diag.line}, Column ${diag.column}: ${diag.message}"
                }
                return ExecutionResponse(
                    isSuccess = false,
                    output = "",
                    error = "Compilation failed:\n$formattedErrors",
                    diagnostics = compilationResult.diagnostics,
                    durationMs = durationMs
                )
            }

            val executionOutput: org.andrew.jpad.core.runner.ExecutionOutput
            val runTime = measureTimeMillis {
                executionOutput = runner.run(compilationResult.compiledClasses, parsed.fullClassName)
            }
            durationMs += runTime

            return ExecutionResponse(
                isSuccess = executionOutput.exitCode == 0,
                output = executionOutput.stdout,
                error = executionOutput.stderr,
                diagnostics = compilationResult.diagnostics,
                durationMs = durationMs,
                dumps = executionOutput.dumps
            )
        }

        // For statements, expressions, top-level functions, records, sealed types: evaluate via JShell
        val executionOutput: org.andrew.jpad.core.runner.ExecutionOutput
        val runTime = measureTimeMillis {
            executionOutput = snippetRunner.execute(userCode)
        }
        durationMs += runTime

        val formattedErrors = if (executionOutput.diagnostics.isNotEmpty()) {
            "Compilation failed:\n" + executionOutput.diagnostics.joinToString("\n") { diag ->
                "Line ${diag.line}, Column ${diag.column}: ${diag.message}"
            }
        } else {
            executionOutput.stderr
        }

        return ExecutionResponse(
            isSuccess = executionOutput.exitCode == 0,
            output = executionOutput.stdout,
            error = if (executionOutput.exitCode != 0) formattedErrors else "",
            diagnostics = executionOutput.diagnostics,
            durationMs = durationMs,
            dumps = executionOutput.dumps
        )
    }
}
