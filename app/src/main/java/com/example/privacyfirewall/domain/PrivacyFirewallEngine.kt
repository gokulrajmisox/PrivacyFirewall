package com.example.privacyfirewall.domain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyFirewallEngine(private val context: Context) {
    
    private val regexDetector = RegexDetector()
    private val aiDetector = AiDetector(context)

    suspend fun analyzeText(text: String): List<RegexDetector.DetectionResult> {
        return withContext(Dispatchers.Default) {
            val results = mutableListOf<RegexDetector.DetectionResult>()
            
            // Run regex detection
            results.addAll(regexDetector.detect(text))
            
            // Run AI detection
            results.addAll(aiDetector.detect(text))
            
            // Remove duplicates and overlapping results if necessary
            // For now, just return all
            results.distinctBy { it.startIndex to it.endIndex to it.threatType }
        }
    }
}
