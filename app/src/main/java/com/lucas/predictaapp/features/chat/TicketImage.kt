package com.lucas.predictaapp.features.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Captura y preparación de fotos de tickets para mandarlas al modelo de visión. */
object TicketImage {
    // ~1.5k px es el sweet spot de visión de Claude: legible sin inflar tokens.
    private const val MAX_DIMENSION = 1568
    private const val JPEG_QUALITY = 82

    /** Uri de FileProvider en cache para que la cámara escriba la foto. */
    fun createCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "tickets").apply { mkdirs() }
        val file = File(dir, "ticket_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Lee el Uri (cámara o galería), lo reescala y lo devuelve como JPEG base64. */
    suspend fun toBase64Jpeg(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(context, uri)
            ?: throw IllegalStateException("No se pudo decodificar la imagen")
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        // Primera pasada: solo bounds para calcular el factor de submuestreo.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null

        var sample = 1
        var largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / 2 >= MAX_DIMENSION) {
            sample *= 2
            largest /= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
