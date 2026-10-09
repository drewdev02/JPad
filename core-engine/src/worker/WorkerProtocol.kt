package org.andrew.jpad.core.worker

import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.dump.DumpValue
import java.io.Serializable

/**
 * Messages exchanged between the UI process and the isolated worker JVM.
 * They travel over the worker's stdin/stdout as Java-serialized objects.
 */
sealed interface WorkerRequest : Serializable {
    data class RunProgram(
        val compiledClasses: Map<String, ByteArray>,
        val mainClassName: String,
        val args: List<String> = emptyList(),
        val classpath: List<String> = emptyList()
    ) : WorkerRequest

    data class RunSnippet(
        val code: String,
        val classpath: List<String> = emptyList()
    ) : WorkerRequest
}

data class WorkerResponse(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val dumps: List<DumpValue> = emptyList(),
    val diagnostics: List<CompilationDiagnostic> = emptyList()
) : Serializable
