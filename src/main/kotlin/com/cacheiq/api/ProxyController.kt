package com.cacheiq.api

import com.cacheiq.domain.port.input.CacheInputPort
import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.ChatResponse
import com.cacheiq.domain.model.Usage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.runBlocking

@RestController
@RequestMapping("/proxy")
@Tag(name = "CacheIQ Proxy", description = "Proxy with semantic cache")
class ProxyController(
    private val cacheInputPort: CacheInputPort
) {
    
    private val logger = LoggerFactory.getLogger(ProxyController::class.java)
    
    @PostMapping("/**")
    @Operation(
        summary = "Proxy con cache semantico",
        description = "Proxy that uses semantic cache to avoid redundant LLM calls. " +
            "Flow: 1) Generate embedding from prompt, 2) Search cache using cosine similarity, " +
            "3) HIT: return cached response, 4) MISS: call LLM and store response."
    )
    @ApiResponse(responseCode = "200", description = "Respuesta del LLM (HIT o MISS)")
    @ApiResponse(responseCode = "400", description = "Request invalido")
    @ApiResponse(responseCode = "500", description = "Error interno")
    fun proxy(
        @RequestBody request: ChatRequest,
        @Parameter(description = "Tenant ID", required = true)
        @RequestHeader("X-Tenant-Id") tenantId: String,
        @Parameter(description = "LLM Provider override")
        @RequestHeader(value = "X-Llm-Provider", required = false) llmProvider: String?
    ): ResponseEntity<ChatResponse> {
        return try {
            logger.info("Proxy request received for tenant: $tenantId")
            
            val result = runBlocking {
                cacheInputPort.handle(request, tenantId, llmProvider)
            }
            
            logger.info("Request processed, hit=${result.hit}")
            
            ResponseEntity.ok()
                .header("X-Cache", if (result.hit) "HIT" else "MISS")
                .header("X-Cache-Llm-Model", result.llmModel)
                .header("X-Cache-Llm-Provider", result.llmProvider)
                .header("X-Cache-Embedding-Model", result.embeddingModel)
                .body(
                    ChatResponse(
                        id = "cacheiq-${System.currentTimeMillis()}",
                        content = result.response,
                        model = result.llmModel,
                        finishReason = "stop",
                        usage = Usage(
                            promptTokens = if (result.hit) 0 else 100,
                            completionTokens = if (result.hit) 0 else 50,
                            totalTokens = if (result.hit) 0 else 150
                        )
                    )
                )
        } catch (e: Exception) {
            logger.error("Error processing request: ${e.message}", e)
            ResponseEntity.internalServerError()
                .body(
                    ChatResponse(
                        id = "error-${System.currentTimeMillis()}",
                        content = "Error: ${e.message}",
                        model = "error",
                        finishReason = "error",
                        usage = Usage(0, 0, 0)
                    )
                )
        }
    }
}