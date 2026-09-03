package com.example.privacyfirewall.domain

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import kotlin.math.max

class AiDetector(private val context: Context) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null

    // For Xenova/bert-base-NER-uncased, the output labels are typically:
    // 0: O, 1: B-MISC, 2: I-MISC, 3: B-PER, 4: I-PER, 5: B-ORG, 6: I-ORG, 7: B-LOC, 8: I-LOC
    private val idToLabel = mapOf(
        0 to "O", 1 to "B-MISC", 2 to "I-MISC",
        3 to "B-PER", 4 to "I-PER",
        5 to "B-ORG", 6 to "I-ORG",
        7 to "B-LOC", 8 to "I-LOC"
    )

    private lateinit var tokenizer: BertTokenizer

    init {
        try {
            val modelBytes = context.assets.open("model_quantized.onnx").readBytes()
            ortSession = ortEnv.createSession(modelBytes, OrtSession.SessionOptions())
            tokenizer = BertTokenizer(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapEntityType(entity: String): RegexDetector.ThreatType? {
        val cleanEntity = entity.replace(Regex("^[BI]-"), "").uppercase()
        return when (cleanEntity) {
            "PER", "PERSON" -> RegexDetector.ThreatType.PERSON
            "ORG", "ORGANIZATION" -> RegexDetector.ThreatType.ORGANIZATION
            "LOC", "LOCATION" -> RegexDetector.ThreatType.LOCATION
            else -> null
        }
    }

    fun detect(text: String, threshold: Float = 0.5f): List<RegexDetector.DetectionResult> {
        val results = mutableListOf<RegexDetector.DetectionResult>()
        if (text.isBlank() || ortSession == null) return results

        try {
            // 1. Tokenize the text
            val tokenizationResult = tokenizer.tokenize(text)
            val inputIds = tokenizationResult.inputIds
            val attentionMask = tokenizationResult.attentionMask
            val tokens = tokenizationResult.tokens

            // 2. Run Inference
            val inputIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(inputIds), longArrayOf(1, inputIds.size.toLong()))
            val attentionMaskTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(attentionMask), longArrayOf(1, attentionMask.size.toLong()))
            
            // BERT-base usually also expects token_type_ids
            val tokenTypeIds = LongArray(inputIds.size) { 0L }
            val tokenTypeIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenTypeIds), longArrayOf(1, tokenTypeIds.size.toLong()))
            
            val inputs = mapOf(
                "input_ids" to inputIdsTensor, 
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )
            
            val output = ortSession?.run(inputs)
            
            // logits shape is [1, sequence_length, num_labels]
            val logits = output?.get(0)?.value as Array<Array<FloatArray>>
            val sequenceLogits = logits[0]

            val nerTokens = mutableListOf<NerToken>()
            
            // 3. Map logits back to labels and tokens
            for (i in sequenceLogits.indices) {
                val tokenStr = tokens[i]
                if (tokenStr == "[CLS]" || tokenStr == "[SEP]") continue

                val tokenLogits = sequenceLogits[i]
                
                // Find argmax for the label
                var maxScore = -Float.MAX_VALUE
                var maxIndex = 0
                for (j in tokenLogits.indices) {
                    if (tokenLogits[j] > maxScore) {
                        maxScore = tokenLogits[j]
                        maxIndex = j
                    }
                }

                // Simple softmax approximation for score (or just sigmoid if thresholding raw logits)
                // For simplicity, we just use raw logit score for threshold comparison or a pseudo-probability
                // Real softmax:
                val expSum = tokenLogits.sumOf { kotlin.math.exp(it.toDouble()) }
                val probability = (kotlin.math.exp(maxScore.toDouble()) / expSum).toFloat()

                val label = idToLabel[maxIndex] ?: "O"
                
                // We don't have exact character offsets from this simple tokenizer yet,
                // so we approximate start/end for the aggregation logic. 
                // A production tokenizer tracks exact offsets.
                nerTokens.add(NerToken(label, tokenStr, probability, 0, 0))
            }

            // Clean up tensors
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
            output?.close()

            // 4. Process BIO tags and Aggregate
            val aggregated = aggregateTokens(nerTokens, threshold)
            results.addAll(aggregated)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    // This mimics the aggregation logic from your transformer-detector.js
    private fun aggregateTokens(
        tokens: List<NerToken>,
        threshold: Float
    ): List<RegexDetector.DetectionResult> {
        val filtered = tokens.filter { 
            it.score >= threshold && !it.entity.endsWith("MISC") && it.entity != "O"
        }

        val aggregated = mutableListOf<RegexDetector.DetectionResult>()
        var currentEntity: NerEntity? = null

        for (token in filtered) {
            val isBeginning = token.entity.startsWith("B-")
            val isContinuation = token.entity.startsWith("I-")
            val threatType = mapEntityType(token.entity) ?: continue
            val word = token.word.replace(Regex("^##"), "")

            if (isBeginning) {
                currentEntity?.let { aggregated.add(it.toResult()) }
                currentEntity = NerEntity(threatType, word, token.score, token.start, token.end)
            } else if (isContinuation && currentEntity != null && currentEntity.type == threatType) {
                val gap = token.start - currentEntity.end
                if (gap <= 1) {
                    currentEntity.value += word
                } else {
                    currentEntity.value += " $word"
                }
                currentEntity.end = token.end
                currentEntity.score = kotlin.math.min(currentEntity.score, token.score)
            } else {
                currentEntity?.let { aggregated.add(it.toResult()) }
                currentEntity = NerEntity(threatType, word, token.score, token.start, token.end)
            }
        }
        currentEntity?.let { aggregated.add(it.toResult()) }

        // Deduplicate by type (keeping longest)
        return aggregated
            .groupBy { it.threatType }
            .map { entry -> entry.value.maxByOrNull { it.matchedText.length }!! }
    }

    private data class NerToken(val entity: String, val word: String, val score: Float, val start: Int, val end: Int)
    
    private data class NerEntity(val type: RegexDetector.ThreatType, var value: String, var score: Float, val start: Int, var end: Int) {
        fun toResult() = RegexDetector.DetectionResult(type, value.trim().replace(Regex("\\s+"), " "), start, end)
    }

    fun close() {
        ortSession?.close()
        ortEnv.close()
    }
}
