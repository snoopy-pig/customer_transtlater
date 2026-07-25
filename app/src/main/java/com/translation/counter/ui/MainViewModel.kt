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

    private val appPrefs = AppPreferences(application)
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
    private val _isMicEnabled = MutableStateFlow(true)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    // Subtitle Text States
    private val _currentKoreanSubtitle = MutableStateFlow("")
    val currentKoreanSubtitle: StateFlow<String> = _currentKoreanSubtitle.asStateFlow()

    private val _currentGuestSubtitle = MutableStateFlow("")
    val currentGuestSubtitle: StateFlow<String> = _currentGuestSubtitle.asStateFlow()

    // Flag language choice for Guest
    private val _guestTargetLanguage = MutableStateFlow(TargetLanguage.ENGLISH)
    val guestTargetLanguage: StateFlow<TargetLanguage> = _guestTargetLanguage.asStateFlow()

    // API Key State for Dialog
    private val _geminiApiKey = MutableStateFlow(appPrefs.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private var lastHandledMessageId = ""

    init {
        // Initialize AiTranslationEngine with saved preferences
        AiTranslationEngine.geminiApiKey = appPrefs.getGeminiApiKey()

        audioEngine.onSpeechRecognized = { text, isFinal ->
            if (isFinal && text.isNotBlank()) {
                handleSpeechRecognizedInput(text)
            }
        }

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

    fun saveGeminiApiKey(key: String) {
        appPrefs.saveGeminiApiKey(key)
        _geminiApiKey.value = appPrefs.getGeminiApiKey()
        Toast.makeText(getApplication(), "Gemini API Key가 저장되었습니다.", Toast.LENGTH_SHORT).show()
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

    // Dynamic Language Switching anytime during session
    fun changeGuestLanguage(language: TargetLanguage) {
        _guestTargetLanguage.value = language
        val room = _selectedRoom.value ?: return
        if (currentSession.value?.isActive == true) {
            firebaseService.startSession(room.roomId, language.code) {
                startListeningForGuest(language)
            }
            Toast.makeText(getApplication(), "언어가 ${language.displayName}로 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleMicState() {
        val newState = !_isMicEnabled.value
        _isMicEnabled.value = newState
        audioEngine.toggleMic(newState)
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

    fun resetToSetup() {
        audioEngine.stopListening()
        firebaseService.detachListeners()
        _selectedRoom.value = null
        _selectedRole.value = null
        _currentKoreanSubtitle.value = ""
        _currentGuestSubtitle.value = ""
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

            resetToSetup()
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseService.detachListeners()
        audioEngine.destroy()
    }
}
