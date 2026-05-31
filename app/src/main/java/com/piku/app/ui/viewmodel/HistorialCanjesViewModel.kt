package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.CanjeComercioItem
import com.piku.app.data.repository.ComercioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialCanjesUiState(
    val canjes: List<CanjeComercioItem> = emptyList(),
    val cargando: Boolean = false,
    val cargandoMas: Boolean = false,
    val error: String? = null,
    val buscar: String = "",
    val estadoFiltro: String? = null,
    val pagina: Int = 1,
    val total: Int = 0,
    val hayMas: Boolean = false
)

class HistorialCanjesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ComercioRepository(application)
    private val _uiState = MutableStateFlow(HistorialCanjesUiState())
    val uiState: StateFlow<HistorialCanjesUiState> = _uiState.asStateFlow()

    private val limite = 20

    init {
        cargar(refrescar = true)
    }

    fun onBuscarChange(texto: String) {
        _uiState.update { it.copy(buscar = texto) }
    }

    fun aplicarBusqueda() {
        cargar(refrescar = true)
    }

    fun setEstadoFiltro(estado: String?) {
        _uiState.update { it.copy(estadoFiltro = estado) }
        cargar(refrescar = true)
    }

    fun cargarMas() {
        if (_uiState.value.cargandoMas || !_uiState.value.hayMas) return
        cargar(refrescar = false)
    }

    fun cargar(refrescar: Boolean) {
        viewModelScope.launch {
            val pagina = if (refrescar) 1 else _uiState.value.pagina + 1
            _uiState.update {
                it.copy(
                    cargando = refrescar,
                    cargandoMas = !refrescar,
                    error = null
                )
            }
            try {
                val (lista, total) = repo.historialCanjes(
                    pagina = pagina,
                    limite = limite,
                    estado = _uiState.value.estadoFiltro,
                    buscar = _uiState.value.buscar
                )
                _uiState.update { state ->
                    val merged = if (refrescar) lista else state.canjes + lista
                    state.copy(
                        canjes = merged,
                        total = total,
                        pagina = pagina,
                        hayMas = merged.size < total,
                        cargando = false,
                        cargandoMas = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        cargandoMas = false,
                        error = e.message ?: "Error al cargar canjes"
                    )
                }
            }
        }
    }
}
