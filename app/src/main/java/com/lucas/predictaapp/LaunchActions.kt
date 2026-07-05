package com.lucas.predictaapp

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Acción con la que se abrió la app (widget, app shortcut, share de otra app o
 * QuickActions). MainActivity la deja acá y la UI la consume donde corresponda:
 * el scaffold para la carga manual, el chat para voz/ticket/compartidos.
 */
sealed class LaunchAction {
    data object ManualExpense : LaunchAction()
    data object VoiceChat : LaunchAction()
    data object ScanTicket : LaunchAction()
    data class SharedText(val text: String) : LaunchAction()
    data class SharedImage(val uri: Uri) : LaunchAction()
}

object LaunchActions {
    /** Valor del extra en los Intents de widget/shortcuts. */
    const val EXTRA_ACTION = "predicta_action"
    const val ACTION_MANUAL = "manual"
    const val ACTION_VOICE = "voice"
    const val ACTION_SCAN = "scan"

    private val _pending = MutableStateFlow<LaunchAction?>(null)
    val pending: StateFlow<LaunchAction?> = _pending

    fun set(action: LaunchAction) {
        _pending.value = action
    }

    /** El consumidor la marca procesada para que no se re-dispare en recomposición. */
    fun clear() {
        _pending.value = null
    }

    fun fromExtra(value: String?): LaunchAction? = when (value) {
        ACTION_MANUAL -> LaunchAction.ManualExpense
        ACTION_VOICE -> LaunchAction.VoiceChat
        ACTION_SCAN -> LaunchAction.ScanTicket
        else -> null
    }
}
