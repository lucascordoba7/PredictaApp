package com.lucas.predictaapp.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.lucas.predictaapp.data.local.Session
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "CaptureActionReceiver"

/** Botones de las notificaciones de captura: descartar, elegir categoría o responder el motivo. */
class CaptureActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L).takeIf { it > 0 } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Sin sesión las escrituras a Supabase irían con userId vacío y no tocarían nada.
                if (!Session.isActive && !UserPreferencesRepository.restoreSession(context)) {
                    CaptureNotifications.cancel(context, expenseId)
                    return@launch
                }
                when (intent.action) {
                    ACTION_DISCARD -> {
                        CaptureNotifications.cancel(context, expenseId)
                        ExpensesRepository.delete(expenseId)
                    }
                    ACTION_SET_CATEGORY -> {
                        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: return@launch
                        setCategory(context, expenseId, category)
                    }
                    ACTION_REPLY -> {
                        val answer = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(CaptureNotifications.KEY_PURPOSE)?.toString()?.trim()
                        if (answer.isNullOrEmpty()) {
                            CaptureNotifications.cancel(context, expenseId)
                            return@launch
                        }
                        reply(context, expenseId, answer, intent.getStringExtra(EXTRA_COUNTERPARTY).orEmpty())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun setCategory(context: Context, expenseId: Long, category: String) {
        val ok = ExpensesRepository.setCategory(expenseId, category)
        CaptureNotifications.showResult(context, expenseId, if (ok) "Listo, quedó en $category" else "Ese gasto ya no existe")
    }

    private suspend fun reply(context: Context, expenseId: Long, answer: String, counterparty: String) {
        val categories = CategoryRepository.categories.first()
            .filter { it.type == CategoryType.EXPENSE }.map { it.name }
            .ifEmpty { ExpenseCategories.expenseNames }
        val result = try {
            PaymentTextParser.classifyPurpose(answer, counterparty, categories)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo clasificar la respuesta", e)
            null
        }
        when (result) {
            is PurposeAnswer.Category -> setCategory(context, expenseId, result.name)
            PurposeAnswer.NotExpense -> {
                ExpensesRepository.delete(expenseId)
                CaptureNotifications.showResult(context, expenseId, "Listo, no lo cuento como gasto")
            }
            null -> CaptureNotifications.showResult(
                context, expenseId, "No entendí \"$answer\". Quedó como estaba; editalo desde la app.",
            )
        }
    }

    companion object {
        const val ACTION_DISCARD = "com.lucas.predictaapp.action.CAPTURE_DISCARD"
        const val ACTION_SET_CATEGORY = "com.lucas.predictaapp.action.CAPTURE_SET_CATEGORY"
        const val ACTION_REPLY = "com.lucas.predictaapp.action.CAPTURE_REPLY"
        const val EXTRA_EXPENSE_ID = "expense_id"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_COUNTERPARTY = "counterparty"
    }
}
