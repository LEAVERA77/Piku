package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.Recompensa
import com.piku.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CanjesUiState(
    val recompensas: List<Recompensa> = emptyList(),
    val puntosDisponibles: Int = 0,
    val recompensaSeleccionada: Recompensa? = null,
    val mostrarConfirmacion: Boolean = false,
    val mensajeExito: String? = null,
    val codigoCanje: String? = null,
    val cargando: Boolean = true,
    val canjeando: Boolean = false,
    val error: String? = null
)

class CanjesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsuarioRepository(application)
    private val _uiState = MutableStateFlow(CanjesUiState())
    val uiState: StateFlow<CanjesUiState> = _uiState.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val (puntos, lista) = repo.listarRecompensasDisponibles()
                _uiState.update {
                    it.copy(
                        puntosDisponibles = puntos,
                        recompensas = lista,
                        cargando = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = e.message ?: "No se pudieron cargar las recompensas"
                    )
                }
            }
        }
    }

    fun solicitarCanje(recompensa: Recompensa) {
        _uiState.update {
            it.copy(recompensaSeleccionada = recompensa, mostrarConfirmacion = true)
        }
    }

    fun confirmarCanje() {
        val recompensa = _uiState.value.recompensaSeleccionada ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(canjeando = true) }
            try {
                val res = repo.canjearRecompensa(recompensa.id)
                val msg = buildString {
                    append(res.mensaje)
                    res.codigoCanje?.let { append("\nCódigo: $it") }
                }
                _uiState.update {
                    it.copy(
                        puntosDisponibles = res.puntosRestantes ?: (it.puntosDisponibles - recompensa.puntosRequeridos),
                        mostrarConfirmacion = false,
                        recompensaSeleccionada = null,
                        mensajeExito = msg,
                        codigoCanje = res.codigoCanje,
                        canjeando = false
                    )
                }
                cargar()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        canjeando = false,
                        mostrarConfirmacion = false,
                        error = e.message ?: "No se pudo canjear"
                    )
                }
            }
        }
    }

    fun cancelarCanje() {
        _uiState.update {
            it.copy(mostrarConfirmacion = false, recompensaSeleccionada = null)
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensajeExito = null, codigoCanje = null, error = null) }
    }
}
