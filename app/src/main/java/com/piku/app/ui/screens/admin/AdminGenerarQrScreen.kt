package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.ui.components.BotonPiku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGenerarQrScreen(onBack: () -> Unit) {
    var monto by remember { mutableStateOf("1000") }
    var resultado by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generar QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto de la compra") },
                modifier = Modifier.fillMaxWidth()
            )
            BotonPiku("Generar QR", {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val res = RetrofitInstance.api.generarQr(
                            mapOf("monto" to (monto.toDoubleOrNull() ?: 0.0))
                        )
                        @Suppress("UNCHECKED_CAST")
                        val qr = res["qr"] as? Map<*, *>
                        resultado = "Código: ${qr?.get("codigo")}\nExpira: ${qr?.get("expira_at")}"
                    } catch (e: Exception) {
                        resultado = e.message
                    }
                }
            }, modifier = Modifier.fillMaxWidth())
            resultado?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
