package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.port.output.PiiDetectorPort
import org.springframework.stereotype.Component

@Component
open class RegexPiiDetector : PiiDetectorPort {
    
    // Regex patterns for structured PII (level 1)
    private val patterns = listOf(
        // Email
        Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""),
        
        // Phone (various formats)
        Regex("""(\+?1?[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"""),
        
        // Credit card
        Regex("""\b(?:\d[ -]*?){13,16}\b"""),
        
        // Argentine CUIT
        Regex("""\b\d{2}-\d{8}-\d{1}\b"""),
        
        // Argentine DNI
        Regex("""\b\d{7,8}\b"""),
        
        // CBU (Argentine bank account)
        Regex("""\b\d{22}\b""")
    )
    
    override suspend fun containsPii(text: String): Boolean {
        return patterns.any { it.containsMatchIn(text) }
    }
}