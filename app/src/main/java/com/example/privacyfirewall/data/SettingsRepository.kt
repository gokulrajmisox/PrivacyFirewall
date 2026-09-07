package com.example.privacyfirewall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val IS_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val PROTECTED_APPS = stringSetPreferencesKey("protected_apps")
        val THREATS_BLOCKED = intPreferencesKey("threats_blocked")
        
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

    val threatsBlockedCount: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[THREATS_BLOCKED] ?: 0
        }

    suspend fun incrementThreatsBlocked(count: Int = 1) {
        dataStore.edit { preferences ->
            val current = preferences[THREATS_BLOCKED] ?: 0
            preferences[THREATS_BLOCKED] = current + count
        }
    }

    suspend fun resetThreatsBlocked() {
        dataStore.edit { preferences ->
            preferences[THREATS_BLOCKED] = 0
        }
    }
}
