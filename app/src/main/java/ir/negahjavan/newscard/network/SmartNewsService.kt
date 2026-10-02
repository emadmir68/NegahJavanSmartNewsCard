package ir.negahjavan.newscard.network

import ir.negahjavan.newscard.BuildConfig
import ir.negahjavan.newscard.model.CardTemplate
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.NewsLength
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.model.SmartSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SmartNewsService {

    suspend fun analyze(text: String): SmartAnalysis = withContext(Dispatchers.IO) {
        val base = BuildConfig.AI_BASE_URL.trim().trimEnd('/')
        if (base.isBlank()) return@withContext localFallback(text)

        try {
            val connection = (URL("$base/analyze").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 35_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val body = JSONObject().put("text", text).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode !in 200..299) {
                return@withContext localFallback(text)
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val title = json.optString("title").ifBlank { smartTitle(text) }
            val lead = json.optString("lead").ifBlank { smartLead(text) }
            val category = parseCategory(json.optString("category"))
            val length = parseLength(json.optString("length"), text)
            val template = parseTemplate(json.optString("template"), category, length)
            val badge = json.optString("badge").ifBlank { smartBadge(text, category) }
            val slides = parseSlides(json.optJSONArray("slides"))
                .ifEmpty { makeSlides(text, title, lead, badge, length) }

            SmartAnalysis(
                title = title,
                lead = lead,
                category = category,
                badge = badge,
                length = length,
                recommendedTemplate = template,
                slides = slides,
                accentWord = json.optString("accent_word").ifBlank { pickAccent(title) },
                hashtags = json.optJSONArray("hashtags")?.toStringList().orEmpty(),
                source = "online",
                decisionNote = json.optString("decision_note").ifBlank {
                    autoDecisionNote(length, slides.size, template, category)
                }
            )
        } catch (_: Exception) {
            localFallback(text)
        }
    }

    private fun localFallback(text: String): SmartAnalysis {
        val clean = normalize(text)
        val category = detectCategory(clean)
        val length = detectLength(clean)
        val title = smartTitle(text)
        val lead = smartLead(text)
        val badge = smartBadge(clean, category)
        val template = chooseTemplate(category, length)
        val slides = makeSlides(clean, title, lead, badge, length)

        return SmartAnalysis(
            title = title,
            lead = lead,
            category = category,
            badge = badge,
            length = length,
            recommendedTemplate = template,
            slides = slides,
            accentWord = pickAccent(title),
            hashtags = listOf("نگاه_جوان", category.fa.replace(' ', '_')),
            source = "local",
            decisionNote = autoDecisionNote(length, slides.size, template, category)
        )
    }

    private fun normalize(text: String): String =
        text.replace(Regex("[\\t ]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    private fun smartTitle(text: String): String {
        val paragraphs = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val first = paragraphs.firstOrNull().orEmpty()
        val clean = normalize(text).replace("\n", " ")

        return when {
            first.length in 12..115 -> first
            first.length > 115 -> shortenAtBoundary(first, 108)
            clean.length > 108 -> shortenAtBoundary(clean, 108)
            clean.isNotBlank() -> clean
            else -> "خبر جدید"
        }
    }

    private fun smartLead(text: String): String {
        val paragraphs = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val source = when {
            paragraphs.size >= 2 -> paragraphs[1]
            else -> sentenceList(text).drop(1).firstOrNull() ?: normalize(text)
        }
        return shortenAtBoundary(source, 190)
    }

    private fun detectLength(text: String): NewsLength {
        val n = normalize(text).length
        return when {
            n < 420 -> NewsLength.SHORT
            n < 1050 -> NewsLength.MEDIUM
            else -> NewsLength.LONG
        }
    }

    private fun makeSlides(
        text: String,
        title: String,
        lead: String,
        badge: String,
        length: NewsLength
    ): List<SmartSlide> {
        if (length != NewsLength.LONG) {
            val body = if (length == NewsLength.SHORT) lead else mediumBody(text, lead)
            return listOf(SmartSlide(title = title, body = body, badge = badge))
        }

        val sentences = sentenceList(text)
            .filterNot { it.contains(title.take(40)) }
            .map { it.trim(' ', '🔷', '▪', '•') }
            .filter { it.length > 25 }

        val chunks = sequentialChunks(sentences, 3)
        val headings = listOf("نکات اصلی", "ادامه خبر", "جمع‌بندی")

        val result = mutableListOf(
            SmartSlide(
                title = title,
                body = shortenAtBoundary(lead, 210),
                badge = badge
            )
        )

        chunks.take(3).forEachIndexed { index, chunk ->
            result += SmartSlide(
                title = headings[index],
                body = shortenAtBoundary(chunk, 430),
                badge = if (index == 2) "جمع‌بندی" else "ادامه"
            )
        }

        while (result.size < 4) {
            val i = (result.size - 1).coerceIn(0, 2)
            result += SmartSlide(
                title = headings[i],
                body = shortenAtBoundary(lead, 300),
                badge = if (i == 2) "جمع‌بندی" else "ادامه"
            )
        }

        return result.take(4)
    }

    private fun mediumBody(text: String, lead: String): String {
        val sentences = sentenceList(text)
            .filter { it.length > 25 }
            .drop(1)
            .take(2)
            .joinToString(" ")

        return shortenAtBoundary(
            listOf(lead, sentences).filter { it.isNotBlank() }.joinToString(" "),
            330
        )
    }

    private fun sequentialChunks(sentences: List<String>, count: Int): List<String> {
        if (sentences.isEmpty()) return emptyList()
        val result = MutableList(count) { mutableListOf<String>() }
        val per = kotlin.math.ceil(sentences.size.toDouble() / count).toInt().coerceAtLeast(1)

        sentences.forEachIndexed { index, sentence ->
            val bucket = (index / per).coerceAtMost(count - 1)
            result[bucket] += sentence
        }

        return result.map { it.joinToString(" ") }.filter { it.isNotBlank() }
    }

    private fun sentenceList(text: String): List<String> =
        normalize(text)
            .replace("\n", " ")
            .split(Regex("(?<=[.!؟])\\s+|(?=🔷)|(?=▪)|(?=•)"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun shortenAtBoundary(text: String, max: Int): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= max) return clean

        val cut = clean.take(max)
        val lastSpace = cut.lastIndexOf(' ')
        val safe = if (lastSpace > (max * 0.65).toInt()) cut.take(lastSpace) else cut
        return safe.trimEnd(' ', '،', ',', ':', '؛') + "…"
    }

    private fun smartBadge(text: String, category: NewsCategory): String {
        val t = text.lowercase()
        return when {
            category == NewsCategory.BREAKING -> "فوری"
            "مطالبه" in t && ("مردم" in t || "شهرستان" in t || "روستا" in t) -> "مطالبه مردم"
            category == NewsCategory.JUDICIAL -> "قضایی"
            "گزارش" in t -> "گزارش"
            "اطلاعیه" in t -> "اطلاعیه"
            "هشدار" in t -> "هشدار"
            category == NewsCategory.POLITICS -> "سیاسی"
            category == NewsCategory.ECONOMY -> "اقتصاد"
            category == NewsCategory.SPORTS -> "ورزش"
            category == NewsCategory.CULTURE -> "فرهنگ"
            category == NewsCategory.TECHNOLOGY -> "فناوری"
            category == NewsCategory.LOCAL -> "محلی"
            else -> "ویژه"
        }
    }

    private fun chooseTemplate(category: NewsCategory, length: NewsLength): CardTemplate =
        when {
            category == NewsCategory.BREAKING -> CardTemplate.BREAKING_PLUS
            length == NewsLength.LONG -> CardTemplate.EDITORIAL
            category in setOf(NewsCategory.JUDICIAL, NewsCategory.POLITICS, NewsCategory.ECONOMY) ->
                CardTemplate.EDITORIAL
            else -> CardTemplate.SIGNATURE
        }

    private fun detectCategory(text: String): NewsCategory {
        val t = text.lowercase()
        return when {
            listOf("فوری", "لحظاتی", "هشدار فوری").any(t::contains) -> NewsCategory.BREAKING
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

    private fun pickAccent(title: String): String =
        title.split(' ')
            .map { it.trim('،', ',', ':', '؛', '.', '!', '؟') }
            .filter { it.length >= 5 }
            .maxByOrNull { it.length }
            .orEmpty()

    private fun parseCategory(value: String): NewsCategory =
        NewsCategory.entries.firstOrNull {
            value.equals(it.name, true) || value.contains(it.fa)
        } ?: NewsCategory.GENERAL

    private fun parseLength(value: String, text: String): NewsLength =
        NewsLength.entries.firstOrNull { value.equals(it.name, true) }
            ?: detectLength(text)

    private fun parseTemplate(
        value: String,
        category: NewsCategory,
        length: NewsLength
    ): CardTemplate =
        CardTemplate.entries.firstOrNull { value.equals(it.name, true) }
            ?: chooseTemplate(category, length)

    private fun parseSlides(array: JSONArray?): List<SmartSlide> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let {
                val title = it.optString("title")
                val body = it.optString("body")
                if (title.isBlank() && body.isBlank()) null
                else SmartSlide(
                    title = title.ifBlank { "ادامه خبر" },
                    body = body,
                    badge = it.optString("badge")
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

    private fun autoDecisionNote(
        length: NewsLength,
        slideCount: Int,
        template: CardTemplate,
        category: NewsCategory
    ): String {
        val base = when (length) {
            NewsLength.SHORT ->
                "خبر کوتاه تشخیص داده شد؛ یک کارت فشرده با قالب " + template.fa + " ساخته شد."
            NewsLength.MEDIUM ->
                "خبر متوسط تشخیص داده شد؛ یک کارت کامل با تیتر، لید و نکات اصلی ساخته شد."
            NewsLength.LONG ->
                "خبر بلند تشخیص داده شد؛ متن به " + slideCount + " اسلاید تقسیم و قالب " + template.fa + " انتخاب شد."
        }
        return base + " دسته: " + category.fa
    }
}
