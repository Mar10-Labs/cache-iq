package com.cacheiq.domain.port.output

interface PiiDetectorPort {
    suspend fun containsPii(text: String): Boolean
}