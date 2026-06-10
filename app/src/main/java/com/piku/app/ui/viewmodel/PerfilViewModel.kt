package com.piku.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piku.app.data.model.ActualizarPerfilRequest
import com.piku.app.data.model.NivelUsuario
import com.piku.app.data.model.PerfilUsuarioDto
import com.piku.app.data.repository.AuthRepository
import com.piku.app.data.repository.PerfilRepository
import com.piku.app.ui.PikuAvataresEmoji
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PerfilUiState(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val editando: Boolean = false,
    val perfil: PerfilUsuarioDto? = null,
    val nombre: String = "",
    val telefono: String = "",
    val direccionEntrega: String = "",
    val ciudad: String = "",
    val provincia: String = "",
    val codigoPostal: String = "",
    val notasEntrega: String = "",
    val avatarUrl: String? = null,
    val error: String? = null,
    val mensajeExito: String? = null
)

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PerfilRepository(application)
    private val authRepo = AuthRepository(application)

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val p = repo.obtenerPerfil()
                _uiState.update {
                    it.copy(
                        cargando = false,
                        perfil = p,
                        nombre = p.nombre,
                        telefono = p.telefono.orEmpty(),
                        direccionEntrega = p.direccionEntrega.orEmpty(),
                        ciudad = p.ciudad.orEmpty(),
                        provincia = p.provincia.orEmpty(),
                        codigoPostal = p.codigoPostal.orEmpty(),
                        notasEntrega = p.notasEntrega.orEmpty(),
                        avatarUrl = p.avatarUrl
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = e.message ?: "Error al cargar perfil")
                }
            }
        }
    }

    fun setEditando(valor: Boolean) {
        _uiState.update { state ->
            if (!valor && state.perfil != null) {
                // Cancelar edición: revertir campos a los valores guardados.
                val p = state.perfil
                state.copy(
                    editando = false,
                    mensajeExito = null,
                    error = null,
                    nombre = p.nombre,
                    telefono = p.telefono.orEmpty(),
                    direccionEntrega = p.direccionEntrega.orEmpty(),
                    ciudad = p.ciudad.orEmpty(),
                    provincia = p.provincia.orEmpty(),
                    codigoPostal = p.codigoPostal.orEmpty(),
                    notasEntrega = p.notasEntrega.orEmpty(),
                    avatarUrl = p.avatarUrl
                )
            } else {
                state.copy(editando = valor, mensajeExito = null, error = null)
            }
        }
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onTelefonoChange(v: String) = _uiState.update { it.copy(telefono = v) }
    fun onDireccionChange(v: String) = _uiState.update { it.copy(direccionEntrega = v) }
    fun onCiudadChange(v: String) = _uiState.update { it.copy(ciudad = v) }
    fun onProvinciaChange(v: String) = _uiState.update { it.copy(provincia = v) }
    fun onCodigoPostalChange(v: String) = _uiState.update { it.copy(codigoPostal = v) }
    fun onNotasChange(v: String) = _uiState.update { it.copy(notasEntrega = v) }

    fun seleccionarAvatarEmoji(emoji: String) {
        _uiState.update { it.copy(avatarUrl = "emoji:$emoji") }
    }

    fun subirFotoAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            try {
                val ctx = getApplication<Application>()
                val tmp = File.createTempFile("avatar_", ".jpg", ctx.cacheDir)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                val url = repo.subirAvatar(tmp)
                tmp.delete()
                _uiState.update { it.copy(guardando = false, avatarUrl = url) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, error = e.message ?: "No se pudo subir la foto")
                }
            }
        }
    }

    fun guardarPerfil() {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.nombre.trim().length < 2) {
                _uiState.update { it.copy(error = "El nombre es obligatorio") }
                return@launch
            }
            _uiState.update { it.copy(guardando = true, error = null) }
            try {
                val actualizado = repo.actualizarPerfil(
                    ActualizarPerfilRequest(
                        nombre = s.nombre.trim(),
                        telefono = s.telefono.trim().ifEmpty { null },
                        avatarUrl = s.avatarUrl,
                        direccionEntrega = s.direccionEntrega.trim().ifEmpty { null },
                        ciudad = s.ciudad.trim().ifEmpty { null },
                        provincia = s.provincia.trim().ifEmpty { null },
                        codigoPostal = s.codigoPostal.trim().ifEmpty { null },
                        notasEntrega = s.notasEntrega.trim().ifEmpty { null }
                    )
                )
                _uiState.update {
                    it.copy(
                        guardando = false,
                        editando = false,
                        perfil = actualizado,
                        mensajeExito = "Perfil guardado",
                        avatarUrl = actualizado.avatarUrl
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, error = e.message ?: "Error al guardar")
                }
            }
        }
    }

    fun nivelActual(): NivelUsuario {
        val pts = _uiState.value.perfil?.puntosSaldo ?: 0
        return NivelUsuario.desdePuntos(pts)
    }

    fun cerrarSesion(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepo.logout()
            onDone()
        }
    }

    val avataresEmoji: List<String> = PikuAvataresEmoji.opciones
}
