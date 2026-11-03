package com.lucas.predictaapp.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Escucha las notificaciones del sistema y pasa a [PaymentCapture] solo las de
 * [PaymentApps]. Requiere que el usuario habilite "Acceso a notificaciones" para Predicta.
 */
class PaymentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!PaymentApps.isPaymentApp(sbn.packageName)) return
        val notification = sbn.notification
        // El resumen de un grupo repite (o agrega) lo que ya traen las hijas.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val text = readText(notification) ?: return

        scope.launch { PaymentCapture.process(
                applicationContext, CaptureOrigin.NOTIFICATION, sbn.packageName, text, sbn.postTime,
            ) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        isConnected = false
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        isConnected = false
        scope.cancel()
        super.onDestroy()
    }

    private fun readText(notification: Notification): String? {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        // bigText trae el texto completo cuando el contentText viene truncado.
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        return "$title\n$body".trim().takeIf { it.isNotEmpty() }
    }

    companion object {
        /** true = el sistema conectó el listener en este proceso (ver [PaymentScreenReader.isRunning]). */
        @Volatile
        var isConnected = false
            private set

        /**
         * Acceso dado pero listener desconectado (pasa tras reinstalar): se le pide al sistema
         * que lo vuelva a conectar. A diferencia de accesibilidad, esto no requiere al usuario.
         */
        fun rebindIfStuck(context: Context) {
            if (isConnected || !isEnabled(context)) return
            requestRebind(ComponentName(context, PaymentNotificationListener::class.java))
        }

        fun isEnabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }
}
