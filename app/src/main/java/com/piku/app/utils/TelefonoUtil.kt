package com.piku.app.utils

object TelefonoUtil {

    fun soloDigitos(texto: String): String = texto.filter { it.isDigit() }

    /**
     * Valida teléfono argentino para comercio (obligatorio, mín. 8 dígitos).
     */
    fun validarComercio(telefono: String): String? {
        val digits = soloDigitos(telefono.trim())
        if (digits.isEmpty()) return "El teléfono es obligatorio para comercios"
        if (digits.length < 8) return "El teléfono debe tener al menos 8 dígitos (ej: 3434567890)"
        if (digits.length > 15) return "El teléfono es demasiado largo"
        return null
    }
}
