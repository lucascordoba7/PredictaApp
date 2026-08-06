package com.lucas.predictaapp.data.local

/**
 * Cuenta activa en este dispositivo. Los repos la leen para filtrar y para estampar
 * el dueño en cada escritura a Supabase.
 *
 * Vive en memoria y se rehidrata desde DataStore al arrancar
 * ([UserPreferencesRepository.restoreSession]). Es `@Volatile` porque la escribe el
 * hilo de UI (login) y la leen las corrutinas de IO de los repos.
 *
 * Sin password: el id de cuenta se resuelve por email contra la tabla `profiles`.
 * No es un mecanismo de seguridad — cualquiera que sepa el email entra. Es lo que
 * evita que dos cuentas se pisen los datos, no lo que las protege.
 */
object Session {
    @Volatile
    var userId: String = ""
        private set

    val isActive: Boolean get() = userId.isNotBlank()

    fun open(id: String) { userId = id }

    fun close() { userId = "" }

    /**
     * Id para una cuenta nueva. Opaco y generado en el cliente: no hay servidor de auth
     * que los reparta. La cuenta original quedó con el literal 'me' (ver la migración
     * 001_cuentas_por_email.sql), y este formato no colisiona con eso.
     */
    fun newUserId(): String = "u_" + java.util.UUID.randomUUID().toString()
}
