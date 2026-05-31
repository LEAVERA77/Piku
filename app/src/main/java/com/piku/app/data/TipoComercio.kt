package com.piku.app.data

enum class TipoComercio(
    val id: String,
    val etiqueta: String,
    val emoji: String,
    val categoria: String
) {
    CAFETERIA("cafeteria", "Cafetería", "☕", "cafeteria"),
    RESTAURANTE("restaurante", "Restaurante", "🍽️", "restaurant"),
    ROPA("ropa", "Tienda de ropa", "👕", "indumentaria"),
    SUPERMERCADO("supermercado", "Supermercado", "🛒", "supermercado"),
    FARMACIA("farmacia", "Farmacia", "💊", "farmacia"),
    PELUQUERIA("peluqueria", "Peluquería", "✂️", "servicios"),
    LIBRERIA("libreria", "Librería", "📚", "otros"),
    OTRO("otro", "Otro", "🏪", "otros");

    companion object {
        fun desdeId(raw: String?): TipoComercio {
            val id = raw?.trim()?.lowercase() ?: return OTRO
            return entries.firstOrNull { it.id == id || it.categoria == id } ?: OTRO
        }

        fun emojiPara(comercio: com.piku.app.data.model.Comercio): String {
            if (!comercio.iconoEmoji.isNullOrBlank()) return comercio.iconoEmoji
            return desdeId(comercio.tipoComercio ?: comercio.categoria).emoji
        }
    }
}
