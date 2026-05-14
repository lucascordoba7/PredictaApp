package com.lucas.predictaapp.ui.theme

import androidx.compose.ui.graphics.Color

val categoryColorMap: Map<String, Color> = mapOf(
    "Supermercado"    to Color(0xFF5CB85C),
    "Delivery"        to Color(0xFFFF6B6B),
    "Salidas"         to Color(0xFFC678DD),
    "Café"            to Color(0xFFE8A83E),
    "Transporte"      to Color(0xFF61AFEF),
    "Combustible"     to Color(0xFFE5C07B),
    "Salud"           to Color(0xFF56B6C2),
    "Farmacia"        to Color(0xFF98C379),
    "Educación"       to Color(0xFF4D9DE0),
    "Entretenimiento" to Color(0xFFFF8C69),
    "Ropa"            to Color(0xFFF92672),
    "Tecnología"      to Color(0xFF6699CC),
    "Hogar"           to Color(0xFFBA8C63),
    "Servicios"       to Color(0xFF7F8C8D),
    "Suscripciones"   to Color(0xFF9B59B6),
    "Viajes"          to Color(0xFF1ABC9C),
    "Deporte"         to Color(0xFF2ECC71),
    "Mascotas"        to Color(0xFFFF9F43),
    "Regalos"         to Color(0xFFEC407A),
    "Otros"           to Color(0xFF636E72),
    "Ingreso"         to Color(0xFF2ECC71),
)

fun categoryColor(category: String): Color =
    categoryColorMap[category] ?: Color(0xFF636E72)
