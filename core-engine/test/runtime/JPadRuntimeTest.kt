package org.andrew.jpad.runtime

import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.dump.ObjectDumper
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JPadRuntimeTest {

    @BeforeTest
    @AfterTest
    fun setupAndTearDown() {
        JPad.clearCache()
        JPad.setDumpConsumer(null)
    }

    @Test
    fun `cache computes value once and reuses cached instance`() {
        val counter = AtomicInteger(0)

        val val1 = JPad.cache("expensive_task") {
            counter.incrementAndGet()
            "Computed Result"
        }

        assertEquals("Computed Result", val1)
        assertEquals(1, counter.get())
        assertTrue(JPad.isCached("expensive_task"))
        assertEquals(1, JPad.cacheSize())

        // Second call should return cached value without calling supplier
        val val2 = JPad.cache("expensive_task") {
            counter.incrementAndGet()
            "Should not be returned"
        }

        assertEquals("Computed Result", val2)
        assertEquals(1, counter.get(), "Supplier should not have been called again")
    }

    @Test
    fun `cache with auto-key works transparently`() {
        val counter = AtomicInteger(0)

        fun fetch() = JPad.cache<Int> {
            counter.incrementAndGet()
        }

        val res1 = fetch()
        val res2 = fetch()

        assertEquals(1, res1)
        assertEquals(1, res2)
        assertEquals(1, counter.get())
        assertEquals(1, JPad.cacheSize())
    }

    @Test
    fun `clearCache removes specific key or all entries`() {
        JPad.cache("k1") { "v1" }
        JPad.cache("k2") { "v2" }
        assertEquals(2, JPad.cacheSize())

        assertTrue(JPad.clearCache("k1"))
        assertFalse(JPad.isCached("k1"))
        assertTrue(JPad.isCached("k2"))
        assertEquals(1, JPad.cacheSize())

        JPad.clearCache()
        assertEquals(0, JPad.cacheSize())
        assertFalse(JPad.isCached("k2"))
    }

    @Test
    fun `benchmark executes iterations and calculates statistics`() {
        val executedRuns = AtomicInteger(0)
        val dumps = mutableListOf<Any?>()

        JPad.setDumpConsumer { obj, _ ->
            dumps.add(obj)
        }

        val result = JPad.benchmark("Math sqrt test", 20) {
            executedRuns.incrementAndGet()
            Math.sqrt(42.0 * executedRuns.get())
        }

        assertNotNull(result)
        assertEquals("Math sqrt test", result.name)
        assertEquals(20, result.iterations)
        assertTrue(result.warmupIterations >= 2)
        assertTrue(executedRuns.get() >= 22) // warmup + iterations
        assertTrue(result.avgTimeMs >= 0.0)
        assertTrue(result.minTimeMs >= 0.0)
        assertTrue(result.maxTimeMs >= result.minTimeMs)
        assertTrue(result.medianTimeMs >= result.minTimeMs)
        assertTrue(result.p95TimeMs >= result.minTimeMs)
        assertTrue(result.throughputOpsPerSec > 0.0)

        // Verifies it triggered dump
        assertEquals(1, dumps.size)
        assertTrue(dumps.first() is JPad.BenchmarkResult)
    }

    @Test
    fun `benchmark supplier overload returns computed value`() {
        val computed = JPad.benchmark<String>("String concat", 10, java.util.function.Supplier {
            "hello" + " world"
        })
        assertEquals("hello world", computed)
    }
}
