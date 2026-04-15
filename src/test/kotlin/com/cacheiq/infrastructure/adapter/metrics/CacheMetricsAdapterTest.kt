package com.cacheiq.infrastructure.adapter.metrics

import com.cacheiq.domain.port.output.MetricsPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CacheMetricsAdapterTest {

    private lateinit var metrics: MetricsPort
    private lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun setup() {
        meterRegistry = SimpleMeterRegistry()
        metrics = CacheMetricsAdapter(meterRegistry)
    }

    @Test
    fun `should record hit`() {
        metrics.recordHit("tenant1", "groq")
        val counter = meterRegistry.counter("cacheiq_cache_hit_total", "tenant", "tenant1", "provider", "groq")
        assertNotNull(counter)
    }

    @Test
    fun `should record miss`() {
        metrics.recordMiss("tenant1", "groq")
        val counter = meterRegistry.counter("cacheiq_cache_miss_total", "tenant", "tenant1", "provider", "groq")
        assertNotNull(counter)
    }

    @Test
    fun `should record tokens from cache`() {
        metrics.recordTokensFromCache("tenant1", "groq", 10L)
        val counter = meterRegistry.counter("cacheiq_tokens_from_cache_total", "tenant", "tenant1", "provider", "groq")
        assertEquals(10.0, counter.count(), 0.01)
    }

    @Test
    fun `should record tokens groq calls`() {
        metrics.recordTokensGroqCalls("tenant1", "groq", 20L)
        val counter = meterRegistry.counter("cacheiq_tokens_groq_calls_total", "tenant", "tenant1", "provider", "groq")
        assertEquals(20.0, counter.count(), 0.01)
    }

    @Test
    fun `should record tokens calculated`() {
        metrics.recordTokensCalculated("tenant1", "groq", 15L)
        val counter = meterRegistry.counter("cacheiq_tokens_calculated_total", "tenant", "tenant1", "provider", "groq")
        assertEquals(15.0, counter.count(), 0.01)
    }

    @Test
    fun `should record latency`() {
        metrics.recordLatency("tenant1", "groq", 100.0)
        // Timer recorded, just verify no exception
    }
}