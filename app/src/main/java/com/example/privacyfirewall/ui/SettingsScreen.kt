package com.example.privacyfirewall.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val ruleStates = remember {
        mutableStateMapOf(
            "Email" to true,
            "Phone Number" to true,
            "Credit Card" to true,
            "API Keys" to true,
            "Names (AI)" to true,
            "Organizations (AI)" to true,
            "Locations (AI)" to true
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firewall Settings") },
                navigationIcon = {
                    Button(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Detection Rules", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            ruleStates.keys.forEach { rule ->
                val isChecked = ruleStates[rule] ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(rule)
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { newState ->
                            ruleStates[rule] = newState
                        }
                    )
                }
            }
        }
    }
}
