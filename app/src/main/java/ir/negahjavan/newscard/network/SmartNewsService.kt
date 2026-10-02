package ir.negahjavan.newscard.network

import ir.negahjavan.newscard.BuildConfig
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SmartNewsService {

    suspend fun analyze(text: String): SmartAnalysis = withContext(Dispatchers.IO) {
        val parsed = parseSource(text)
        val base = BuildConfig.AI_BASE_URL.trim().trimEnd('/')

        if (base.isBlank()) {
            return@withContext localAnalysis(parsed)
        }

        try {
            val connection = (URL(base + "/analyze").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 22_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val body = JSONObject().put("text", parsed.fullText).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode !in 200..299) {
                return@withContext localAnalysis(parsed)
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val localCategory = detectCategory(parsed.fullText)
            val localLength = detectLength(parsed.fullText)
            val category = parseCategory(json.optString("category"), localCategory)
            val length = parseLength(json.optString("length"), localLength)
            val badge = cleanInline(json.optString("badge"))
                .ifBlank { smartBadge(parsed.fullText, category) }
            val template = parseTemplate(json.optString("template"), category, length)

            SmartAnalysis(
                title = parsed.title,
                lead = firstUsefulSentence(parsed.bodyText),
                category = category,
                badge = badge,
                length = length,
                recommendedTemplate = template,
                slides = emptyList(),
                fullText = parsed.fullText,
                bodyText = parsed.bodyText,
                titleFromSource = parsed.titleFromSource,
                accentWord = pickAccent(parsed.title),
                hashtags = listOf("نگاه_جوان", category.fa.replace(' ', '_')),
                source = "online",
                decisionNote = decisionNote(length, parsed.bodyText)
            )
        } catch (_: Exception) {
            localAnalysis(parsed)
        }
    }

    private fun localAnalysis(parsed: ParsedSource): SmartAnalysis {
        val category = detectCategory(parsed.fullText)
        val length = detectLength(parsed.fullText)
        val badge = smartBadge(parsed.fullText, category)

        return SmartAnalysis(
            title = parsed.title,
            lead = firstUsefulSentence(parsed.bodyText),
            category = category,
            badge = badge,
            length = length,
            recommendedTemplate = chooseTemplate(category, length),
            slides = emptyList(),
            fullText = parsed.fullText,
            bodyText = parsed.bodyText,
            titleFromSource = parsed.titleFromSource,
            accentWord = pickAccent(parsed.title),
            hashtags = listOf("نگاه_جوان", category.fa.replace(' ', '_')),
            source = "local",
            decisionNote = decisionNote(length, parsed.bodyText)
        )
    }

    private data class ParsedSource(
        val fullText: String,
        val title: String,
        val bodyText: String,
        val titleFromSource: Boolean
    )

    private fun parseSource(text: String): ParsedSource {
        val normalized = normalizePreservingParagraphs(text)
        val lines = normalized.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ParsedSource(
                fullText = "",
                title = "خبر جدید",
                bodyText = "",
                titleFromSource = false
            )
        }

        val first = cleanDecorativeMarks(lines.first())
        val hasSeparateBody = lines.size >= 2

        val firstLooksLikeTitle =
            hasSeparateBody &&
                first.length in 12..180 &&
                !first.endsWith(".") &&
                !first.endsWith("؛")

        val title = if (firstLooksLikeTitle) {
            first
        } else {
            deriveTitle(normalized)
        }

        val body = if (firstLooksLikeTitle) {
            lines.drop(1)
                .map(::cleanDecorativeMarks)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
                .trim()
        } else {
            normalized
                .lines()
                .map(::cleanDecorativeMarks)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
                .trim()
        }

        return ParsedSource(
            fullText = normalized,
            title = title,
            bodyText = body,
            titleFromSource = firstLooksLikeTitle
        )
    }

    private fun normalizePreservingParagraphs(text: String): String =
        text.replace("\r", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .replace(Regex("[\\t ]+"), " ")
            .trim()

    private fun cleanDecorativeMarks(value: String): String =
        value
            .replace("⚖️", "")
            .replace("⚖", "")
            .replace("🔷️", "")
            .replace("🔷", "")
            .replace("🔹", "")
            .replace("🔸", "")
            .replace("▪️", "")
            .replace("▪", "")
            .replace("▫️", "")
            .replace("▫", "")
            .replace(Regex("^[-–—•]+\\s*"), "")
            .replace(Regex("[\\t ]+"), " ")
            .trim()

    private fun deriveTitle(text: String): String {
        val firstSentence = text
            .replace("\n", " ")
            .split(Regex("(?<=[.!؟])\\s+"))
            .firstOrNull()
            .orEmpty()
            .let(::cleanDecorativeMarks)

        return naturalCut(
            firstSentence.ifBlank { cleanDecorativeMarks(text) },
            118
        ).ifBlank { "خبر جدید" }
    }

    private fun firstUsefulSentence(body: String): String {
        if (body.isBlank()) return ""
        return body
            .replace("\n", " ")
            .split(Regex("(?<=[.!؟])\\s+"))
            .map(::cleanDecorativeMarks)
            .firstOrNull { it.length >= 24 }
            ?.let { naturalCut(it, 190) }
            .orEmpty()
    }

    private fun naturalCut(text: String, max: Int): String {
        val clean = cleanInline(text)
        if (clean.length <= max) return clean

        val part = clean.take(max)
        val punctuation = listOf('؟', '!', '.', '؛')
            .map { part.lastIndexOf(it) }
            .maxOrNull() ?: -1

        if (punctuation >= (max * .62f).toInt()) {
            return part.take(punctuation + 1).trim()
        }

        val lastSpace = part.lastIndexOf(' ')
        val safe = if (lastSpace >= (max * .66f).toInt()) {
            part.take(lastSpace)
        } else {
            part
        }

        return safe.trimEnd(' ', '،', ',', ':', '؛') + "…"
    }

    private fun cleanInline(value: String): String =
        cleanDecorativeMarks(value)
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun detectLength(text: String): NewsLength {
        val chars = text.replace("\n", " ").length
        val sentences = text
            .replace("\n", " ")
            .split(Regex("[.!؟]+"))
            .count { it.trim().length > 20 }

        return when {
            chars <= 360 && sentences <= 3 -> NewsLength.SHORT
            chars <= 1050 && sentences <= 7 -> NewsLength.MEDIUM
            else -> NewsLength.LONG
        }
    }

    private fun smartBadge(text: String, category: NewsCategory): String {
        val t = text.lowercase()
        return when {
            category == NewsCategory.BREAKING -> "فوری"
            "مطالبه" in t && ("مردم" in t || "شهرستان" in t || "روستا" in t) ->
                "مطالبه مردم"
            "اطلاعیه" in t -> "اطلاعیه"
            "هشدار" in t -> "هشدار"
            category == NewsCategory.JUDICIAL -> "قضایی"
            category == NewsCategory.POLITICS -> "سیاسی"
            category == NewsCategory.ECONOMY -> "اقتصادی"
            category == NewsCategory.SPORTS -> "ورزشی"
            category == NewsCategory.CULTURE -> "فرهنگی"
            category == NewsCategory.TECHNOLOGY -> "فناوری"
            category == NewsCategory.LOCAL -> "محلی"
            category == NewsCategory.SOCIAL -> "اجتماعی"
            else -> "خبر"
        }
    }

    private fun detectCategory(text: String): NewsCategory {
        val t = text.lowercase()
        return when {
            listOf("فوری", "لحظاتی پیش", "هم اکنون", "هم‌اکنون", "هشدار فوری").any(t::contains) ->
                NewsCategory.BREAKING
            listOf("دادگستری", "دادگاه", "قوه قضایی", "قضایی", "دادستان", "حقوق عامه").any(t::contains) ->
                NewsCategory.JUDICIAL
            listOf("انتخابات", "مجلس", "دولت", "وزیر", "سیاسی").any(t::contains) ->
                NewsCategory.POLITICS
            listOf("فوتبال", "ورزش", "تیم", "مسابقه").any(t::contains) ->
                NewsCategory.SPORTS
            listOf("اقتصاد", "قیمت", "بازار", "بورس", "دلار").any(t::contains) ->
                NewsCategory.ECONOMY
            listOf("فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن").any(t::contains) ->
                NewsCategory.TECHNOLOGY
            listOf("فرهنگ", "هنر", "سینما", "کتاب").any(t::contains) ->
                NewsCategory.CULTURE
            listOf("ساردوئیه", "جیرفت", "کهنوج", "عنبرآباد", "روستا", "شهرستان").any(t::contains) ->
                NewsCategory.LOCAL
            listOf("شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه").any(t::contains) ->
                NewsCategory.SOCIAL
            else -> NewsCategory.GENERAL
        }
    }

    private fun chooseTemplate(
        category: NewsCategory,
        length: NewsLength
    ): CardTemplate =
        when {
            category == NewsCategory.BREAKING -> CardTemplate.BREAKING_PLUS
            length == NewsLength.LONG -> CardTemplate.EDITORIAL
            category in setOf(
                NewsCategory.JUDICIAL,
                NewsCategory.POLITICS,
                NewsCategory.ECONOMY
            ) -> CardTemplate.EDITORIAL
            else -> CardTemplate.SIGNATURE
        }

    private fun parseCategory(
        value: String,
        fallback: NewsCategory
    ): NewsCategory =
        NewsCategory.entries.firstOrNull {
            value.equals(it.name, true) || value.contains(it.fa)
        } ?: fallback

    private fun parseLength(
        value: String,
        fallback: NewsLength
    ): NewsLength =
        NewsLength.entries.firstOrNull { value.equals(it.name, true) }
            ?: fallback

    private fun parseTemplate(
        value: String,
        category: NewsCategory,
        length: NewsLength
    ): CardTemplate =
        CardTemplate.entries.firstOrNull { value.equals(it.name, true) }
            ?: chooseTemplate(category, length)

    private fun pickAccent(title: String): String =
        title.split(' ')
            .map { it.trim('،', ',', ':', '؛', '.', '!', '؟') }
            .filter { it.length >= 5 }
            .maxByOrNull { it.length }
            .orEmpty()

    private fun decisionNote(
        length: NewsLength,
        body: String
    ): String {
        val chars = body.length
        return when (length) {
            NewsLength.SHORT ->
                "متن کامل حفظ می‌شود؛ اگر در یک کارت جا شود همان یک کارت ساخته می‌شود."
            NewsLength.MEDIUM ->
                "صفحه‌بندی با متن کامل انجام می‌شود؛ خلاصه‌سازی اجباری نیست."
            NewsLength.LONG ->
                "متن کامل $chars کاراکتری با محاسبه واقعی فضا بین کارت‌ها تقسیم می‌شود."
        }
    }
}
