package com.piku.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.PerfilUsuarioDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ComercioContactoHelper {

    fun normalizarTelefono(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("54") && digits.length >= 12 -> digits
            digits.length == 10 && digits.first() == '3' -> "54$digits"
            digits.length == 11 && digits.first() == '0' -> "54${digits.drop(1)}"
            else -> digits
        }
    }

    fun abrirWhatsApp(context: Context, telefono: String, mensaje: String) {
        val numero = normalizarTelefono(telefono)
        val texto = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.toString())
        val uri = Uri.parse("https://wa.me/$numero?text=$texto")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    fun llamar(context: Context, telefono: String) {
        val uri = Uri.parse("tel:${telefono.filter { it.isDigit() || it == '+' }}")
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    }

    fun mensajePedidoEnvio(
        comercio: Comercio,
        perfil: PerfilUsuarioDto?,
        articuloNombre: String? = null
    ): String {
        val envio = comercio.textoEnvio() ?: "Envío a domicilio disponible"
        val dir = perfil?.direccionEntrega?.takeIf { it.isNotBlank() }
        val ciudadLinea = listOfNotNull(perfil?.ciudad, perfil?.provincia, perfil?.codigoPostal)
            .filter { !it.isNullOrBlank() }
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
        val notas = perfil?.notasEntrega?.takeIf { it.isNotBlank() }
        val miTel = perfil?.telefono?.takeIf { it.isNotBlank() }

        return buildString {
            appendLine("Hola, consulto desde la app *Piku*.")
            appendLine("Quiero pedir *envío a domicilio* en *${comercio.nombre}*.")
            appendLine()
            appendLine("📦 $envio")
            articuloNombre?.let {
                appendLine("🛒 Artículo de interés: $it")
            }
            appendLine()
            if (dir != null) {
                appendLine("📍 Dirección: $dir")
                ciudadLinea?.let { appendLine("🏙️ $it") }
                notas?.let { appendLine("📝 Notas: $it") }
            } else {
                appendLine("📍 (Completaré mi dirección de entrega)")
            }
            miTel?.let { appendLine("📞 Mi teléfono: $it") }
            appendLine()
            appendLine("Gracias.")
        }.trim()
    }

    fun mensajeConsultarMasOfertas(comercio: Comercio): String = """
        Hola, consulto desde la app *Piku* (${comercio.nombre}).

        Me interesan *artículos u ofertas con descuento* que no están publicados en la app.

        ¿Qué tienen disponible? Gracias.
    """.trimIndent()
}
