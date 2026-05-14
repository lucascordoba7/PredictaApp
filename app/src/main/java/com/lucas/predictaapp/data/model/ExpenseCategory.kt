package com.lucas.predictaapp.data.model

object ExpenseCategories {
    val all = listOf(
        "Supermercado",
        "Delivery",
        "Salidas",
        "Café",
        "Transporte",
        "Combustible",
        "Salud",
        "Farmacia",
        "Educación",
        "Entretenimiento",
        "Ropa",
        "Tecnología",
        "Hogar",
        "Servicios",
        "Suscripciones",
        "Viajes",
        "Deporte",
        "Mascotas",
        "Regalos",
        "Otros",
    )

    private val emojis = mapOf(
        "Supermercado"    to "🛒",
        "Delivery"        to "🛵",
        "Salidas"         to "🍽",
        "Café"            to "☕",
        "Transporte"      to "🚕",
        "Combustible"     to "⛽",
        "Salud"           to "💊",
        "Farmacia"        to "🏥",
        "Educación"       to "📚",
        "Entretenimiento" to "🎬",
        "Ropa"            to "👕",
        "Tecnología"      to "💻",
        "Hogar"           to "🏠",
        "Servicios"       to "⚡",
        "Suscripciones"   to "📱",
        "Viajes"          to "✈️",
        "Deporte"         to "🏃",
        "Mascotas"        to "🐾",
        "Regalos"         to "🎁",
        "Otros"           to "💸",
        "Ingreso"         to "💰",
    )

    fun emojiFor(category: String) = emojis[category] ?: "💸"

    val promptList = all.joinToString("|")
}
