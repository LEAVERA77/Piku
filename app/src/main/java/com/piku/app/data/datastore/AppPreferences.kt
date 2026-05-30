package com.piku.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appDataStore by preferencesDataStore("piku_app")

object AppPreferences {

    private val KEY_UBICACION_INSTALL_ID = stringPreferencesKey("ubicacion_prompt_install_id")

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
}
