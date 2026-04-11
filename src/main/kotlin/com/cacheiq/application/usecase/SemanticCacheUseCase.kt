package com.cacheiq.application.usecase

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.CacheResponse
import com.cacheiq.domain.model.CacheEntry
import com.cacheiq.domain.model.Constants
import com.cacheiq.domain.model.TenantId
import com.cacheiq.domain.port.input.CacheInputPort
import com.cacheiq.domain.port.output.EmbeddingPort
import com.cacheiq.domain.port.output.LlmClientPort
import com.cacheiq.domain.port.output.PiiDetectorPort
import com.cacheiq.domain.port.output.MetricsPort
import com.cacheiq.domain.repository.CacheRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SemanticCacheUseCase(
    private val embeddingAdapter: EmbeddingPort,
    private val llmClient: LlmClientPort,
    private val piiDetector: PiiDetectorPort,
    private val cacheRepository: CacheRepository,
    private val metrics: MetricsPort
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
        
        val lastMessage = request.messages.lastOrNull()
        if (lastMessage == null || lastMessage.content.isBlank()) {
            logger.warn("No messages or empty content in request")
            return CacheResponse(
                hit = false,
                response = "No message provided",
                llmModel = request.model,
                llmProvider = effectiveProvider,
                embeddingModel = "none"
            )
        }
        
        val prompt = lastMessage.content
        logger.info("Processing prompt: ${prompt.take(50)}...")
        
        val embedding = try {
            embeddingAdapter.embed(prompt)
        } catch (e: Exception) {
            logger.error("Failed to generate embedding: ${e.message}", e)
            return CacheResponse(
                hit = false,
                response = "Failed to generate embedding: ${e.message}",
                llmModel = request.model,
                llmProvider = effectiveProvider,
                embeddingModel = "error"
            )
        }
        
        if (embedding.values.isEmpty()) {
            logger.error("Embedding is empty")
            return CacheResponse(
                hit = false,
                response = "Failed to generate embedding",
                llmModel = request.model,
                llmProvider = effectiveProvider,
                embeddingModel = "error"
            )
        }
        
        val cacheEntries = try {
            cacheRepository.findSimilar(
                embedding = embedding,
                embeddingModelVersion = Constants.EMBEDDING_MODEL,
                llmModelVersion = request.model,
                llmProvider = effectiveProvider,
                tenantId = TenantId(tenantId),
                threshold = 0.8
            )
        } catch (e: Exception) {
            logger.error("Failed to search cache: ${e.message}", e)
            emptyList()
        }
        
        if (cacheEntries.isNotEmpty()) {
            val best = cacheEntries.first()
            val latency = System.currentTimeMillis() - startTime
            
            metrics.recordHit(tenantId, effectiveProvider)
            metrics.recordLatency(tenantId, effectiveProvider, latency.toDouble())
            
            logger.info("Cache HIT - similarity: ${embedding.cosineSimilarity(best.embedding)}")
            
            return CacheResponse(
                hit = true,
                response = best.response,
                llmModel = best.llmModel,
                llmProvider = best.llmProvider,
                embeddingModel = best.embeddingModel,
                similarity = embedding.cosineSimilarity(best.embedding)
            )
        }
        
        logger.info("Cache MISS - calling LLM")
        
        val llmResponse = try {
            llmClient.complete(request)
        } catch (e: Exception) {
            logger.error("LLM call failed: ${e.message}", e)
            return CacheResponse(
                hit = false,
                response = "LLM call failed: ${e.message}",
                llmModel = request.model,
                llmProvider = effectiveProvider,
                embeddingModel = Constants.EMBEDDING_MODEL
            )
        }
        
        val hasPii = try {
            piiDetector.containsPii(llmResponse.content)
        } catch (e: Exception) {
            logger.warn("PII detection failed: ${e.message}")
            false
        }
        
        if (!hasPii) {
            try {
                val entry = CacheEntry(
                    prompt = prompt,
                    response = llmResponse.content,
                    embedding = embedding,
                    llmModel = llmResponse.model,
                    llmProvider = effectiveProvider,
                    tenantId = TenantId(tenantId)
                )
                cacheRepository.save(entry)
                logger.info("Saved to cache")
            } catch (e: Exception) {
                logger.error("Failed to save to cache: ${e.message}", e)
            }
        } else {
            logger.info("Response contains PII - not caching")
        }
        
        val latency = System.currentTimeMillis() - startTime
        metrics.recordMiss(tenantId, effectiveProvider)
        metrics.recordLatency(tenantId, effectiveProvider, latency.toDouble())
        metrics.recordTokensSaved(tenantId, effectiveProvider, llmResponse.usage.totalTokens.toLong())
        
        return CacheResponse(
            hit = false,
            response = llmResponse.content,
            llmModel = llmResponse.model,
            llmProvider = effectiveProvider,
            embeddingModel = Constants.EMBEDDING_MODEL
        )
    }
}