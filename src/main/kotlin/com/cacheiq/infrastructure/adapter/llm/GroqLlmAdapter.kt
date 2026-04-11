package com.cacheiq.infrastructure.adapter.llm

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.ChatResponse
import com.cacheiq.domain.model.Usage
import com.cacheiq.domain.port.output.LlmClientPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GroqLlmAdapter(
    @Value("\${llm.provider.groq.api-key:}") private val apiKey: String,
    @Value("\${llm.provider.groq.model:llama-3.3-70b-versatile}") private val model: String
) : LlmClientPort {
    
    private val logger = LoggerFactory.getLogger(GroqLlmAdapter::class.java)
    
    override val providerName: String = "groq"
    
    override suspend fun complete(request: ChatRequest): ChatResponse {
        if (apiKey.isBlank()) {
            logger.warn("No Groq API key configured - returning mock response for V1")
            val prompt = request.messages.lastOrNull()?.content ?: ""
            return ChatResponse(
                id = "mock-${System.currentTimeMillis()}",
                content = generateMockResponse(prompt),
                model = model,
                finishReason = "stop",
                usage = Usage(
                    promptTokens = prompt.split(" ").size,
                    completionTokens = 20,
                    totalTokens = prompt.split(" ").size + 20
                )
            )
        }
        
        // Real implementation would use WebClient here
        logger.error("Real LLM calls not implemented - API key present but WebClient not available")
        return ChatResponse(
            id = "not-implemented-${System.currentTimeMillis()}",
            content = "LLM integration not fully implemented - configure WebClient for production",
            model = model,
            finishReason = "stop",
            usage = Usage(
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0
            )
        )
    }
    
    private fun generateMockResponse(prompt: String): String {
        return when {
            prompt.contains("hola", ignoreCase = true) || prompt.contains("hello", ignoreCase = true) ->
                "Hola! Soy CacheIQ, un proxy con cache semántico. Estoy funcionando correctamente en V1!"
            prompt.contains("cómo", ignoreCase = true) || prompt.contains("how", ignoreCase = true) ->
                "Estoy funcionando bien, gracias por preguntar. Estoy corriendo en Docker con PostgreSQL y Redis."
            prompt.contains("ayuda", ignoreCase = true) || prompt.contains("help", ignoreCase = true) ->
                "Puedes usar el endpoint POST /proxy/** con el header X-Tenant-Id para probar el cache semántico."
            else ->
                "Mensaje recibido: '$prompt'. Esta es una respuesta mock de V1. Para probar el cache, envia la misma pregunta dos veces."
        }
    }
}