package com.cacheiq.application.usecase

import com.cacheiq.domain.model.*
import com.cacheiq.domain.port.output.EmbeddingPort
import com.cacheiq.domain.port.output.LlmClientPort
import com.cacheiq.domain.port.output.PiiDetectorPort
import com.cacheiq.domain.port.output.MetricsPort
import com.cacheiq.domain.repository.CacheRepository
import com.cacheiq.infrastructure.config.CacheIqConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SemanticCacheUseCaseTest {

    private lateinit var embeddingAdapter: EmbeddingPort
    private lateinit var llmClient: LlmClientPort
    private lateinit var piiDetector: PiiDetectorPort
    private lateinit var cacheRepository: CacheRepository
    private lateinit var metrics: MetricsPort
    private lateinit var useCase: SemanticCacheUseCase

    @BeforeEach
    fun setup() {
        embeddingAdapter = mockk()
        llmClient = mockk()
        piiDetector = mockk()
        cacheRepository = mockk()
        metrics = mockk()
        val config = mockk<CacheIqConfig> {
            every { getLlmModel(any()) } returns "llama-3.3-70b-versatile"
            every { getLlmProvider() } returns "groq"
            every { getEmbeddingModel() } returns "all-MiniLM-L6-v2"
            every { getSimilarityThreshold() } returns 0.8
        }
        useCase = SemanticCacheUseCase(embeddingAdapter, llmClient, piiDetector, cacheRepository, metrics, config)
    }

    @Test
    fun `should return error when prompt is empty`() = runBlocking {
        val request = ChatRequest(model = "llama-3.3-70b-versatile", messages = listOf())
        val result = useCase.handle(request, "test", "groq")

        assertFalse(result.hit)
        assertEquals("No message provided", result.response)
    }

    @Test
    fun `should return MISS when no messages provided`() = runBlocking {
        val request = ChatRequest(model = "llama-3.3-70b-versatile", messages = listOf(ChatMessage("user", "")))
        val result = useCase.handle(request, "test", "groq")

        assertFalse(result.hit)
    }

    @Test
    fun `should fallback to groq provider when null`() = runBlocking {
        val request = ChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(ChatMessage("user", "test"))
        )
        val result = useCase.handle(request, "test", null)

        assertEquals("groq", result.llmProvider)
    }

    @Test
    fun `should use provided provider`() = runBlocking {
        val request = ChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(ChatMessage("user", "test"))
        )
        val result = useCase.handle(request, "test", "openai")

        assertEquals("openai", result.llmProvider)
    }
}