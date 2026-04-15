package com.cacheiq.infrastructure.adapter.token

import com.cacheiq.domain.port.output.TokenCounterPort
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType
import org.springframework.stereotype.Component

@Component
class JtokkitTokenCounterAdapter : TokenCounterPort {

    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private val encoding: Encoding = registry.getEncoding(EncodingType.CL100K_BASE)

    override fun countTokens(text: String): Int = encoding.countTokens(text)
}