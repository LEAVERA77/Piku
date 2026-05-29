package com.piku.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.piku.app.data.MockData
import com.piku.app.data.model.Transaccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SaldoUiState(
    val puntos: Int = MockData.PUNTOS_SALDO,
    val equivalenciaDescuento: Int = MockData.EQUIVALENCIA_DESCUENTO,
    val transacciones: List<Transaccion> = MockData.transacciones,
    val cargando: Boolean = false
)

class SaldoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SaldoUiState())
    val uiState: StateFlow<SaldoUiState> = _uiState.asStateFlow()

    // En el futuro: cargar desde RetrofitInstance.api
    fun refrescar() {
        _uiState.update {
            it.copy(
                puntos = MockData.PUNTOS_SALDO,
                transacciones = MockData.transacciones,
                cargando = false
            )
        }
    }
}
