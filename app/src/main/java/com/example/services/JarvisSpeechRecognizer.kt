package com.example.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onWakeWordDetected: (commandAfterWakeWord: String?) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onListeningState: (isListening: Boolean, isWakeWordMode: Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set
    var isWakeWordMode = false
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDestroyed = false

    fun startListening(wakeWordMode: Boolean = false) {
        if (isDestroyed) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("इस डिवाइस पर स्पीच रिकग्निशन उपलब्ध नहीं है।")
            return
        }

        stopListeningInternal(destroyRecognizer = true)
        isWakeWordMode = wakeWordMode

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningState(true, isWakeWordMode)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        onListeningState(false, isWakeWordMode)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        onListeningState(false, isWakeWordMode)

                        // In wake word mode, timeouts and no-match are expected normal cycles; silently restart
                        if (isWakeWordMode && (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                    error == SpeechRecognizer.ERROR_CLIENT)) {
                            scheduleWakeWordRestart()
                            return
                        }

                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग त्रुटि"
                            SpeechRecognizer.ERROR_CLIENT -> "क्लाइंट त्रुटि"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक्रोफ़ोन अनुमति आवश्यक है"
                            SpeechRecognizer.ERROR_NETWORK -> "नेटवर्क कनेक्शन की समस्या"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "नेटवर्क टाइमआउट"
                            SpeechRecognizer.ERROR_NO_MATCH -> "कोई आवाज़ समझ नहीं आई, कृपया पुनः बोलें"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "सिस्टम व्यस्त है"
                            SpeechRecognizer.ERROR_SERVER -> "सर्वर त्रुटि"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "समय समाप्त, कोई आवाज़ नहीं मिली"
                            else -> "आवाज़ पहचानने में त्रुटि ($error)"
                        }

                        if (!isWakeWordMode) {
                            onError(message)
                        } else {
                            scheduleWakeWordRestart()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningState(false, isWakeWordMode)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()

                        if (!text.isNullOrBlank()) {
                            if (isWakeWordMode) {
                                val wakeResult = parseWakeWord(text)
                                if (wakeResult.isWakeWord) {
                                    onWakeWordDetected(wakeResult.command)
                                } else {
                                    // Spoken words didn't contain "Jarvis", continue listening for wake word
                                    scheduleWakeWordRestart()
                                }
                            } else {
                                onResult(text)
                            }
                        } else {
                            if (isWakeWordMode) {
                                scheduleWakeWordRestart()
                            } else {
                                onError("आवाज़ स्पष्ट नहीं थी।")
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (isWakeWordMode) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim()
                            if (!partial.isNullOrBlank()) {
                                val wakeResult = parseWakeWord(partial)
                                if (wakeResult.isWakeWord && !wakeResult.command.isNullOrBlank()) {
                                    // Detected early wake word in partial stream
                                    stopListeningInternal(destroyRecognizer = true)
                                    onWakeWordDetected(wakeResult.command)
                                }
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("JarvisSpeech", "Error starting speech recognition: ${e.message}")
            if (isWakeWordMode) {
                scheduleWakeWordRestart()
            } else {
                onError("सुनने में समस्या: ${e.message}")
            }
        }
    }

    private data class WakeWordResult(val isWakeWord: Boolean, val command: String?)

    private fun parseWakeWord(input: String): WakeWordResult {
        val lower = input.lowercase()
        val wakeKeywords = listOf(
            "jarvis", "जार्विस", "जारविस", "हे जार्विस", "hey jarvis",
            "hello jarvis", "hi jarvis", "ok jarvis", "ओके जार्विस", "सुनो जार्विस"
        )

        for (kw in wakeKeywords) {
            if (lower.contains(kw)) {
                // Strip the wake word and return remaining command if any
                var command = input
                val idx = lower.indexOf(kw)
                val after = input.substring(idx + kw.length).trim()
                val cleanCommand = after.trimStart(',', '.', ' ', '!', '?')
                return WakeWordResult(
                    isWakeWord = true,
                    command = if (cleanCommand.isNotBlank()) cleanCommand else null
                )
            }
        }
        return WakeWordResult(isWakeWord = false, command = null)
    }

    private fun scheduleWakeWordRestart() {
        if (!isWakeWordMode || isDestroyed) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isWakeWordMode && !isDestroyed && !isListening) {
                startListening(wakeWordMode = true)
            }
        }, 400)
    }

    private fun stopListeningInternal(destroyRecognizer: Boolean = false) {
        mainHandler.removeCallbacksAndMessages(null)
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isListening = false
            onListeningState(false, isWakeWordMode)
        }
        if (destroyRecognizer) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    fun stopListening() {
        isWakeWordMode = false
        stopListeningInternal(destroyRecognizer = true)
    }

    fun pauseWakeWord() {
        mainHandler.removeCallbacksAndMessages(null)
        stopListeningInternal(destroyRecognizer = true)
    }

    fun resumeWakeWord() {
        if (isWakeWordMode && !isDestroyed) {
            startListening(wakeWordMode = true)
        }
    }

    fun destroy() {
        isDestroyed = true
        isWakeWordMode = false
        stopListeningInternal(destroyRecognizer = true)
    }
}
