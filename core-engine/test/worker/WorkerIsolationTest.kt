package org.andrew.jpad.core.worker

import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.di.coreEngineModule
import org.koin.dsl.koinApplication
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkerIsolationTest {

    private val engine = koinApplication { modules(coreEngineModule) }.koin.get<ExecutionEngine>()

    @Test
    fun `System exit in user code does not kill the host and the worker is restarted`() {
        val crashed = engine.execute("System.exit(3);")
        assertFalse(crashed.isSuccess)

        val next = engine.execute("System.out.println(\"still alive\");")
        assertTrue(next.isSuccess, next.error)
        assertTrue(next.output.contains("still alive"))
    }

    @Test
    fun `cancel stops an infinite loop`() {
        var response: org.andrew.jpad.core.ExecutionResponse? = null
        val runner = thread { response = engine.execute("while (true) { }") }

        Thread.sleep(3000)
        engine.cancel()
        runner.join(10_000)

        assertFalse(runner.isAlive, "execute() should return after cancel")
        assertFalse(response!!.isSuccess)
        assertTrue(response!!.error.contains("cancelled"))
    }

    @Test
    fun `worker is reused between runs`() {
        val pid = "System.out.println(ProcessHandle.current().pid());"
        val first = engine.execute(pid).output.trim()
        val second = engine.execute(pid).output.trim()
        assertEquals(first, second)
        assertTrue(first != ProcessHandle.current().pid().toString())
    }
}
