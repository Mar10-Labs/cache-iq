package com.cacheiq.infrastructure.adapter.persistence

import com.cacheiq.domain.model.CacheEntry
import com.cacheiq.domain.model.EmbeddingVector
import com.cacheiq.domain.model.TenantId
import com.cacheiq.domain.repository.CacheRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class PgVectorCacheAdapter(
    private val jdbcTemplate: JdbcTemplate
) : CacheRepository {
    
    private val logger = LoggerFactory.getLogger(PgVectorCacheAdapter::class.java)
    
    init {
        // Enable pgvector extension
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector")
        
        // Create table if not exists (single tenant for V1)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS cache_entries (
                id VARCHAR(255) PRIMARY KEY,
                prompt TEXT NOT NULL,
                response TEXT NOT NULL,
                embedding vector(384),
                embedding_model VARCHAR(64) NOT NULL,
                llm_model VARCHAR(64) NOT NULL,
                llm_provider VARCHAR(32) NOT NULL,
                tenant_id VARCHAR(100) NOT NULL,
                pii_risk_level VARCHAR(16) NOT NULL DEFAULT 'STRUCTURED',
                created_at TIMESTAMP DEFAULT NOW()
            )
        """.trimIndent())
        
        // Create index if not exists
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_cache_embedding 
            ON cache_entries USING hnsw (embedding vector_cosine_ops)
            WITH (m = 16, ef_construction = 64)
        """.trimIndent())
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_cache_version 
            ON cache_entries (embedding_model, llm_model, llm_provider)
        """.trimIndent())
    }
    
    override fun save(entry: CacheEntry) {
        jdbcTemplate.update(
            """
            INSERT INTO cache_entries (id, prompt, response, embedding, embedding_model, llm_model, llm_provider, tenant_id, pii_risk_level)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                prompt = EXCLUDED.prompt,
                response = EXCLUDED.response,
                embedding = EXCLUDED.embedding
            """.trimIndent(),
            entry.id,
            entry.prompt,
            entry.response,
            entry.embedding.values,
            entry.embeddingModel,
            entry.llmModel,
            entry.llmProvider,
            entry.tenantId.value,
            entry.piiRiskLevel.name
        )
    }
    
    override fun findSimilar(
        embedding: EmbeddingVector,
        embeddingModelVersion: String,
        llmModelVersion: String,
        llmProvider: String,
        tenantId: TenantId,
        threshold: Double
    ): List<CacheEntry> {
        logger.info("Searching cache for tenant=${tenantId.value}, embeddingModel=$embeddingModelVersion, llmModel=$llmModelVersion, provider=$llmProvider, threshold=$threshold")
        
        val sql = """
            SELECT id, prompt, response, embedding, embedding_model, llm_model, llm_provider, tenant_id, pii_risk_level
            FROM cache_entries
            WHERE embedding <=> ?::vector < ?
              AND embedding_model = ?
              AND llm_model = ?
              AND llm_provider = ?
              AND tenant_id = ?
            ORDER BY embedding <=> ?::vector
            LIMIT 3
        """.trimIndent()
        
        val rowMapper = RowMapper { rs: java.sql.ResultSet, _ ->
            val embeddingStr = rs.getString("embedding")
            val floatArray = parsePgVectorArray(embeddingStr)
            
            CacheEntry(
                id = rs.getString("id"),
                prompt = rs.getString("prompt"),
                response = rs.getString("response"),
                embedding = EmbeddingVector(floatArray),
                embeddingModel = rs.getString("embedding_model"),
                llmModel = rs.getString("llm_model"),
                llmProvider = rs.getString("llm_provider"),
                tenantId = TenantId(rs.getString("tenant_id")),
                piiRiskLevel = com.cacheiq.domain.model.PiiRiskLevel.valueOf(rs.getString("pii_risk_level"))
            )
        }
        
        return jdbcTemplate.query(sql, rowMapper, 
            embeddingToPgVectorString(embedding.values),
            threshold,
            embeddingModelVersion,
            llmModelVersion,
            llmProvider,
            tenantId.value,
            embeddingToPgVectorString(embedding.values)
        )
    }
    
    private fun embeddingToPgVectorString(values: FloatArray): String {
        return values.joinToString(",", "[", "]")
    }
    
    private fun parsePgVectorArray(arrayStr: String): FloatArray {
        val trimmed = arrayStr.trim()
        if (trimmed.isEmpty() || trimmed == "[]") {
            return FloatArray(384)
        }
        val values = trimmed
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().toFloat() }
        return values.toFloatArray()
    }
}