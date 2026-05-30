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
import com.piku.app.data.model.Rubro
import com.piku.app.data.nominatim.NominatimClient
import com.piku.app.data.nominatim.NominatimResult
import com.piku.app.data.repository.MapaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MensajeChat(val rol: String, val texto: String)

data class MapaUiState(
    val cargando: Boolean = true,
    val cargandoViewport: Boolean = false,
    val comercios: List<Comercio> = emptyList(),
    val rubros: List<Rubro> = emptyList(),
    val rubrosSeleccionados: Set<String> = emptySet(),
    val busquedaNombre: String = "",
    val userLat: Double = -34.6037,
    val userLon: Double = -58.3816,
    val tieneUbicacionReal: Boolean = false,
    val busquedaDireccion: String = "",
    val resultadosBusqueda: List<NominatimResult> = emptyList(),
    val comercioSeleccionado: Comercio? = null,
    val error: String? = null,
    val chatAbierto: Boolean = false,
    val mensajesChat: List<MensajeChat> = emptyList(),
    val preguntaChat: String = "",
    val cargandoChat: Boolean = false,
    val comercioSugeridoId: String? = null
) {
    val comerciosVisibles: List<Comercio>
        get() {
            var lista = comercios
            if (rubrosSeleccionados.isNotEmpty()) {
                lista = lista.filter { c -> coincideRubro(c, rubros, rubrosSeleccionados) }
            }
            val q = busquedaNombre.trim()
            if (q.length >= 2) {
                lista = lista.filter { it.nombre.contains(q, ignoreCase = true) }
            }
            return lista
        }

    val contadorVisibles: Int get() = comerciosVisibles.size
}

private fun coincideRubro(comercio: Comercio, catalogo: List<Rubro>, seleccionados: Set<String>): Boolean {
    val cat = comercio.categoria?.lowercase()?.trim()?.replace("é", "e") ?: "otros"
    val rubrosActivos = catalogo.filter { seleccionados.contains(it.id) }
    return rubrosActivos.any { rubro ->
        rubro.categorias.any { c ->
            val norm = c.lowercase().replace("é", "e")
            cat == norm || cat.contains(norm)
        }
    }
}

class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MapaRepository(application)
    private val nominatim = NominatimClient.get(application)
    private var viewportJob: Job? = null
    private var ultimoViewport: Viewport? = null

    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    private data class Viewport(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    init {
        viewModelScope.launch {
            try {
                val rubros = repo.listarRubros()
                _uiState.update { it.copy(rubros = rubros) }
            } catch (_: Exception) {
                // rubros opcionales
            }
        }
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
                } else null

                val lat = location?.latitude ?: _uiState.value.userLat
                val lon = location?.longitude ?: _uiState.value.userLon
                _uiState.update {
                    it.copy(
                        userLat = lat,
                        userLon = lon,
                        tieneUbicacionReal = location != null
                    )
                }
                val comercios = repo.listarComerciosInicial(lat, lon)
                _uiState.update {
                    it.copy(cargando = false, comercios = comercios, error = null)
                }
                repo.registrarEvento("mapa_abierto")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = e.message ?: "Error al cargar comercios en el mapa"
                    )
                }
            }
        }
    }

    fun onViewportChanged(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        val vp = Viewport(minLat, maxLat, minLon, maxLon)
        if (ultimoViewport != null && ultimoViewport == vp) return
        ultimoViewport = vp
        viewportJob?.cancel()
        viewportJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(cargandoViewport = true) }
            try {
                val s = _uiState.value
                val lista = repo.listarComerciosEnViewport(
                    s.userLat, s.userLon, minLat, maxLat, minLon, maxLon
                )
                _uiState.update { it.copy(comercios = lista, cargandoViewport = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(cargandoViewport = false) }
            }
        }
    }

    fun centrarEnUsuario() {
        if (!_uiState.value.tieneUbicacionReal) {
            cargarUbicacionYComercios(conUbicacion = true)
        }
    }

    fun toggleRubro(rubroId: String) {
        _uiState.update { state ->
            val next = state.rubrosSeleccionados.toMutableSet()
            if (next.contains(rubroId)) next.remove(rubroId) else next.add(rubroId)
            state.copy(rubrosSeleccionados = next)
        }
    }

    fun setBusquedaNombre(q: String) {
        _uiState.update { it.copy(busquedaNombre = q) }
        if (q.length >= 2) {
            viewModelScope.launch { repo.registrarEvento("busqueda") }
        }
    }

    fun buscarDireccion(query: String) {
        _uiState.update { it.copy(busquedaDireccion = query) }
        if (query.length < 3) {
            _uiState.update { it.copy(resultadosBusqueda = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val resultados = nominatim.geocode(query)
                _uiState.update { it.copy(resultadosBusqueda = resultados) }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Error en búsqueda de dirección") }
            }
        }
    }

    fun seleccionarComercio(comercio: Comercio?) {
        _uiState.update { it.copy(comercioSeleccionado = comercio) }
        if (comercio != null) {
            viewModelScope.launch { repo.registrarEvento("vista_comercio", comercio.id) }
        }
    }

    fun centrarEnResultado(resultado: NominatimResult) {
        val lat = resultado.lat.toDoubleOrNull() ?: return
        val lon = resultado.lon.toDoubleOrNull() ?: return
        _uiState.update {
            it.copy(userLat = lat, userLon = lon, resultadosBusqueda = emptyList())
        }
    }

    fun abrirChat() {
        _uiState.update {
            val mensajes = if (it.mensajesChat.isEmpty()) {
                listOf(
                    MensajeChat(
                        "piku",
                        "¡Hola! Soy Piku. Preguntame dónde canjear tus puntos o qué te conviene cerca."
                    )
                )
            } else it.mensajesChat
            it.copy(chatAbierto = true, mensajesChat = mensajes)
        }
    }

    fun cerrarChat() {
        _uiState.update { it.copy(chatAbierto = false) }
    }

    fun setPreguntaChat(texto: String) {
        _uiState.update { it.copy(preguntaChat = texto) }
    }

    fun enviarPreguntaChat() {
        val pregunta = _uiState.value.preguntaChat.trim()
        if (pregunta.isBlank() || _uiState.value.cargandoChat) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cargandoChat = true,
                    preguntaChat = "",
                    mensajesChat = it.mensajesChat + MensajeChat("usuario", pregunta)
                )
            }
            try {
                val s = _uiState.value
                val res = repo.chatPiku(pregunta, s.userLat, s.userLon)
                _uiState.update {
                    it.copy(
                        cargandoChat = false,
                        mensajesChat = it.mensajesChat + MensajeChat("piku", res.respuesta),
                        comercioSugeridoId = res.comercio_sugerido_id
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargandoChat = false,
                        mensajesChat = it.mensajesChat + MensajeChat(
                            "piku",
                            e.message ?: "No pude conectar con el asistente. Probá más tarde."
                        )
                    )
                }
            }
        }
    }

    fun seleccionarComercioSugerido() {
        val id = _uiState.value.comercioSugeridoId ?: return
        val comercio = _uiState.value.comercios.find { it.id == id }
            ?: _uiState.value.comerciosVisibles.find { it.id == id }
        if (comercio != null) {
            seleccionarComercio(comercio)
            _uiState.update { it.copy(chatAbierto = false) }
        }
    }
}
