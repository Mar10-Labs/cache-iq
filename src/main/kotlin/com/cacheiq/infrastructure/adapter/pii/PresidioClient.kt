package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.model.PiiEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class PresidioClient(
    private val webClient: WebClient,
    @Value("\${presidio.timeout-ms:30}") private val timeoutMs: Long = 30,
    @Value("\${presidio.base-url:http://localhost:3001}") private val baseUrl: String = "http://localhost:3001"
) {
    companion object {
        private const val PII_SCORE_THRESHOLD = 0.85
    }
    
    data class PresidioRequest(val text: String, val language: String = "en", val entities: List<String> = listOf("PERSON", "PHONE", "EMAIL", "CREDIT_CARD", "CUIT", "DNI"))
    
    data class PresidioResponse(
        val results: List<DetectedEntity>
    )
    
    data class DetectedEntity(
        val entity_type: String,
        val text: String,
        val start: Int,
        val end: Int,
        val score: Double
    )
    
    fun analyze(text: String): Mono<List<PiiEntity>> {
        val request = PresidioRequest(text)
        return webClient.post()
            .uri("$baseUrl/analyze")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PresidioResponse::class.java)
            .timeout(Duration.ofMillis(timeoutMs))
            .map { response ->
                response.results
                    .filter { it.score >= PII_SCORE_THRESHOLD }
                    .map { entity ->
                        PiiEntity(
                            type = entity.entity_type,
                            text = entity.text,
                            start = entity.start,
                            end = entity.end,
                            score = entity.score
                        )
                    }
            }
            .onErrorResume { Mono.just(emptyList()) }
    }
    
    suspend fun analyzeBlocking(text: String): List<PiiEntity> {
        return analyze(text).block() ?: emptyList()
    }
}