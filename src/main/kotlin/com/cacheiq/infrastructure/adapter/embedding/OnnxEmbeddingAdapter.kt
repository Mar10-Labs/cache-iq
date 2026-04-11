package com.cacheiq.infrastructure.adapter.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.cacheiq.domain.model.EmbeddingVector
import com.cacheiq.domain.port.output.EmbeddingPort
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.nio.LongBuffer

@Component
class OnnxEmbeddingAdapter(
    @Value("classpath:models/model.onnx") private val modelResource: Resource,
    @Value("classpath:models/tokenizer.json") private val tokenizerResource: Resource,
    @Value("classpath:models/tokenizer_config.json") private val tokenizerConfigResource: Resource,
    @Value("\${embedding.model.dimensions:384}") private val dimensions: Int,
    @Value("\${embedding.model.max_length:256}") private val maxLength: Int
) : EmbeddingPort {

    private val logger = LoggerFactory.getLogger(OnnxEmbeddingAdapter::class.java)
    
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    
    private val tokenizer: Tokenizer
    
    init {
        logger.info("Loading ONNX model from: ${modelResource.filename}")
        
        val modelBytes = modelResource.inputStream.readBytes()
        session = environment.createSession(modelBytes, OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        })
        
        logger.info("ONNX model loaded. Input names: ${session.inputNames}, Output names: ${session.outputNames}")
        
        tokenizer = Tokenizer(
            tokenizerJson = ObjectMapper().readTree(tokenizerResource.inputStream),
            configJson = ObjectMapper().readTree(tokenizerConfigResource.inputStream)
        )
        
        logger.info("Tokenizer loaded. vocab size: ${tokenizer.vocabSize}")
    }

    override suspend fun embed(text: String): EmbeddingVector {
        logger.debug("Generating embedding for text: ${text.take(50)}...")
        
        // Tokenize
        val tokenResult = tokenizer.tokenize(text, maxLength)
        val inputIds = tokenResult.first.map { it.toLong() }.toLongArray()
        val attentionMask = tokenResult.second.map { it.toLong() }.toLongArray()
        
        // Create input tensors using explicit LongBuffer
        val inputIdsBuffer = LongBuffer.wrap(inputIds)
        val attentionMaskBuffer = LongBuffer.wrap(attentionMask)
        
        val inputIdsTensor = OnnxTensor.createTensor(environment, inputIdsBuffer, longArrayOf(1, inputIds.size.toLong()))
        val attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMaskBuffer, longArrayOf(1, attentionMask.size.toLong()))
        
        // Run inference
        val inputs = mapOf<String, OnnxTensor>(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        )
        
        val output = session.run(inputs)
        val embeddings = output.get(0).value as Array<FloatArray>
        
        // Mean pooling
        val attentionMaskLong = attentionMask.toList()
        val pooled = meanPool(embeddings[0], attentionMaskLong)
        
        // L2 normalize
        val normalized = normalize(pooled)
        
        logger.debug("Generated embedding with ${normalized.size} dimensions")
        return EmbeddingVector(normalized)
    }
    
    private fun meanPool(embeddings: FloatArray, attentionMask: List<Long>): FloatArray {
        var sum = 0L
        for (m in attentionMask) {
            sum += m
        }
        val seqLen = sum.toInt()
        if (seqLen == 0) return FloatArray(dimensions)
        
        val result = FloatArray(dimensions)
        val maskSize = minOf(attentionMask.size, embeddings.size / dimensions)
        
        for (i in 0 until maskSize) {
            if (attentionMask[i] > 0) {
                val offset = i * dimensions
                for (d in 0 until dimensions) {
                    result[d] = result[d] + embeddings[offset + d]
                }
            }
        }
        
        // Average
        for (d in 0 until dimensions) {
            result[d] = result[d] / seqLen
        }
        return result
    }
    
    private fun normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = kotlin.math.sqrt(sumSquares.toDouble()).toFloat()
        if (norm > 0) {
            return FloatArray(vector.size) { i -> vector[i] / norm }
        }
        return vector
    }
}

class Tokenizer(
    private val tokenizerJson: JsonNode,
    private val configJson: JsonNode
) {
    private val vocab: Map<String, Int> = buildMap {
        tokenizerJson.get("model").get("vocab").fields().forEach { (key, value) ->
            put(key, value.asInt())
        }
    }
    
    val vocabSize: Int = vocab.size
    
    private val doLowerCase = configJson.get("do_lower_case").asBoolean()
    private val padToken = configJson.get("pad_token").asText()
    private val padTokenId = vocab[padToken] ?: 0
    private val unkToken = configJson.get("unk_token").asText()
    private val unkTokenId = vocab[unkToken] ?: 100
    
    fun tokenize(text: String, maxLength: Int): Pair<List<Int>, List<Int>> {
        val processedText = if (doLowerCase) text.lowercase() else text
        
        val tokens = mutableListOf<Int>()
        var i = 0
        while (i < processedText.length) {
            var longestMatch: Pair<String, Int>? = null
            for (len in minOf(processedText.length - i, 20) downTo 1) {
                val substr = processedText.substring(i, i + len)
                if (substr in vocab) {
                    longestMatch = Pair(substr, len)
                    break
                }
            }
            
            if (longestMatch != null) {
                tokens.add(vocab[longestMatch.first]!!)
                i += longestMatch.second
            } else {
                tokens.add(unkTokenId)
                i += 1
            }
        }
        
        // Truncate
        val truncated = if (tokens.size > maxLength - 2) {
            tokens.take(maxLength - 2)
        } else {
            tokens
        }
        
        // Add special tokens [CLS] [SEP]
        val clsId = vocab["[CLS]"] ?: 101
        val sepId = vocab["[SEP]"] ?: 102
        val inputIds = listOf(clsId) + truncated + listOf(sepId)
        
        // Attention mask
        val attentionMask = List(inputIds.size) { 1 }
        
        // Pad
        val paddedInputIds = inputIds.toMutableList()
        val paddedAttentionMask = attentionMask.toMutableList()
        while (paddedInputIds.size < maxLength) {
            paddedInputIds.add(padTokenId)
            paddedAttentionMask.add(0)
        }
        
        return Pair(paddedInputIds, paddedAttentionMask)
    }
}