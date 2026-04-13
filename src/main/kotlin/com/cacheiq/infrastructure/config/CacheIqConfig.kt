package com.cacheiq.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CacheIqConfig(
    @Value("\${llm.provider.groq.model:llama-3.3-70b-versatile}") val defaultLlmModel: String,
    @Value("\${llm.provider.groq.provider:groq}") val defaultLlmProvider: String,
    @Value("\${embedding.model.name:all-MiniLM-L6-v2}") val defaultEmbeddingModel: String,
    @Value("\${embedding.model.dimensions:384}") val embeddingDimensions: Int,
    @Value("\${cache.similarity-threshold:0.8}") val similarityThresholdValue: Double
) {
    fun getLlmModel(model: String?): String = model ?: defaultLlmModel
    fun getLlmProvider(): String = defaultLlmProvider
    fun getEmbeddingModel(): String = defaultEmbeddingModel
    fun getSimilarityThreshold(): Double = similarityThresholdValue
}