package com.piku.app.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.piku.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EscanerUiState(
    val escaneando: Boolean = true,
    val linternaActiva: Boolean = false,
    val ultimoCodigo: String? = null,
    val escaneoExitoso: Boolean = false,
    val validando: Boolean = false,
    val puntosGanados: Int? = null,
    val saldoActual: Int? = null,
    val comercioNombre: String? = null,
    val mensaje: String = "Apunta al código QR del comercio",
    val error: String? = null
)

class EscanerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsuarioRepository(application)
    private val _uiState = MutableStateFlow(EscanerUiState())
    val uiState: StateFlow<EscanerUiState> = _uiState.asStateFlow()

    private var procesando = false

    fun onCodigoEscaneado(raw: String) {
        if (procesando) return
        val codigo = normalizarCodigoQr(raw)
        if (codigo.isBlank()) return
        if (_uiState.value.validando) return
        if (_uiState.value.escaneoExitoso && _uiState.value.ultimoCodigo == codigo) return

        procesando = true
        _uiState.update {
            it.copy(
                ultimoCodigo = codigo,
                escaneando = false,
                validando = true,
                escaneoExitoso = false,
                error = null,
                puntosGanados = null,
                saldoActual = null,
                comercioNombre = null,
                mensaje = "Validando compra…"
            )
        }

        viewModelScope.launch {
            try {
                val (lat, lon) = obtenerUbicacion()
                val res = repo.validarEscaneo(codigo, lat, lon)
                _uiState.update {
                    it.copy(
                        validando = false,
                        escaneoExitoso = true,
                        puntosGanados = res.puntosGanados,
                        saldoActual = res.saldoActual,
                        comercioNombre = res.comercio,
                        mensaje = res.mensaje
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        validando = false,
                        escaneando = true,
                        ultimoCodigo = codigo,
                        error = e.message ?: "No se pudo validar el QR",
                        mensaje = "Volvé a intentar"
                    )
                }
            } finally {
                procesando = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Pair<Double?, Double?> {
        return try {
            val fused = LocationServices.getFusedLocationProviderClient(getApplication())
            val location: Location? = try {
                fused.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await() ?: fused.lastLocation.await()
            } catch (_: Exception) {
                fused.lastLocation.await()
            }
            location?.latitude to location?.longitude
        } catch (_: Exception) {
            null to null
        }
    }

    fun alternarLinterna() {
        _uiState.update { it.copy(linternaActiva = !it.linternaActiva) }
    }

    fun reiniciarEscaneo() {
        procesando = false
        _uiState.update {
            EscanerUiState(
                escaneando = true,
                linternaActiva = it.linternaActiva
            )
        }
    }

    companion object {
        fun normalizarCodigoQr(raw: String): String {
            val t = raw.trim()
            Regex("(?:codigo|qr)=([^&\\s]+)", RegexOption.IGNORE_CASE)
                .find(t)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { return it.trim() }
            return t
        }
    }
}
