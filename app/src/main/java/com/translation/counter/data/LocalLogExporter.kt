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

class LocalLogExporter(private val context: Context) {

    private val TAG = "LocalLogExporter"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun getStorageDir(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CounterTranslationLogs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun exportSessionLogs(
        sessionData: SessionData?,
        messages: List<ChatMessage>
    ): List<File> {
        if (sessionData == null) {
            Log.w(TAG, "No session data available for export.")
            return emptyList()
        }

        val generatedFiles = mutableListOf<File>()
        val storageDir = getStorageDir()
        val dateNow = Date(sessionData.startTimeMillis)

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val yyyymmdd = dateFormat.format(dateNow)
        val hhmmss = timeFormat.format(dateNow)
        val roomX = "Room${sessionData.roomNumber}"

        val durationSeconds = if (sessionData.endTimeMillis != null) {
            (sessionData.endTimeMillis - sessionData.startTimeMillis) / 1000
        } else {
            0L
        }

        try {
            // 1. Session JSON File (Session_YYYYMMDD_HHMMSS_RoomX.json)
            val sessionJsonFile = File(storageDir, "Session_${yyyymmdd}_${hhmmss}_$roomX.json")
            val sessionJsonObject = JsonObject().apply {
                addProperty("session_id", sessionData.sessionId)
                addProperty("room_number", sessionData.roomNumber)
                addProperty("date", yyyymmdd)
                addProperty("start_time", fullDateFormat.format(Date(sessionData.startTimeMillis)))
                addProperty("end_time", sessionData.endTimeMillis?.let { fullDateFormat.format(Date(it)) } ?: "")
                addProperty("duration_seconds", durationSeconds)
                addProperty("guest_language", sessionData.guestLanguageCode)

                val transcriptArray = JsonArray()
                messages.forEach { msg ->
                    val msgObj = JsonObject().apply {
                        addProperty("timestamp", msg.formattedTime)
                        addProperty("speaker", msg.speaker)
                        addProperty("korean_text", msg.koreanText)
                        addProperty("guest_text", msg.guestText)
                    }
                    transcriptArray.add(msgObj)
                }
                add("transcript", transcriptArray)
            }
            sessionJsonFile.writeText(gson.toJson(sessionJsonObject))
            generatedFiles.add(sessionJsonFile)
            Log.d(TAG, "Generated Session JSON: ${sessionJsonFile.absolutePath}")

            // 2. Summary Text File (Summary_YYYYMMDD_HHMMSS_RoomX.txt)
            val summaryTxtFile = File(storageDir, "Summary_${yyyymmdd}_${hhmmss}_$roomX.txt")
            val summaryBuilder = StringBuilder().apply {
                appendLine("==================================================")
                appendLine("  1:1 창구 통역 대화 요약서 (Summary Report)")
                appendLine("==================================================")
                appendLine("세션 ID      : ${sessionData.sessionId}")
                appendLine("창구 번호    : ${sessionData.roomNumber}")
                appendLine("시작 시간    : ${fullDateFormat.format(Date(sessionData.startTimeMillis))}")
                appendLine("종료 시간    : ${sessionData.endTimeMillis?.let { fullDateFormat.format(Date(it)) } ?: "-"}")
                appendLine("총 진행 시간 : $durationSeconds 초")
                appendLine("손님 언어    : ${sessionData.guestLanguageCode}")
                appendLine("--------------------------------------------------")
                appendLine(" [대화 구성 내역 (Bilingual Transcript)]")
                appendLine("--------------------------------------------------")
                messages.forEachIndexed { index, msg ->
                    appendLine("[${index + 1}] (${msg.formattedTime}) ${msg.speaker}")
                    appendLine("  [한국어] ${msg.koreanText}")
                    appendLine("  [손님]   ${msg.guestText}")
                    appendLine()
                }
                appendLine("==================================================")
            }
            summaryTxtFile.writeText(summaryBuilder.toString())
            generatedFiles.add(summaryTxtFile)
            Log.d(TAG, "Generated Summary TXT: ${summaryTxtFile.absolutePath}")

            // 3. Weekly AI Analysis File (Weekly_Log_YYYY_WW_RoomX.json)
            val calendar = Calendar.getInstance().apply { time = dateNow }
            val year = calendar.get(Calendar.YEAR)
            val weekNo = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.WEEK_OF_YEAR))
            val weeklyFileName = "Weekly_Log_${year}_W${weekNo}_$roomX.json"
            val weeklyFile = File(storageDir, weeklyFileName)

            val weeklyJsonArray = if (weeklyFile.exists()) {
                try {
                    JsonParser.parseString(weeklyFile.readText()).asJsonArray
                } catch (e: Exception) {
                    JsonArray()
                }
            } else {
                JsonArray()
            }
            weeklyJsonArray.add(sessionJsonObject)
            weeklyFile.writeText(gson.toJson(weeklyJsonArray))
            generatedFiles.add(weeklyFile)
            Log.d(TAG, "Updated Weekly AI Analysis File: ${weeklyFile.absolutePath}")

            // 4. Daily Cumulative Master Database File (Daily_Master_YYYYMMDD.json)
            val masterFileName = "Daily_Master_${yyyymmdd}.json"
            val masterFile = File(storageDir, masterFileName)

            val masterJsonArray = if (masterFile.exists()) {
                try {
                    JsonParser.parseString(masterFile.readText()).asJsonArray
                } catch (e: Exception) {
                    JsonArray()
                }
            } else {
                JsonArray()
            }
            // Prepend new session to the top for master log
            val updatedMasterArray = JsonArray()
            updatedMasterArray.add(sessionJsonObject)
            masterJsonArray.forEach { updatedMasterArray.add(it) }

            masterFile.writeText(gson.toJson(updatedMasterArray))
            generatedFiles.add(masterFile)
            Log.d(TAG, "Updated Daily Master DB File: ${masterFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export session logs", e)
        }

        return generatedFiles
    }
}
