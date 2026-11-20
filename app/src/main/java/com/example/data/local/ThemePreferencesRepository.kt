package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.taktPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "takt_preferences"
)

class ThemePreferencesRepository(private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    val isDarkMode: Flow<Boolean> = context.taktPrefsDataStore.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    suspend fun toggleTheme() {
        context.taktPrefsDataStore.edit { prefs ->
            val current = prefs[darkModeKey] ?: true
            prefs[darkModeKey] = !current
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.taktPrefsDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}
