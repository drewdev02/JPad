package org.andrew.jpad.core.worker

import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Owns the worker JVM: starts it lazily, reuses it between runs, and replaces it
 * when it crashes or is cancelled.
 */
class WorkerSupervisor {

    private class Session(
        val process: Process,
        val output: ObjectOutputStream,
        val input: ObjectInputStream
    )

    private val lock = Any()
    private var session: Session? = null

    @Volatile
    private var cancelRequested = false

    init {
        Runtime.getRuntime().addShutdownHook(Thread { session?.process?.destroyForcibly() })
    }

    fun execute(request: WorkerRequest): WorkerResponse = synchronized(lock) {
        cancelRequested = false
        val current = try {
            session ?: start().also { session = it }
        } catch (e: IOException) {
            return WorkerResponse("", "Could not start worker process: ${e.message}", 1)
        }

        try {
            current.output.writeObject(request)
            current.output.flush()
            current.output.reset()
            current.input.readObject() as WorkerResponse
        } catch (e: Exception) {
            val wasCancelled = cancelRequested
            stop()
            if (wasCancelled) {
                WorkerResponse("", "Execution cancelled", CANCELLED_EXIT_CODE)
            } else {
                WorkerResponse("", "Worker process terminated unexpectedly (${e.javaClass.simpleName})", 1)
            }
        }
    }

    /** Kills the worker, unblocking any [execute] call in flight. Safe to call from any thread. */
    fun cancel() {
        cancelRequested = true
        session?.process?.destroyForcibly()
    }

    fun stop() {
        session?.process?.destroyForcibly()
        session = null
    }

    private fun start(): Session {
        val javaBin = File(System.getProperty("java.home"), "bin/java").path
        val process = ProcessBuilder(
            javaBin,
            "-cp", System.getProperty("java.class.path"),
            "org.andrew.jpad.core.worker.WorkerMainKt"
        )
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        // Header handshake order matters: we write ours first, the worker answers with its own.
        val output = ObjectOutputStream(process.outputStream).also { it.flush() }
        val input = ObjectInputStream(process.inputStream)
        return Session(process, output, input)
    }

    companion object {
        const val CANCELLED_EXIT_CODE = 130
    }
}
