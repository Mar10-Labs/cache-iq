package com.cacheiq.domain.port.output

interface TokenCounterPort {
    fun countTokens(text: String): Int
}