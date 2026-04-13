package com.cacheiq.domain.port.output

import com.cacheiq.domain.model.PiiEntity

interface PiiDetectorPort {
    suspend fun containsPii(text: String): Boolean
    suspend fun detectEntities(text: String): List<PiiEntity> = emptyList()
}