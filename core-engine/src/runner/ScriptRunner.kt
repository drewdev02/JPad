package org.andrew.jpad.core.runner

import org.andrew.jpad.core.compiler.CompilationDiagnostic
import org.andrew.jpad.core.dump.DumpValue

data class ExecutionOutput(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val throwable: Throwable? = null,
    val dumps: List<DumpValue> = emptyList(),
    val diagnostics: List<CompilationDiagnostic> = emptyList()
)

interface ScriptRunner {
    fun run(
        compiledClasses: Map<String, ByteArray>,
        mainClassName: String,
        args: Array<String> = emptyArray()
    ): ExecutionOutput
}
