package com.piku.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore("piku_auth")

object AuthDataStore {

    @Volatile
    var cachedToken: String? = null

    private val KEY_TOKEN = stringPreferencesKey("auth_token")
    private val KEY_EMAIL = stringPreferencesKey("user_email")
    private val KEY_ROL = stringPreferencesKey("user_rol")
    private val KEY_NOMBRE = stringPreferencesKey("user_nombre")
    private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")

    suspend fun saveSession(
        context: Context,
        token: String,
        email: String,
        rol: String,
        nombre: String,
        biometricEnabled: Boolean = false
    ) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_EMAIL] = email
            prefs[KEY_ROL] = rol
            prefs[KEY_NOMBRE] = nombre
            prefs[KEY_BIOMETRIC] = biometricEnabled
        }
        cachedToken = token
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.authDataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun token(context: Context): String? {
        val t = context.authDataStore.data.first()[KEY_TOKEN]
        cachedToken = t
        return t
    }

    suspend fun warmCache(context: Context) {
        token(context)
    }

    fun tokenSync(): String? = cachedToken

    suspend fun rol(context: Context): String? =
        context.authDataStore.data.first()[KEY_ROL]

    fun biometricEnabledFlow(context: Context): Flow<Boolean> =
        context.authDataStore.data.map { it[KEY_BIOMETRIC] ?: false }

    suspend fun isBiometricEnabled(context: Context): Boolean =
        context.authDataStore.data.first()[KEY_BIOMETRIC] ?: false

    suspend fun hasSession(context: Context): Boolean =
        !token(context).isNullOrBlank()

    suspend fun clear(context: Context) {
        context.authDataStore.edit { it.clear() }
        cachedToken = null
    }
}
