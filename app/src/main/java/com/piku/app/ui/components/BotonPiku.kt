package com.piku.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku

enum class EstiloBotonPiku {
    PRIMARIO,
    SECUNDARIO,
    CONTORNO
}

@Composable
fun BotonPiku(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: EstiloBotonPiku = EstiloBotonPiku.PRIMARIO,
    habilitado: Boolean = true,
    icono: ImageVector? = null
) {
    val forma = RoundedCornerShape(16.dp)
    val alturaMinima = Modifier.height(48.dp)

    val contenido: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = texto, style = MaterialTheme.typography.labelLarge)
        }
    }

    when (estilo) {
        EstiloBotonPiku.PRIMARIO -> Button(
            onClick = onClick,
            modifier = modifier.then(alturaMinima),
            enabled = habilitado,
            shape = forma,
            colors = ButtonDefaults.buttonColors(
                containerColor = NaranjaPiku,
                contentColor = Color.White,
                disabledContainerColor = NaranjaPiku.copy(alpha = 0.4f)
            )
        ) { contenido() }

        EstiloBotonPiku.SECUNDARIO -> Button(
            onClick = onClick,
            modifier = modifier.then(alturaMinima),
            enabled = habilitado,
            shape = forma,
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdePiku,
                contentColor = Color.White
            )
        ) { contenido() }

        EstiloBotonPiku.CONTORNO -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.then(alturaMinima),
            enabled = habilitado,
            shape = forma,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdePiku)
        ) { contenido() }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBotonPiku() {
    PikuTheme {
        BotonPiku(
            texto = "ESCANEAR QR",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
