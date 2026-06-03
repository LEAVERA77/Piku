package com.piku.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

private fun permisosGaleria(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun tienePermiso(context: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

private fun faltanPermisos(context: Context, permisos: Array<String>): Array<String> =
    permisos.filterNot { tienePermiso(context, it) }.toTypedArray()

/**
 * Galería o cámara con permisos de cámara y almacenamiento.
 */
@Composable
fun rememberImagePicker(
    onPicked: (Uri) -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val accionPendiente = remember { mutableStateOf<(() -> Unit)?>(null) }

    val permisosCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) accionPendiente.value?.invoke()
        accionPendiente.value = null
    }

    val permisosGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) accionPendiente.value?.invoke()
        accionPendiente.value = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onPicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) cameraUri.value?.let(onPicked) }

    fun abrirCamara() {
        val abrir = {
            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri.value = uri
            cameraLauncher.launch(uri)
        }
        if (tienePermiso(context, Manifest.permission.CAMERA)) {
            abrir()
        } else {
            accionPendiente.value = abrir
            permisosCamara.launch(Manifest.permission.CAMERA)
        }
    }

    fun abrirGaleria() {
        val abrir = { galleryLauncher.launch("image/*") }
        val faltantes = faltanPermisos(context, permisosGaleria())
        if (faltantes.isEmpty()) {
            abrir()
        } else {
            accionPendiente.value = abrir
            permisosGaleriaLauncher.launch(faltantes)
        }
    }

    return remember {
        {
            android.app.AlertDialog.Builder(context)
                .setTitle("Elegir foto")
                .setItems(arrayOf("Cámara", "Galería")) { _, which ->
                    when (which) {
                        0 -> abrirCamara()
                        1 -> abrirGaleria()
                    }
                }
                .create()
                .show()
        }
    }
}
