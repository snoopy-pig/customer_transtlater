package com.translation.counter.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.translation.counter.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseService = FirebaseTranslationService()
    private val audioEngine = AudioTranslationEngine(application)
    private val logExporter = LocalLogExporter(application)

    // Setup state
    private val _selectedRoom = MutableStateFlow<CounterRoom?>(null)
    val selectedRoom: StateFlow<CounterRoom?> = _selectedRoom.asStateFlow()

    private val _selectedRole = MutableStateFlow<DeviceRole?>(null)
    val selectedRole: StateFlow<DeviceRole?> = _selectedRole.asStateFlow()

    // Session State
    val currentSession: StateFlow<SessionData?> = firebaseService.currentSession
    val chatMessages: StateFlow<List<ChatMessage>> = firebaseService.chatMessages

    // Audio states
    val isListening = audioEngine.isListening

    // Subtitle Text States
    private val _currentKoreanSubtitle = MutableStateFlow("")
    val currentKoreanSubtitle: StateFlow<String> = _currentKoreanSubtitle.asStateFlow()

    private val _currentGuestSubtitle = MutableStateFlow("")
    val currentGuestSubtitle: StateFlow<String> = _currentGuestSubtitle.asStateFlow()

    // Flag language choice for Guest
    private val _guestTargetLanguage = MutableStateFlow(TargetLanguage.ENGLISH)
    val guestTargetLanguage: StateFlow<TargetLanguage> = _guestTargetLanguage.asStateFlow()

    private var lastHandledMessageId = ""

    init {
        audioEngine.onSpeechRecognized = { text, isFinal ->
            if (isFinal && text.isNotBlank()) {
                handleSpeechRecognizedInput(text)
            }
        }

        // Listen for remote incoming messages and update real-time prompt text without playing TTS voice
        viewModelScope.launch {
            chatMessages.collect { messages ->
                val latest = messages.lastOrNull()
                if (latest != null && latest.id != lastHandledMessageId) {
                    lastHandledMessageId = latest.id
                    _currentKoreanSubtitle.value = latest.koreanText
                    _currentGuestSubtitle.value = latest.guestText
                }
            }
        }
    }

    fun selectRoomAndRole(room: CounterRoom, role: DeviceRole) {
        _selectedRoom.value = room
        _selectedRole.value = role
        firebaseService.attachRoomListener(room.roomId)

        if (role == DeviceRole.STAFF) {
            startListeningForStaff()
        }
    }

    fun selectGuestLanguageAndStartSession(language: TargetLanguage) {
        val room = _selectedRoom.value ?: return
        _guestTargetLanguage.value = language

        firebaseService.startSession(room.roomId, language.code) { session ->
            startListeningForGuest(language)
        }
    }

    private fun startListeningForGuest(language: TargetLanguage) {
        val locale = when (language) {
            TargetLanguage.ENGLISH -> Locale.US
            TargetLanguage.SIMPLIFIED_CHINESE -> Locale.CHINA
            TargetLanguage.TRADITIONAL_CHINESE -> Locale.TAIWAN
            TargetLanguage.JAPANESE -> Locale.JAPAN
        }
        audioEngine.startListening(locale)
    }

    private fun startListeningForStaff() {
        audioEngine.startListening(Locale.KOREA)
    }

    private fun handleSpeechRecognizedInput(recognizedText: String) {
        val role = _selectedRole.value ?: return
        val currentLang = _guestTargetLanguage.value

        viewModelScope.launch {
            if (role == DeviceRole.STAFF) {
                // Staff Spoke Korean -> AI Translates to Guest Language
                _currentKoreanSubtitle.value = recognizedText
                
                val translatedGuestText = AiTranslationEngine.translateWithAi(
                    text = recognizedText,
                    sourceLangCode = "ko-KR",
                    targetLangCode = currentLang.code
                )
                _currentGuestSubtitle.value = translatedGuestText

                val message = ChatMessage(
                    sessionId = currentSession.value?.sessionId ?: "",
                    speaker = SpeakerType.STAFF.name,
                    koreanText = recognizedText,
                    guestText = translatedGuestText,
                    guestLanguageCode = currentLang.code
                )
                firebaseService.sendMessage(message)

            } else {
                // Guest Spoke Foreign Language -> AI Translates to Korean
                _currentGuestSubtitle.value = recognizedText
                
                val translatedKoreanText = AiTranslationEngine.translateWithAi(
                    text = recognizedText,
                    sourceLangCode = currentLang.code,
                    targetLangCode = "ko-KR"
                )
                _currentKoreanSubtitle.value = translatedKoreanText

                val message = ChatMessage(
                    sessionId = currentSession.value?.sessionId ?: "",
                    speaker = SpeakerType.GUEST.name,
                    koreanText = translatedKoreanText,
                    guestText = recognizedText,
                    guestLanguageCode = currentLang.code
                )
                firebaseService.sendMessage(message)
            }
        }
    }

    fun sendManualSpeech(text: String) {
        if (text.isBlank()) return
        handleSpeechRecognizedInput(text)
    }

    fun endSessionByStaff() {
        val room = _selectedRoom.value ?: return
        audioEngine.stopListening()

        firebaseService.endSession(room.roomId) { messages, session ->
            val exportedFiles = logExporter.exportSessionLogs(session, messages)
            val msg = if (exportedFiles.isNotEmpty()) {
                "세션 종료 완료! ${exportedFiles.size}개 로컬 파일 저장됨"
            } else {
                "세션 종료 완료."
            }
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()

            _currentKoreanSubtitle.value = ""
            _currentGuestSubtitle.value = ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseService.detachListeners()
        audioEngine.destroy()
    }
}
