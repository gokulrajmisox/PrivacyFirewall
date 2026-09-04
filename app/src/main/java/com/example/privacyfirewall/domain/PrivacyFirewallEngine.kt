package com.example.privacyfirewall.domain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyFirewallEngine(private val context: Context) {
    
    private val regexDetector = RegexDetector()
    val aiDetector = AiDetector(context)

    val isModelReady: Boolean
        get() = aiDetector.isModelLoaded

    suspend fun analyzeText(text: String): List<RegexDetector.DetectionResult> {
        return withContext(Dispatchers.Default) {
            val results = mutableListOf<RegexDetector.DetectionResult>()
            
            // 1. Run regex detection
            results.addAll(regexDetector.detect(text))
            
            // 2. Run AI NER detection
            results.addAll(aiDetector.detect(text))
            
            // Deduplicate overlapping results
            results.distinctBy { "${it.startIndex}-${it.endIndex}-${it.threatType}" }
        }
    }

    fun redactText(text: String, threats: List<RegexDetector.DetectionResult>): String {
        if (threats.isEmpty() || text.isEmpty()) return text

        // Sort threats in ascending order of start index
        val sorted = threats.filter { it.startIndex in 0..text.length && it.endIndex in it.startIndex..text.length }
            .sortedBy { it.startIndex }

        val sb = StringBuilder()
        var lastIndex = 0

        for (threat in sorted) {
            if (threat.startIndex < lastIndex) continue // Skip overlapping fragments
            if (threat.startIndex > lastIndex) {
                sb.append(text.substring(lastIndex, threat.startIndex))
            }
            sb.append("[REDACTED: ${threat.threatType.name}]")
            lastIndex = threat.endIndex
        }

        if (lastIndex < text.length) {
            sb.append(text.substring(lastIndex))
        }

        return sb.toString()
    }
}
