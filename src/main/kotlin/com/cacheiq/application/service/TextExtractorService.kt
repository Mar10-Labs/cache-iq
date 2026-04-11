package com.cacheiq.application.service

import org.springframework.stereotype.Service

@Service
class TextExtractorService {
    
    fun extractText(text: String): String {
        return text
            .replace(Regex("""```[\s\S]*?```"""), "")
            .replace(Regex("""`[^`]+`"""), "")
            .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
            .replace(Regex("""\*([^*]+)\*"""), "$1")
            .replace(Regex("""#+\s+"""), "")
            .replace(Regex("""\n+"""), "\n")
            .trim()
    }
}