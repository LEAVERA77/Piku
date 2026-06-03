package com.piku.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.piku.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore("piku_app")

object AppPreferences {

    private val KEY_UBICACION_INSTALL_ID = stringPreferencesKey("ubicacion_prompt_install_id")
    private val KEY_ONBOARDING_TIPO = booleanPreferencesKey("onboarding_tipo_hecho")
    private val KEY_ROL_PREFERIDO = stringPreferencesKey("rol_preferido_inicio")
    private val KEY_TEMA = stringPreferencesKey("theme_mode")
    private val KEY_ULTIMO_HITO = intPreferencesKey("ultimo_hito_celebrado")

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

    fun temaFlow(context: Context): Flow<ThemeMode> =
        context.appDataStore.data.map { prefs ->
            ThemeMode.fromId(prefs[KEY_TEMA])
        }

    suspend fun guardarTema(context: Context, mode: ThemeMode) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_TEMA] = mode.id
        }
    }

    suspend fun ultimoHitoCelebrado(context: Context): Int =
        context.appDataStore.data.first()[KEY_ULTIMO_HITO] ?: 0

    suspend fun marcarHitoCelebrado(context: Context, hito: Int) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_ULTIMO_HITO] = hito
        }
    }
}
