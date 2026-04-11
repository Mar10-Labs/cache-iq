package com.cacheiq.domain.port.output

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.ChatResponse

interface LlmClientPort {
    val providerName: String
    suspend fun complete(request: ChatRequest): ChatResponse
}