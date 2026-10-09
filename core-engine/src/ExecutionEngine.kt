package org.andrew.jpad.core

import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.compiler.DiagnosticKind
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.dependencies.DependencyResolver
import org.andrew.jpad.core.dependencies.MavenDependencyResolver
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
    private val snippetRunner: SnippetRunner,
    private val dependencyResolver: DependencyResolver = MavenDependencyResolver()
) {

    /** Stops the code currently running, if any. */
    fun cancel() {
        (runner as? CancellableRunner)?.cancel()
        (snippetRunner as? CancellableRunner)?.cancel()
    }

    fun extractDependencies(sourceCode: String): List<org.andrew.jpad.core.dependencies.MavenCoordinate> =
        dependencyResolver.extractDependencies(sourceCode)

    fun resolveDependencies(
        coordinates: List<org.andrew.jpad.core.dependencies.MavenCoordinate>,
        onProgress: ((description: String, progress: Float?) -> Unit)? = null
    ): org.andrew.jpad.core.dependencies.ResolutionResult =
        dependencyResolver.resolve(coordinates, onProgress = onProgress)

    fun execute(
        userCode: String,
        onProgress: ((taskDescription: String, progress: Float?) -> Unit)? = null
    ): ExecutionResponse {
        var durationMs = 0L

        val coordinates = dependencyResolver.extractDependencies(userCode)
        val classpathJars = if (coordinates.isNotEmpty()) {
            val resolutionResult: org.andrew.jpad.core.dependencies.ResolutionResult
            val resTime = measureTimeMillis {
                resolutionResult = dependencyResolver.resolve(coordinates, onProgress = onProgress)
            }
            durationMs += resTime
            if (!resolutionResult.isSuccess) {
                return ExecutionResponse(
                    isSuccess = false,
                    output = "",
                    error = "Dependency resolution failed:\n${resolutionResult.errorMessage ?: "Unknown error"}",
                    durationMs = durationMs
                )
            }
            resolutionResult.classpathJars
        } else {
            emptyList()
        }

        val parsed = wrapper.wrap(userCode)

        // If it's a full Java class with main method, compile directly 1:1 without altering the code
        if (parsed.mode == SnippetMode.PROGRAM) {
            val compilationResult: org.andrew.jpad.core.compiler.CompilationResult
            val compilationTime = measureTimeMillis {
                compilationResult = compiler.compile(listOf(parsed.generatedSource), classpath = classpathJars)
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
                executionOutput = runner.run(
                    compiledClasses = compilationResult.compiledClasses,
                    mainClassName = parsed.fullClassName,
                    classpath = classpathJars
                )
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
            executionOutput = snippetRunner.execute(userCode, classpath = classpathJars)
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
