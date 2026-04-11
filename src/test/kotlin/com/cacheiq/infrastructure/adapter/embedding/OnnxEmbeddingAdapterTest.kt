package com.cacheiq.infrastructure.adapter.embedding

import com.cacheiq.domain.model.EmbeddingVector
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import java.io.File

@Disabled("Requires ONNX model - run with Docker for integration test")
class OnnxEmbeddingAdapterTest {

    private lateinit var adapter: OnnxEmbeddingAdapter

    @BeforeEach
    fun setup() {
        adapter = OnnxEmbeddingAdapter(
            modelResource = org.springframework.core.io.ClassPathResource("models/model.onnx"),
            tokenizerResource = org.springframework.core.io.ClassPathResource("models/tokenizer.json"),
            tokenizerConfigResource = org.springframework.core.io.ClassPathResource("models/tokenizer_config.json"),
            dimensions = 384,
            maxLength = 256
        )
    }

    @Test
    fun `should generate embedding with correct dimensions`() = runTest {
        val embedding = adapter.embed("Hello world")
        
        assertEquals(384, embedding.values.size)
    }

    @Test
    fun `should return deterministic embeddings for same text`() = runTest {
        val embedding1 = adapter.embed("Hello world")
        val embedding2 = adapter.embed("Hello world")
        
        assertEquals(1f, embedding1.cosineSimilarity(embedding2), 0.001f)
    }

    @Test
    fun `should return high similarity for similar texts`() = runTest {
        val embedding1 = adapter.embed("How do I reset my password?")
        val embedding2 = adapter.embed("How to recover my password?")
        
        val similarity = embedding1.cosineSimilarity(embedding2)
        
        assertTrue(similarity > 0.7f, "Similar texts should have similarity > 0.7, got: $similarity")
    }

    @Test
    fun `should return low similarity for different texts`() = runTest {
        val embedding1 = adapter.embed("Hello world")
        val embedding2 = adapter.embed("What's the weather today?")
        
        val similarity = embedding1.cosineSimilarity(embedding2)
        
        assertTrue(similarity < 0.5f, "Different texts should have similarity < 0.5, got: $similarity")
    }

    @Test
    fun `should generate normalized embeddings`() = runTest {
        val embedding = adapter.embed("Test text")
        
        val norm = embedding.norm()
        assertEquals(1f, norm, 0.001f)
    }

    @Test
    fun `should handle empty text`() = runTest {
        val embedding = adapter.embed("")
        
        assertEquals(384, embedding.values.size)
    }

    @Test
    fun `should handle long text`() = runTest {
        val longText = "word ".repeat(300)
        val embedding = adapter.embed(longText)
        
        assertEquals(384, embedding.values.size)
    }
}

class TokenizerTest {

    private lateinit var tokenizer: Tokenizer

    @BeforeEach
    fun setup() {
        val modelDir = File("src/main/resources/models")
        if (modelDir.exists()) {
            val tokenizerJson = com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(File(modelDir, "tokenizer.json"))
            val tokenizerConfig = com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(File(modelDir, "tokenizer_config.json"))
            tokenizer = Tokenizer(tokenizerJson, tokenizerConfig)
        }
    }

    @Test
    fun `should tokenize simple text`() {
        val result = tokenizer.tokenize("hello world", 256)
        
        assertEquals(256, result.first.size)
        assertEquals(256, result.second.size)
    }

    @Test
    fun `should add special tokens`() {
        val result = tokenizer.tokenize("test", 256)
        
        // Should have [CLS] at start and [SEP] at end (before padding)
        assertTrue(result.first.any { it == 101 }, "Should have [CLS] token")
        assertTrue(result.first.any { it == 102 }, "Should have [SEP] token")
    }

    @Test
    fun `should handle unknown tokens`() {
        // Should not throw
        val result = tokenizer.tokenize("xyz123 unknown", 256)
        
        assertEquals(256, result.first.size)
    }
}