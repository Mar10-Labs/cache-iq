package com.cacheiq.infrastructure.adapter.metrics

import com.cacheiq.domain.port.output.MetricsPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
open class CacheMetricsAdapter(
    private val meterRegistry: MeterRegistry
) : MetricsPort {
    
    override fun recordHit(tenant: String, provider: String) {
        meterRegistry.counter("cacheiq_cache_hit_total", "tenant", tenant, "provider", provider)
            .increment()
    }
    
    override fun recordMiss(tenant: String, provider: String) {
        meterRegistry.counter("cacheiq_cache_miss_total", "tenant", tenant, "provider", provider)
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
        meterRegistry.counter("cacheiq_tokens_saved_total", "tenant", tenant, "provider", provider)
            .increment(tokens.toDouble())
    }
    
    override fun recordTokensFromCache(tenant: String, provider: String, tokens: Long) {
        meterRegistry.counter("cacheiq_tokens_from_cache_total", "tenant", tenant, "provider", provider)
            .increment(tokens.toDouble())
    }
    
    override fun recordTokensGroqCalls(tenant: String, provider: String, tokens: Long) {
        meterRegistry.counter("cacheiq_tokens_groq_calls_total", "tenant", tenant, "provider", provider)
            .increment(tokens.toDouble())
    }
    
    override fun recordTokensCalculated(tenant: String, provider: String, tokens: Long) {
        meterRegistry.counter("cacheiq_tokens_calculated_total", "tenant", tenant, "provider", provider)
            .increment(tokens.toDouble())
    }
    
    override fun recordPiiBlocked(tenant: String, reason: String) {
        meterRegistry.counter("cacheiq_pii_blocked_total", "tenant", tenant, "reason", reason)
            .increment()
    }
    
    override fun recordPiiRiskLevel(tenant: String, level: String) {
        meterRegistry.counter("cacheiq_pii_risk_level_total", "tenant", tenant, "level", level)
            .increment()
    }
    
    override fun recordPresidioLatency(tenant: String, latencyMs: Double) {
        Timer.builder("cacheiq_presidio_latency_ms")
            .tag("tenant", tenant)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(latencyMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
    }
}