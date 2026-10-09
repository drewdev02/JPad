package org.andrew.jpad.core.worker

import org.andrew.jpad.core.runner.CancellableRunner
import org.andrew.jpad.core.runner.ExecutionOutput
import org.andrew.jpad.core.runner.ScriptRunner
import org.andrew.jpad.core.runner.SnippetRunner

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
        args: Array<String>
    ): ExecutionOutput =
        worker.execute(WorkerRequest.RunProgram(compiledClasses, mainClassName, args.toList())).toOutput()

    override fun cancel() = worker.cancel()
}

class RemoteSnippetRunner(private val worker: WorkerSupervisor) : SnippetRunner, CancellableRunner {
    override fun execute(code: String): ExecutionOutput =
        worker.execute(WorkerRequest.RunSnippet(code)).toOutput()

    override fun cancel() = worker.cancel()
}
