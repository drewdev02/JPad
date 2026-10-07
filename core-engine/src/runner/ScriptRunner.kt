package org.andrew.jpad.core.runner

data class ExecutionOutput(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val throwable: Throwable? = null
)

interface ScriptRunner {
    fun run(
        compiledClasses: Map<String, ByteArray>,
        mainClassName: String,
        args: Array<String> = emptyArray()
    ): ExecutionOutput
}
