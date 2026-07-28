package com.translation.counter.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LocalWeekLogger(private val context: Context) {

    private val TAG = "LocalWeekLogger"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun saveSessionToWeeklyJson(session: SessionData, messages: List<ChatMessage>): File? {
        try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            val weekInfoStr = String.format(Locale.US, "%d-W%02d", year, weekOfYear)
            val fileName = "${year}_Week_W${String.format(Locale.US, "%02d", weekOfYear)}_Logs.json"

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val logFile = File(dir, fileName)

            val rootJsonObj: JsonObject
            val sessionsArray: JsonArray

            if (logFile.exists() && logFile.length() > 0) {
                val existingText = logFile.readText()
                val parsed = JsonParser.parseString(existingText).asJsonObject
                rootJsonObj = parsed
                sessionsArray = parsed.getAsJsonArray("sessions") ?: JsonArray()
            } else {
                rootJsonObj = JsonObject()
                rootJsonObj.addProperty("week_info", weekInfoStr)
                sessionsArray = JsonArray()
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val startTimeIso = isoFormat.format(Date(session.startTimeMillis))
            
            val endTimeMillisVal = session.endTimeMillis ?: 0L
            val endTime = if (endTimeMillisVal > 0L) endTimeMillisVal else System.currentTimeMillis()
            val endTimeIso = isoFormat.format(Date(endTime))

            val firstGuestMessage = messages.firstOrNull { it.speaker == SpeakerType.GUEST.name }?.guestText ?: ""

            val newSessionObj = JsonObject()
            newSessionObj.addProperty("session_id", "${session.startTimeMillis}_${session.guestLanguageCode}")
            newSessionObj.addProperty("timestamp_start", startTimeIso)
            newSessionObj.addProperty("timestamp_end", endTimeIso)
            newSessionObj.addProperty("language", session.guestLanguageCode)
            newSessionObj.addProperty("initial_intent", firstGuestMessage)

            val messagesJsonArray = JsonArray()
            messages.forEachIndexed { index, msg ->
                val msgObj = JsonObject()
                msgObj.addProperty("turn", index + 1)
                msgObj.addProperty("speaker", if (msg.speaker == SpeakerType.GUEST.name) "tourist" else "staff")
                msgObj.addProperty("timestamp", isoFormat.format(Date(msg.timestampMillis)))
                msgObj.addProperty("original_text", if (msg.speaker == SpeakerType.GUEST.name) msg.guestText else msg.koreanText)
                msgObj.addProperty("translated_text", if (msg.speaker == SpeakerType.GUEST.name) msg.koreanText else msg.guestText)
                messagesJsonArray.add(msgObj)
            }
            newSessionObj.add("messages", messagesJsonArray)

            // UNSHIFT: Insert new session at Index 0 (front of array)
            val updatedSessionsArray = JsonArray()
            updatedSessionsArray.add(newSessionObj)
            sessionsArray.forEach { updatedSessionsArray.add(it) }

            rootJsonObj.add("sessions", updatedSessionsArray)

            logFile.writeText(gson.toJson(rootJsonObj))
            Log.d(TAG, "Successfully saved weekly session log to: ${logFile.absolutePath}")
            return logFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save weekly session JSON log", e)
            return null
        }
    }
}
