package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrResult(
    val text: String,
    val confidence: Float, // 0.0f to 1.0f
    val lineCount: Int,
    val wordCount: Int
)

object OcrService {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = processVisionText(visionText.text)
                        continuation.resume(Result.success(result))
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun recognizeText(context: Context, uri: Uri): Result<OcrResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = processVisionText(visionText.text)
                        continuation.resume(Result.success(result))
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun processVisionText(rawText: String): OcrResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return OcrResult(
                text = "",
                confidence = 0.0f,
                lineCount = 0,
                wordCount = 0
            )
        }

        val lines = trimmed.lines().filter { it.isNotBlank() }
        val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Compute actual confidence based on recognized structure and character distribution
        // High printable alphanumeric ratio and coherent lines indicate clean OCR capture
        val totalChars = trimmed.length.coerceAtLeast(1)
        val validChars = trimmed.count { it.isLetterOrDigit() || it in "=,-><|*(){}:_δΔ+ \n" }
        val charRatio = validChars.toFloat() / totalChars.toFloat()

        // Line quality score
        val lineScore = (lines.size.coerceIn(1, 10).toFloat() / 10f) * 0.3f
        val qualityScore = (charRatio * 0.7f + lineScore).coerceIn(0.1f, 0.99f)

        return OcrResult(
            text = trimmed,
            confidence = qualityScore,
            lineCount = lines.size,
            wordCount = words.size
        )
    }
}
