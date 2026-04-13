package com.cacheiq.infrastructure.adapter.pii

import com.cacheiq.domain.model.PiiEntity
import com.cacheiq.domain.port.output.PiiDetectorPort
import org.springframework.stereotype.Component

@Component
open class RegexPiiDetector : PiiDetectorPort {
    
    data class PatternDef(val pattern: Regex, val type: String)
    
    private val patterns = listOf(
        PatternDef(Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""), "EMAIL"),
        PatternDef(Regex("""(\+?1?[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"""), "PHONE"),
        PatternDef(Regex("""\b(?:\d[ -]*?){13,16}\b"""), "CREDIT_CARD"),
        PatternDef(Regex("""\b\d{2}-\d{8}-\d{1}\b"""), "CUIT"),
        PatternDef(Regex("""\b\d{7,8}\b"""), "DNI"),
        PatternDef(Regex("""\b\d{22}\b"""), "CBU")
    )
    
    override suspend fun containsPii(text: String): Boolean {
        return patterns.any { it.pattern.containsMatchIn(text) }
    }
    
    override suspend fun detectEntities(text: String): List<PiiEntity> {
        val entities = mutableListOf<PiiEntity>()
        for (def in patterns) {
            def.pattern.findAll(text).forEach { match ->
                entities.add(
                    PiiEntity(
                        type = def.type,
                        text = match.value,
                        start = match.range.first,
                        end = match.range.last + 1,
                        score = 1.0
                    )
                )
            }
        }
        return entities.sortedBy { it.start }
    }
}