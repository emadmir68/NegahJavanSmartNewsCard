package ir.negahjavan.newscard.model

import android.net.Uri

enum class NewsCategory(val fa: String) {
    BREAKING("فوری"),
    JUDICIAL("قضایی"),
    POLITICS("سیاسی"),
    SOCIAL("اجتماعی"),
    LOCAL("محلی"),
    ECONOMY("اقتصادی"),
    SPORTS("ورزشی"),
    CULTURE("فرهنگی"),
    TECHNOLOGY("فناوری"),
    GENERAL("خبر")
}

enum class NewsLength(val fa: String) {
    SHORT("کوتاه"),
    MEDIUM("متوسط"),
    LONG("بلند")
}

enum class CardFormat(val fa: String, val width: Int, val height: Int) {
    STORY("استوری 9:16", 1080, 1920),
    PORTRAIT("پست 4:5", 1080, 1350),
    SQUARE("مربع 1:1", 1080, 1080),
    LANDSCAPE("افقی 16:9", 1920, 1080)
}

enum class CardTemplate(val fa: String) {
    SIGNATURE("امضای نگاه"),
    BREAKING_PLUS("فوری پلاس"),
    EDITORIAL("ادیتوریال")
}

enum class PremiumTemplate(
    val fa: String,
    val shortFa: String
) {
    LUXURY_MINIMAL("امضای طلایی", "طلایی"),
    PHOTO_PREMIUM("استودیو بنفش", "بنفش"),
    EDITORIAL_GLASS("ردلاین ادیتوریال", "ردلاین")
}

data class NewsDraft(
    val rawText: String = "",
    val imageUri: Uri? = null,
)

data class SmartSlide(
    val title: String,
    val body: String,
    val badge: String = ""
)

data class SmartPage(
    val title: String = "",
    val body: String,
    val badge: String = "",
    val showImage: Boolean = false,
    val isFirst: Boolean = false,
    val isContinuation: Boolean = false,
    val bodyTextSizePx: Float = 30f
)

data class SmartAnalysis(
    val title: String,
    val lead: String,
    val category: NewsCategory,
    val badge: String,
    val length: NewsLength,
    val recommendedTemplate: CardTemplate,
    val slides: List<SmartSlide> = emptyList(),
    val fullText: String = "",
    val bodyText: String = "",
    val titleFromSource: Boolean = false,
    val accentWord: String = "",
    val hashtags: List<String> = emptyList(),
    val source: String = "local",
    val decisionNote: String = ""
)
