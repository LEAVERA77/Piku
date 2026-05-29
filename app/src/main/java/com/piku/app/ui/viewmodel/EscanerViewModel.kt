package com.piku.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EscanerUiState(
    val escaneando: Boolean = true,
    val linternaActiva: Boolean = false,
    val ultimoCodigo: String? = null,
    val escaneoExitoso: Boolean = false,
    val mensaje: String = "Apunta al código QR del comercio"
)

class EscanerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EscanerUiState())
    val uiState: StateFlow<EscanerUiState> = _uiState.asStateFlow()

    fun onCodigoEscaneado(codigo: String) {
        if (_uiState.value.ultimoCodigo == codigo && _uiState.value.escaneoExitoso) return
        _uiState.update {
            it.copy(
                ultimoCodigo = codigo,
                escaneoExitoso = true,
                escaneando = false,
                mensaje = "¡Código leído correctamente!"
            )
        }
    }

    fun alternarLinterna() {
        _uiState.update { it.copy(linternaActiva = !it.linternaActiva) }
    }

    fun reiniciarEscaneo() {
        _uiState.update {
            EscanerUiState(
                escaneando = true,
                linternaActiva = it.linternaActiva
            )
        }
    }

    fun setLinterna(activa: Boolean) {
        _uiState.update { it.copy(linternaActiva = activa) }
    }
}
