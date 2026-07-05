package com.lucas.predictaapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lucas.predictaapp.LaunchActions
import com.lucas.predictaapp.MainActivity
import com.lucas.predictaapp.R

/**
 * Widget de pantalla principal: registrar un gasto en un tap.
 * Cada botón abre MainActivity con el extra de acción; LaunchActions hace el resto
 * (＋ → sheet de carga manual, mic → chat dictando, cámara → escanear ticket).
 */
class PredictaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    private fun buildViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_predicta).apply {
            setOnClickPendingIntent(R.id.widget_root, launchPending(context, null, 0))
            setOnClickPendingIntent(R.id.widget_voice, launchPending(context, LaunchActions.ACTION_VOICE, 1))
            setOnClickPendingIntent(R.id.widget_camera, launchPending(context, LaunchActions.ACTION_SCAN, 2))
            setOnClickPendingIntent(R.id.widget_add, launchPending(context, LaunchActions.ACTION_MANUAL, 3))
        }

    private fun launchPending(context: Context, launchExtra: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Acción distinta por botón: Intents que solo difieren en extras se pisan entre sí.
            action = "com.lucas.predictaapp.widget.$requestCode"
            launchExtra?.let { putExtra(LaunchActions.EXTRA_ACTION, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
