package com.cacheiq.infrastructure.adapter.embedding

import com.cacheiq.domain.model.EmbeddingVector
import com.cacheiq.domain.port.output.EmbeddingPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OnnxEmbeddingAdapter(
    @Value("\${embedding.model.dimensions:384}") private val dimensions: Int
) : EmbeddingPort {
    
    private val logger = LoggerFactory.getLogger(OnnxEmbeddingAdapter::class.java)
    
    override suspend fun embed(text: String): EmbeddingVector {
        logger.info("Generating placeholder embedding for text: ${text.take(30)}...")
        
        // V1: Generate deterministic pseudo-random embeddings based on text hash
        // In production: load ONNX model, tokenize, run inference, apply mean pooling + L2 normalize
        val textHash = text.hashCode()
        val random = kotlin.random.Random(textHash.toLong())
        val values = FloatArray(dimensions) { random.nextFloat() }
        
        // L2 normalize
        val norm = kotlin.math.sqrt(values.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in values.indices) {
                values[i] = values[i] / norm
            }
        }
        
        logger.debug("Generated embedding with ${values.size} dimensions, norm=$norm")
        return EmbeddingVector(values)
    }
}