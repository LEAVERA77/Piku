package com.piku.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppSigningHelper {

    /** SHA-1 del certificado con el que está firmada esta instalación (para Google Cloud). */
    fun sha1Instalada(context: Context): String? = runCatching {
        val pm = context.packageManager
        val packageName = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        } ?: return@runCatching null

        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(signatures.first().toByteArray())
        digest.joinToString(":") { b -> "%02X".format(b) }
    }.getOrNull()
}
