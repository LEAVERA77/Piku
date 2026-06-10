package com.piku.app.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.util.Log
import com.piku.app.data.CerritoGeo
import com.piku.app.data.ComerciosCerritoDemo
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.Rubro
import com.piku.app.data.nominatim.NominatimAddress
import com.piku.app.data.nominatim.NominatimAddressFormatter
import com.piku.app.data.nominatim.NominatimRepository
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

import com.piku.app.utils.MapRubroUtil

data class MensajeChat(val rol: String, val texto: String)

data class MapaUiState(
    val cargando: Boolean = false,
    val cargandoViewport: Boolean = false,
    val comercios: List<Comercio> = emptyList(),
    val rubros: List<Rubro> = emptyList(),
    val rubrosSeleccionados: Set<String> = emptySet(),
    val busquedaNombre: String = "",
    val mapCenterLat: Double = CerritoGeo.CENTRO_LAT,
    val mapCenterLon: Double = CerritoGeo.CENTRO_LON,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val tieneUbicacionReal: Boolean = false,
    val zoomMapa: Double = ZOOM_DEFAULT,
    val panelExpandido: Boolean = true,
    val busquedaDireccion: String = "",
    val direccionEditadaPorUsuario: Boolean = false,
    val contextoDireccion: NominatimAddress? = null,
    val resultadosBusqueda: List<NominatimResult> = emptyList(),
    val sugerenciasDireccion: List<NominatimResult> = emptyList(),
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
            return lista.sortedWith(
                compareByDescending<Comercio> { it.destacado && !it.esOpenStreetMap() }
                    .thenBy { it.distanciaMetros ?: Int.MAX_VALUE }
                    .thenBy { it.nombre }
            )
        }

    val contadorVisibles: Int get() = comerciosVisibles.size

    val refLat: Double get() = gpsLat ?: mapCenterLat
    val refLon: Double get() = gpsLon ?: mapCenterLon
}

private const val ZOOM_UBICACION = 17.0
private const val ZOOM_DEFAULT = 16.0
private const val ZOOM_CERRITO_CERCA = 17.0

private const val TAG = "MapaViewModel"

private fun coincideRubro(comercio: Comercio, catalogo: List<Rubro>, seleccionados: Set<String>): Boolean =
    MapRubroUtil.coincideRubro(comercio, catalogo, seleccionados)

