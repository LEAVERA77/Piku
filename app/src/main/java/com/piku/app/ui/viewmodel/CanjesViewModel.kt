package com.piku.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.piku.app.data.MockData
import com.piku.app.data.model.Recompensa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CanjesUiState(
    val recompensas: List<Recompensa> = MockData.recompensas,
    val puntosDisponibles: Int = MockData.PUNTOS_SALDO,
    val recompensaSeleccionada: Recompensa? = null,
    val mostrarConfirmacion: Boolean = false,
    val mensajeExito: String? = null
)

class CanjesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CanjesUiState())
    val uiState: StateFlow<CanjesUiState> = _uiState.asStateFlow()

    fun solicitarCanje(recompensa: Recompensa) {
        _uiState.update {
            it.copy(recompensaSeleccionada = recompensa, mostrarConfirmacion = true)
        }
    }

    fun confirmarCanje() {
        val recompensa = _uiState.value.recompensaSeleccionada ?: return
        if (_uiState.value.puntosDisponibles >= recompensa.puntosRequeridos) {
            _uiState.update {
                it.copy(
                    puntosDisponibles = it.puntosDisponibles - recompensa.puntosRequeridos,
                    mostrarConfirmacion = false,
                    recompensaSeleccionada = null,
                    mensajeExito = "¡Canjeaste ${recompensa.nombre}!"
                )
            }
        }
    }

    fun cancelarCanje() {
        _uiState.update {
            it.copy(mostrarConfirmacion = false, recompensaSeleccionada = null)
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensajeExito = null) }
    }
}
