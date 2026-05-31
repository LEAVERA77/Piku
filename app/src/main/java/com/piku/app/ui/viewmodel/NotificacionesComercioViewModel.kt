package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.NotificacionComercio
import com.piku.app.data.repository.ComercioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionesComercioUiState(
    val notificaciones: List<NotificacionComercio> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class NotificacionesComercioViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ComercioRepository(application)
    private val _uiState = MutableStateFlow(NotificacionesComercioUiState())
    val uiState: StateFlow<NotificacionesComercioUiState> = _uiState.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val (lista, _) = repo.listarNotificaciones(limite = 50, soloNoLeidas = false)
                _uiState.update { it.copy(notificaciones = lista, cargando = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = e.message ?: "Error al cargar")
                }
            }
        }
    }

    fun marcarLeida(id: String) {
        viewModelScope.launch {
            try {
                repo.marcarNotificacionLeida(id)
                _uiState.update { state ->
                    state.copy(
                        notificaciones = state.notificaciones.map {
                            if (it.id == id) it.copy(leida = true) else it
                        }
                    )
                }
            } catch (_: Exception) {
                refrescar()
            }
        }
    }
}
