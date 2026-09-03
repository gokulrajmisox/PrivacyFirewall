package com.example.privacyfirewall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val IS_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val PROTECTED_APPS = stringSetPreferencesKey("protected_apps")
        
        // Default protected apps
        val DEFAULT_PROTECTED_APPS = setOf(
            "com.openai.chatgpt",
            "com.google.android.apps.bard",
            "com.whatsapp"
        )
    }

    val isProtectionEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[IS_PROTECTION_ENABLED] ?: true
        }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_PROTECTION_ENABLED] = enabled
        }
    }

    val protectedApps: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PROTECTED_APPS] ?: DEFAULT_PROTECTED_APPS
        }

    suspend fun setProtectedApps(apps: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PROTECTED_APPS] = apps
        }
    }
}
