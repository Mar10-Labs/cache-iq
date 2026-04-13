package com.cacheiq.infrastructure.adapter.llm

import com.cacheiq.domain.port.output.LlmClientPort
import org.springframework.stereotype.Component

@Component
class LlmProviderRegistry(
    private val adapters: List<LlmClientPort>
) {
    
    private val registry = adapters.associateBy { it.providerName.lowercase() }
    
    fun resolve(providerName: String): LlmClientPort {
        val normalized = providerName.lowercase()
        return registry[normalized] 
            ?: throw IllegalStateException("Provider '$providerName' not registered. Available: ${registry.keys}")
    }
    
    fun listProviders(): List<String> = registry.keys.toList()
    
    fun isSupported(providerName: String): Boolean = registry.containsKey(providerName.lowercase())
}