package org.andrew.jpad.core

import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import org.andrew.jpad.core.runner.ScriptRunner
import kotlin.system.measureTimeMillis

data class ExecutionResponse(
    val isSuccess: Boolean,
    val output: String,
    val error: String,
    val diagnostics: List<CompilationDiagnostic> = emptyList(),
    val durationMs: Long = 0
)

class ExecutionEngine(
    private val compiler: JavaCompilerService = InMemoryJavaCompiler(),
    private val wrapper: SnippetWrapper = SnippetWrapper(),
    private val runner: ScriptRunner = LocalInMemoryRunner()
) {

    fun execute(userCode: String): ExecutionResponse {
        val parsed = wrapper.wrap(userCode)
        var durationMs = 0L

        val compilationResult: org.andrew.jpad.core.compiler.CompilationResult
        val compilationTime = measureTimeMillis {
            compilationResult = compiler.compile(listOf(parsed.generatedSource))
        }
        durationMs += compilationTime

        if (!compilationResult.isSuccess) {
            val errorDiagnostics = compilationResult.diagnostics.filter { it.kind == DiagnosticKind.ERROR }
            val formattedErrors = errorDiagnostics.joinToString("\n") { diag ->
                val adjustedLine = if (diag.line > parsed.lineOffset) diag.line - parsed.lineOffset else diag.line
                "Line $adjustedLine, Column ${diag.column}: ${diag.message}"
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
            durationMs = durationMs
        )
    }
}