class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MapaRepository(application)
    private val nominatim = NominatimRepository(application)

    private var viewportJob: Job? = null
    private var ultimoViewport: Viewport? = null
    private var omitirProximoViewport = true

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
        cargarUbicacionYComercios(conUbicacion = false)
    }

    @SuppressLint("MissingPermission")
    fun cargarUbicacionYComercios(conUbicacion: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = it.comercios.isEmpty(), error = null) }
            try {
                val fused = LocationServices.getFusedLocationProviderClient(getApplication())
                var location: Location? = null
                if (conUbicacion) {
                    try {
                        location = fused.lastLocation.await()
                    } catch (_: Exception) {
                        // sin lastLocation
                    }
                    if (location == null) {
                        try {
                            location = fused.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                CancellationTokenSource().token
                            ).await()
                        } catch (_: Exception) {
                            // GPS lento: seguimos con última posición conocida
                        }
                    }
                }

                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val enCerrito = CerritoGeo.enZonaCerrito(lat, lon)
                    // Siempre centramos en la ubicación real del usuario:
                    // antes el mapa saltaba a Cerrito aunque estuvieras en otra ciudad.
                    _uiState.update {
                        it.copy(
                            gpsLat = lat,
                            gpsLon = lon,
                            tieneUbicacionReal = true,
                            mapCenterLat = lat,
                            mapCenterLon = lon,
                            zoomMapa = if (enCerrito) ZOOM_CERRITO_CERCA else ZOOM_DEFAULT
                        )
                    }
                    aplicarCalleInferida(lat, lon)
                }

                val s = _uiState.value
                val refLat = s.refLat
                val refLon = s.refLon
                val comercios = try {
                    val desdeApi = repo.listarComerciosInicial(refLat, refLon)
                    val catalogo = CerritoGeo.listaMapaCerrito(desdeApi)
                    CerritoGeo.conDistanciaDesde(refLat, refLon, catalogo)
                } catch (e: Exception) {
                    Log.w(TAG, "Error API comercios; usando demo Cerrito", e)
                    // Solo usamos el demo si no hay nada cargado: nunca pisar datos reales.
                    _uiState.value.comercios.ifEmpty {
                        CerritoGeo.conDistanciaDesde(refLat, refLon, ComerciosCerritoDemo.lista)
                    }
                }
                Log.d(TAG, "Total comercios en mapa: ${comercios.size}")
                _uiState.update {
                    it.copy(cargando = false, comercios = comercios, error = null)
                }
                repo.registrarEvento("mapa_abierto")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar mapa", e)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        comercios = it.comercios.ifEmpty { ComerciosCerritoDemo.lista },
                        error = null
                    )
                }
            }
        }
    }

    fun onViewportChanged(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        if (omitirProximoViewport) {
            omitirProximoViewport = false
            return
        }
        val vp = Viewport(minLat, maxLat, minLon, maxLon)
        if (ultimoViewport != null && ultimoViewport == vp) return
        ultimoViewport = vp
        viewportJob?.cancel()
        viewportJob = viewModelScope.launch {
            delay(800)
            try {
                val s = _uiState.value
                // Carga solo lo visible en el mapa (bbox) y lo une con lo ya cargado,
                // así los pines no desaparecen al mover el mapa.
                val margenLat = (maxLat - minLat) * 0.25
                val margenLon = (maxLon - minLon) * 0.25
                val desdeApi = repo.listarComerciosEnViewport(
                    s.refLat,
                    s.refLon,
                    minLat - margenLat,
                    maxLat + margenLat,
                    minLon - margenLon,
                    maxLon + margenLon
                )
                val nuevos = CerritoGeo.listaMapaCerrito(desdeApi)
                val combinado = (nuevos + s.comercios).distinctBy { it.id }
                val actualizado = CerritoGeo.conDistanciaDesde(s.refLat, s.refLon, combinado)
                _uiState.update { it.copy(comercios = actualizado) }
            } catch (_: Exception) {
                // mantener catálogo actual
            }
        }
    }

    fun centrarEnUsuario() {
        if (!_uiState.value.tieneUbicacionReal) {
            cargarUbicacionYComercios(conUbicacion = true)
            return
        }
        val lat = _uiState.value.gpsLat ?: return
        val lon = _uiState.value.gpsLon ?: return
        val enCerrito = CerritoGeo.enZonaCerrito(lat, lon)
        _uiState.update {
            it.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomMapa = if (enCerrito) ZOOM_CERRITO_CERCA else ZOOM_UBICACION
            )
        }
    }

    fun expandirCluster(lat: Double, lon: Double, zoomActual: Double) {
        _uiState.update {
            it.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomMapa = (zoomActual + 1.35).coerceAtMost(20.0)
            )
        }
    }

    fun togglePanelExpandido() {
        _uiState.update { it.copy(panelExpandido = !it.panelExpandido) }
    }

    private fun aplicarCalleInferida(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val lista = nominatim.sugerenciasPorUbicacion(lat, lon)
                val calle = nominatim.inferirCalle(lat, lon)
                val contexto = lista.firstOrNull()?.address
                _uiState.update { state ->
                    val nuevoTexto = if (!state.direccionEditadaPorUsuario && !calle.isNullOrBlank()) {
                        calle
                    } else {
                        state.busquedaDireccion
                    }
                    state.copy(
                        sugerenciasDireccion = lista,
                        contextoDireccion = contexto ?: state.contextoDireccion,
                        busquedaDireccion = nuevoTexto
                    )
                }
            } catch (_: Exception) {
                // sin sugerencias
            }
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
        if (q.trim().length >= 2) {
            viewModelScope.launch { repo.registrarEvento("busqueda") }
        }
    }

    fun buscarDireccion(query: String) {
        _uiState.update { it.copy(busquedaDireccion = query, direccionEditadaPorUsuario = true) }
        val s = _uiState.value
        if (query.length < 2) {
            _uiState.update { it.copy(resultadosBusqueda = emptyList()) }
            if (s.tieneUbicacionReal) {
                aplicarCalleInferida(s.refLat, s.refLon)
            }
            return
        }
        viewModelScope.launch {
            try {
                val consulta = NominatimAddressFormatter.consultaGeocode(query, s.contextoDireccion)
                val resultados = nominatim.buscarCerca(s.refLat, s.refLon, consulta)
                _uiState.update { it.copy(resultadosBusqueda = resultados, error = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Error en búsqueda de dirección") }
            }
        }
    }

    fun irADireccion() {
        val s = _uiState.value
        val texto = s.busquedaDireccion.trim()
        if (texto.length < 2) {
            _uiState.update { it.copy(error = "Escribí calle y número") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoViewport = true, error = null) }
            try {
                val resultado = nominatim.resolverDireccion(s.refLat, s.refLon, texto)
                if (resultado == null) {
                    _uiState.update {
                        it.copy(
                            cargandoViewport = false,
                            error = "No encontramos esa dirección cerca"
                        )
                    }
                    return@launch
                }
                centrarEnResultado(resultado, actualizarTexto = false)
                _uiState.update { it.copy(cargandoViewport = false) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        cargandoViewport = false,
                        error = "No pudimos ubicar la dirección"
                    )
                }
            }
        }
    }

    fun etiquetaDireccion(resultado: NominatimResult): String =
        NominatimAddressFormatter.etiquetaSugerencia(resultado)

    fun seleccionarComercio(comercio: Comercio?) {
        _uiState.update { it.copy(comercioSeleccionado = comercio) }
        if (comercio != null) {
            viewModelScope.launch { repo.registrarEvento("vista_comercio", comercio.id) }
        }
    }

    fun centrarEnResultado(resultado: NominatimResult, actualizarTexto: Boolean = true) {
        val lat = resultado.lat.toDoubleOrNull() ?: return
        val lon = resultado.lon.toDoubleOrNull() ?: return
        val textoCampo = if (actualizarTexto) {
            NominatimAddressFormatter.textoParaCampo(resultado).ifBlank {
                NominatimAddressFormatter.calleDesde(resultado.address, resultado.displayName).orEmpty()
            }
        } else {
            _uiState.value.busquedaDireccion
        }
        _uiState.update {
            it.copy(
                mapCenterLat = lat,
                mapCenterLon = lon,
                zoomMapa = ZOOM_UBICACION,
                resultadosBusqueda = emptyList(),
                sugerenciasDireccion = emptyList(),
                busquedaDireccion = textoCampo,
                contextoDireccion = resultado.address ?: it.contextoDireccion,
                error = null
            )
        }
    }

    fun enviarPreguntaSugerida(texto: String) {
        if (texto.isBlank() || _uiState.value.cargandoChat) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cargandoChat = true,
                    preguntaChat = "",
                    mensajesChat = it.mensajesChat + MensajeChat("usuario", texto)
                )
            }
            try {
                val s = _uiState.value
                val res = repo.chatPiku(texto, s.refLat, s.refLon)
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
                            e.message ?: "No pude conectar con el asistente."
                        )
                    )
                }
            }
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
                val res = repo.chatPiku(pregunta, s.refLat, s.refLon)
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
