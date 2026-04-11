package com.cacheiq.domain.port.output

import com.cacheiq.domain.model.EmbeddingVector

interface EmbeddingPort {
    suspend fun embed(text: String): EmbeddingVector
}