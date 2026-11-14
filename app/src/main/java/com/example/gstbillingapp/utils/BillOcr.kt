package com.example.gstbillingapp.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device OCR (ML Kit, Latin script). Handwriting and Devanagari are NOT reliably read. */
object BillOcr {
    suspend fun readText(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    cont.resume(result.text)
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    cont.resumeWithException(e)
                }
            cont.invokeOnCancellation { recognizer.close() }
        }
    }
}
