package ir.negahjavan.newscard.model

import android.net.Uri

enum class NewsCategory(val fa: String) {
    BREAKING("فوری"), POLITICS("سیاسی"), SOCIAL("اجتماعی"), ECONOMY("اقتصادی"),
    SPORTS("ورزشی"), CULTURE("فرهنگی"), TECHNOLOGY("فناوری"), GENERAL("خبر")
}

enum class CardFormat(val fa: String, val width: Int, val height: Int) {
    STORY("استوری 9:16", 1080, 1920),
    LANDSCAPE("افقی 16:9", 1920, 1080),
    PORTRAIT("پست 4:5", 1080, 1350),
    SQUARE("مربع 1:1", 1080, 1080)
}

enum class CardTemplate(val fa: String, val subtitle: String) {
    SIGNATURE("امضای نگاه", "لوکس، تمیز و مناسب اکثر خبرها"),
    BREAKING_PLUS("فوری پلاس", "پرقدرت برای خبر فوری و تیترهای مهم"),
    EDITORIAL("ادیتوریال", "مجله‌ای، مینیمال و مناسب گزارش و تحلیل")
}

data class NewsDraft(
    val rawText: String = "",
    val imageUri: Uri? = null,
)

data class SmartAnalysis(
    val title: String,
    val lead: String,
    val category: NewsCategory,
    val accentWord: String = "",
    val hashtags: List<String> = emptyList(),
    val source: String = "local"
)
