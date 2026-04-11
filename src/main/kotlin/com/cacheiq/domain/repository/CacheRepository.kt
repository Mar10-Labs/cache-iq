package com.cacheiq.domain.repository

import com.cacheiq.domain.model.EmbeddingVector
import com.cacheiq.domain.model.TenantId

interface CacheRepository {
    fun save(entry: com.cacheiq.domain.model.CacheEntry)
    fun findSimilar(
        embedding: EmbeddingVector,
        embeddingModelVersion: String,
        llmModelVersion: String,
        llmProvider: String,
        tenantId: TenantId,
        threshold: Double
    ): List<com.cacheiq.domain.model.CacheEntry>
}