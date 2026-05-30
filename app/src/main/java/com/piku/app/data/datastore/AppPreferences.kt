package com.piku.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appDataStore by preferencesDataStore("piku_app")

object AppPreferences {

    private val KEY_UBICACION_INSTALL_ID = stringPreferencesKey("ubicacion_prompt_install_id")
    private val KEY_ONBOARDING_TIPO = booleanPreferencesKey("onboarding_tipo_hecho")
    private val KEY_ROL_PREFERIDO = stringPreferencesKey("rol_preferido_inicio")

    const val ROL_CLIENTE = "cliente"
    const val ROL_COMERCIO = "comercio"

    suspend fun debeSolicitarUbicacion(context: Context, installId: String): Boolean {
        val guardado = context.appDataStore.data.first()[KEY_UBICACION_INSTALL_ID]
        return guardado != installId
    }

    suspend fun marcarUbicacionSolicitada(context: Context, installId: String) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_UBICACION_INSTALL_ID] = installId
        }
    }

    suspend fun reiniciarPromptUbicacion(context: Context) {
        context.appDataStore.edit { prefs ->
            prefs.remove(KEY_UBICACION_INSTALL_ID)
        }
    }

    suspend fun necesitaElegirTipoUsuario(context: Context): Boolean {
        val prefs = context.appDataStore.data.first()
        return prefs[KEY_ONBOARDING_TIPO] != true
    }

    suspend fun guardarTipoUsuarioInicio(context: Context, rol: String) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_TIPO] = true
            prefs[KEY_ROL_PREFERIDO] = rol
        }
    }

    suspend fun rolPreferidoInicio(context: Context): String? =
        context.appDataStore.data.first()[KEY_ROL_PREFERIDO]

    suspend fun reiniciarOnboarding(context: Context) {
        context.appDataStore.edit { prefs ->
            prefs.remove(KEY_ONBOARDING_TIPO)
            prefs.remove(KEY_ROL_PREFERIDO)
        }
    }
}
