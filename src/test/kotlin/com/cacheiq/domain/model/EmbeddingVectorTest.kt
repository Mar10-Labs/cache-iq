package com.cacheiq.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EmbeddingVectorTest {

    @Test
    fun `should calculate cosine similarity correctly`() {
        val v1 = EmbeddingVector(floatArrayOf(1f, 0f))
        val v2 = EmbeddingVector(floatArrayOf(1f, 0f))
        
        assertEquals(1f, v1.cosineSimilarity(v2), 0.001f)
    }

    @Test
    fun `should return zero for orthogonal vectors`() {
        val v1 = EmbeddingVector(floatArrayOf(1f, 0f))
        val v2 = EmbeddingVector(floatArrayOf(0f, 1f))
        
        assertEquals(0f, v1.cosineSimilarity(v2), 0.001f)
    }

    @Test
    fun `should return negative for opposite vectors`() {
        val v1 = EmbeddingVector(floatArrayOf(1f, 0f))
        val v2 = EmbeddingVector(floatArrayOf(-1f, 0f))
        
        assertEquals(-1f, v1.cosineSimilarity(v2), 0.001f)
    }

    @Test
    fun `should calculate norm correctly`() {
        val v = EmbeddingVector(floatArrayOf(3f, 4f))
        
        assertEquals(5f, v.norm(), 0.001f)
    }

    @Test
    fun `should throw when creating empty embedding`() {
        assertThrows(IllegalArgumentException::class.java) {
            EmbeddingVector(floatArrayOf())
        }
    }

    @Test
    fun `should create with valid values`() {
        val values = floatArrayOf(0.1f, 0.2f, 0.3f)
        val embedding = EmbeddingVector(values)
        
        assertEquals(3, embedding.values.size)
    }
}