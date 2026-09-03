package com.example.privacyfirewall.domain

class RegexDetector {
    
    enum class ThreatType {
        EMAIL,
        PHONE,
        CREDIT_CARD,
        API_KEY,
        SSN,
        JWT,
        IP_ADDRESS,
        PERSON, // From AI
        ORGANIZATION, // From AI
        LOCATION // From AI
    }

    data class DetectionResult(
        val threatType: ThreatType,
        val matchedText: String,
        val startIndex: Int,
        val endIndex: Int
    )

    private val patterns = mapOf(
        ThreatType.EMAIL to Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
        ThreatType.PHONE to Regex("(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}"),
        ThreatType.CREDIT_CARD to Regex("\\b(?:\\d[ -]*?){13,16}\\b"),
        ThreatType.API_KEY to Regex("(?i)(api[_-]?key|secret|token)[\"']?\\s*[:=]\\s*[\"']?[A-Za-z0-9_-]{20,}"),
        ThreatType.SSN to Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        ThreatType.JWT to Regex("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
        ThreatType.IP_ADDRESS to Regex("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")
    )

    fun detect(text: String): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        
        patterns.forEach { (type, regex) ->
            regex.findAll(text).forEach { matchResult ->
                results.add(
                    DetectionResult(
                        threatType = type,
                        matchedText = matchResult.value,
                        startIndex = matchResult.range.first,
                        endIndex = matchResult.range.last + 1
                    )
                )
            }
        }
        
        return results
    }
}
