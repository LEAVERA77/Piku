package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.Transaccion
import com.piku.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaldoUiState(
    val puntos: Int = 0,
    val equivalenciaDescuento: Int = 0,
    val mensajeSaldo: String? = null,
    val transacciones: List<Transaccion> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class SaldoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsuarioRepository(application)
    private val _uiState = MutableStateFlow(SaldoUiState())
    val uiState: StateFlow<SaldoUiState> = _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val saldo = repo.obtenerSaldo()
                val historial = repo.obtenerHistorial()
                _uiState.update {
                    it.copy(
                        puntos = saldo.puntos,
                        equivalenciaDescuento = saldo.equivalencia(),
                        mensajeSaldo = saldo.mensaje,
                        transacciones = historial,
                        cargando = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = e.message ?: "No se pudo cargar el saldo")
                }
            }
        }
    }
}
