package com.piku.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.piku.app.data.MockData
import com.piku.app.data.model.NivelUsuario
import com.piku.app.data.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PerfilUiState(
    val usuario: Usuario = MockData.usuario,
    val sesionActiva: Boolean = true
)

class PerfilViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    fun cerrarSesion() {
        _uiState.update { it.copy(sesionActiva = false) }
    }

    fun nivelActual(): NivelUsuario = NivelUsuario.desdePuntos(_uiState.value.usuario.puntos)
}
