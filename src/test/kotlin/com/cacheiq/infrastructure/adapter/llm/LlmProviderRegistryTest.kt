package com.cacheiq.infrastructure.adapter.llm

import com.cacheiq.domain.port.output.LlmClientPort
import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.ChatResponse
import com.cacheiq.domain.model.Usage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LlmProviderRegistryTest {
    
    private lateinit var registry: LlmProviderRegistry
    
    private val mockGroqAdapter = object : LlmClientPort {
        override val providerName = "groq"
        override suspend fun complete(request: ChatRequest) = ChatResponse(
            id = "groq-mock", content = "groq response", model = "llama",
            finishReason = "stop", usage = Usage(10, 10, 20)
        )
    }
    
    private val mockClaudeAdapter = object : LlmClientPort {
        override val providerName = "claude"
        override suspend fun complete(request: ChatRequest) = ChatResponse(
            id = "claude-mock", content = "claude response", model = "claude",
            finishReason = "stop", usage = Usage(10, 10, 20)
        )
    }
    
    @BeforeEach
    fun setup() {
        registry = LlmProviderRegistry(listOf(mockGroqAdapter, mockClaudeAdapter))
    }
    
    @Test
    fun `resolve returns correct adapter`() {
        val adapter = registry.resolve("groq")
        assertEquals("groq", adapter.providerName)
    }
    
    @Test
    fun `resolve case insensitive`() {
        val adapter = registry.resolve("GROQ")
        assertEquals("groq", adapter.providerName)
    }
    
    @Test
    fun `listProviders returns all providers`() {
        val providers = registry.listProviders()
        assertEquals(2, providers.size)
        assertTrue(providers.contains("groq"))
        assertTrue(providers.contains("claude"))
    }
    
    @Test
    fun `isSupported returns true for existing provider`() {
        assertTrue(registry.isSupported("groq"))
        assertTrue(registry.isSupported("CLAUDE"))
    }
    
    @Test
    fun `isSupported returns false for non-existing provider`() {
        assertFalse(registry.isSupported("openai"))
        assertFalse(registry.isSupported("unknown"))
    }
    
    @Test
    fun `resolve throws for unknown provider`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            registry.resolve("openai")
        }
        assertTrue(exception.message!!.contains("openai"))
    }
}