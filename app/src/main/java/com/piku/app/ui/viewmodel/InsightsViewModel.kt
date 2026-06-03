package com.piku.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.ComercioInsightsResponse
import com.piku.app.data.repository.ComercioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val cargando: Boolean = false,
    val insights: ComercioInsightsResponse? = null,
    val error: String? = null
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ComercioRepository(application)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val data = repo.obtenerInsights()
                _uiState.update { it.copy(cargando = false, insights = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }
}
