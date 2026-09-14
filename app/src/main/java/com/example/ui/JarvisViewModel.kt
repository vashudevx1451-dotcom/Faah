package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CommandEntity
import com.example.data.database.JarvisDatabase
import com.example.data.gemini.GeminiRepository
import com.example.data.preferences.JarvisPreferences
import com.example.services.JarvisSpeechRecognizer
import com.example.services.JarvisVoiceEngine
import com.example.services.PhoneAutomationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JarvisUiState(
    val state: JarvisState = JarvisState.IDLE,
    val lastUserCommand: String = "",
    val lastJarvisResponse: String = "नमस्ते सर, मैं जार्विस हूँ। बताइए आज क्या सहायता करूँ?",
    val audioRms: Float = 0f,
    val isTorchActive: Boolean = false,
    val isApiKeyConfigured: Boolean = false,
    val isWakeWordActive: Boolean = true,
    val isWakeWordListening: Boolean = false,
    val errorMessage: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val preferences = JarvisPreferences(context)
    private val geminiRepo = GeminiRepository(context)
    private val phoneController = PhoneAutomationController(context)
    private val database = JarvisDatabase.getDatabase(context)
    private val commandDao = database.commandDao()

    private val _uiState = MutableStateFlow(
        JarvisUiState(isWakeWordActive = preferences.wakeWordEnabled)
    )
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    val commandHistory: StateFlow<List<CommandEntity>> = commandDao.getAllCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Engine for Hindi Male Speech Output
    private var voiceEngine: JarvisVoiceEngine? = null

    // Speech Recognizer for Voice Input & Wake Word
    private var speechRecognizer: JarvisSpeechRecognizer? = null

    init {
        initVoiceEngine()
        initSpeechRecognizer()
        checkApiKeyStatus()

        // Greet on first open if autoSpeak is on
        if (preferences.autoSpeak) {
            voiceEngine?.speak("नमस्ते सर! मैं जार्विस हूँ। सिस्टम सक्रिय है।")
        }

        // Auto-start wake word listening if enabled
        if (preferences.wakeWordEnabled) {
            startWakeWordListening()
        }
    }

    private fun initVoiceEngine() {
        voiceEngine = JarvisVoiceEngine(context) { isSpeaking ->
            _uiState.value = _uiState.value.copy(
                state = if (isSpeaking) JarvisState.SPEAKING else JarvisState.IDLE
            )
            if (isSpeaking) {
                // Pause wake word while speaking to prevent echo loop
                speechRecognizer?.pauseWakeWord()
            } else {
                // Resume wake word once speaking finishes
                if (preferences.wakeWordEnabled) {
                    speechRecognizer?.resumeWakeWord()
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = JarvisSpeechRecognizer(
            context = context,
            onWakeWordDetected = { commandAfterWakeWord ->
                handleWakeWordDetected(commandAfterWakeWord)
            },
            onResult = { query ->
                processUserCommand(query)
            },
            onError = { err ->
                _uiState.value = _uiState.value.copy(
                    state = JarvisState.IDLE,
                    errorMessage = err,
                    audioRms = 0f
                )
            },
            onRmsChanged = { rms ->
                _uiState.value = _uiState.value.copy(audioRms = rms)
            },
            onListeningState = { listening, isWakeWord ->
                _uiState.value = _uiState.value.copy(
                    state = if (listening && !isWakeWord) JarvisState.LISTENING else if (!listening && _uiState.value.state == JarvisState.LISTENING) JarvisState.IDLE else _uiState.value.state,
                    isWakeWordListening = listening && isWakeWord,
                    audioRms = if (!listening) 0f else _uiState.value.audioRms
                )
            }
        )
    }

    fun startWakeWordListening() {
        _uiState.value = _uiState.value.copy(
            isWakeWordActive = true,
            isWakeWordListening = true
        )
        speechRecognizer?.startListening(wakeWordMode = true)
    }

    fun toggleWakeWordMode() {
        val newMode = !preferences.wakeWordEnabled
        preferences.wakeWordEnabled = newMode
        _uiState.value = _uiState.value.copy(isWakeWordActive = newMode)

        if (newMode) {
            speechRecognizer?.startListening(wakeWordMode = true)
        } else {
            speechRecognizer?.stopListening()
            _uiState.value = _uiState.value.copy(isWakeWordListening = false)
        }
    }

    private fun handleWakeWordDetected(commandAfterWakeWord: String?) {
        if (!commandAfterWakeWord.isNullOrBlank()) {
            // User said "Jarvis call papa" or "Jarvis torch on karo"
            processUserCommand(commandAfterWakeWord)
        } else {
            // User just called "Jarvis" or "हे जार्विस"
            val promptMsg = "जी ${preferences.userName} सर? मैं सुन रहा हूँ, बताइए क्या आज्ञा है?"
            _uiState.value = _uiState.value.copy(
                lastJarvisResponse = promptMsg,
                state = JarvisState.SPEAKING
            )
            voiceEngine?.speak(promptMsg)

            // After speaking the acknowledgment, listen for command
            viewModelScope.launch {
                kotlinx.coroutines.delay(1800)
                startListening()
            }
        }
    }

    fun checkApiKeyStatus() {
        val key = geminiRepo.getEffectiveApiKey()
        _uiState.value = _uiState.value.copy(
            isApiKeyConfigured = key.isNotBlank()
        )
    }

    fun startListening() {
        voiceEngine?.stop()
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            state = JarvisState.LISTENING
        )
        speechRecognizer?.startListening(wakeWordMode = false)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(
            state = JarvisState.IDLE,
            audioRms = 0f
        )
        // Resume wake word if active
        if (preferences.wakeWordEnabled) {
            speechRecognizer?.resumeWakeWord()
        }
    }

    fun toggleListening() {
        if (_uiState.value.state == JarvisState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun processUserCommand(command: String) {
        if (command.isBlank()) return

        stopListening()
        _uiState.value = _uiState.value.copy(
            state = JarvisState.PROCESSING,
            lastUserCommand = command,
            errorMessage = null
        )

        viewModelScope.launch {
            // 1. Try local phone automation first
            val localResult = phoneController.processCommand(command)

            if (localResult.handled) {
                _uiState.value = _uiState.value.copy(
                    lastJarvisResponse = localResult.responseText,
                    isTorchActive = phoneController.isTorchOn,
                    state = JarvisState.IDLE
                )

                // Save to Room history
                commandDao.insertCommand(
                    CommandEntity(
                        userCommand = command,
                        jarvisResponse = localResult.responseText,
                        actionType = localResult.actionType,
                        isSuccess = localResult.isSuccess
                    )
                )

                if (preferences.autoSpeak) {
                    voiceEngine?.speak(localResult.responseText)
                }
            } else {
                // 2. Not a local control command; pass to Gemini AI Studio
                val result = geminiRepo.askJarvis(command)
                result.fold(
                    onSuccess = { reply ->
                        _uiState.value = _uiState.value.copy(
                            lastJarvisResponse = reply,
                            state = JarvisState.IDLE
                        )

                        // Save to Room history
                        commandDao.insertCommand(
                            CommandEntity(
                                userCommand = command,
                                jarvisResponse = reply,
                                actionType = "AI_QUERY",
                                isSuccess = true
                            )
                        )

                        if (preferences.autoSpeak) {
                            voiceEngine?.speak(reply)
                        }
                    },
                    onFailure = { error ->
                        val fallbackReply = error.message ?: "माफ़ कीजिए सर, अनुरोध पूरा नहीं हो पाया।"
                        _uiState.value = _uiState.value.copy(
                            lastJarvisResponse = fallbackReply,
                            errorMessage = fallbackReply,
                            state = JarvisState.ERROR
                        )

                        commandDao.insertCommand(
                            CommandEntity(
                                userCommand = command,
                                jarvisResponse = fallbackReply,
                                actionType = "ERROR",
                                isSuccess = false
                            )
                        )

                        if (preferences.autoSpeak) {
                            voiceEngine?.speak(fallbackReply)
                        }
                    }
                )
            }
        }
    }

    fun testJarvisVoice() {
        val userName = preferences.userName
        voiceEngine?.speak("नमस्ते $userName सर! मेरी आवाज़ बिल्कुल साफ़ और स्पष्ट आ रही है। मैं आपका निजी AI असिस्टेंट जार्विस हूँ।")
    }

    fun speakText(text: String) {
        voiceEngine?.speak(text)
    }

    fun stopSpeaking() {
        voiceEngine?.stop()
        _uiState.value = _uiState.value.copy(state = JarvisState.IDLE)
    }

    fun testMaleVoice(pitch: Float, speed: Float) {
        preferences.voicePitch = pitch
        preferences.voiceSpeed = speed
        voiceEngine?.speak("नमस्ते सर, मैं जार्विस हूँ। आपकी सभी आज्ञाओं के लिए पूरी तरह से तैयार हूँ।")
    }

    fun saveSettings(apiKey: String, userName: String, pitch: Float, speed: Float, autoSpeak: Boolean) {
        preferences.customApiKey = apiKey
        preferences.userName = userName
        preferences.voicePitch = pitch
        preferences.voiceSpeed = speed
        preferences.autoSpeak = autoSpeak
        checkApiKeyStatus()
    }

    fun clearHistory() {
        viewModelScope.launch {
            commandDao.clearHistory()
        }
    }

    fun triggerQuickAction(commandText: String) {
        processUserCommand(commandText)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        voiceEngine?.shutdown()
    }
}
