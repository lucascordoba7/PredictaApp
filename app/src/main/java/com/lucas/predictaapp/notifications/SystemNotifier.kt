package com.lucas.predictaapp.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lucas.predictaapp.MainActivity
import com.lucas.predictaapp.R
import com.lucas.predictaapp.data.model.DateGroup
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.repository.NotificationsRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Postea una notificación del sistema Android y la deja también en el feed in-app.
 * Dedup por id determinista (NotificationLog): cada evento se notifica una sola vez.
 */
object SystemNotifier {
    const val EXTRA_TARGET = "notif_target"
    private const val ACCENT = 0xFFE8A83E.toInt() // amber de la marca
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun post(context: Context, notif: PendingNotif) {
        if (NotificationLog.wasFired(context, notif.id)) return

        // Notificación del sistema (solo si hay permiso; en <13 siempre permitido).
        if (hasPermission(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET, notif.target.name)
            }
            val pending = PendingIntent.getActivity(
                context,
                notif.id.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val builder = NotificationCompat.Builder(context, notif.channel)
                .setSmallIcon(R.drawable.ic_stat_predicta)
                .setColor(ACCENT)
                .setContentTitle("${notif.emoji}  ${notif.title}")
                .setContentText(notif.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notif.body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            NotificationManagerCompat.from(context).notify(notif.id.hashCode(), builder.build())
        }

        // Feed in-app (queda en el inbox + badge), aunque no haya permiso de sistema.
        NotificationsRepository.add(
            Notification(
                id = notif.id,
                type = notif.type,
                icon = notif.emoji,
                typeLabel = notif.typeLabel,
                title = notif.title,
                body = notif.body,
                time = LocalTime.now().format(timeFmt),
                dateGroup = DateGroup.TODAY,
                unread = true,
            ),
        )

        NotificationLog.markFired(context, notif.id)
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
