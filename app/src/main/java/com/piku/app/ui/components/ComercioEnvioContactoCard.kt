package com.piku.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.PerfilUsuarioDto
import com.piku.app.data.repository.PerfilRepository
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.utils.ComercioContactoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ComercioEnvioContactoCard(
    comercio: Comercio,
    articuloNombre: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var perfil by remember { mutableStateOf<PerfilUsuarioDto?>(null) }
    var esCliente by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val rol = withContext(Dispatchers.IO) { AuthDataStore.rol(context) }
        esCliente = rol == "cliente"
        if (esCliente) {
            perfil = try {
                withContext(Dispatchers.IO) { PerfilRepository(context).obtenerPerfil() }
            } catch (_: Exception) {
                null
            }
        }
    }

    val telefono = comercio.telefonoContacto?.trim()?.takeIf { it.isNotBlank() }
    if (!comercio.realizaEnvios && telefono == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (comercio.realizaEnvios) {
                Text("Envío a domicilio", style = MaterialTheme.typography.titleSmall)
                comercio.textoEnvio()?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (esCliente && perfil?.direccionEntrega.isNullOrBlank()) {
                    Text(
                        "Completá tu dirección en Perfil para enviarla automáticamente por WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (telefono != null) {
                    BotonPiku(
                        texto = "Pedir envío por WhatsApp",
                        onClick = {
                            ComercioContactoHelper.abrirWhatsApp(
                                context,
                                telefono,
                                ComercioContactoHelper.mensajePedidoEnvio(
                                    comercio,
                                    perfil,
                                    articuloNombre
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            ComercioContactoHelper.llamar(
                                context,
                                telefono
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Llamar para coordinar envío")
                    }
                } else {
                    Text(
                        "Este comercio hace envíos; configurá un teléfono de contacto en el panel comercio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (telefono != null) {
                if (comercio.realizaEnvios) {
                    Text(
                        "Más artículos y descuentos",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text("Contacto directo", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Pedí por teléfono/WhatsApp productos u ofertas que no figuran en la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "📞 $telefono",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdePiku
                )
                OutlinedButton(
                    onClick = {
                        ComercioContactoHelper.abrirWhatsApp(
                            context,
                            telefono,
                            ComercioContactoHelper.mensajeConsultarMasOfertas(comercio)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Consultar más ofertas por WhatsApp")
                }
            }
        }
    }
}
