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
        ThreatType.EMAIL to Regex("\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b"),
        ThreatType.PHONE to Regex("\\b(?:\\+\\d{1,2}[\\s.-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\\b"),
        ThreatType.CREDIT_CARD to Regex("\\b(?:\\d[ -]*?){13,16}\\b"),
        ThreatType.API_KEY to Regex("(?i)(?:api[_-]?key|secret|token|bearer)[\"']?\\s*[:=]\\s*[\"']?([A-Za-z0-9_.-]{16,})[\"']?|\\b(sk-[a-zA-Z0-9_-]{16,}|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9_]{30,})\\b"),
        ThreatType.SSN to Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        ThreatType.JWT to Regex("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
        ThreatType.IP_ADDRESS to Regex("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")
    )

    fun detect(text: String): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        
        patterns.forEach { (type, regex) ->
            regex.findAll(text).forEach { matchResult ->
                val g1 = if (matchResult.groups.size > 1) matchResult.groups[1] else null
                val g2 = if (matchResult.groups.size > 2) matchResult.groups[2] else null
                val matchedGroup = g1 ?: g2

                val (matchedStr, start, end) = if (matchedGroup != null) {
                    Triple(matchedGroup.value, matchedGroup.range.first, matchedGroup.range.last + 1)
                } else {
                    Triple(matchResult.value, matchResult.range.first, matchResult.range.last + 1)
                }

                results.add(
                    DetectionResult(
                        threatType = type,
                        matchedText = matchedStr,
                        startIndex = start,
                        endIndex = end
                    )
                )
            }
        }
        
        // Deduplicate overlapping / redundant matches
        return results.distinctBy { "${it.threatType}-${it.startIndex}-${it.endIndex}" }
            .filter { r1 ->
                results.none { r2 ->
                    r2 !== r1 && r2.threatType == r1.threatType &&
                    r2.startIndex <= r1.startIndex && r2.endIndex >= r1.endIndex &&
                    (r2.endIndex - r2.startIndex) > (r1.endIndex - r1.startIndex)
                }
            }
    }
}
