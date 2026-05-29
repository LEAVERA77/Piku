package com.piku.app.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.piku.app.data.model.Comercio
import com.piku.app.data.nominatim.NominatimClient
import com.piku.app.data.nominatim.NominatimResult
import com.piku.app.data.repository.ComercioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MapaUiState(
    val cargando: Boolean = true,
    val comercios: List<Comercio> = emptyList(),
    val userLat: Double = -34.6037,
    val userLon: Double = -58.3816,
    val busqueda: String = "",
    val resultadosBusqueda: List<NominatimResult> = emptyList(),
    val comercioSeleccionado: Comercio? = null,
    val error: String? = null
)

class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ComercioRepository(application)
    private val nominatim = NominatimClient.get(application)

    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    init {
        cargarUbicacionYComercios(conUbicacion = false)
    }

    @SuppressLint("MissingPermission")
    fun cargarUbicacionYComercios(conUbicacion: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val fused = LocationServices.getFusedLocationProviderClient(getApplication())
                val location: Location? = if (conUbicacion) {
                    try {
                        fused.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token
                        ).await()
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                val lat = location?.latitude ?: _uiState.value.userLat
                val lon = location?.longitude ?: _uiState.value.userLon
                val comercios = repo.listarComercios(lat, lon)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        comercios = comercios,
                        userLat = lat,
                        userLon = lon
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = e.message ?: "Error al cargar mapa")
                }
            }
        }
    }

    fun buscarDireccion(query: String) {
        _uiState.update { it.copy(busqueda = query) }
        if (query.length < 3) {
            _uiState.update { it.copy(resultadosBusqueda = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val resultados = nominatim.geocode(query)
                _uiState.update { it.copy(resultadosBusqueda = resultados) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error en búsqueda Nominatim") }
            }
        }
    }

    fun seleccionarComercio(comercio: Comercio?) {
        _uiState.update { it.copy(comercioSeleccionado = comercio) }
    }

    fun centrarEnResultado(resultado: NominatimResult) {
        val lat = resultado.lat.toDoubleOrNull() ?: return
        val lon = resultado.lon.toDoubleOrNull() ?: return
        _uiState.update {
            it.copy(userLat = lat, userLon = lon, resultadosBusqueda = emptyList())
        }
    }
}
