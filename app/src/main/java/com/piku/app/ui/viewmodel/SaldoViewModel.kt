package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.model.Transaccion
import com.piku.app.data.repository.UsuarioRepository
import com.piku.app.utils.CompartirLogro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaldoUiState(
    val puntos: Int = 0,
    val equivalenciaDescuentoArs: Int = 0,
    val mensajeSaldo: String? = null,
    val puntosCompras: Int = 0,
    val puntosBonos: Int = 0,
    val puntosCanjes: Int = 0,
    val desgloseDisponible: Boolean = false,
    val transacciones: List<Transaccion> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val mensajeInfo: String? = null,
    val hitoParaCompartir: Int? = null
)

class SaldoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsuarioRepository(application)
    private val _uiState = MutableStateFlow(SaldoUiState())
    val uiState: StateFlow<SaldoUiState> = _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, mensajeInfo = null) }
            try {
                val saldo = repo.obtenerSaldo()
                val historial = runCatching { repo.obtenerHistorial() }.getOrDefault(emptyList())
                val desglose = runCatching { repo.obtenerDesglose() }.getOrNull()
                val ultimoHito = AppPreferences.ultimoHitoCelebrado(getApplication())
                val hito = CompartirLogro.hitoNuevo(saldo.puntos, ultimoHito)
                _uiState.update {
                    it.copy(
                        puntos = saldo.puntos,
                        equivalenciaDescuentoArs = saldo.equivalenciaArs(),
                        mensajeSaldo = saldo.mensaje,
                        puntosCompras = desglose?.compras ?: 0,
                        puntosBonos = desglose?.bonos ?: 0,
                        puntosCanjes = desglose?.canjes ?: 0,
                        desgloseDisponible = desglose != null,
                        transacciones = historial,
                        cargando = false,
                        error = null,
                        hitoParaCompartir = hito
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = e.message ?: "No se pudo cargar el saldo"
                    )
                }
            }
        }
    }

    fun compartirPiku(onCompartir: () -> Unit) {
        onCompartir()
        viewModelScope.launch {
            runCatching { repo.bonificacionCompartir() }
                .onSuccess { res ->
                    if (res.otorgado) {
                        _uiState.update { it.copy(mensajeInfo = res.mensaje) }
                        refrescar()
                    } else {
                        _uiState.update {
                            it.copy(mensajeInfo = res.mensaje ?: "Ya recibiste puntos por compartir hoy")
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(mensajeInfo = e.message ?: "No se pudieron acreditar los puntos")
                    }
                }
        }
    }

    fun descartarHito() {
        _uiState.update { it.copy(hitoParaCompartir = null) }
    }

    fun marcarHitoCompartido(hito: Int) {
        viewModelScope.launch {
            AppPreferences.marcarHitoCelebrado(getApplication(), hito)
            _uiState.update { it.copy(hitoParaCompartir = null) }
        }
    }
}
