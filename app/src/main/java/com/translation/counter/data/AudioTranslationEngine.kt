package com.translation.counter.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioTranslationEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "AudioTranslationEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // State Flows for UI
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var currentListeningLocale: Locale = Locale.KOREA

    // Speech Result Callback
    var onSpeechRecognized: ((text: String, isFinal: Boolean) -> Unit)? = null

    init {
        initTts()
        initStt()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.language = Locale.KOREA
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    pauseListeningForTts()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    handler.postDelayed({
                        resumeListeningAfterTts()
                    }, 500)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    handler.postDelayed({
                        resumeListeningAfterTts()
                    }, 500)
                }
            })
            Log.d(TAG, "TTS Initialized successfully")
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    private fun initStt() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    Log.w(TAG, "STT Error code: $error")
                    if (_isListening.value && !_isSpeaking.value) {
                        handler.postDelayed({ startListening(currentListeningLocale) }, 1000)
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _recognizedText.value = text
                        onSpeechRecognized?.invoke(text, true)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _recognizedText.value = text
                        onSpeechRecognized?.invoke(text, false)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(locale: Locale) {
        if (_isSpeaking.value) {
            Log.d(TAG, "Skipping STT start because TTS is currently speaking.")
            return
        }
        currentListeningLocale = locale
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
        _isListening.value = false
    }

    private fun pauseListeningForTts() {
        stopListening()
    }

    private fun resumeListeningAfterTts() {
        startListening(currentListeningLocale)
    }

    fun speak(text: String, locale: Locale) {
        if (text.isBlank()) return

        handler.post {
            if (!isTtsInitialized || textToSpeech == null) {
                Log.w(TAG, "TTS not ready yet. Retrying in 300ms...")
                handler.postDelayed({ speak(text, locale) }, 300)
                return@post
            }

            pauseListeningForTts()

            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language $locale missing/not supported, fallback to US")
                textToSpeech?.setLanguage(Locale.US)
            }

            val utteranceId = "UTTERANCE_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val speakResult = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Log.d(TAG, "TTS speak result: $speakResult for text: $text (Locale: $locale)")
        }
    }

    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String {
        if (text.isBlank()) return ""
        return mockOrApiTranslate(text, sourceLangCode, targetLangCode)
    }

    private fun mockOrApiTranslate(text: String, source: String, target: String): String {
        val trimmed = text.trim()
        
        if (source.startsWith("ko", ignoreCase = true)) {
            when (target) {
                "en-US" -> {
                    return when {
                        trimmed.contains("안녕하세요") -> "Hello, welcome to our service counter."
                        trimmed.contains("여권") -> "Please present your passport."
                        trimmed.contains("서류") -> "Please fill out this application form."
                        trimmed.contains("감사합니다") -> "Thank you. Have a nice day!"
                        trimmed.contains("어디") -> "Where are you heading?"
                        trimmed.contains("비자") -> "Do you have a valid visa?"
                        else -> "Hello. $trimmed"
                    }
                }
                "zh-CN" -> {
                    return when {
                        trimmed.contains("안녕하세요") -> "您好，欢迎来到服务柜台。"
                        trimmed.contains("여권") -> "请出示您的护照。"
                        trimmed.contains("서류") -> "请填写此申请表。"
                        trimmed.contains("감사합니다") -> "谢谢，祝您生活愉快！"
                        else -> "您好 $trimmed"
                    }
                }
                "zh-TW" -> {
                    return when {
                        trimmed.contains("안녕하세요") -> "您好，歡迎來到服務櫃檯。"
                        trimmed.contains("여권") -> "請出示您的護照。"
                        trimmed.contains("서류") -> "請填寫此申請表。"
                        trimmed.contains("감사합니다") -> "謝謝，祝您生活愉快！"
                        else -> "您好 $trimmed"
                    }
                }
                "ja-JP" -> {
                    return when {
                        trimmed.contains("안녕하세요") -> "こんにちは、窓口へようこそ。"
                        trimmed.contains("여권") -> "パスポートをご提示ください。"
                        trimmed.contains("서류") -> "こちらの申請書にご記入ください。"
                        trimmed.contains("감사합니다") -> "ありがとうございます。よい一日を！"
                        else -> "こんにちは $trimmed"
                    }
                }
            }
        } else {
            // Guest to Korean
            return when {
                trimmed.contains("hello", ignoreCase = true) || trimmed.contains("你好") || trimmed.contains("こんにちは") -> "안녕하세요, 창구에 오신 것을 환영합니다."
                trimmed.contains("passport", ignoreCase = true) || trimmed.contains("护照") || trimmed.contains("パスポート") -> "여기 제 여권입니다."
                trimmed.contains("help", ignoreCase = true) || trimmed.contains("帮助") || trimmed.contains("助けて") -> "도움을 요청하고 싶습니다."
                trimmed.contains("thank", ignoreCase = true) || trimmed.contains("谢谢") || trimmed.contains("ありがとう") -> "감사합니다. 안녕히 가세요."
                else -> "안녕하세요. $trimmed"
            }
        }

        return "$trimmed (Translated)"
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTranslationEngine", e)
        }
    }
}
