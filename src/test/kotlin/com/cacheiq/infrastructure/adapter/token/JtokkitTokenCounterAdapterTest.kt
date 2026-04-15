package com.cacheiq.infrastructure.adapter.token

import com.cacheiq.domain.port.output.TokenCounterPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JtokkitTokenCounterAdapterTest {

    private lateinit var tokenCounter: TokenCounterPort

    @BeforeEach
    fun setup() {
        tokenCounter = JtokkitTokenCounterAdapter()
    }

    @Test
    fun `should count tokens for simple text`() {
        val text = "Hello world"
        val count = tokenCounter.countTokens(text)
        assertTrue(count > 0, "Should return token count > 0")
    }

    @Test
    fun `should count tokens for longer text`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val count = tokenCounter.countTokens(text)
        assertTrue(count > 5, "Should count more than 5 tokens")
    }

    @Test
    fun `should handle empty text`() {
        val text = ""
        val count = tokenCounter.countTokens(text)
        assertTrue(count >= 0, "Should handle empty text")
    }

    @Test
    fun `should count tokens consistently`() {
        val text = "Machine learning is great"
        val count1 = tokenCounter.countTokens(text)
        val count2 = tokenCounter.countTokens(text)
        assertEquals(count1, count2, "Should return consistent count")
    }
}