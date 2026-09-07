package com.example.privacyfirewall.domain

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

class AiDetector(context: Context) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    var isModelLoaded: Boolean = false
        private set

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
            isModelLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
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
        if (text.isBlank() || ortSession == null || !isModelLoaded) return results

        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null
        var tokenTypeIdsTensor: OnnxTensor? = null
        var output: OrtSession.Result? = null

        try {
            // 1. Tokenize text with exact character spans
            val tokenizationResult = tokenizer.tokenize(text)
            val inputIds = tokenizationResult.inputIds
            val attentionMask = tokenizationResult.attentionMask
            val tokens = tokenizationResult.tokens

            if (inputIds.isEmpty()) return results

            // 2. Prepare tensors & Run Inference
            inputIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(inputIds), longArrayOf(1, inputIds.size.toLong()))
            attentionMaskTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(attentionMask), longArrayOf(1, attentionMask.size.toLong()))
            val tokenTypeIds = LongArray(inputIds.size)
            tokenTypeIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenTypeIds), longArrayOf(1, tokenTypeIds.size.toLong()))

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            output = ortSession?.run(inputs)
            val logits = output?.get(0)?.value as? Array<Array<FloatArray>> ?: return results
            val sequenceLogits = logits[0]

            val nerTokens = mutableListOf<NerToken>()

            // 3. Map logits back to labels and tokens with exact text character offsets
            for (i in sequenceLogits.indices) {
                if (i >= tokens.size) break
                val tokenSpan = tokens[i]
                if (tokenSpan.token == "[CLS]" || tokenSpan.token == "[SEP]") continue

                val tokenLogits = sequenceLogits[i]
                var maxScore = -Float.MAX_VALUE
                var maxIndex = 0
                for (j in tokenLogits.indices) {
                    if (tokenLogits[j] > maxScore) {
                        maxScore = tokenLogits[j]
                        maxIndex = j
                    }
                }

                // Softmax probability calculation
                val expSum = tokenLogits.sumOf { kotlin.math.exp(it.toDouble()) }
                val probability = if (expSum > 0) (kotlin.math.exp(maxScore.toDouble()) / expSum).toFloat() else 0f
                val label = idToLabel[maxIndex] ?: "O"

                // Pass real start and end character offsets from tokenSpan!
                nerTokens.add(NerToken(label, tokenSpan.token, probability, tokenSpan.start, tokenSpan.end))
            }

            // 4. Process BIO tags and Aggregate with real text slices
            val aggregated = aggregateTokens(text, nerTokens, threshold)
            results.addAll(aggregated)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputIdsTensor?.close()
            attentionMaskTensor?.close()
            tokenTypeIdsTensor?.close()
            output?.close()
        }

        return results
    }

    private fun aggregateTokens(
        text: String,
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

            if (isBeginning) {
                currentEntity?.let { aggregated.add(it.toResult(text)) }
                currentEntity = NerEntity(threatType, token.score, token.start, token.end)
            } else if (isContinuation && currentEntity != null && currentEntity.type == threatType) {
                // Extend entity span with real token.end
                currentEntity.end = token.end
                currentEntity.score = kotlin.math.min(currentEntity.score, token.score)
            } else {
                currentEntity?.let { aggregated.add(it.toResult(text)) }
                currentEntity = NerEntity(threatType, token.score, token.start, token.end)
            }
        }
        currentEntity?.let { aggregated.add(it.toResult(text)) }

        // Deduplicate overlapping / subsumed matches without dropping distinct entities
        return aggregated
            .distinctBy { "${it.threatType}-${it.startIndex}-${it.endIndex}" }
            .filter { r1 ->
                aggregated.none { r2 ->
                    r2 !== r1 && r2.threatType == r1.threatType &&
                    r2.startIndex <= r1.startIndex && r2.endIndex >= r1.endIndex &&
                    (r2.endIndex - r2.startIndex) > (r1.endIndex - r1.startIndex)
                }
            }
    }

    private data class NerToken(val entity: String, val word: String, val score: Float, val start: Int, val end: Int)

    private data class NerEntity(val type: RegexDetector.ThreatType, var score: Float, val start: Int, var end: Int) {
        fun toResult(text: String): RegexDetector.DetectionResult {
            val safeStart = start.coerceIn(0, text.length)
            val safeEnd = end.coerceIn(safeStart, text.length)
            val matched = if (safeStart < safeEnd) text.substring(safeStart, safeEnd) else ""
            return RegexDetector.DetectionResult(type, matched, safeStart, safeEnd)
        }
    }

    fun close() {
        ortSession?.close()
        ortEnv.close()
    }
}
