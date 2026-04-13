package com.cacheiq.infrastructure.adapter.llm

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.ChatResponse
import com.cacheiq.domain.model.Usage
import com.cacheiq.domain.port.output.LlmClientPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

@Component
class GroqLlmAdapter(
    @Value("\${llm.provider.groq.api-key:}") private val apiKey: String,
    @Value("\${llm.provider.groq.model:llama-3.3-70b-versatile}") private val model: String,
    @Value("\${llm.provider.groq.base-url:https://api.groq.com/openai/v1}") private val baseUrl: String
) : LlmClientPort {
    
    private val logger = LoggerFactory.getLogger(GroqLlmAdapter::class.java)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()
    
    override val providerName: String = "groq"
    
    override suspend fun complete(request: ChatRequest): ChatResponse {
        if (apiKey.isBlank()) {
            logger.warn("No Groq API key configured - returning mock response")
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
        
        return try {
            val prompt = request.messages.lastOrNull()?.content ?: ""
            val groqRequest = mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "temperature" to 0.7
            )
            
            val response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(groqRequest)
                .retrieve()
                .bodyToMono(GroqResponse::class.java)
                .timeout(Duration.ofSeconds(30))
                .block()
            
            response?.let {
                ChatResponse(
                    id = it.id ?: "groq-${System.currentTimeMillis()}",
                    content = it.choices?.firstOrNull()?.message?.content ?: "",
                    model = it.model ?: model,
                    finishReason = it.choices?.firstOrNull()?.finish_reason ?: "stop",
                    usage = Usage(
                        promptTokens = it.usage?.prompt_tokens ?: 0,
                        completionTokens = it.usage?.completion_tokens ?: 0,
                        totalTokens = it.usage?.total_tokens ?: 0
                    )
                )
            } ?: ChatResponse(
                id = "error-${System.currentTimeMillis()}",
                content = "Empty response from Groq",
                model = model,
                finishReason = "stop",
                usage = Usage(0, 0, 0)
            )
        } catch (e: Exception) {
            logger.error("Error calling Groq API: ${e.message}", e)
            ChatResponse(
                id = "error-${System.currentTimeMillis()}",
                content = "Error calling LLM: ${e.message}",
                model = model,
                finishReason = "stop",
                usage = Usage(0, 0, 0)
            )
        }
    }
    
    private fun generateMockResponse(prompt: String): String {
        return when {
            prompt.contains("hola", ignoreCase = true) || prompt.contains("hello", ignoreCase = true) ->
                "Hola! Soy CacheIQ, un proxy con cache semántico. Estoy funcionando correctamente en V1!"
            prompt.contains("cómo", ignoreCase = true) || prompt.contains("how", ignoreCase = true) ->
                "Estoy funcionando bien, gracias por preguntar. Estoy corriendo en Docker con PostgreSQL."
            prompt.contains("ayuda", ignoreCase = true) || prompt.contains("help", ignoreCase = true) ->
                "Puedes usar el endpoint POST /proxy/** con el header X-Tenant-Id para probar el cache semántico."
            else ->
                "Mensaje recibido: '$prompt'. Esta es una respuesta mock de V1. Para probar el cache, envia la misma pregunta dos veces."
        }
    }
    
    private data class GroqResponse(
        val id: String?,
        val model: String?,
        val choices: List<Choice>?,
        val usage: GroqUsage?
    )
    
    private data class Choice(
        val index: Int?,
        val message: Message?,
        val finish_reason: String?
    )
    
    private data class Message(
        val role: String?,
        val content: String?
    )
    
    private data class GroqUsage(
        val prompt_tokens: Int?,
        val completion_tokens: Int?,
        val total_tokens: Int?
    )
}