package com.lucas.predictaapp.capture

/**
 * Apps cuyas notificaciones se leen para cargar pagos solos. El resto se descarta sin
 * mirar el contenido. Paquetes verificados en el equipo del usuario (2026-11).
 *
 * Mercado Libre (`com.mercadolibre`) queda afuera a propósito: sus avisos de compra
 * duplican el de Mercado Pago, que es quien efectivamente cobra.
 */
object PaymentApps {
    private val names = mapOf(
        "com.mercadopago.wallet" to "Mercado Pago",
        "ar.com.santander.rio.mbanking" to "Santander",
        "com.tarjetanaranja.ncuenta" to "Naranja X",
        "ar.com.personalpay" to "Personal Pay",
    )

    fun isPaymentApp(packageName: String): Boolean = packageName in names

    fun displayName(packageName: String): String = names[packageName] ?: packageName
}
