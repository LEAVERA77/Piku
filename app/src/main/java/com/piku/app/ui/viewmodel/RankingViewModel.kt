package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.RankingComercioItem
import com.piku.app.data.repository.MapaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val cargando: Boolean = false,
    val mes: String = "",
    val ranking: List<RankingComercioItem> = emptyList(),
    val error: String? = null
)

class RankingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MapaRepository(application)
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = repo.obtenerRanking()
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mes = res.mes,
                        ranking = res.ranking
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }
}
