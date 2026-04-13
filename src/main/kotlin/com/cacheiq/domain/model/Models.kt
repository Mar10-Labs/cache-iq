package com.cacheiq.domain.model

import java.time.Instant
import java.util.UUID

data class CacheEntry(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val response: String,
    val embedding: EmbeddingVector,
    val embeddingModel: String = "all-MiniLM-L6-v2",
    val llmModel: String = "llama-3.3-70b-versatile",
    val llmProvider: String = "groq",
    val tenantId: TenantId,
    val piiRiskLevel: PiiRiskLevel = PiiRiskLevel.STRUCTURED,
    val totalTokens: Int = 0,
    val createdAt: Instant = Instant.now()
)

@JvmInline
value class EmbeddingVector(val values: FloatArray) {
    init { require(values.isNotEmpty()) { "Embedding cannot be empty" } }
    
    fun dot(other: EmbeddingVector): Float = 
        values.zip(other.values).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
    
    fun norm(): Float = 
        kotlin.math.sqrt(values.sumOf { (it * it).toDouble() }).toFloat()
    
    fun cosineSimilarity(other: EmbeddingVector): Float {
        val dotProduct = dot(other)
        val norms = norm() * other.norm()
        return if (norms > 0) dotProduct / norms else 0f
    }
    
    fun toBytes(): FloatArray = values
    
    override fun toString(): String = "EmbeddingVector(size=${values.size})"
}

@JvmInline
value class TenantId(val value: String) {
    init { require(value.isNotBlank()) { "TenantId cannot be blank" } }
}

enum class PiiRiskLevel { NONE, STRUCTURED, CONTEXTUAL }

enum class RouteType { TECHNICAL, TRANSACTIONAL, SUPPORT }

data class CacheResponse(
    val hit: Boolean,
    val response: String,
    val llmModel: String,
    val llmProvider: String,
    val embeddingModel: String,
    val similarity: Float? = null,
    val usage: Usage? = null
)

data class ChatRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<ChatMessage> = emptyList(),
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String,
    val content: String,
    val model: String,
    val finishReason: String,
    val usage: Usage
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)