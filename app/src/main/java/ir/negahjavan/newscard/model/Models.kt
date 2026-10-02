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

data class NewsDraft(
    val rawText: String = "",
    val imageUri: Uri? = null,
)

data class SmartSlide(
    val title: String,
    val body: String,
    val badge: String = ""
)

data class SmartAnalysis(
    val title: String,
    val lead: String,
    val category: NewsCategory,
    val badge: String,
    val length: NewsLength,
    val recommendedTemplate: CardTemplate,
    val slides: List<SmartSlide>,
    val accentWord: String = "",
    val hashtags: List<String> = emptyList(),
    val source: String = "local",
    val decisionNote: String = ""
)
