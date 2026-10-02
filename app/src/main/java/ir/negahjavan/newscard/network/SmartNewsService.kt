package ir.negahjavan.newscard.network

import ir.negahjavan.newscard.BuildConfig
import ir.negahjavan.newscard.model.NewsCategory
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                readTimeout = 25_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val body = JSONObject().put("text", text).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return@withContext localFallback(text)
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            SmartAnalysis(
                title = json.optString("title").ifBlank { smartTitle(text) },
                lead = json.optString("lead").ifBlank { smartLead(text) },
                category = parseCategory(json.optString("category")),
                accentWord = json.optString("accent_word"),
                hashtags = json.optJSONArray("hashtags")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
                }.orEmpty(),
                source = "online"
            )
        } catch (_: Exception) {
            localFallback(text)
        }
    }

    private fun localFallback(text: String): SmartAnalysis {
        val category = detectCategory(text)
        val title = smartTitle(text)
        return SmartAnalysis(
            title = title,
            lead = smartLead(text),
            category = category,
            accentWord = pickAccent(title),
            hashtags = listOf("نگاه_جوان", category.fa.replace(' ', '_')),
            source = "local"
        )
    }

    private fun smartTitle(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        val first = clean.split(Regex("[.!؟!\\n]"), limit = 2).firstOrNull().orEmpty().trim()
        return when {
            first.length in 12..90 -> first
            clean.length > 90 -> clean.take(86).trimEnd() + "…"
            clean.isNotBlank() -> clean
            else -> "عنوان خبر را وارد کنید"
        }
    }

    private fun smartLead(text: String): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 135) return clean
        val candidate = clean.dropWhile { it != '.' && it != '؟' && it != '!' }
            .drop(1).trim()
        return (if (candidate.length > 40) candidate else clean).take(150).trimEnd() + "…"
    }

    private fun pickAccent(title: String): String = title.split(' ')
        .filter { it.length >= 4 }
        .maxByOrNull { it.length }
        .orEmpty()

    private fun detectCategory(text: String): NewsCategory {
        val t = text.lowercase()
        return when {
            listOf("فوری", "لحظاتی", "هشدار").any(t::contains) -> NewsCategory.BREAKING
            listOf("دولت", "مجلس", "وزیر", "انتخابات", "سیاسی").any(t::contains) -> NewsCategory.POLITICS
            listOf("فوتبال", "ورزش", "تیم", "مسابقه").any(t::contains) -> NewsCategory.SPORTS
            listOf("اقتصاد", "قیمت", "بازار", "بورس", "دلار").any(t::contains) -> NewsCategory.ECONOMY
            listOf("فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن").any(t::contains) -> NewsCategory.TECHNOLOGY
            listOf("فرهنگ", "هنر", "سینما", "کتاب").any(t::contains) -> NewsCategory.CULTURE
            listOf("شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه").any(t::contains) -> NewsCategory.SOCIAL
            else -> NewsCategory.GENERAL
        }
    }

    private fun parseCategory(value: String): NewsCategory {
        return NewsCategory.entries.firstOrNull {
            value.equals(it.name, ignoreCase = true) || value.contains(it.fa)
        } ?: NewsCategory.GENERAL
    }
}
