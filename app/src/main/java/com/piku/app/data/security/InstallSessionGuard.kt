package com.piku.app.data.security

import android.content.Context
import android.os.Build
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Evita que una sesión/huella restaurada por backup de Android sobreviva a una reinstalación.
 * Tras desinstalar, [firstInstallTime] cambia; si hay token viejo, se limpia.
 */
object InstallSessionGuard {

    private const val MARKER_DIR = "no_backup"
    private const val MARKER_FILE = "install_id.txt"

    fun apply(context: Context) {
        runBlocking {
            val installId = obtenerInstallId(context)
            val marcadorGuardado = leerMarcador(context)
            val tieneSesion = AuthDataStore.hasSession(context)

            if (tieneSesion && marcadorGuardado != installId) {
                AuthDataStore.clear(context)
            }
            if (marcadorGuardado != installId) {
                guardarMarcador(context, installId)
                AppPreferences.reiniciarPromptUbicacion(context)
            }
        }
    }

    fun currentInstallId(context: Context): String = obtenerInstallId(context)

    private fun obtenerInstallId(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        return "${info.firstInstallTime}_${info.lastUpdateTime}"
    }

    private fun archivoMarcador(context: Context): File {
        val dir = File(context.filesDir, MARKER_DIR)
        return File(dir, MARKER_FILE)
    }

    private fun leerMarcador(context: Context): String? {
        val file = archivoMarcador(context)
        if (!file.exists()) return null
        return file.readText().trim().takeIf { it.isNotEmpty() }
    }

    private fun guardarMarcador(context: Context, installId: String) {
        val file = archivoMarcador(context)
        file.parentFile?.mkdirs()
        file.writeText(installId)
    }
}
