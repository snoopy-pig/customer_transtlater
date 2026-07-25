package com.translation.counter.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("counter_translation_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API = "key_gemini_api"
        const val DEFAULT_KEY = ""
    }

    fun getGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API, "") ?: ""
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key.trim()).apply()
        AiTranslationEngine.geminiApiKey = key.trim()
    }
}
