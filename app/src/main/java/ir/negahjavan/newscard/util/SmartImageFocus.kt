package ir.negahjavan.newscard.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object SmartImageFocus {

    data class FocusPoint(
        val x: Float,
        val y: Float,
        val faceCount: Int,
        val left: Float = .35f,
        val top: Float = .30f,
        val right: Float = .65f,
        val bottom: Float = .70f
    ) {
        val faceSpanX: Float get() = (right - left).coerceIn(0f, 1f)
        val faceSpanY: Float get() = (bottom - top).coerceIn(0f, 1f)
    }

    suspend fun detect(bitmap: Bitmap): FocusPoint =
        suspendCancellableCoroutine { continuation ->
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(.08f)
                .build()

            val detector = FaceDetection.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val point = if (faces.isEmpty()) {
                        FocusPoint(.5f, .46f, 0)
                    } else {
                        val minLeft = faces.minOf { it.boundingBox.left }
                            .toFloat().div(bitmap.width).coerceIn(0f, 1f)
                        val minTop = faces.minOf { it.boundingBox.top }
                            .toFloat().div(bitmap.height).coerceIn(0f, 1f)
                        val maxRight = faces.maxOf { it.boundingBox.right }
                            .toFloat().div(bitmap.width).coerceIn(0f, 1f)
                        val maxBottom = faces.maxOf { it.boundingBox.bottom }
                            .toFloat().div(bitmap.height).coerceIn(0f, 1f)

                        FocusPoint(
                            x = ((minLeft + maxRight) / 2f).coerceIn(.08f, .92f),
                            y = ((minTop + maxBottom) / 2f).coerceIn(.08f, .88f),
                            faceCount = faces.size,
                            left = minLeft,
                            top = minTop,
                            right = maxRight,
                            bottom = maxBottom
                        )
                    }

                    if (continuation.isActive) continuation.resume(point)
                    detector.close()
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(FocusPoint(.5f, .46f, 0))
                    }
                    detector.close()
                }
        }
}
