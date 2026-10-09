package org.andrew.jpad.core.worker

import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.core.runner.ExecutionOutput
import org.andrew.jpad.core.runner.JShellRunner
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Entry point of the isolated worker JVM. Executes user code on behalf of the UI process
 * and exits as soon as the UI closes the pipe (or dies).
 */
fun main() {
    // stdout is the IPC channel; anything user code prints is captured by the runners,
    // and any stray output is sent to stderr so it can never corrupt the protocol.
    val channel = System.out
    System.setOut(System.err)

    val output = ObjectOutputStream(channel).also { it.flush() }
    val input = ObjectInputStream(System.`in`)

    val dumper = ObjectDumper()
    val programRunner = LocalInMemoryRunner(dumper)
    val snippetRunner = JShellRunner(dumper)

    while (true) {
        val request = try {
            input.readObject() as WorkerRequest
        } catch (_: EOFException) {
            return
        }

        val result = when (request) {
            is WorkerRequest.RunProgram ->
                programRunner.run(request.compiledClasses, request.mainClassName, request.args.toTypedArray())
            is WorkerRequest.RunSnippet -> snippetRunner.execute(request.code)
        }

        output.writeObject(result.toResponse())
        output.flush()
        output.reset()
    }
}

private fun ExecutionOutput.toResponse() =
    WorkerResponse(stdout, stderr, exitCode, dumps.map { it.wireSafe() }, diagnostics)

/**
 * Raw values may be instances of user-defined classes (e.g. an enum declared in the snippet),
 * which the UI process cannot deserialize. Replace anything but JDK scalars with its text form.
 */
private fun DumpValue.wireSafe(): DumpValue = when (this) {
    is DumpValue.Primitive -> when (rawValue) {
        null, is String, is Number, is Boolean, is Char -> this
        else -> copy(rawValue = rawValue.toString())
    }
    is DumpValue.KeyValueList -> copy(entries = entries.map { (k, v) -> k to v.wireSafe() })
    is DumpValue.Table -> copy(rows = rows.map { row -> row.mapValues { it.value.wireSafe() } })
    is DumpValue.Text -> this
}
