package com.translation.counter.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.google.gson.JsonParser

object AiTranslationEngine {

    private const val TAG = "AiTranslationEngine"
    
    // Optional User Provided API Key
    var customApiKey: String = ""

    suspend fun translateWithAi(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        return withContext(Dispatchers.IO) {
            try {
                // Parse language code ISO 639-1
                val src = parseLangIsoCode(sourceLangCode)
                val tgt = parseLangIsoCode(targetLangCode)

                // High Speed Real-time Neural Translation REST API
                val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$src&tl=$tgt&dt=t&q=" + URLEncoder.encode(trimmed, "UTF-8")
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                    val responseStr = reader.use { it.readText() }
                    conn.disconnect()

                    val jsonArray = JsonParser.parseString(responseStr).asJsonArray
                    val sentencesArray = jsonArray.get(0).asJsonArray
                    val translatedBuilder = StringBuilder()
                    for (i in 0 until sentencesArray.size()) {
                        val sentenceObj = sentencesArray.get(i).asJsonArray
                        translatedBuilder.append(sentenceObj.get(0).asString)
                    }
                    val result = translatedBuilder.toString()
                    Log.d(TAG, "AI Translation Success ($src -> $tgt): '$trimmed' => '$result'")
                    return@withContext result
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI Translation failed, using fallback parser", e)
            }

            return@withContext trimmed
        }
    }

    private fun parseLangIsoCode(code: String): String {
        val lower = code.lowercase()
        return when {
            lower.startsWith("ko") -> "ko"
            lower.startsWith("en") -> "en"
            lower.contains("cn") || lower.contains("zh-hans") -> "zh-CN"
            lower.contains("tw") || lower.contains("zh-hant") -> "zh-TW"
            lower.startsWith("ja") -> "ja"
            else -> "en"
        }
    }
}
