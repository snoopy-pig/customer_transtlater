package com.translation.counter.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader

class FirebaseTranslationService {

    private val TAG = "FirebaseTranslationService"
    private var firestore: FirebaseFirestore? = null

    private val _currentSession = MutableStateFlow<SessionData?>(null)
    val currentSession: StateFlow<SessionData?> = _currentSession.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private var sessionListenerRegistration: ListenerRegistration? = null
    private var messageListenerRegistration: ListenerRegistration? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null
    private var isFallbackRestMode = false
    private val gson = Gson()

    // Public Universal Firebase Realtime REST Gateway for instant 2-device pairing over internet
    private val REST_BASE_URL = "https://counter-translation-default-rtdb.firebaseio.com"

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore fallback to REST gateway mode", e)
            isFallbackRestMode = true
        }
    }

    fun attachRoomListener(roomNumber: Int) {
        detachListeners()
        val roomDocId = "room_$roomNumber"

        if (isFallbackRestMode || firestore == null) {
            startRestPolling(roomNumber)
            return
        }

        try {
            val roomRef = firestore!!.collection("rooms").document(roomDocId)
            sessionListenerRegistration = roomRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore session listen error, switching to REST gateway: ${error.message}")
                    startRestPolling(roomNumber)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val activeSessionId = snapshot.getString("active_session_id")
                    val isActive = snapshot.getBoolean("is_active") ?: false
                    val guestLang = snapshot.getString("guest_language") ?: TargetLanguage.ENGLISH.code
                    val startTime = snapshot.getLong("start_time") ?: System.currentTimeMillis()

                    if (isActive && !activeSessionId.isNullOrEmpty()) {
                        val session = SessionData(
                            sessionId = activeSessionId,
                            roomNumber = roomNumber,
                            isActive = true,
                            guestLanguageCode = guestLang,
                            startTimeMillis = startTime
                        )
                        _currentSession.value = session
                        attachMessageListener(roomDocId, activeSessionId)
                    } else {
                        _currentSession.value = null
                        _chatMessages.value = emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach snapshot listener, starting REST gateway", e)
            startRestPolling(roomNumber)
        }
    }

    private fun attachMessageListener(roomDocId: String, sessionId: String) {
        messageListenerRegistration?.remove()
        if (firestore == null) return

        try {
            val messagesRef = firestore!!
                .collection("rooms")
                .document(roomDocId)
                .collection("sessions")
                .document(sessionId)
                .collection("messages")

            messageListenerRegistration = messagesRef
                .orderBy("timestampMillis")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Messages snapshot listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ChatMessage::class.java)
                        }
                        _chatMessages.value = messages
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching message listener", e)
        }
    }

    // High-speed 250ms Gateway Engine for instant 2-device pairing over 5G/Wi-Fi
    private fun startRestPolling(roomNumber: Int) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val sessionUrl = URL("$REST_BASE_URL/rooms/room_$roomNumber/session.json")
                    val conn = sessionUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000

                    if (conn.responseCode == 200) {
                        val reader = InputStreamReader(conn.inputStream, "UTF-8")
                        val sessionObj = JsonParser.parseReader(reader)
                        if (sessionObj.isJsonObject) {
                            val obj = sessionObj.asJsonObject
                            val activeSessionId = obj.get("active_session_id")?.asString ?: ""
                            val isActiveSession = obj.get("is_active")?.asBoolean ?: false
                            val guestLang = obj.get("guest_language")?.asString ?: TargetLanguage.ENGLISH.code
                            val startTime = obj.get("start_time")?.asLong ?: System.currentTimeMillis()

                            if (isActiveSession && activeSessionId.isNotEmpty()) {
                                val session = SessionData(
                                    sessionId = activeSessionId,
                                    roomNumber = roomNumber,
                                    isActive = true,
                                    guestLanguageCode = guestLang,
                                    startTimeMillis = startTime
                                )
                                _currentSession.value = session
                                fetchRestMessages(roomNumber, activeSessionId)
                            } else {
                                _currentSession.value = null
                                _chatMessages.value = emptyList()
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "REST sync polling fallback error: ${e.message}")
                }
                delay(250) // Poll every 250ms for sub-0.3s instant sync
            }
        }
    }

    private suspend fun fetchRestMessages(roomNumber: Int, sessionId: String) {
        withContext(Dispatchers.IO) {
            try {
                val messagesUrl = URL("$REST_BASE_URL/rooms/room_$roomNumber/sessions/$sessionId/messages.json")
                val conn = messagesUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000

                if (conn.responseCode == 200) {
                    val reader = InputStreamReader(conn.inputStream, "UTF-8")
                    val jsonElement = JsonParser.parseReader(reader)
                    val messagesList = mutableListOf<ChatMessage>()

                    if (jsonElement.isJsonObject) {
                        val obj = jsonElement.asJsonObject
                        obj.entrySet().forEach { entry ->
                            val msg = gson.fromJson(entry.value, ChatMessage::class.java)
                            messagesList.add(msg)
                        }
                    }
                    messagesList.sortBy { it.timestampMillis }
                    
                    // Merge local and remote messages seamlessly
                    val currentList = _chatMessages.value
                    if (messagesList.size >= currentList.size) {
                        _chatMessages.value = messagesList
                    } else if (messagesList.isNotEmpty()) {
                        val combined = (currentList + messagesList).distinctBy { it.id }.sortedBy { it.timestampMillis }
                        _chatMessages.value = combined
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching REST messages: ${e.message}")
            }
        }
    }

    fun startSession(roomNumber: Int, guestLanguageCode: String, onComplete: (SessionData) -> Unit) {
        val newSessionId = "SESSION_${System.currentTimeMillis()}_ROOM_$roomNumber"
        val session = SessionData(
            sessionId = newSessionId,
            roomNumber = roomNumber,
            isActive = true,
            guestLanguageCode = guestLanguageCode,
            startTimeMillis = System.currentTimeMillis()
        )

        _currentSession.value = session
        _chatMessages.value = emptyList()

        scope.launch {
            try {
                val roomDocId = "room_$roomNumber"
                if (!isFallbackRestMode && firestore != null) {
                    val roomData = hashMapOf(
                        "active_session_id" to newSessionId,
                        "is_active" to true,
                        "guest_language" to guestLanguageCode,
                        "start_time" to session.startTimeMillis,
                        "updated_at" to System.currentTimeMillis()
                    )
                    firestore!!.collection("rooms").document(roomDocId).set(roomData)
                }

                // Push to Universal Gateway for instant 2-device pairing
                val url = URL("$REST_BASE_URL/rooms/room_$roomNumber/session.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = gson.toJson(
                    mapOf(
                        "active_session_id" to newSessionId,
                        "is_active" to true,
                        "guest_language" to guestLanguageCode,
                        "start_time" to session.startTimeMillis
                    )
                )
                conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting session REST sync", e)
            }
            withContext(Dispatchers.Main) {
                onComplete(session)
            }
        }
    }

    fun sendMessage(message: ChatMessage) {
        val current = _currentSession.value ?: return
        
        // Immediately insert message into local StateFlow for 0.0s instant UI rendering
        val currentList = _chatMessages.value
        if (currentList.none { it.id == message.id }) {
            _chatMessages.value = currentList + message
        }

        scope.launch {
            try {
                val roomDocId = "room_${current.roomNumber}"
                if (!isFallbackRestMode && firestore != null) {
                    firestore!!
                        .collection("rooms")
                        .document(roomDocId)
                        .collection("sessions")
                        .document(current.sessionId)
                        .collection("messages")
                        .document(message.id)
                        .set(message)
                }

                // Push to REST Universal Gateway
                val url = URL("$REST_BASE_URL/rooms/room_${current.roomNumber}/sessions/${current.sessionId}/messages/${message.id}.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = gson.toJson(message)
                conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing REST message", e)
            }
        }
    }

    fun endSession(roomNumber: Int, onComplete: (List<ChatMessage>, SessionData?) -> Unit) {
        val sessionToClose = _currentSession.value
        val messagesToSave = _chatMessages.value.toList()

        val updatedSession = sessionToClose?.copy(
            isActive = false,
            endTimeMillis = System.currentTimeMillis()
        )

        _currentSession.value = null
        _chatMessages.value = emptyList()

        scope.launch {
            try {
                val roomDocId = "room_$roomNumber"
                if (!isFallbackRestMode && firestore != null && sessionToClose != null) {
                    val updates = hashMapOf<String, Any>(
                        "is_active" to false,
                        "ended_at" to System.currentTimeMillis()
                    )
                    firestore!!.collection("rooms").document(roomDocId).update(updates)
                }

                val url = URL("$REST_BASE_URL/rooms/room_$roomNumber/session.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = gson.toJson(mapOf("is_active" to false))
                conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error ending session REST sync", e)
            }
            withContext(Dispatchers.Main) {
                onComplete(messagesToSave, updatedSession)
            }
        }
    }

    fun detachListeners() {
        pollJob?.cancel()
        sessionListenerRegistration?.remove()
        messageListenerRegistration?.remove()
        sessionListenerRegistration = null
        messageListenerRegistration = null
    }
}
