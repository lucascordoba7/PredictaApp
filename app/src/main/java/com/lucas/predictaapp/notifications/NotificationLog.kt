package com.lucas.predictaapp.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Registro de notificaciones ya disparadas (por id determinista) para no repetirlas.
 * Vive en su propio DataStore para no mezclarse con el perfil.
 */
object NotificationLog {
    private val Context.store by preferencesDataStore("notif_log")
    private val FIRED = stringSetPreferencesKey("fired_ids")

    suspend fun wasFired(context: Context, id: String): Boolean =
        context.store.data.first()[FIRED]?.contains(id) == true

    suspend fun markFired(context: Context, id: String) {
        context.store.edit { prefs ->
            val set = prefs[FIRED]?.toMutableSet() ?: mutableSetOf()
            set.add(id)
            prefs[FIRED] = set
        }
    }
}
