package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.preferences.JarvisPreferences
import java.util.Locale

class JarvisVoiceEngine(
    private val context: Context,
    private val onStateChange: (isSpeaking: Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set
    private var pendingSpeech: String? = null
    private val preferences = JarvisPreferences(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("JarvisTTS", "Failed to construct TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsInstance = tts ?: return

            // Audio attributes for media playback
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                ttsInstance.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.e("JarvisTTS", "AudioAttributes setup error: ${e.message}")
            }

            // Try setting Hindi locale first
            var langSet = false
            try {
                val hindiLocale = Locale("hi", "IN")
                val result = ttsInstance.setLanguage(hindiLocale)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    langSet = true
                }
            } catch (e: Exception) {
                Log.e("JarvisTTS", "Hindi locale error: ${e.message}")
            }

            if (!langSet) {
                try {
                    ttsInstance.setLanguage(Locale("en", "IN"))
                } catch (_: Exception) {
                    ttsInstance.setLanguage(Locale.getDefault())
                }
            }

            // Attempt to pick a deep Male voice
            selectMaleVoice(ttsInstance)

            // Set pitch and speed (deeper male tone for Jarvis)
            ttsInstance.setPitch(preferences.voicePitch)
            ttsInstance.setSpeechRate(preferences.voiceSpeed)

            ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStateChange(true)
                }

                override fun onDone(utteranceId: String?) {
                    onStateChange(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onStateChange(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onStateChange(false)
                    Log.e("JarvisTTS", "TTS Error code: $errorCode")
                }
            })

            isInitialized = true
            Log.d("JarvisTTS", "Jarvis TTS initialized successfully")

            // Speak any pending speech queued before initialization finished
            pendingSpeech?.let {
                val toSpeak = it
                pendingSpeech = null
                speak(toSpeak)
            }
        } else {
            Log.e("JarvisTTS", "TTS Initialization failed with status: $status")
        }
    }

    private fun selectMaleVoice(ttsInstance: TextToSpeech) {
        try {
            val voices = ttsInstance.voices ?: return
            val maleVoice = voices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                val isHindi = voice.locale.language.startsWith("hi")
                val isMale = name.contains("male") || name.contains("#male") ||
                        name.contains("hi-in-x-hie") || name.contains("hi-in-x-hia") ||
                        name.contains("man") || name.contains("deep")
                isHindi && isMale
            } ?: voices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                name.contains("male") || name.contains("#male")
            } ?: voices.firstOrNull { voice ->
                voice.locale.language.startsWith("hi")
            }

            if (maleVoice != null) {
                ttsInstance.voice = maleVoice
                Log.d("JarvisTTS", "Selected voice: ${maleVoice.name}")
            }
        } catch (e: Exception) {
            Log.e("JarvisTTS", "Error picking male voice: ${e.message}")
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_REPLY_${System.currentTimeMillis()}") {
        if (!isInitialized) {
            pendingSpeech = text
            Log.d("JarvisTTS", "TTS not initialized yet, queued pending speech")
            return
        }
        val ttsInstance = tts ?: return

        // Unmute check: if media volume is 0, give it a small audible level
        try {
            audioManager?.let { am ->
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol == 0) {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.4).toInt().coerceAtLeast(1), 0)
                }
            }
        } catch (_: Exception) {}

        // Update pitch and rate dynamically from preferences
        ttsInstance.setPitch(preferences.voicePitch)
        ttsInstance.setSpeechRate(preferences.voiceSpeed)

        // Clean text: strip markdown symbols like ** or # for clean voice output
        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("_", "")
            .replace("🤖", "")
            .replace("🗣️", "")
            .replace("👤", "")
            .trim()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        val res = ttsInstance.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (res == TextToSpeech.ERROR) {
            Log.e("JarvisTTS", "ttsInstance.speak returned ERROR")
        }
    }

    fun stop() {
        tts?.stop()
        onStateChange(false)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
