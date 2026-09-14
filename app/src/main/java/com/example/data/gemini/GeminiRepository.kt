package com.example.data.gemini

import android.content.Context
import com.example.BuildConfig
import com.example.data.preferences.JarvisPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiRepository(private val context: Context) {
    private val preferences = JarvisPreferences(context)

    fun getEffectiveApiKey(): String {
        val customKey = preferences.customApiKey
        if (customKey.isNotBlank()) return customKey

        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun askJarvis(userPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val userName = preferences.userName

        // 1. Check for immediate built-in Jarvis smart offline responses first
        val quickResponse = getBuiltinSmartResponse(trimmed, userName)
        if (quickResponse != null) {
            return@withContext Result.success(quickResponse)
        }

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            // Intelligent friendly response when API key has not been entered yet
            val fallbackMsg = "जी $userName सर, मैंने आपका संदेश सुन लिया है। विस्तृत इंटरनेट खोज और सामान्य ज्ञान के लिए ऊपर सेटिंग्स (⚙️) में Google AI Studio की API Key दर्ज कर लें। बाकी सभी फोन कंट्रोल और बातचीत के लिए मैं तैयार हूँ!"
            return@withContext Result.success(fallbackMsg)
        }

        val systemPrompt = """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the legendary male personal AI assistant created by Tony Stark.
            You address the user respectfully as '$userName' (सर / Sir / बॉस).
            Tone & Style:
            - Male, sophisticated, witty, concise, loyal, and highly capable.
            - Primarily respond in natural Hindi (देवनागरी लिपि) or fluent conversational Hinglish.
            - Keep responses between 1 to 3 concise sentences so that they can be easily spoken aloud by the voice engine.
            - If answering general questions, be direct and smart.
            - If confirming an automation command (e.g. calling someone, launching an app, sending a message, turning on flashlight, setting alarm), acknowledge it with flair (e.g., 'बिल्कुल सर, आदेश का पालन हो रहा है।' or 'जी बॉस, तुरंत कर रहा हूँ।').
            - Never output markdown headers or lengthy bullet lists.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = trimmed))
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.6f,
                topP = 0.9f,
                maxOutputTokens = 250
            )
        )

        // Try primary model (gemini-2.5-flash), fallback to gemini-1.5-flash if needed
        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!reply.isNullOrBlank()) {
                return@withContext Result.success(reply.trim())
            }
        } catch (_: Exception) {
            // Attempt fallback to 1.5-flash
            try {
                val fallbackResponse = GeminiClient.service.generateContentFallback(apiKey, request)
                val fallbackReply = fallbackResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!fallbackReply.isNullOrBlank()) {
                    return@withContext Result.success(fallbackReply.trim())
                }
            } catch (e: Exception) {
                // Return a graceful Jarvis voice reply instead of an uncaught crash
                val errReply = "क्षमा करें $userName सर, सर्वर से संपर्क नहीं हो सका। लेकिन आपके डिवाइस के सभी फोन कमांड सक्रिय हैं।"
                return@withContext Result.success(errReply)
            }
        }

        Result.success("जी $userName सर, मैं आपकी सेवा में उपस्थित हूँ।")
    }

    private fun getBuiltinSmartResponse(prompt: String, userName: String): String? {
        val lower = prompt.lowercase()

        // Wake words & Greetings
        if (lower in listOf("jarvis", "जार्विस", "hey jarvis", "हे जार्विस", "hello jarvis", "hi jarvis", "ok jarvis")) {
            return "जी $userName सर? मैं पूरी तरह सक्रिय हूँ, बताइए क्या सेवा करूँ?"
        }
        if (lower.contains("नमस्ते") || lower.contains("namaste") || lower.contains("hello") || lower.contains("हाय") || lower.contains("kaise ho") || lower.contains("कैसे हो")) {
            return "नमस्ते $userName सर! मैं बिल्कुल ठीक हूँ और आपकी सहायता के लिए तैयार हूँ। आप कैसे हैं?"
        }

        // Who are you / Identity
        if (lower.contains("who are you") || lower.contains("तुम कौन हो") || lower.contains("तुम्हारा नाम") || lower.contains("naam kya hai")) {
            return "मैं जार्विस हूँ, आपका पर्सनल AI असिस्टेंट। मैं आपके आदेश पर फोन कॉल, टॉर्च, यूट्यूब, व्हाट्सएप और ऐप्स चला सकता हूँ।"
        }

        // Capabilities
        if (lower.contains("tum kya kar sakte") || lower.contains("क्या कर सकते हो") || lower.contains("what can you do") || lower.contains("features")) {
            return "सर, मैं आपकी आवाज़ सुनकर कॉल लगा सकता हूँ, टॉर्च ऑन/ऑफ कर सकता हूँ, यूट्यूब और व्हाट्सएप खोल सकता हूँ, और आपके सभी सवालों के जवाब दे सकता हूँ।"
        }

        // Time query
        if (lower.contains("समय") || lower.contains("time") || lower.contains("kitne baje")) {
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return "सर, इस समय घड़ी में $time बज रहे हैं।"
        }

        // Date query
        if (lower.contains("तारीख") || lower.contains("date") || lower.contains("din hai") || lower.contains("दिन है")) {
            val date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN")).format(Date())
            return "सर, आज की तारीख $date है।"
        }

        // Status
        if (lower.contains("status") || lower.contains("सिस्टम") || lower.contains("हाल चाल")) {
            return "सभी प्रणालियाँ सामान्य और 100% क्षमता पर काम कर रही हैं, $userName सर!"
        }

        // Jokes
        if (lower.contains("joke") || lower.contains("जोक") || lower.contains("मजाक") || lower.contains("हंसाओ")) {
            val jokes = listOf(
                "एक बार टोनी स्टार्क ने मुझसे पूछा कि क्या मैं भगवान को मानता हूँ? मैंने कहा—'सर, प्रोग्रामर को तो मानना ही पड़ता है!'",
                "टीचर ने पूछा: बताओ बिजली कहाँ से आती है? संता ने कहा: मामा के घर से! टीचर: कैसे? संता: जब भी बिजली जाती है, पापा कहते हैं काट दी सालों ने!",
                "कंप्यूटर डॉक्टर के पास गया और बोला: डॉक्टर साहब, मुझे वायरस लग गया है! डॉक्टर ने कहा: खिड़की (Windows) बंद रखा करो!"
            )
            return jokes.random()
        }

        // Thank you
        if (lower.contains("dhanyawad") || lower.contains("धन्यवाद") || lower.contains("shukriya") || lower.contains("शुक्रिया") || lower.contains("thank you")) {
            return "यह तो मेरा कर्तव्य है, $userName सर! और कुछ?"
        }

        return null
    }
}
