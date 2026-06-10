package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.DesafioItem
import com.piku.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesafiosUiState(
    val cargando: Boolean = false,
    val reclamandoId: String? = null,
    val desafios: List<DesafioItem> = emptyList(),
    val error: String? = null,
    val mensajeExito: String? = null
)

class DesafiosViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsuarioRepository(application)
    private val _uiState = MutableStateFlow(DesafiosUiState())
    val uiState: StateFlow<DesafiosUiState> = _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val lista = repo.obtenerDesafios()
                _uiState.update { it.copy(cargando = false, desafios = lista) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    fun completar(desafioId: String) {
        if (_uiState.value.reclamandoId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, mensajeExito = null, reclamandoId = desafioId) }
            try {
                val res = repo.completarDesafio(desafioId)
                _uiState.update { it.copy(mensajeExito = res.mensaje, reclamandoId = null) }
                refrescar()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, reclamandoId = null) }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensajeExito = null, error = null) }
    }
}
