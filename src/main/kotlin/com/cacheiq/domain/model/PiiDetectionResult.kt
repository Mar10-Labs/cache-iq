package com.cacheiq.domain.model

data class PiiDetectionResult(
    val riskLevel: PiiRiskLevel,
    val detectedEntities: List<PiiEntity> = emptyList(),
    val requiresPresidio: Boolean = false,
    val confidence: Double = 1.0
)

data class PiiEntity(
    val type: String,
    val text: String,
    val start: Int,
    val end: Int,
    val score: Double
)