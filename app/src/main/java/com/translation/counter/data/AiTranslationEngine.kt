package com.translation.counter.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser

object AiTranslationEngine {

    private const val TAG = "AiTranslationEngine"
    
    // User Provided Gemini API Key
    var geminiApiKey: String = System.getProperty("GEMINI_API_KEY") ?: ""

    suspend fun translateWithAi(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        return withContext(Dispatchers.IO) {
            val srcName = getLangName(sourceLangCode)
            val tgtName = getLangName(targetLangCode)

            // 1. Try Google Gemini AI Model Translation First
            if (geminiApiKey.isNotBlank()) {
                val geminiResult = translateWithGeminiApi(trimmed, srcName, tgtName)
                if (geminiResult.isNotBlank()) {
                    Log.d(TAG, "Gemini AI Translation Success: '$trimmed' => '$geminiResult'")
                    return@withContext geminiResult
                }
            }

            // 2. High Speed Fallback Translation Engine
            return@withContext translateWithGoogleNeural(trimmed, sourceLangCode, targetLangCode)
        }
    }

    private fun translateWithGeminiApi(text: String, srcName: String, tgtName: String): String {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val prompt = "You are an expert real-time 1:1 counter translator. Translate the following text from $srcName to $tgtName. Return ONLY the final translated sentence without quotation marks, markdown formatting, or introductory explanation.\n\nText to translate: $text"

            val rootObj = JsonObject()
            val contentsArr = JsonArray()
            val contentObj = JsonObject()
            val partsArr = JsonArray()
            val partObj = JsonObject()
            
            partObj.addProperty("text", prompt)
            partsArr.add(partObj)
            contentObj.add("parts", partsArr)
            contentsArr.add(contentObj)
            rootObj.add("contents", contentsArr)

            val jsonPayload = rootObj.toString()

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(jsonPayload)
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseStr = reader.use { it.readText() }
                conn.disconnect()

                val resJson = JsonParser.parseString(responseStr).asJsonObject
                val candidates = resJson.getAsJsonArray("candidates")
                if (candidates != null && candidates.size() > 0) {
                    val candidate = candidates.get(0).asJsonObject
                    val content = candidate.getAsJsonObject("content")
                    val parts = content.getAsJsonArray("parts")
                    if (parts != null && parts.size() > 0) {
                        val translatedText = parts.get(0).asJsonObject.get("text").asString.trim()
                        return translatedText
                    }
                }
            } else {
                Log.w(TAG, "Gemini API HTTP Error code: ${conn.responseCode}")
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API translation error", e)
        }
        return ""
    }

    private fun translateWithGoogleNeural(text: String, sourceLangCode: String, targetLangCode: String): String {
        try {
            val src = parseLangIsoCode(sourceLangCode)
            val tgt = parseLangIsoCode(targetLangCode)

            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$src&tl=$tgt&dt=t&q=" + URLEncoder.encode(text, "UTF-8")
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
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
                return translatedBuilder.toString()
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Neural translation fallback error", e)
        }
        return text
    }

    private fun getLangName(code: String): String {
        val lower = code.lowercase()
        return when {
            lower.startsWith("ko") -> "Korean"
            lower.startsWith("en") -> "English"
            lower.contains("cn") || lower.contains("zh-hans") -> "Simplified Chinese"
            lower.contains("tw") || lower.contains("zh-hant") -> "Traditional Chinese"
            lower.startsWith("ja") -> "Japanese"
            else -> "English"
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
