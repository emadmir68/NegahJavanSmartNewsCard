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
        val faceCount: Int
    )

    suspend fun detect(bitmap: Bitmap): FocusPoint =
        suspendCancellableCoroutine { continuation ->
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val detector = FaceDetection.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val point = if (faces.isEmpty()) {
                        FocusPoint(.5f, .46f, 0)
                    } else {
                        val centers = faces.map { face ->
                            val box = face.boundingBox
                            Pair(
                                (box.centerX().toFloat() / bitmap.width).coerceIn(0f, 1f),
                                (box.centerY().toFloat() / bitmap.height).coerceIn(0f, 1f)
                            )
                        }
                        FocusPoint(
                            centers.map { it.first }.average().toFloat().coerceIn(.15f, .85f),
                            centers.map { it.second }.average().toFloat().coerceIn(.15f, .80f),
                            faces.size
                        )
                    }
                    if (continuation.isActive) continuation.resume(point)
                    detector.close()
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(FocusPoint(.5f, .46f, 0))
                    detector.close()
                }
        }
}
