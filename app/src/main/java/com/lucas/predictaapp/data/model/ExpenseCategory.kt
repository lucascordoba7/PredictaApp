package com.lucas.predictaapp.data.model

object ExpenseCategories {
    val seed: List<Category> = listOf(
        Category(name = "Supermercado",    emoji = "🛒", color = "#E8A83E", type = CategoryType.EXPENSE,  sortOrder = 0),
        Category(name = "Delivery",        emoji = "🛵", color = "#E07A7A", type = CategoryType.EXPENSE,  sortOrder = 1),
        Category(name = "Salidas",         emoji = "🍽", color = "#E66B57", type = CategoryType.EXPENSE,  sortOrder = 2),
        Category(name = "Café",            emoji = "☕", color = "#C58F6F", type = CategoryType.EXPENSE,  sortOrder = 3),
        Category(name = "Transporte",      emoji = "🚌", color = "#6A92E8", type = CategoryType.EXPENSE,  sortOrder = 4),
        Category(name = "Combustible",     emoji = "⛽", color = "#C58F6F", type = CategoryType.EXPENSE,  sortOrder = 5),
        Category(name = "Salud",           emoji = "💊", color = "#E87AB6", type = CategoryType.EXPENSE,  sortOrder = 6),
        Category(name = "Farmacia",        emoji = "🏥", color = "#6FD3C5", type = CategoryType.EXPENSE,  sortOrder = 7),
        Category(name = "Educación",       emoji = "📚", color = "#9DE8A0", type = CategoryType.EXPENSE,  sortOrder = 8),
        Category(name = "Entretenimiento", emoji = "🎬", color = "#9D7AE8", type = CategoryType.EXPENSE,  sortOrder = 9),
        Category(name = "Ropa",            emoji = "👕", color = "#8AA8E8", type = CategoryType.EXPENSE,  sortOrder = 10),
        Category(name = "Tecnología",      emoji = "💻", color = "#6A92E8", type = CategoryType.EXPENSE,  sortOrder = 11),
        Category(name = "Hogar",           emoji = "🏠", color = "#6FC58B", type = CategoryType.EXPENSE,  sortOrder = 12),
        Category(name = "Servicios",       emoji = "⚡", color = "#8AA8E8", type = CategoryType.EXPENSE,  sortOrder = 13),
        Category(name = "Suscripciones",   emoji = "🔁", color = "#9D7AE8", type = CategoryType.EXPENSE,  sortOrder = 14),
        Category(name = "Viajes",          emoji = "✈️", color = "#6FD3C5", type = CategoryType.EXPENSE,  sortOrder = 15),
        Category(name = "Deporte",         emoji = "🏃", color = "#6FC58B", type = CategoryType.EXPENSE,  sortOrder = 16),
        Category(name = "Mascotas",        emoji = "🐾", color = "#6FD3C5", type = CategoryType.EXPENSE,  sortOrder = 17),
        Category(name = "Regalos",         emoji = "🎁", color = "#E8D14A", type = CategoryType.EXPENSE,  sortOrder = 18),
        Category(name = "Otros",           emoji = "💸", color = "#636E72", type = CategoryType.EXPENSE,  sortOrder = 19),
        Category(name = "Salario",         emoji = "💼", color = "#6FC58B", type = CategoryType.INCOME,   sortOrder = 20),
        Category(name = "Freelance",       emoji = "💰", color = "#E8A83E", type = CategoryType.INCOME,   sortOrder = 21),
        Category(name = "Inversiones",     emoji = "📈", color = "#6FC58B", type = CategoryType.INCOME,   sortOrder = 22),
        Category(name = "Otros ingresos",  emoji = "🏦", color = "#E66B57", type = CategoryType.INCOME,   sortOrder = 23),
    )

    private val emojiMap: Map<String, String> = seed.associate { it.name to it.emoji } +
        mapOf("Ingreso" to "💰")
    private val colorMap: Map<String, String> = seed.associate { it.name to it.color } +
        mapOf("Ingreso" to "#6FC58B")

    fun emojiFor(name: String): String = emojiMap[name] ?: "💸"
    fun colorHexFor(name: String): String = colorMap[name] ?: "#636E72"

    val expenseNames: List<String> = seed.filter { it.type == CategoryType.EXPENSE }.map { it.name }
}
