package com.lucas.predictaapp.features.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Dictado con el SpeechRecognizer nativo de Android (sin API key; usa el motor del
 * sistema). Los resultados parciales van saliendo en vivo por onText (cada uno
 * reemplaza al anterior), así el campo del chat se llena mientras hablás.
 */
@Stable
class VoiceInputState internal constructor(val isAvailable: Boolean) {
    var listening by mutableStateOf(false)
        internal set
    internal var startAction: () -> Unit = {}
    internal var stopAction: () -> Unit = {}

    fun toggle() = if (listening) stopAction() else startAction()
}

@Composable
fun rememberVoiceInput(onText: (String) -> Unit): VoiceInputState {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    val state = remember { VoiceInputState(SpeechRecognizer.isRecognitionAvailable(context)) }

    val recognizer = remember {
        if (!state.isAvailable) return@remember null
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.firstResult()?.takeIf { it.isNotBlank() }?.let { currentOnText(it) }
                }

                override fun onResults(results: Bundle?) {
                    state.listening = false
                    results?.firstResult()?.let { currentOnText(it) }
                }

                override fun onError(error: Int) {
                    state.listening = false
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        val r = recognizer ?: return
        state.listening = true
        r.startListening(recognitionIntent())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) startListening() }

    state.startAction = {
        when {
            recognizer == null -> Unit
            hasAudioPermission(context) -> startListening()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // stopListening no corta en seco: dispara onResults con lo reconocido hasta ahí.
    state.stopAction = { recognizer?.stopListening() }

    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }
    return state
}

private fun Bundle.firstResult(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
}

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
