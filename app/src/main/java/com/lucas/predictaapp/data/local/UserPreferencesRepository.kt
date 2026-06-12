package com.lucas.predictaapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lucas.predictaapp.data.model.Profile
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserSetup(
    val name: String,
    val email: String,
    val income: Int,
    val paydayDay: Int,
    val fixedMonthly: Int,
)

object UserPreferencesRepository {
    private val KEY_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_EMAIL = stringPreferencesKey("user_email")
    private val KEY_INCOME = intPreferencesKey("user_income")
    private val KEY_PAYDAY_DAY = intPreferencesKey("user_payday_day")
    private val KEY_FIXED_MONTHLY = intPreferencesKey("user_fixed_monthly")

    /**
     * Por ahora: usuario fijo, sin registro real. Se siembra al arrancar si no hay perfil
     * (ni local ni en la nube). Quitar cuando se implemente Auth / multiusuario.
     */
    val DEFAULT_SETUP = UserSetup(
        name = "Lucas",
        email = "lucascordoba77@gmail.com",
        income = 1_500_000,
        paydayDay = 10,
        fixedMonthly = 0,
    )

    fun isOnboardingDone(context: Context): Flow<Boolean> =
        context.userDataStore.data.map { it[KEY_DONE] ?: false }

    fun getUserSetup(context: Context): Flow<UserSetup?> =
        context.userDataStore.data.map { prefs ->
            val name = prefs[KEY_NAME] ?: return@map null
            UserSetup(
                name = name,
                email = prefs[KEY_EMAIL] ?: "",
                income = prefs[KEY_INCOME] ?: 0,
                paydayDay = prefs[KEY_PAYDAY_DAY] ?: 1,
                fixedMonthly = prefs[KEY_FIXED_MONTHLY] ?: 0,
            )
        }

    suspend fun completeOnboarding(context: Context, setup: UserSetup) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_DONE] = true
            prefs[KEY_NAME] = setup.name
            prefs[KEY_EMAIL] = setup.email
            prefs[KEY_INCOME] = setup.income
            prefs[KEY_PAYDAY_DAY] = setup.paydayDay
            prefs[KEY_FIXED_MONTHLY] = setup.fixedMonthly
        }
        pushProfile(setup)
    }

    /** Respalda el perfil en Supabase (tabla `profiles`, fila única id="me"). */
    suspend fun pushProfile(setup: UserSetup) {
        try {
            SupabaseProvider.client?.from("profiles")?.upsert(
                Profile(
                    name = setup.name,
                    email = setup.email,
                    income = setup.income,
                    paydayDay = setup.paydayDay,
                    fixedMonthly = setup.fixedMonthly,
                ),
            )
        } catch (e: Exception) { SyncErrors.report("profiles.upsert", e) }
    }

    /**
     * Baja el perfil de Supabase a DataStore si existe. Llamar al arrancar, antes de
     * decidir onboarding: si hay perfil en la nube, marca onboarding como hecho y rellena
     * los datos, así tras reinstalar no te pide registrarte. Con timeout para no colgar
     * la pantalla de carga si no hay red.
     */
    suspend fun pullProfile(context: Context) {
        val remote = try {
            withTimeoutOrNull(4000) {
                SupabaseProvider.client?.from("profiles")?.select()?.decodeSingleOrNull<Profile>()
            }
        } catch (e: Exception) {
            SyncErrors.report("profiles.pull", e)
            null
        } ?: return

        context.userDataStore.edit { prefs ->
            prefs[KEY_DONE] = true
            prefs[KEY_NAME] = remote.name
            prefs[KEY_EMAIL] = remote.email
            prefs[KEY_INCOME] = remote.income
            prefs[KEY_PAYDAY_DAY] = remote.paydayDay
            prefs[KEY_FIXED_MONTHLY] = remote.fixedMonthly
        }
    }

    suspend fun signIn(context: Context): Boolean =
        context.userDataStore.data.first()[KEY_DONE] ?: false

    suspend fun reset(context: Context) {
        context.userDataStore.edit { it.clear() }
    }
}
