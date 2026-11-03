package com.lucas.predictaapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

/**
 * Preferencias del teléfono, no de la cuenta: viven aparte de `user_prefs` para que cerrar
 * sesión (que borra ese store) no las pise.
 */
object AppSettings {
    private val Context.settingsStore by preferencesDataStore("app_settings")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    fun themeMode(context: Context): Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        prefs[KEY_THEME]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsStore.edit { it[KEY_THEME] = mode.name }
    }
}
