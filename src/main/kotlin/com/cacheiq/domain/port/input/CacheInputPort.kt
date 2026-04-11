package com.cacheiq.domain.port.input

import com.cacheiq.domain.model.ChatRequest
import com.cacheiq.domain.model.CacheResponse

interface CacheInputPort {
    suspend fun handle(request: ChatRequest, tenantId: String, provider: String? = null): CacheResponse
}