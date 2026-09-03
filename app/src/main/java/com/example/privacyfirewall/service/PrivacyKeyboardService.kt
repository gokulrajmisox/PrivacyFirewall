package com.example.privacyfirewall.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import com.example.privacyfirewall.domain.PrivacyFirewallEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrivacyKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var firewallEngine: PrivacyFirewallEngine
    
    // State for Compose
    private val keyboardState = mutableStateOf("")
    private val warningState = mutableStateOf<String?>(null)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        firewallEngine = PrivacyFirewallEngine(this)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PrivacyKeyboardService)
            setViewTreeViewModelStoreOwner(this@PrivacyKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@PrivacyKeyboardService)
            
            setContent {
                MaterialTheme {
                    KeyboardUI(
                        text = keyboardState.value,
                        warning = warningState.value,
                        onKeyPress = { key -> handleKeyPress(key) },
                        onSend = { handleSend() },
                        onDelete = { handleDelete() },
                        onClearWarning = { warningState.value = null }
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
        keyboardState.value = ""
        warningState.value = null
    }

    private fun handleKeyPress(key: String) {
        keyboardState.value += key
        currentInputConnection?.commitText(key, 1)
        checkText(keyboardState.value)
    }

    private fun handleDelete() {
        if (keyboardState.value.isNotEmpty()) {
            keyboardState.value = keyboardState.value.dropLast(1)
            currentInputConnection?.deleteSurroundingText(1, 0)
            checkText(keyboardState.value)
        }
    }

    private fun handleSend() {
        // Simple enter key action
        currentInputConnection?.performEditorAction(currentInputEditorInfo.imeOptions)
        keyboardState.value = ""
    }

    private fun checkText(text: String) {
        serviceScope.launch {
            val threats = firewallEngine.analyzeText(text)
            if (threats.isNotEmpty()) {
                val threatNames = threats.map { it.threatType.name }.distinct().joinToString(", ")
                warningState.value = "Sensitive Info Detected: $threatNames"
            } else {
                warningState.value = null
            }
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
    text: String,
    warning: String?,
    onKeyPress: (String) -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
    onClearWarning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
        if (warning != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("🛡️ PrivacyFirewall", style = MaterialTheme.typography.titleSmall)
                    Text(warning, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onClearWarning) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Extremely simplified keyboard layout for demo
        val keys = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("z", "x", "c", "v", "b", "n", "m"),
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", ".", " ")
        )
        
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Button(onClick = { onKeyPress(key) }, modifier = Modifier.weight(1f).padding(2.dp)) {
                        Text(key)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onDelete, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("DEL")
            }
            Button(onClick = onSend, modifier = Modifier.weight(1f).padding(2.dp)) {
                Text("SEND")
            }
        }
    }
}
