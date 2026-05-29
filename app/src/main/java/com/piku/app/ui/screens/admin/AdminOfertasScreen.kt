package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun AdminOfertasScreen(onBack: () -> Unit) {
    var lista by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var nombre by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf("100") }
    var mensaje by remember { mutableStateOf<String?>(null) }

    fun recargar() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val res = RetrofitInstance.api.recompensasComercio()
                @Suppress("UNCHECKED_CAST")
                lista = res["recompensas"] as? List<Map<String, Any?>> ?: emptyList()
            } catch (e: Exception) {
                mensaje = e.message
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ofertas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(puntos, { puntos = it }, label = { Text("Puntos") }, modifier = Modifier.fillMaxWidth())
            BotonPiku("Crear oferta", {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        RetrofitInstance.api.crearRecompensa(
                            mapOf(
                                "nombre" to nombre,
                                "puntosRequeridos" to (puntos.toIntOrNull() ?: 100),
                                "icono" to "🎁"
                            )
                        )
                        mensaje = "Oferta creada"
                        recargar()
                    } catch (e: Exception) {
                        mensaje = e.message
                    }
                }
            }, modifier = Modifier.fillMaxWidth())
            mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            LazyColumn {
                items(lista) { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "${item["nombre"]} — ${item["puntos_requeridos"] ?: item["puntosRequeridos"]} pts",
                            Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
