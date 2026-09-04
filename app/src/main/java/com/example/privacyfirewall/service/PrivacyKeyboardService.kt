package com.example.privacyfirewall.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.privacyfirewall.data.SettingsRepository
import com.example.privacyfirewall.domain.PrivacyFirewallEngine
import com.example.privacyfirewall.domain.RegexDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PrivacyKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var firewallEngine: PrivacyFirewallEngine
    private lateinit var settingsRepository: SettingsRepository
    
    // State for Compose UI
    private val keyboardTextState = mutableStateOf("")
    private val warningState = mutableStateOf<String?>(null)
    private val statusMessageState = mutableStateOf<String?>(null)
    private var detectedThreats: List<RegexDetector.DetectionResult> = emptyList()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        firewallEngine = PrivacyFirewallEngine(this)
        settingsRepository = SettingsRepository(this)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PrivacyKeyboardService)
            setViewTreeViewModelStoreOwner(this@PrivacyKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@PrivacyKeyboardService)
            
            setContent {
                MaterialTheme {
                    KeyboardUI(
                        warning = warningState.value,
                        statusMessage = statusMessageState.value,
                        onKeyPress = { key -> handleKeyPress(key) },
                        onSend = { handleSend() },
                        onDelete = { handleDelete() },
                        onRedact = { handleRedact() },
                        onClearWarning = { 
                            warningState.value = null
                            detectedThreats = emptyList()
                        }
                    )
                }
            }
        }
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardTextState.value = ""
        warningState.value = null
        statusMessageState.value = null
        detectedThreats = emptyList()
    }

    private fun handleKeyPress(key: String) {
        keyboardTextState.value += key
        currentInputConnection?.commitText(key, 1)
        checkCurrentInput()
    }

    private fun handleDelete() {
        if (keyboardTextState.value.isNotEmpty()) {
            keyboardTextState.value = keyboardTextState.value.dropLast(1)
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        checkCurrentInput()
    }

    private fun handleSend() {
        val imeOptions = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_DONE
        currentInputConnection?.performEditorAction(imeOptions)
        keyboardTextState.value = ""
        warningState.value = null
        statusMessageState.value = null
        detectedThreats = emptyList()
    }

    private fun checkCurrentInput() {
        // Read directly from InputConnection or fallback to local keystroke buffer
        val textBefore = currentInputConnection?.getTextBeforeCursor(1000, 0)?.toString()
        val textToAnalyze = if (!textBefore.isNullOrEmpty()) textBefore else keyboardTextState.value

        if (textToAnalyze.isBlank()) {
            warningState.value = null
            detectedThreats = emptyList()
            return
        }

        serviceScope.launch {
            val threats = firewallEngine.analyzeText(textToAnalyze)
            if (threats.isNotEmpty()) {
                detectedThreats = threats
                val threatNames = threats.map { it.threatType.name }.distinct().joinToString(", ")
                warningState.value = "⚠️ Sensitive Data Detected: $threatNames"
            } else {
                warningState.value = null
                detectedThreats = emptyList()
            }
        }
    }

    private fun handleRedact() {
        val textBefore = currentInputConnection?.getTextBeforeCursor(1000, 0)?.toString()
        val originalText = if (!textBefore.isNullOrEmpty()) textBefore else keyboardTextState.value

        if (originalText.isEmpty() || detectedThreats.isEmpty()) return

        val sanitizedText = firewallEngine.redactText(originalText, detectedThreats)
        val threatsCount = detectedThreats.size

        // Replace the text in the host application's input field
        currentInputConnection?.let { ic ->
            ic.beginBatchEdit()
            ic.deleteSurroundingText(originalText.length, 0)
            ic.commitText(sanitizedText, 1)
            ic.endBatchEdit()
        }

        keyboardTextState.value = sanitizedText
        warningState.value = null
        detectedThreats = emptyList()
        statusMessageState.value = "✅ $threatsCount Threat(s) Redacted & Sanitized!"

        // Update real DataStore metric
        serviceScope.launch {
            settingsRepository.incrementThreatsBlocked(threatsCount)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceJob.cancel()
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

@Composable
fun KeyboardUI(
    warning: String?,
    statusMessage: String?,
    onKeyPress: (String) -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onRedact: () -> Unit,
    onClearWarning: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2124))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Redaction / Warning Alert Banner
        if (warning != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "🛡️ PrivacyFirewall Alert",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = warning,
                        color = Color(0xFFFFEBEE),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onClearWarning) {
                            Text("Ignore", color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRedact,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text("🛡️ REDACT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (statusMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text(
                    text = statusMessage,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // QWERTY Key Rows
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
        val numbersRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val symbolsRow = listOf("-", "_", "=", ":", "/", ".", "@")

        // Numbers Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            numbersRow.forEach { key ->
                KeyButton(label = key, modifier = Modifier.weight(1f)) { onKeyPress(key) }
            }
        }

        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row1.forEach { key ->
                val label = if (isShifted) key.uppercase() else key
                KeyButton(label = label, modifier = Modifier.weight(1f)) { onKeyPress(label) }
            }
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row2.forEach { key ->
                val label = if (isShifted) key.uppercase() else key
                KeyButton(label = label, modifier = Modifier.weight(1f)) { onKeyPress(label) }
            }
        }

        // Row 3 with Shift & Backspace
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { isShifted = !isShifted },
                modifier = Modifier.weight(1.3f).padding(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isShifted) Color(0xFF00E676) else Color(0xFF424242)),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("⇧", fontSize = 14.sp, color = if (isShifted) Color.Black else Color.White)
            }

            row3.forEach { key ->
                val label = if (isShifted) key.uppercase() else key
                KeyButton(label = label, modifier = Modifier.weight(1f)) { onKeyPress(label) }
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1.3f).padding(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("⌫", fontSize = 14.sp, color = Color.White)
            }
        }

        // Common symbols row for API keys & emails
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            symbolsRow.forEach { sym ->
                KeyButton(label = sym, modifier = Modifier.weight(1f), bgColor = Color(0xFF37474F)) { onKeyPress(sym) }
            }
        }

        // Space & Send Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { onKeyPress(" ") },
                modifier = Modifier.weight(3f).padding(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
            ) {
                Text("SPACE", fontSize = 12.sp, color = Color.White)
            }

            Button(
                onClick = onSend,
                modifier = Modifier.weight(1.2f).padding(1.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("SEND", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF2C2F33),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(1.dp).height(38.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
