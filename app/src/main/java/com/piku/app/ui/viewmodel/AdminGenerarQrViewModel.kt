package com.piku.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.QrGenerado
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.utils.QrBitmapEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminGenerarQrUiState(
    val monto: String = "1000",
    val cargando: Boolean = false,
    val qr: QrGenerado? = null,
    val expiraEnMinutos: Int? = null,
    val qrBitmap: Bitmap? = null,
    val error: String? = null,
    val limiteAlcanzado: Boolean = false
)

class AdminGenerarQrViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ComercioRepository(application)
    private val _uiState = MutableStateFlow(AdminGenerarQrUiState())
    val uiState: StateFlow<AdminGenerarQrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repo.obtenerEstadoSuscripcion() }
                .onSuccess { sub ->
                    _uiState.update { it.copy(limiteAlcanzado = sub.limiteAlcanzado) }
                }
        }
    }

    fun onMontoChange(value: String) {
        _uiState.update { it.copy(monto = value, error = null) }
    }

    fun generar() {
        if (_uiState.value.limiteAlcanzado) {
            _uiState.update {
                it.copy(error = "Límite de puntos mensual alcanzado. Actualizá tu plan en Más → Suscripción")
            }
            return
        }
        val monto = _uiState.value.monto.toDoubleOrNull()
        if (monto == null || monto < 0) {
            _uiState.update { it.copy(error = "Ingresá un monto válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, qr = null, qrBitmap = null) }
            try {
                val res = repo.generarQr(monto)
                val codigo = res.qr.codigo
                val bitmap = QrBitmapEncoder.encode(codigo)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        qr = res.qr,
                        expiraEnMinutos = res.expiraEnMinutos,
                        qrBitmap = bitmap
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = e.message ?: "Error al generar QR")
                }
            }
        }
    }

    fun limpiar() {
        _uiState.update { AdminGenerarQrUiState(monto = it.monto) }
    }
}
