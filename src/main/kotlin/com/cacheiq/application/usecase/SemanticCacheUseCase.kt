package com.cacheiq.application.usecase

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.CacheResponse
import com.cacheiq.domain.model.CacheEntry
import com.cacheiq.domain.model.TenantId
import com.cacheiq.domain.model.EmbeddingVector
import com.cacheiq.domain.model.ChatResponse
import com.cacheiq.domain.model.Usage
import com.cacheiq.domain.port.input.CacheInputPort
import com.cacheiq.domain.port.output.EmbeddingPort
import com.cacheiq.domain.port.output.LlmClientPort
import com.cacheiq.domain.port.output.PiiDetectorPort
import com.cacheiq.domain.port.output.MetricsPort
import com.cacheiq.domain.port.output.TokenCounterPort
import com.cacheiq.domain.repository.CacheRepository
import com.cacheiq.infrastructure.config.CacheIqConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SemanticCacheUseCase(
    private val embeddingAdapter: EmbeddingPort,
    private val llmClient: LlmClientPort,
    private val piiDetector: PiiDetectorPort,
    private val cacheRepository: CacheRepository,
    private val metrics: MetricsPort,
    private val config: CacheIqConfig,
    private val tokenCounter: TokenCounterPort
) : CacheInputPort {
    
    private val logger = LoggerFactory.getLogger(SemanticCacheUseCase::class.java)

    override suspend fun handle(request: ChatRequest, tenantId: String, provider: String?): CacheResponse {
        return try {
            handleInternal(request, tenantId, provider)
        } catch (e: Exception) {
            logger.error("Unexpected error in handle: ${e.message}", e)
            CacheResponse(
                hit = false,
                response = "Error: ${e.message}",
                llmModel = request.model,
                llmProvider = provider ?: "groq",
                embeddingModel = "error"
            )
        }
    }
    
    private suspend fun handleInternal(request: ChatRequest, tenantId: String, provider: String?): CacheResponse {
        val effectiveProvider = provider ?: "groq"
        val startTime = System.currentTimeMillis()
        
        val prompt = validatePrompt(request) ?: return CacheResponse(
            hit = false,
            response = "No message provided",
            llmModel = request.model,
            llmProvider = effectiveProvider,
            embeddingModel = "none"
        )
        
        val promptTokens = tokenCounter.countTokens(prompt)
        
        val embedding = generateEmbedding(prompt, request.model, effectiveProvider) ?: return CacheResponse(
            hit = false,
            response = "Failed to generate embedding",
            llmModel = request.model,
            llmProvider = effectiveProvider,
            embeddingModel = "error"
        )
        
        val cachedEntry = searchCache(embedding, request.model, effectiveProvider, tenantId)
        if (cachedEntry != null) {
            recordHitMetrics(tenantId, effectiveProvider, startTime, promptTokens.toLong())
            return CacheResponse(
                hit = true,
                response = cachedEntry.response,
                llmModel = cachedEntry.llmModel,
                llmProvider = cachedEntry.llmProvider,
                embeddingModel = cachedEntry.embeddingModel,
                similarity = embedding.cosineSimilarity(cachedEntry.embedding),
                usage = Usage(0, 0, promptTokens)
            )
        }
        
        val llmResponse = callLlm(request, effectiveProvider) ?: return CacheResponse(
            hit = false,
            response = "LLM call failed",
            llmModel = request.model,
            llmProvider = effectiveProvider,
            embeddingModel = config.getEmbeddingModel()
        )
        
        saveToCacheIfNeeded(prompt, embedding, llmResponse, effectiveProvider, tenantId)
        recordMissMetrics(tenantId, effectiveProvider, startTime, llmResponse.usage.totalTokens.toLong())
        
        return CacheResponse(
            hit = false,
            response = llmResponse.content,
            llmModel = llmResponse.model,
            llmProvider = effectiveProvider,
            embeddingModel = config.getEmbeddingModel(),
            usage = llmResponse.usage
        )
    }
    
    private fun validatePrompt(request: ChatRequest): String? {
        val lastMessage = request.messages.lastOrNull()
        if (lastMessage == null || lastMessage.content.isBlank()) {
            logger.warn("No messages or empty content in request")
            return null
        }
        val prompt = lastMessage.content
        logger.info("Processing prompt: ${prompt.take(50)}...")
        return prompt
    }
    
    private suspend fun generateEmbedding(prompt: String, llmModel: String, provider: String): EmbeddingVector? {
        return try {
            val embedding = embeddingAdapter.embed(prompt)
            if (embedding.values.isEmpty()) {
                logger.error("Embedding is empty")
                return null
            }
            embedding
        } catch (e: Exception) {
            logger.error("Failed to generate embedding: ${e.message}", e)
            null
        }
    }
    
    private suspend fun searchCache(embedding: EmbeddingVector, llmModel: String, provider: String, tenantId: String): CacheEntry? {
        return try {
            val entries = cacheRepository.findSimilar(
                embedding = embedding,
                embeddingModelVersion = config.getEmbeddingModel(),
                llmModelVersion = llmModel,
                llmProvider = provider,
                tenantId = TenantId(tenantId),
                threshold = 1.0 - config.getSimilarityThreshold()
            )
            entries.firstOrNull()
        } catch (e: Exception) {
            logger.error("Failed to search cache: ${e.message}", e)
            null
        }
    }
    
    private suspend fun callLlm(request: ChatRequest, provider: String): ChatResponse? {
        return try {
            llmClient.complete(request)
        } catch (e: Exception) {
            logger.error("LLM call failed: ${e.message}", e)
            null
        }
    }
    
    private suspend fun saveToCacheIfNeeded(prompt: String, embedding: EmbeddingVector, llmResponse: ChatResponse, provider: String, tenantId: String) {
        val hasPii = try {
            piiDetector.containsPii(prompt)
        } catch (e: Exception) {
            logger.warn("PII detection failed: ${e.message}")
            false
        }
        
        if (hasPii) {
            logger.info("Prompt contains PII - not caching")
            return
        }
        
        try {
            val entry = CacheEntry(
                prompt = prompt,
                response = llmResponse.content,
                embedding = embedding,
                llmModel = llmResponse.model,
                llmProvider = provider,
                tenantId = TenantId(tenantId),
                totalTokens = llmResponse.usage.totalTokens
            )
            cacheRepository.save(entry)
            logger.info("Saved to cache")
        } catch (e: Exception) {
            logger.error("Failed to save to cache: ${e.message}", e)
        }
    }
    
    private fun recordHitMetrics(tenantId: String, provider: String, startTime: Long, tokens: Long) {
        val latency = System.currentTimeMillis() - startTime
        metrics.recordHit(tenantId, provider)
        metrics.recordLatency(tenantId, provider, latency.toDouble())
        metrics.recordTokensFromCache(tenantId, provider, tokens)
        metrics.recordTokensCalculated(tenantId, provider, tokens)
        logger.info("Cache HIT - latency: ${latency}ms, tokens: $tokens")
    }
    
    private fun recordMissMetrics(tenantId: String, provider: String, startTime: Long, tokens: Long) {
        val latency = System.currentTimeMillis() - startTime
        metrics.recordMiss(tenantId, provider)
        metrics.recordLatency(tenantId, provider, latency.toDouble())
        metrics.recordTokensSaved(tenantId, provider, tokens)
        metrics.recordTokensGroqCalls(tenantId, provider, tokens)
    }
}
