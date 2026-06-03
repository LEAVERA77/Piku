package com.piku.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.RecompensaPublica
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OfertaCard(
    oferta: RecompensaPublica,
    onVerMas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = oferta.icono ?: "🎁",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        oferta.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val tipo = oferta.resumenBeneficio()
                    if (tipo.isNotBlank()) {
                        Text(
                            tipo,
                            style = MaterialTheme.typography.labelLarge,
                            color = NaranjaPiku
                        )
                    }
                    Text(
                        "${oferta.puntosRequeridos} pts para canjear",
                        style = MaterialTheme.typography.bodySmall,
                        color = VerdePiku
                    )
                    vigenciaTexto(oferta)?.let { vigencia ->
                        Text(
                            vigencia,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onVerMas,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ver más")
            }
        }
    }
}

private fun vigenciaTexto(oferta: RecompensaPublica): String? {
    val iso = oferta.vigenciaHasta ?: return null
    return try {
        val instant = Instant.parse(iso.replace(" ", "T").let { s ->
            if (s.endsWith("Z") || s.contains("+")) s else "${s}Z"
        })
        val fecha = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "AR"))
            .withZone(ZoneId.systemDefault())
            .format(instant)
        "Vigente hasta $fecha"
    } catch (_: Exception) {
        null
    }
}
