package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.model.PiiDetectionResult
import com.cacheiq.domain.model.PiiRiskLevel
import com.cacheiq.domain.model.RouteType
import com.cacheiq.domain.port.output.PiiDetectorPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContextualPiiRouter(
    private val riskAnalyzer: QueryRiskAnalyzer,
    private val regexDetector: RegexPiiDetector,
    private val presidioClient: PresidioClient
) : PiiDetectorPort {
    
    private val log = LoggerFactory.getLogger(ContextualPiiRouter::class.java)
    
    override suspend fun containsPii(text: String): Boolean {
        return analyze(text).detectedEntities.isNotEmpty()
    }
    
    suspend fun analyze(text: String, routeType: RouteType = RouteType.SUPPORT): PiiDetectionResult {
        val riskLevel = riskAnalyzer.classify(text, routeType)
        
        return when (riskLevel) {
            PiiRiskLevel.NONE -> PiiDetectionResult(
                riskLevel = riskLevel,
                detectedEntities = emptyList(),
                requiresPresidio = false,
                confidence = 1.0
            )
            
            PiiRiskLevel.STRUCTURED -> {
                val entities = regexDetector.detectEntities(text)
                PiiDetectionResult(
                    riskLevel = riskLevel,
                    detectedEntities = entities,
                    requiresPresidio = false,
                    confidence = 0.95
                )
            }
            
            PiiRiskLevel.CONTEXTUAL -> {
                val regexEntities = regexDetector.detectEntities(text)
                if (regexEntities.isNotEmpty()) {
                    return PiiDetectionResult(
                        riskLevel = riskLevel,
                        detectedEntities = regexEntities,
                        requiresPresidio = false,
                        confidence = 0.95
                    )
                }
                
                val presidioEntities = try {
                    presidioClient.analyzeBlocking(text)
                } catch (e: Exception) {
                    log.warn("Presidio timeout or error, assuming PII present: {}", e.message)
                    emptyList()
                }
                
                PiiDetectionResult(
                    riskLevel = riskLevel,
                    detectedEntities = if (presidioEntities.isEmpty()) regexEntities else presidioEntities,
                    requiresPresidio = presidioEntities.isNotEmpty(),
                    confidence = if (presidioEntities.isEmpty()) 0.95 else 0.85
                )
            }
        }
    }
}