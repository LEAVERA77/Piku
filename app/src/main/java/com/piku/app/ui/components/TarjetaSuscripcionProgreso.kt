package com.piku.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.SuscripcionEstadoResponse
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TarjetaSuscripcionProgreso(
    estado: SuscripcionEstadoResponse,
    onActualizarPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formato = NumberFormat.getNumberInstance(Locale("es", "AR"))
    val porcentaje = estado.porcentajeUso.coerceIn(0, 100)
    val ilimitado = estado.puntosLimite == null
    val colorBarra = when {
        estado.limiteAlcanzado -> MaterialTheme.colorScheme.error
        porcentaje > 80 -> NaranjaPiku
        else -> VerdePiku
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "📊 Plan ${estado.etiquetaPlan()} (${estado.textoLimitePuntos()})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            if (!ilimitado) {
                LinearProgressIndicator(
                    progress = { porcentaje / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = colorBarra,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$porcentaje% usado",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorBarra
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Usados: ${formato.format(estado.puntosUsadosMes)} PP · Restan: ${formato.format(estado.puntosRestantes ?: 0)} PP",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Puntos ilimitados este mes · ${formato.format(estado.puntosUsadosMes)} PP otorgados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdePiku
                )
            }
            if (estado.plan != "pro") {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onActualizarPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val siguiente = when (estado.plan) {
                        "gratuito" -> "ACTUALIZAR A BÁSICO por \$5 USD"
                        else -> "ACTUALIZAR A PRO por \$15 USD"
                    }
                    Text(siguiente, color = NaranjaPiku)
                }
            }
            if (estado.limiteAlcanzado) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Límite mensual alcanzado. No podés generar QR hasta actualizar el plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
