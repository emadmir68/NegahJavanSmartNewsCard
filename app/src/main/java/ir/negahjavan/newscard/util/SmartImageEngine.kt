package ir.negahjavan.newscard.util

import android.graphics.Bitmap
import kotlin.math.max

object SmartImageEngine {

    enum class Mode {
        SMART_CROP,
        FIT_BLUR
    }

    data class ImagePlan(
        val mode: Mode,
        val focusX: Float,
        val focusY: Float,
        val faceCount: Int,
        val reason: String
    )

    suspend fun plan(
        bitmap: Bitmap,
        destinationAspect: Float
    ): ImagePlan {
        val focus = SmartImageFocus.detect(bitmap)
        val sourceAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val mismatch = max(
            sourceAspect / destinationAspect,
            destinationAspect / sourceAspect
        )

        val canCropWithoutCuttingFaces = if (focus.faceCount > 0) {
            cropContainsFaces(
                sourceAspect = sourceAspect,
                destinationAspect = destinationAspect,
                focus = focus
            )
        } else {
            true
        }

        val groupNeedsFit =
            focus.faceCount >= 2 &&
                (focus.faceSpanX > .36f || focus.faceSpanY > .42f || mismatch > 1.12f)

        val extremeAspect = mismatch > 1.42f
        val lowResolution = bitmap.width < 720 || bitmap.height < 480

        val mode = when {
            !canCropWithoutCuttingFaces -> Mode.FIT_BLUR
            groupNeedsFit -> Mode.FIT_BLUR
            extremeAspect -> Mode.FIT_BLUR
            lowResolution && mismatch > 1.18f -> Mode.FIT_BLUR
            else -> Mode.SMART_CROP
        }

        val reason = when {
            !canCropWithoutCuttingFaces -> "حفظ کامل چهره‌ها"
            groupNeedsFit -> "عکس گروهی"
            extremeAspect -> "نسبت تصویر نامتناسب"
            lowResolution && mismatch > 1.18f -> "حفظ کیفیت عکس"
            focus.faceCount == 1 -> "تمرکز روی چهره"
            else -> "کراپ متعادل"
        }

        return ImagePlan(
            mode = mode,
            focusX = focus.x,
            focusY = focus.y,
            faceCount = focus.faceCount,
            reason = reason
        )
    }

    private fun cropContainsFaces(
        sourceAspect: Float,
        destinationAspect: Float,
        focus: SmartImageFocus.FocusPoint
    ): Boolean {
        val margin = .035f

        val cropLeft: Float
        val cropTop: Float
        val cropRight: Float
        val cropBottom: Float

        if (sourceAspect > destinationAspect) {
            val cropWidth = (destinationAspect / sourceAspect).coerceIn(0f, 1f)
            cropLeft = (focus.x - cropWidth / 2f)
                .coerceIn(0f, 1f - cropWidth)
            cropRight = cropLeft + cropWidth
            cropTop = 0f
            cropBottom = 1f
        } else {
            val cropHeight = (sourceAspect / destinationAspect).coerceIn(0f, 1f)
            cropTop = (focus.y - cropHeight / 2f)
                .coerceIn(0f, 1f - cropHeight)
            cropBottom = cropTop + cropHeight
            cropLeft = 0f
            cropRight = 1f
        }

        return focus.left >= cropLeft + margin &&
            focus.right <= cropRight - margin &&
            focus.top >= cropTop + margin &&
            focus.bottom <= cropBottom - margin
    }
}
