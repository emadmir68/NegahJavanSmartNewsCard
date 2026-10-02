package ir.negahjavan.newscard.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import ir.negahjavan.newscard.BuildConfig
import ir.negahjavan.newscard.model.SmartAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object OnlineNewsImageGenerator {
    data class Generated(val uri: Uri, val source: String)

    suspend fun generate(context: Context, analysis: SmartAnalysis): Generated =
        withContext(Dispatchers.IO) {
            val base = BuildConfig.AI_BASE_URL.trim().trimEnd('/')

            if (base.isNotBlank()) {
                runCatching {
                    val connection = (URL(base + "/generate-image").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 8_000
                        readTimeout = 65_000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("Accept", "application/json")
                    }
                    val payload = JSONObject()
                        .put("title", analysis.title)
                        .put("text", analysis.fullText.take(12000))
                        .put("category", analysis.category.name)
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) error("image api unavailable")

                    val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val bytes = Base64.decode(response.getString("image_base64"), Base64.DEFAULT)
                    val file = File(context.cacheDir, "negahjavan_ai_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { it.write(bytes) }
                    Generated(Uri.fromFile(file), response.optString("source", "online-ai-image"))
                }.getOrNull()?.let { return@withContext it }
            }

            Generated(
                uri = AutoNewsImageGenerator.generate(context, analysis),
                source = "local-graphic"
            )
        }
}
