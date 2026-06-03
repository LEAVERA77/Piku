package com.piku.app.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File

/**
 * Galería o cámara; la compresión JPEG (estilo GestorNova) se aplica al subir vía [ImageUploadHelper].
 */
@Composable
fun rememberImagePicker(
    onPicked: (Uri) -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onPicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) cameraUri.value?.let(onPicked) }

    return remember {
        {
            val sheet = android.app.AlertDialog.Builder(context)
                .setTitle("Elegir foto")
                .setItems(arrayOf("Cámara", "Galería")) { _, which ->
                    when (which) {
                        0 -> {
                            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            cameraUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                        1 -> galleryLauncher.launch("image/*")
                    }
                }
                .create()
            sheet.show()
        }
    }
}
