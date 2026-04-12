package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.model.PiiRiskLevel
import com.cacheiq.domain.model.RouteType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class QueryRiskAnalyzer(
    @Value("\${pii.risk-analyzer.personal-indicators: soy,mi nombre,mis datos,cancela,suscripcion,mi cuenta,name,last name,address,phone,account,my name,cancel,subscription,email}")
    personalIndicatorsConfig: String
) {
    
    private val personalIndicators: Set<String> = personalIndicatorsConfig.split(",").map { it.trim().lowercase() }.toSet()
    
    private val structuredPattern = Regex(
        """@|\d{4}[-\s]?\d{4}|\d{10,11}|CUIT|DNI|CBU|\d{13,16}"""
    )
    
    fun classify(text: String, routeType: RouteType): PiiRiskLevel {
        if (routeType == RouteType.TECHNICAL) {
            return PiiRiskLevel.NONE
        }
        
        if (structuredPattern.containsMatchIn(text)) {
            return PiiRiskLevel.STRUCTURED
        }
        
        val lower = text.lowercase()
        if (personalIndicators.any { lower.contains(it) }) {
            return PiiRiskLevel.CONTEXTUAL
        }
        
        return PiiRiskLevel.STRUCTURED
    }
}