package com.translation.counter.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioTranslationEngine(private val context: Context) {

    private val TAG = "AudioTranslationEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isContinuousListening = false

    // State Flows for UI
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var currentListeningLocale: Locale = Locale.KOREA

    // Speech Result Callback
    var onSpeechRecognized: ((text: String, isFinal: Boolean) -> Unit)? = null

    init {
        initStt()
    }

    private fun initStt() {
        handler.post {
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
                        // Continuous Listening Auto Restart
                        scheduleAutoRestart()
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        Log.w(TAG, "STT Error code: $error")
                        // Continuous Listening Auto Restart even on error
                        scheduleAutoRestart()
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            if (text.isNotBlank()) {
                                _recognizedText.value = text
                                onSpeechRecognized?.invoke(text, true)
                            }
                        }
                        // Continuous Listening Auto Restart immediately after result
                        scheduleAutoRestart()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            if (text.isNotBlank()) {
                                _recognizedText.value = text
                                onSpeechRecognized?.invoke(text, false)
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun scheduleAutoRestart() {
        if (!isContinuousListening) return
        handler.removeCallbacksAndMessages("RESTART_STT")
        handler.postDelayed({
            if (isContinuousListening) {
                startListeningInternal(currentListeningLocale)
            }
        }, 300)
    }

    fun startListening(locale: Locale) {
        isContinuousListening = true
        currentListeningLocale = locale
        startListeningInternal(locale)
    }

    private fun startListeningInternal(locale: Locale) {
        handler.post {
            try {
                speechRecognizer?.stopListening()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.d(TAG, "Continuous STT Started for locale: $locale")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                if (isContinuousListening) {
                    handler.postDelayed({ startListeningInternal(locale) }, 1000)
                }
            }
        }
    }

    fun stopListening() {
        isContinuousListening = false
        handler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
            _isListening.value = false
        }
    }

    fun destroy() {
        isContinuousListening = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTranslationEngine", e)
        }
    }
}
