package com.piku.app.ui.theme

enum class ThemeMode(val id: String, val etiqueta: String) {
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro"),
    SYSTEM("system", "Seguir sistema");

    companion object {
        fun fromId(raw: String?): ThemeMode =
            entries.firstOrNull { it.id == raw?.lowercase() } ?: SYSTEM
    }
}
