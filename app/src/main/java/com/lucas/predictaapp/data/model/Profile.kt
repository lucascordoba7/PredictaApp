package com.lucas.predictaapp.data.model

import kotlinx.serialization.Serializable

/**
 * Espejo del perfil en Supabase (tabla `profiles`). Single-user: una sola fila con
 * id fijo "me". El perfil "real" vive en DataStore; esto solo lo respalda en la nube
 * para sobrevivir reinstalaciones.
 */
@Serializable
data class Profile(
    val id: String = "me",
    val name: String,
    val email: String = "",
    val income: Int = 0,
    val paydayDay: Int = 1,
    val fixedMonthly: Int = 0,
)
