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
    var isMicEnabled = true // Safe toggle switch for mic

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
                        scheduleAutoRestart()
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        Log.w(TAG, "STT Error code: $error")
                        // Avoid rapid error loop restarts
                        if (error != SpeechRecognizer.ERROR_CLIENT) {
                            scheduleAutoRestart(1500)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            if (text.isNotBlank() && text.length >= 2) {
                                _recognizedText.value = text
                                onSpeechRecognized?.invoke(text, true)
                            }
                        }
                        scheduleAutoRestart(1000)
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

    private fun scheduleAutoRestart(delayMs: Long = 1200) {
        if (!isContinuousListening || !isMicEnabled) return
        handler.removeCallbacksAndMessages("RESTART_STT")
        handler.postDelayed({
            if (isContinuousListening && isMicEnabled) {
                startListeningInternal(currentListeningLocale)
            }
        }, delayMs)
    }

    fun toggleMic(enabled: Boolean) {
        isMicEnabled = enabled
        if (!enabled) {
            stopListening()
        } else {
            startListening(currentListeningLocale)
        }
    }

    fun startListening(locale: Locale) {
        isContinuousListening = true
        isMicEnabled = true
        currentListeningLocale = locale
        startListeningInternal(locale)
    }

    private fun startListeningInternal(locale: Locale) {
        if (!isMicEnabled) return
        handler.post {
            try {
                speechRecognizer?.stopListening()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    
                    // Enhanced STT parameters for complete sentence listening without early truncation
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                }
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.d(TAG, "STT Listening Started for locale: $locale")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                if (isContinuousListening && isMicEnabled) {
                    handler.postDelayed({ startListeningInternal(locale) }, 2000)
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
        isMicEnabled = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTranslationEngine", e)
        }
    }
}
