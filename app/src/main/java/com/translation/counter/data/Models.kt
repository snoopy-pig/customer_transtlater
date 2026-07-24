package com.translation.counter.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeviceRole(val label: String) {
    STAFF("Staff Device (직원용 카운터)"),
    GUEST("Guest Device (손님용 카운터)")
}

enum class CounterRoom(val roomId: Int, val displayName: String) {
    ROOM_1(1, "Room 1 (창구 1)"),
    ROOM_2(2, "Room 2 (창구 2)"),
    ROOM_3(3, "Room 3 (창구 3)"),
    ROOM_4(4, "Room 4 (창구 4)");

    companion object {
        fun fromId(id: Int): CounterRoom = values().find { it.roomId == id } ?: ROOM_1
    }
}

enum class TargetLanguage(
    val code: String,
    val localeTag: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val countryCode: String
) {
    ENGLISH("en-US", "en-US", "English", "English", "🇺🇸", "US"),
    SIMPLIFIED_CHINESE("zh-CN", "zh-CN", "Chinese (Simp.)", "简体中文", "🇨🇳", "CN"),
    TRADITIONAL_CHINESE("zh-TW", "zh-TW", "Chinese (Trad.)", "繁體中文", "🇹🇼", "TW"),
    JAPANESE("ja-JP", "ja-JP", "Japanese", "日本語", "🇯🇵", "JP");

    companion object {
        fun fromCode(code: String): TargetLanguage =
            values().find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

enum class SpeakerType(val label: String) {
    STAFF("Staff (직원)"),
    GUEST("Guest (손님)")
}

data class SessionData(
    val sessionId: String = "",
    val roomNumber: Int = 1,
    val isActive: Boolean = false,
    val guestLanguageCode: String = TargetLanguage.ENGLISH.code,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null
)

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (1000..9999).random(),
    val sessionId: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val speaker: String = SpeakerType.STAFF.name,
    val koreanText: String = "",
    val guestText: String = "",
    val guestLanguageCode: String = TargetLanguage.ENGLISH.code
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestampMillis))
        }
}
