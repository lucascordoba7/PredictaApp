package com.lucas.predictaapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
    }

    suspend fun signIn(context: Context): Boolean =
        context.userDataStore.data.first()[KEY_DONE] ?: false

    suspend fun reset(context: Context) {
        context.userDataStore.edit { it.clear() }
    }
}
