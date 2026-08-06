package com.lucas.predictaapp.data.model

import kotlinx.serialization.Serializable

/**
 * Una fila por cuenta en Supabase (tabla `profiles`). El `email` es único y es la
 * credencial: no hay password. `id` es el identificador opaco de la cuenta y es lo que
 * viaja como `userId` en el resto de las tablas.
 *
 * La cuenta original conserva el id literal "me" por la migración 001; las nuevas
 * usan el formato de [com.lucas.predictaapp.data.local.Session.newUserId].
 *
 * El perfil vive en DataStore para lectura rápida; esta tabla es la fuente de verdad
 * al iniciar sesión en un dispositivo nuevo.
 */
@Serializable
data class Profile(
    val id: String,
    val name: String,
    val email: String = "",
    val income: Int = 0,
    val paydayDay: Int = 1,
    val fixedMonthly: Int = 0,
)
