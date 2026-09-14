package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "custom_gemini_api_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_VOICE_SPEED = "voice_speed"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_HINDI_MODE = "hindi_mode"
        private const val KEY_WAKE_WORD = "wake_word_enabled"
    }

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD, value).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "सर") ?: "सर"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 0.85f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat(KEY_VOICE_SPEED, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_SPEED, value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var hindiMode: Boolean
        get() = prefs.getBoolean(KEY_HINDI_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_HINDI_MODE, value).apply()
}
