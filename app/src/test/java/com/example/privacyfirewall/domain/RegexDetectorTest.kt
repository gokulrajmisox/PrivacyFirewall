package com.example.privacyfirewall.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexDetectorTest {
    private val detector = RegexDetector()

    @Test
    fun testDetectEmail() {
        val results = detector.detect("Send to test@example.com immediately.")
        assertEquals(1, results.size)
        assertEquals(RegexDetector.ThreatType.EMAIL, results[0].threatType)
        assertEquals("test@example.com", results[0].matchedText)
    }

    @Test
    fun testDetectPhone() {
        val results = detector.detect("Call me at 123-456-7890 please.")
        assertEquals(1, results.size)
        assertEquals(RegexDetector.ThreatType.PHONE, results[0].threatType)
        assertEquals("123-456-7890", results[0].matchedText)
    }

    @Test
    fun testDetectSSN() {
        val results = detector.detect("My SSN is 123-45-6789.")
        assertEquals(1, results.size)
        assertEquals(RegexDetector.ThreatType.SSN, results[0].threatType)
        assertEquals("123-45-6789", results[0].matchedText)
    }

    @Test
    fun testDetectApiKey() {
        val results = detector.detect("Here is my key: sk-live-1234567890abcdef1234")
        assertEquals(1, results.size)
        assertEquals(RegexDetector.ThreatType.API_KEY, results[0].threatType)
        assertEquals("sk-live-1234567890abcdef1234", results[0].matchedText)
    }

    @Test
    fun testMultipleThreats() {
        val results = detector.detect("Contact test@test.com or call 555-123-4567")
        assertEquals(2, results.size)
        assertTrue(results.any { it.threatType == RegexDetector.ThreatType.EMAIL })
        assertTrue(results.any { it.threatType == RegexDetector.ThreatType.PHONE })
    }

    @Test
    fun testOverlappingThreatsRedaction() {
        val text = "Contact me test@test.com and 555-123-4567 for info."
        val threats = detector.detect(text)
        
        val sorted = threats.filter { it.startIndex in 0..text.length && it.endIndex in it.startIndex..text.length }
            .sortedWith(compareBy({ it.startIndex }, { -it.endIndex }))

        val sb = StringBuilder()
        var lastIndex = 0

        for (threat in sorted) {
            if (threat.startIndex < lastIndex) {
                if (threat.endIndex > lastIndex) {
                    sb.append("[REDACTED: ${threat.threatType.name}]")
                    lastIndex = threat.endIndex
                }
                continue
            }
            if (threat.startIndex > lastIndex) {
                sb.append(text.substring(lastIndex, threat.startIndex))
            }
            sb.append("[REDACTED: ${threat.threatType.name}]")
            lastIndex = threat.endIndex
        }

        if (lastIndex < text.length) {
            sb.append(text.substring(lastIndex))
        }

        val redacted = sb.toString()
        assertTrue(!redacted.contains("test@test.com"))
        assertTrue(!redacted.contains("555-123-4567"))
        assertEquals("Contact me [REDACTED: EMAIL] and [REDACTED: PHONE] for info.", redacted)
    }
}
