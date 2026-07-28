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
    private val nsdEngine = NsdWebSocketEngine(application)
    private val audioEngine = AudioTranslationEngine(application)
    private val weekLogger = LocalWeekLogger(application)

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
        AiTranslationEngine.geminiApiKey = appPrefs.getGeminiApiKey()

        audioEngine.onSpeechRecognized = { text, isFinal ->
            if (isFinal && text.isNotBlank()) {
                handleSpeechRecognizedInput(text)
            }
        }

        // Bind NSD P2P Events
        nsdEngine.onSessionStarted = { langCode ->
            val lang = TargetLanguage.values().firstOrNull { it.code == langCode } ?: TargetLanguage.ENGLISH
            _guestTargetLanguage.value = lang
        }

        nsdEngine.onChatMessageReceived = { msg ->
            if (chatMessages.value.none { it.id == msg.id }) {
                firebaseService.sendMessage(msg)
            }
        }

        // Remote Mic Trigger Event handler (Received on tourist phone when staff presses remote button)
        nsdEngine.onRemoteTouristMicTriggered = {
            if (_selectedRole.value == DeviceRole.GUEST) {
                _isMicEnabled.value = true
                audioEngine.startListening(getLocaleForLanguage(_guestTargetLanguage.value))
                Toast.makeText(getApplication(), "🎤 직원이 마이크를 켜주었습니다. 말씀해주세요!", Toast.LENGTH_SHORT).show()
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
            nsdEngine.startStaffServer()
            startListeningForStaff()
        } else {
            nsdEngine.startTouristClient()
        }
    }

    fun selectGuestLanguageAndStartSession(language: TargetLanguage) {
        val room = _selectedRoom.value ?: return
        _guestTargetLanguage.value = language

        nsdEngine.broadcastMessage("START_SESSION", mapOf("lang" to language.code))

        firebaseService.startSession(room.roomId, language.code) { session ->
            startListeningForGuest(language)
        }
    }

    fun changeGuestLanguage(language: TargetLanguage) {
        _guestTargetLanguage.value = language
        val room = _selectedRoom.value ?: return
        nsdEngine.broadcastMessage("START_SESSION", mapOf("lang" to language.code))
        if (currentSession.value?.isActive == true) {
            firebaseService.startSession(room.roomId, language.code) {
                startListeningForGuest(language)
            }
            Toast.makeText(getApplication(), "언어가 ${language.displayName}로 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Remote activate tourist microphone from staff device (직원용 2번 버튼)
    fun triggerRemoteTouristMic() {
        if (_selectedRole.value == DeviceRole.STAFF) {
            nsdEngine.broadcastMessage("TRIGGER_TOURIST_MIC")
            Toast.makeText(getApplication(), "🔊 관광객 마이크를 원격으로 켰습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleMicState() {
        val newState = !_isMicEnabled.value
        _isMicEnabled.value = newState
        audioEngine.toggleMic(newState)
    }

    private fun startListeningForGuest(language: TargetLanguage) {
        val locale = getLocaleForLanguage(language)
        audioEngine.startListening(locale)
    }

    private fun startListeningForStaff() {
        audioEngine.startListening(Locale.KOREA)
    }

    private fun getLocaleForLanguage(language: TargetLanguage): Locale {
        return when (language) {
            TargetLanguage.ENGLISH -> Locale.US
            TargetLanguage.SIMPLIFIED_CHINESE -> Locale.CHINA
            TargetLanguage.TRADITIONAL_CHINESE -> Locale.TAIWAN
            TargetLanguage.JAPANESE -> Locale.JAPAN
        }
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
                nsdEngine.broadcastMessage("CHAT_MESSAGE", message)
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
                nsdEngine.broadcastMessage("CHAT_MESSAGE", message)
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
        nsdEngine.stop()
        _selectedRoom.value = null
        _selectedRole.value = null
        _currentKoreanSubtitle.value = ""
        _currentGuestSubtitle.value = ""
    }

    fun endSessionByStaff() {
        val room = _selectedRoom.value ?: return
        audioEngine.stopListening()

        nsdEngine.broadcastMessage("END_SESSION")

        firebaseService.endSession(room.roomId) { messages, session ->
            if (session != null) {
                val savedFile = weekLogger.saveSessionToWeeklyJson(session, messages)
                val msg = if (savedFile != null) {
                    "대화 종료! 주간 파일 저장됨 (${savedFile.name})"
                } else {
                    "대화 종료 완료."
                }
                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
            }
            resetToSetup()
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseService.detachListeners()
        nsdEngine.stop()
        audioEngine.destroy()
    }
}
