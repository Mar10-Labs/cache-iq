package com.cacheiq.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
open class PresidioConfig(
    @Value("\${pii.sidecar.url:http://localhost:3001}") private val presidioUrl: String
) {
    @Bean
    open fun presidioWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(presidioUrl)
            .build()
    }
}