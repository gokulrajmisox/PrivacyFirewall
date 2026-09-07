package com.example.privacyfirewall.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privacyfirewall.data.SettingsRepository
import com.example.privacyfirewall.domain.PrivacyFirewallEngine
import com.example.privacyfirewall.domain.RegexDetector
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val engine = remember { PrivacyFirewallEngine(context) }

    // Dynamic metrics from DataStore
    val isProtectionEnabled by settingsRepository.isProtectionEnabled.collectAsState(initial = true)
    val protectedApps by settingsRepository.protectedApps.collectAsState(initial = SettingsRepository.DEFAULT_PROTECTED_APPS)
    val threatsBlocked by settingsRepository.threatsBlockedCount.collectAsState(initial = 0)

    // Interactive Demo / Sandbox State
    var testInputText by remember { mutableStateOf("") }
    var detectedThreats by remember { mutableStateOf<List<RegexDetector.DetectionResult>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🛡️ PrivacyFirewall",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Zero-Leakage On-Device AI Shield",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live Protection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProtectionEnabled) Color(0xFF1B5E20) else Color(0xFF37474F)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isProtectionEnabled) Color(0xFF00E676) else Color.LightGray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProtectionEnabled) "Protection is ACTIVE" else "Protection is PAUSED",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real Dynamic Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        count = threatsBlocked.toString(),
                        label = "Threats Blocked"
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    MetricItem(
                        count = "${protectedApps.size}",
                        label = "Protected Apps"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Neural Model Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🧠", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BERT-NER Neural Engine (INT8)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (engine.isModelReady) "Status: Ready • 100% Offline (ONNX)" else "Status: Initializing weights...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (engine.isModelReady) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var analysisJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        // Interactive Sandbox for Judges
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧪 Live Detection Sandbox",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Test on-device detection without leaving the app:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testInputText,
                    onValueChange = {
                        testInputText = it
                        analysisJob?.cancel()
                        analysisJob = coroutineScope.launch {
                            isAnalyzing = true
                            detectedThreats = engine.analyzeText(it)
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Type prompt or paste secrets") },
                    placeholder = { Text("e.g. key is sk-live-1234567890abcdef and SSN is 123-45-6789") },
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            testInputText = "My name is Alice Smith, key is sk-live-9876543210abcdef1234, SSN 123-45-6789"
                            coroutineScope.launch {
                                isAnalyzing = true
                                detectedThreats = engine.analyzeText(testInputText)
                                isAnalyzing = false
                            }
                        }
                    ) {
                        Text("Fill Sample PII/Key", fontSize = 12.sp)
                    }

                    if (detectedThreats.isNotEmpty()) {
                        Button(
                            onClick = {
                                val sanitized = engine.redactText(testInputText, detectedThreats)
                                val count = detectedThreats.size
                                testInputText = sanitized
                                detectedThreats = emptyList()
                                coroutineScope.launch {
                                    settingsRepository.incrementThreatsBlocked(count)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text("🛡️ REDACT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Show detection results
                if (detectedThreats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚠️ Detected ${detectedThreats.size} Threat(s):",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            detectedThreats.forEach { t ->
                                Text(
                                    text = "• [${t.threatType.name}] \"${t.matchedText}\"",
                                    color = Color(0xFFB71C1C),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enable Keyboard Action Button
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⌨️ Enable Privacy Keyboard in Android Settings")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Firewall Settings Navigation
        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⚙️ Firewall Settings")
        }
    }
}

@Composable
fun MetricItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
