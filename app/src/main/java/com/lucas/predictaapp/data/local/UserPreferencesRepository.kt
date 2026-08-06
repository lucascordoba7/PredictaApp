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
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_NAME = stringPreferencesKey("user_name")
    private val KEY_EMAIL = stringPreferencesKey("user_email")
    private val KEY_INCOME = intPreferencesKey("user_income")
    private val KEY_PAYDAY_DAY = intPreferencesKey("user_payday_day")
    private val KEY_FIXED_MONTHLY = intPreferencesKey("user_fixed_monthly")

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

    /** Guarda la cuenta activa en disco y en [Session], para que los repos la vean. */
    private suspend fun persistSession(context: Context, userId: String, setup: UserSetup) {
        Session.open(userId)
        context.userDataStore.edit { prefs ->
            prefs[KEY_DONE] = true
            prefs[KEY_USER_ID] = userId
            prefs[KEY_NAME] = setup.name
            prefs[KEY_EMAIL] = setup.email
            prefs[KEY_INCOME] = setup.income
            prefs[KEY_PAYDAY_DAY] = setup.paydayDay
            prefs[KEY_FIXED_MONTHLY] = setup.fixedMonthly
        }
    }

    /**
     * Rehidrata [Session] desde disco al arrancar. Devuelve true si había cuenta abierta.
     * Sin esto los repos arrancarían sin userId y no filtrarían nada.
     */
    suspend fun restoreSession(context: Context): Boolean {
        val userId = context.userDataStore.data.first()[KEY_USER_ID] ?: return false
        if (userId.isBlank()) return false
        Session.open(userId)
        return true
    }

    /** Busca una cuenta por email en Supabase. null = no existe (o no hubo red). */
    private suspend fun findProfileByEmail(email: String): Profile? = try {
        withTimeoutOrNull(8000) {
            SupabaseProvider.client?.from("profiles")
                ?.select { filter { ilike("email", email.trim()) } }
                ?.decodeList<Profile>()
                ?.firstOrNull()
        }
    } catch (e: Exception) {
        SyncErrors.report("profiles.findByEmail", e)
        null
    }

    sealed class AuthResult {
        data object Ok : AuthResult()
        data object NotFound : AuthResult()
        data object AlreadyExists : AuthResult()
        data object NoConnection : AuthResult()
    }

    /** Chequeo temprano en el alta: ¿este email ya tiene cuenta? */
    suspend fun emailAvailable(email: String): AuthResult {
        if (SupabaseProvider.client == null) return AuthResult.NoConnection
        return if (findProfileByEmail(email) != null) AuthResult.AlreadyExists else AuthResult.Ok
    }

    /**
     * Login por email contra Supabase (no contra el DataStore local): esto es lo que
     * permite entrar con la misma cuenta en un dispositivo nuevo y recuperar los datos.
     * No hay password — ver la nota en [Session].
     */
    suspend fun signIn(context: Context, email: String): AuthResult {
        if (SupabaseProvider.client == null) return AuthResult.NoConnection
        val profile = findProfileByEmail(email) ?: return AuthResult.NotFound
        persistSession(
            context,
            profile.id,
            UserSetup(
                name = profile.name,
                email = profile.email,
                income = profile.income,
                paydayDay = profile.paydayDay,
                fixedMonthly = profile.fixedMonthly,
            ),
        )
        return AuthResult.Ok
    }

    /**
     * Alta de cuenta nueva. Rechaza si el email ya existe: la regla es que registrarse
     * nunca pise los datos de una cuenta existente — para eso está [signIn].
     */
    suspend fun register(context: Context, setup: UserSetup): AuthResult {
        if (SupabaseProvider.client == null) return AuthResult.NoConnection
        if (findProfileByEmail(setup.email) != null) return AuthResult.AlreadyExists
        val userId = Session.newUserId()
        persistSession(context, userId, setup)
        pushProfile(setup)
        return AuthResult.Ok
    }

    /** Completa el onboarding de una cuenta ya creada (paso de ingreso/día de cobro). */
    suspend fun completeOnboarding(context: Context, setup: UserSetup) {
        persistSession(context, Session.userId, setup)
        pushProfile(setup)
    }

    /** Respalda el perfil de la cuenta activa en Supabase. */
    suspend fun pushProfile(setup: UserSetup) {
        if (!Session.isActive) return
        try {
            SupabaseProvider.client?.from("profiles")?.upsert(
                Profile(
                    id = Session.userId,
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
     * Refresca el perfil de la cuenta activa desde Supabase. Solo corre si ya hay sesión:
     * ya no puede "adivinar" la cuenta, porque hay más de una fila en la tabla.
     * Con timeout para no colgar la pantalla de carga si no hay red.
     */
    suspend fun pullProfile(context: Context) {
        if (!Session.isActive) return
        val remote = try {
            withTimeoutOrNull(4000) {
                SupabaseProvider.client?.from("profiles")
                    ?.select { filter { eq("id", Session.userId) } }
                    ?.decodeSingleOrNull<Profile>()
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

    suspend fun reset(context: Context) {
        Session.close()
        context.userDataStore.edit { it.clear() }
    }
}
