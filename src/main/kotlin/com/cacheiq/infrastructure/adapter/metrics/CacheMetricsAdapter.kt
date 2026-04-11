package com.cacheiq.infrastructure.adapter.metrics

import com.cacheiq.domain.port.output.MetricsPort
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
open class CacheMetricsAdapter(
    private val meterRegistry: MeterRegistry
) : MetricsPort {
    
    override fun recordHit(tenant: String, provider: String) {
        Counter.builder("cacheiq_cache_hit_total")
            .tag("tenant", tenant)
            .tag("provider", provider)
            .register(meterRegistry)
            .increment()
    }
    
    override fun recordMiss(tenant: String, provider: String) {
        Counter.builder("cacheiq_cache_miss_total")
            .tag("tenant", tenant)
            .tag("provider", provider)
            .register(meterRegistry)
            .increment()
    }
    
    override fun recordLatency(tenant: String, provider: String, latencyMs: Double) {
        Timer.builder("cacheiq_cache_lookup_ms")
            .tag("tenant", tenant)
            .tag("provider", provider)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(latencyMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
    }
    
    override fun recordTokensSaved(tenant: String, provider: String, tokens: Long) {
        Counter.builder("cacheiq_tokens_saved_total")
            .tag("tenant", tenant)
            .tag("provider", provider)
            .register(meterRegistry)
            .increment(tokens.toDouble())
    }
}