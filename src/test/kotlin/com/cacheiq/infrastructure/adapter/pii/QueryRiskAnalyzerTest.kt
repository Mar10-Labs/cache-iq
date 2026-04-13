package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.model.PiiRiskLevel
import com.cacheiq.domain.model.RouteType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class QueryRiskAnalyzerTest {
    
    private lateinit var analyzer: QueryRiskAnalyzer
    
    @BeforeEach
    fun setup() {
        analyzer = QueryRiskAnalyzer("soy,mi nombre,mancel,subscription,name,last name,address,phone,account,my name,cancel,subscription,email")
    }
    
    @Test
    fun `TECHNICAL route returns NONE`() {
        assertEquals(PiiRiskLevel.NONE, analyzer.classify("How does HNSW work?", RouteType.TECHNICAL))
        assertEquals(PiiRiskLevel.NONE, analyzer.classify("test@test.com", RouteType.TECHNICAL))
    }
    
    @Test
    fun `structured patterns return STRUCTURED`() {
        assertEquals(PiiRiskLevel.STRUCTURED, analyzer.classify("email test@test.com", RouteType.SUPPORT))
        assertEquals(PiiRiskLevel.STRUCTURED, analyzer.classify("phone 1234-5678", RouteType.TRANSACTIONAL))
        assertEquals(PiiRiskLevel.STRUCTURED, analyzer.classify("CUIT 20-12345678-9", RouteType.SUPPORT))
    }
    
    @Test
    fun `personal indicators return CONTEXTUAL`() {
        assertEquals(PiiRiskLevel.CONTEXTUAL, analyzer.classify("I am John and want to cancel my account", RouteType.SUPPORT))
        assertEquals(PiiRiskLevel.CONTEXTUAL, analyzer.classify("My name is John", RouteType.SUPPORT))
        assertEquals(PiiRiskLevel.CONTEXTUAL, analyzer.classify("I want to know my address", RouteType.SUPPORT))
    }
    
    @Test
    fun `default returns STRUCTURED`() {
        assertEquals(PiiRiskLevel.STRUCTURED, analyzer.classify("How do I reset password?", RouteType.SUPPORT))
        assertEquals(PiiRiskLevel.STRUCTURED, analyzer.classify("What is refund policy?", RouteType.TRANSACTIONAL))
    }
}