package org.andrew.jpad.core.worker

import org.andrew.jpad.core.runner.CancellableRunner
import org.andrew.jpad.core.runner.ExecutionOutput
import org.andrew.jpad.core.runner.ScriptRunner
import org.andrew.jpad.core.runner.SnippetRunner
import java.io.File

private fun WorkerResponse.toOutput() = ExecutionOutput(
    stdout = stdout,
    stderr = stderr,
    exitCode = exitCode,
    dumps = dumps,
    diagnostics = diagnostics
)

class RemoteScriptRunner(private val worker: WorkerSupervisor) : ScriptRunner, CancellableRunner {
    override fun run(
        compiledClasses: Map<String, ByteArray>,
        mainClassName: String,
        args: Array<String>,
        classpath: List<File>
    ): ExecutionOutput =
        worker.execute(
            WorkerRequest.RunProgram(
                compiledClasses = compiledClasses,
                mainClassName = mainClassName,
                args = args.toList(),
                classpath = classpath.map { it.absolutePath }
            )
        ).toOutput()

    override fun cancel() = worker.cancel()
}

class RemoteSnippetRunner(private val worker: WorkerSupervisor) : SnippetRunner, CancellableRunner {
    override fun execute(code: String, classpath: List<File>): ExecutionOutput =
        worker.execute(
            WorkerRequest.RunSnippet(
                code = code,
                classpath = classpath.map { it.absolutePath }
            )
        ).toOutput()

    override fun cancel() = worker.cancel()
}
