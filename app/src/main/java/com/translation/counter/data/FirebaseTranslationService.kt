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
import java.io.OutputStreamWriter

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

    private val PUBLIC_GATEWAY_URL = "https://kvdb.io/8xZ79tP67m9g24W9w2yK4A"

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore fallback to Public Cloud Gateway mode", e)
            isFallbackRestMode = true
        }
    }

    fun attachRoomListener(roomNumber: Int) {
        detachListeners()
        val roomDocId = "room_$roomNumber"

        if (isFallbackRestMode || firestore == null) {
            startGatewayPolling(roomNumber)
            return
        }

        try {
            val roomRef = firestore!!.collection("rooms").document(roomDocId)
            sessionListenerRegistration = roomRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore session listen error, switching to Public Gateway: ${error.message}")
                    startGatewayPolling(roomNumber)
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
            Log.e(TAG, "Failed to attach snapshot listener, starting Public Gateway", e)
            startGatewayPolling(roomNumber)
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

    private fun startGatewayPolling(roomNumber: Int) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val sessionUrl = URL("$PUBLIC_GATEWAY_URL/room_$roomNumber/session")
                    val conn = sessionUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2500
                    conn.readTimeout = 2500

                    if (conn.responseCode == 200) {
                        val reader = InputStreamReader(conn.inputStream, "UTF-8")
                        val responseStr = reader.readText()
                        conn.disconnect()

                        if (responseStr.isNotBlank()) {
                            val obj = JsonParser.parseString(responseStr).asJsonObject
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
                                fetchGatewayMessages(roomNumber, activeSessionId)
                            } else {
                                _currentSession.value = null
                                _chatMessages.value = emptyList()
                            }
                        }
                    } else {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gateway polling error: ${e.message}")
                }
                delay(300)
            }
        }
    }

    private suspend fun fetchGatewayMessages(roomNumber: Int, sessionId: String) {
        withContext(Dispatchers.IO) {
            try {
                val messagesUrl = URL("$PUBLIC_GATEWAY_URL/room_$roomNumber/sessions_$sessionId")
                val conn = messagesUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2500
                conn.readTimeout = 2500

                if (conn.responseCode == 200) {
                    val reader = InputStreamReader(conn.inputStream, "UTF-8")
                    val responseStr = reader.readText()
                    conn.disconnect()

                    if (responseStr.isNotBlank()) {
                        val jsonElement = JsonParser.parseString(responseStr)
                        val messagesList = mutableListOf<ChatMessage>()

                        if (jsonElement.isJsonArray) {
                            val arr = jsonElement.asJsonArray
                            arr.forEach { item ->
                                val msg = gson.fromJson(item, ChatMessage::class.java)
                                messagesList.add(msg)
                            }
                        }
                        messagesList.sortBy { it.timestampMillis }
                        
                        val currentList = _chatMessages.value
                        if (messagesList.size >= currentList.size) {
                            _chatMessages.value = messagesList
                        } else if (messagesList.isNotEmpty()) {
                            val combined = (currentList + messagesList).distinctBy { it.id }.sortedBy { it.timestampMillis }
                            _chatMessages.value = combined
                        } else {
                            // Empty list fallback
                        }
                    } else {
                        // Empty response fallback
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching Gateway messages: ${e.message}")
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
                val payload = gson.toJson(
                    mapOf(
                        "active_session_id" to newSessionId,
                        "is_active" to true,
                        "guest_language" to guestLanguageCode,
                        "start_time" to session.startTimeMillis
                    )
                )
                postToGateway("$PUBLIC_GATEWAY_URL/room_$roomNumber/session", payload)
                postToGateway("$PUBLIC_GATEWAY_URL/room_$roomNumber/sessions_$newSessionId", "[]")

            } catch (e: Exception) {
                Log.e(TAG, "Error starting session Gateway sync", e)
            }
            withContext(Dispatchers.Main) {
                onComplete(session)
            }
        }
    }

    fun sendMessage(message: ChatMessage) {
        val current = _currentSession.value ?: return
        
        val currentList = _chatMessages.value
        val isExist = currentList.any { it.id == message.id }
        
        val updatedList = mutableListOf<ChatMessage>()
        updatedList.addAll(currentList)
        if (!isExist) {
            updatedList.add(message)
        }
        _chatMessages.value = updatedList

        scope.launch {
            try {
                val payload = gson.toJson(updatedList)
                postToGateway("$PUBLIC_GATEWAY_URL/room_${current.roomNumber}/sessions_${current.sessionId}", payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing Gateway message", e)
            }
        }
    }

    fun endSession(roomNumber: Int, onComplete: (List<ChatMessage>, SessionData?) -> Unit) {
        val sessionToClose = _currentSession.value
        val messagesToSave = _chatMessages.value.toList()

        val updatedSession = if (sessionToClose != null) {
            sessionToClose.copy(
                isActive = false,
                endTimeMillis = System.currentTimeMillis()
            )
        } else {
            null
        }

        _currentSession.value = null
        _chatMessages.value = emptyList()

        scope.launch {
            try {
                val payload = gson.toJson(mapOf("is_active" to false))
                postToGateway("$PUBLIC_GATEWAY_URL/room_$roomNumber/session", payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error ending session Gateway sync", e)
            }
            withContext(Dispatchers.Main) {
                onComplete(messagesToSave, updatedSession)
            }
        }
    }

    private fun postToGateway(endpointUrl: String, jsonPayload: String) {
        try {
            val url = URL(endpointUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(jsonPayload)
            writer.flush()
            writer.close()
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "postToGateway error to $endpointUrl", e)
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
