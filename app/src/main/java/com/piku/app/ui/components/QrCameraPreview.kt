package com.piku.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private const val TAG = "PikuQrScanner"

@Composable
fun QrCameraPreview(
    linternaActiva: Boolean,
    escaneoHabilitado: Boolean,
    onCodigoDetectado: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraControl by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val analizador = remember { Executors.newSingleThreadExecutor() }
    var ultimoCodigo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(linternaActiva) {
        cameraControl?.cameraControl?.enableTorch(linternaActiva)
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val escaner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(analizador) { imageProxy ->
                    if (!escaneoHabilitado) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        escaner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    when (barcode.valueType) {
                                        Barcode.TYPE_TEXT,
                                        Barcode.TYPE_URL,
                                        Barcode.TYPE_WIFI,
                                        Barcode.TYPE_CONTACT_INFO -> {
                                            barcode.rawValue?.let { valor ->
                                                if (ultimoCodigo != valor) {
                                                    ultimoCodigo = valor
                                                    vibrar(context)
                                                    Log.d(TAG, "QR escaneado: $valor")
                                                    onCodigoDetectado(valor)
                                                }
                                            }
                                        }
                                        else -> barcode.rawValue?.let { valor ->
                                            if (ultimoCodigo != valor) {
                                                ultimoCodigo = valor
                                                vibrar(context)
                                                Log.d(TAG, "Código escaneado: $valor")
                                                onCodigoDetectado(valor)
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    cameraControl = camera
                    camera.cameraControl.enableTorch(linternaActiva)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar cámara", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )

    DisposableEffect(Unit) {
        onDispose { analizador.shutdown() }
    }
}

private fun vibrar(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(120)
    }
}
