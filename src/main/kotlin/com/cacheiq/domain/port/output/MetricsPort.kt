package com.cacheiq.domain.port.output

interface MetricsPort {
    fun recordHit(tenant: String, provider: String)
    fun recordMiss(tenant: String, provider: String)
    fun recordLatency(tenant: String, provider: String, latencyMs: Double)
    fun recordTokensSaved(tenant: String, provider: String, tokens: Long)
    fun recordTokensFromCache(tenant: String, provider: String, tokens: Long)
}