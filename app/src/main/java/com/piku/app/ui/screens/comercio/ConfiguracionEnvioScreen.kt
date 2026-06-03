package com.piku.app.ui.screens.comercio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.ConfiguracionEnviosRequest
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.BotonPiku
import com.piku.app.utils.TelefonoUtil
import kotlinx.coroutines.launch

private enum class ModoEnvio { GRATIS_SIEMPRE, CON_COSTO, GRATIS_DESDE_MONTO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionEnvioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { ComercioRepository(context) }
    val scope = rememberCoroutineScope()

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mensajeOk by remember { mutableStateOf<String?>(null) }

    var realizaEnvios by remember { mutableStateOf(false) }
    var modoEnvio by remember { mutableStateOf(ModoEnvio.GRATIS_SIEMPRE) }
    var costoEnvio by remember { mutableStateOf("") }
    var minimoCompra by remember { mutableStateOf("") }
    var telefonoContacto by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cargando = true
        try {
            val cfg = repo.obtenerConfigEnvios()
            realizaEnvios = cfg.realizaEnvios
            telefonoContacto = cfg.telefonoContacto.orEmpty()
            modoEnvio = when {
                cfg.envioGratis -> ModoEnvio.GRATIS_SIEMPRE
                cfg.envioMinimoCompra != null && cfg.envioMinimoCompra > 0 -> ModoEnvio.GRATIS_DESDE_MONTO
                else -> ModoEnvio.CON_COSTO
            }
            costoEnvio = cfg.costoEnvio.takeIf { it > 0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
            minimoCompra = cfg.envioMinimoCompra?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty()
        } catch (e: Exception) {
            error = e.message
        } finally {
            cargando = false
        }
    }

    fun guardar() {
        if (realizaEnvios) {
            TelefonoUtil.validarComercio(telefonoContacto)?.let {
                error = it
                return
            }
            when (modoEnvio) {
                ModoEnvio.CON_COSTO -> {
                    val c = costoEnvio.replace(',', '.').toDoubleOrNull()
                    if (c == null || c < 0) {
                        error = "Ingresá un costo de envío válido"
                        return
                    }
                }
                ModoEnvio.GRATIS_DESDE_MONTO -> {
                    val m = minimoCompra.replace(',', '.').toDoubleOrNull()
                    if (m == null || m <= 0) {
                        error = "Ingresá el monto mínimo de compra"
                        return
                    }
                }
                ModoEnvio.GRATIS_SIEMPRE -> Unit
            }
        }
        scope.launch {
            guardando = true
            error = null
            mensajeOk = null
            try {
                val costo = costoEnvio.replace(',', '.').toDoubleOrNull() ?: 0.0
                val minimo = minimoCompra.replace(',', '.').toDoubleOrNull()
                val body = ConfiguracionEnviosRequest(
                    realizaEnvios = realizaEnvios,
                    envioGratis = realizaEnvios && modoEnvio == ModoEnvio.GRATIS_SIEMPRE,
                    costoEnvio = when {
                        !realizaEnvios -> 0.0
                        modoEnvio == ModoEnvio.GRATIS_SIEMPRE -> 0.0
                        modoEnvio == ModoEnvio.CON_COSTO -> costo
                        else -> costo.takeIf { it > 0 } ?: 0.0
                    },
                    envioMinimoCompra = if (realizaEnvios && modoEnvio == ModoEnvio.GRATIS_DESDE_MONTO) {
                        minimo
                    } else null,
                    telefonoContacto = if (realizaEnvios) {
                        TelefonoUtil.soloDigitos(telefonoContacto)
                    } else null
                )
                repo.guardarConfigEnvios(body)
                mensajeOk = "Configuración guardada"
            } catch (e: Exception) {
                error = e.message
            } finally {
                guardando = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de envíos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            cargando -> CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            else -> ConfiguracionEnvioFormContent(
                realizaEnvios = realizaEnvios,
                onRealizaEnviosChange = { realizaEnvios = it },
                modoEnvio = modoEnvio,
                onModoEnvioChange = { modoEnvio = it },
                costoEnvio = costoEnvio,
                onCostoEnvioChange = { costoEnvio = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                minimoCompra = minimoCompra,
                onMinimoCompraChange = { minimoCompra = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                telefonoContacto = telefonoContacto,
                onTelefonoContactoChange = { telefonoContacto = it },
                error = error,
                mensajeOk = mensajeOk,
                guardando = guardando,
                onGuardar = { guardar() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ConfiguracionEnvioFormContent(
    realizaEnvios: Boolean,
    onRealizaEnviosChange: (Boolean) -> Unit,
    modoEnvio: ModoEnvio,
    onModoEnvioChange: (ModoEnvio) -> Unit,
    costoEnvio: String,
    onCostoEnvioChange: (String) -> Unit,
    minimoCompra: String,
    onMinimoCompraChange: (String) -> Unit,
    telefonoContacto: String,
    onTelefonoContactoChange: (String) -> Unit,
    error: String?,
    mensajeOk: String?,
    guardando: Boolean,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("📦 Envíos a domicilio", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Realizás envíos a domicilio?", modifier = Modifier.weight(1f))
            Switch(checked = realizaEnvios, onCheckedChange = onRealizaEnviosChange)
        }
        if (realizaEnvios) {
            Spacer(Modifier.height(16.dp))
            Text("Tipo de envío", style = MaterialTheme.typography.titleSmall)
            ModoEnvio.entries.forEach { modo ->
                val label = when (modo) {
                    ModoEnvio.GRATIS_SIEMPRE -> "Envío gratuito siempre"
                    ModoEnvio.CON_COSTO -> "Envío con costo"
                    ModoEnvio.GRATIS_DESDE_MONTO -> "Envío gratis desde cierto monto"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = modoEnvio == modo,
                            onClick = { onModoEnvioChange(modo) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = modoEnvio == modo, onClick = { onModoEnvioChange(modo) })
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (modoEnvio == ModoEnvio.CON_COSTO || modoEnvio == ModoEnvio.GRATIS_DESDE_MONTO) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = costoEnvio,
                    onValueChange = onCostoEnvioChange,
                    label = { Text(if (modoEnvio == ModoEnvio.CON_COSTO) "Costo de envío ($)" else "Costo si no alcanza el mínimo ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (modoEnvio == ModoEnvio.GRATIS_DESDE_MONTO) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minimoCompra,
                    onValueChange = onMinimoCompraChange,
                    label = { Text("Compras mayores a ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = telefonoContacto,
                onValueChange = onTelefonoContactoChange,
                label = { Text("Teléfono de contacto *") },
                placeholder = { Text("Ej: 3434567890") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Los clientes usarán este número para coordinar el envío.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        mensajeOk?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        BotonPiku(
            texto = if (guardando) "Guardando…" else "Guardar configuración",
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth(),
            habilitado = !guardando
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewConfiguracionEnvioScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Configuración de envíos") })
            }
        ) { padding ->
            ConfiguracionEnvioFormContent(
                realizaEnvios = true,
                onRealizaEnviosChange = {},
                modoEnvio = ModoEnvio.GRATIS_SIEMPRE,
                onModoEnvioChange = {},
                costoEnvio = "",
                onCostoEnvioChange = {},
                minimoCompra = "",
                onMinimoCompraChange = {},
                telefonoContacto = "3434567890",
                onTelefonoContactoChange = {},
                error = null,
                mensajeOk = null,
                guardando = false,
                onGuardar = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
