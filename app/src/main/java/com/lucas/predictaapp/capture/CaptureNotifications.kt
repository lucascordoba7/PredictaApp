package com.lucas.predictaapp.capture

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.lucas.predictaapp.LaunchActions
import com.lucas.predictaapp.MainActivity
import com.lucas.predictaapp.R
import com.lucas.predictaapp.notifications.NotificationChannels
import com.lucas.predictaapp.ui.utils.fmtArs

private const val ACCENT = 0xFFE8A83E.toInt() // amber de la marca
private const val RESULT_TIMEOUT_MS = 6_000L

/** Notificaciones de la captura automática y sus acciones (ver [CaptureActionReceiver]). */
object CaptureNotifications {
    const val KEY_PURPOSE = "purpose"

    /** Comercio o ingreso: la IA clasifica bien, alcanza con poder corregir. */
    fun showCaptured(context: Context, expenseId: Long, payment: DetectedPayment, category: String, appName: String) {
        val title = if (payment.incoming) {
            "Registré un ingreso de $${payment.amount.fmtArs()}"
        } else {
            "Cargué $${payment.amount.fmtArs()} en ${payment.counterparty}"
        }
        val body = if (payment.incoming) {
            "${payment.counterparty} · $category · desde $appName"
        } else {
            "$category · desde $appName"
        }
        val edit = editIntent(context, expenseId)
        post(
            context, expenseId,
            base(context, title, body)
                .setContentIntent(edit)
                .addAction(0, "Editar", edit)
                .addAction(
                    0,
                    if (payment.incoming) "No es ingreso" else "No es gasto",
                    broadcast(context, expenseId, CaptureActionReceiver.ACTION_DISCARD, slot = 0),
                ),
        )
    }

    /**
     * Transferencia a una persona: el destinatario no dice en qué se fue la plata, así
     * que se pregunta. Ya quedó guardada en [savedCategory]; los botones la cambian y
     * "Responder" deja escribir el motivo para que lo clasifique la IA.
     */
    fun askPurpose(
        context: Context,
        expenseId: Long,
        payment: DetectedPayment,
        savedCategory: String,
        suggestions: List<String>,
    ) {
        val title = "Transferiste $${payment.amount.fmtArs()} a ${payment.counterparty}. ¿En qué fue?"
        val body = "Lo guardé en $savedCategory. Tocá otra categoría o respondé, por ejemplo \"asado\" o \"no es gasto\"."
        val builder = base(context, title, body).setContentIntent(editIntent(context, expenseId))

        suggestions.take(2).forEachIndexed { i, category ->
            builder.addAction(
                0, category,
                broadcast(context, expenseId, CaptureActionReceiver.ACTION_SET_CATEGORY, slot = i + 1) {
                    putExtra(CaptureActionReceiver.EXTRA_CATEGORY, category)
                },
            )
        }

        val reply = PendingIntent.getBroadcast(
            context,
            0,
            receiverIntent(context, expenseId, CaptureActionReceiver.ACTION_REPLY, slot = 9)
                .putExtra(CaptureActionReceiver.EXTRA_COUNTERPARTY, payment.counterparty),
            // RemoteInput necesita un PendingIntent mutable para inyectar el texto.
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        builder.addAction(
            NotificationCompat.Action.Builder(0, "Responder", reply)
                .addRemoteInput(RemoteInput.Builder(KEY_PURPOSE).setLabel("¿En qué fue?").build())
                .setAllowGeneratedReplies(false)
                .build(),
        )
        post(context, expenseId, builder)
    }

    /** Reemplaza la notificación por el resultado y se borra sola (corta el spinner del reply). */
    fun showResult(context: Context, expenseId: Long, text: String) {
        post(
            context, expenseId,
            base(context, text, null)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setTimeoutAfter(RESULT_TIMEOUT_MS),
        )
    }

    fun cancel(context: Context, expenseId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(expenseId))
    }

    // Rango propio para no pisar los ids (hash de string) que usa SystemNotifier.
    private fun notificationId(expenseId: Long): Int = 700_000_000 + (expenseId % 100_000_000).toInt()

    private fun base(context: Context, title: String, body: String?) =
        NotificationCompat.Builder(context, NotificationChannels.CAPTURA)
            .setSmallIcon(R.drawable.ic_stat_predicta)
            .setColor(ACCENT)
            .setContentTitle(title)
            .apply { body?.let { setContentText(it).setStyle(NotificationCompat.BigTextStyle().bigText(it)) } }
            .setAutoCancel(true)

    private fun post(context: Context, expenseId: Long, builder: NotificationCompat.Builder) {
        if (!hasPermission(context)) return
        NotificationManagerCompat.from(context).notify(notificationId(expenseId), builder.build())
    }

    private fun editIntent(context: Context, expenseId: Long) = PendingIntent.getActivity(
        context,
        notificationId(expenseId),
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LaunchActions.EXTRA_EXPENSE_ID, expenseId)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(
        context: Context,
        expenseId: Long,
        action: String,
        slot: Int,
        extras: Intent.() -> Unit = {},
    ) = PendingIntent.getBroadcast(
        context,
        0,
        receiverIntent(context, expenseId, action, slot).apply(extras),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // El data URI hace único cada PendingIntent (gasto + botón): los extras no cuentan para eso.
    private fun receiverIntent(context: Context, expenseId: Long, action: String, slot: Int) =
        Intent(context, CaptureActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("predicta://capture/$expenseId/$slot")
            putExtra(CaptureActionReceiver.EXTRA_EXPENSE_ID, expenseId)
        }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
