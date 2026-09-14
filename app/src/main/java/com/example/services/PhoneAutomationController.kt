package com.example.services

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

data class AutomationResult(
    val handled: Boolean,
    val actionType: String,
    val responseText: String,
    val isSuccess: Boolean = true
)

class PhoneAutomationController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    // Current torch state
    var isTorchOn: Boolean = false
        private set

    /**
     * Attempts to parse and execute local phone action.
     * Returns AutomationResult(handled = true) if recognized and executed locally,
     * or AutomationResult(handled = false) if it should be delegated to Gemini AI.
     */
    fun processCommand(command: String): AutomationResult {
        val lower = command.lowercase().trim()

        // 1. Phone Call commands
        if (containsAny(lower, "call", "कॉल", "phone karo", "फोन करो", "dial")) {
            val target = extractCallTarget(lower)
            return if (target.isNotBlank()) {
                initiateCall(target)
            } else {
                AutomationResult(
                    handled = true,
                    actionType = "CALL",
                    responseText = "सर, आप किसे कॉल करना चाहते हैं? कृपया नाम या नंबर बताएं।",
                    isSuccess = false
                )
            }
        }

        // 2. WhatsApp Messaging
        if (containsAny(lower, "whatsapp", "व्हाट्सएप", "वाट्सएप")) {
            return handleWhatsAppCommand(lower)
        }

        // 3. SMS Messaging
        if (containsAny(lower, "sms", "मैसेज", "message", "संदेश")) {
            return handleSmsCommand(lower)
        }

        // 4. Flashlight / Torch
        if (containsAny(lower, "flashlight", "torch", "टॉर्च", "फ्लैशलाइट", "लाइट")) {
            return if (containsAny(lower, "on", "chalu", "चालू", "on karo", "jalao", "जलाओ")) {
                setTorch(true)
            } else if (containsAny(lower, "off", "band", "बंद", "bujhao", "बुझाओ")) {
                setTorch(false)
            } else {
                // Toggle
                setTorch(!isTorchOn)
            }
        }

        // 5. Volume controls
        if (containsAny(lower, "volume", "वॉल्यूम", "sound", "आवाज़", "आवाज")) {
            if (containsAny(lower, "badhao", "बढ़ाओ", "up", "tez", "तेज़", "high")) {
                return changeVolume(AudioManager.ADJUST_RAISE)
            } else if (containsAny(lower, "kam", "कम", "down", "low", "dheeme", "धीमे")) {
                return changeVolume(AudioManager.ADJUST_LOWER)
            } else if (containsAny(lower, "mute", "म्यूट", "silent", "साइलेंट")) {
                return muteAudio()
            } else if (containsAny(lower, "full", "फुल", "अधिकतम", "100")) {
                return setMaxVolume()
            }
        }

        // 6. Battery Status
        if (containsAny(lower, "battery", "बैटरी", "चार्ज", "charge")) {
            return getBatteryStatus()
        }

        // 7. Alarm / Timer
        if (containsAny(lower, "alarm", "अलार्म")) {
            return openAlarm()
        }
        if (containsAny(lower, "timer", "टाइमर")) {
            return openTimer()
        }

        // 8. Open / Launch Apps
        if (containsAny(lower, "open", "खोलो", "launch", "chalao", "चलाओ", "start")) {
            val appResult = handleAppLaunch(lower)
            if (appResult.handled) return appResult
        }

        // 9. Web search
        if (containsAny(lower, "search karo", "सर्च करो", "google pe search", "google par")) {
            val query = lower.replace("google pe search karo", "")
                .replace("google par search karo", "")
                .replace("search karo", "")
                .replace("google", "")
                .trim()
            if (query.isNotBlank()) {
                return searchGoogle(query)
            }
        }

        // 10. YouTube search
        if (containsAny(lower, "youtube pe search", "यूट्यूब पर सर्च", "youtube par chalao")) {
            val query = lower.replace("youtube pe search karo", "")
                .replace("youtube par search karo", "")
                .replace("youtube", "")
                .trim()
            if (query.isNotBlank()) {
                return searchYouTube(query)
            }
        }

        // Not a direct phone control command; should go to Gemini AI
        return AutomationResult(handled = false, actionType = "AI_QUERY", responseText = "")
    }

    // --- Action Handlers ---

    private fun extractCallTarget(cmd: String): String {
        return cmd
            .replace("call karo", "")
            .replace("call lagao", "")
            .replace("call", "")
            .replace("कॉल करो", "")
            .replace("कॉल लगाओ", "")
            .replace("कॉल", "")
            .replace("ko phone karo", "")
            .replace("ko phone lagao", "")
            .replace("phone karo", "")
            .replace("फोन करो", "")
            .replace("dial", "")
            .replace("to", "")
            .replace("ko", "")
            .trim()
    }

    fun initiateCall(target: String): AutomationResult {
        try {
            var phoneNumber = target.filter { it.isDigit() || it == '+' }
            var contactName = target

            // If not a raw phone number, look up in Contacts
            if (phoneNumber.length < 5) {
                val foundNumber = searchContactNumber(target)
                if (foundNumber != null) {
                    phoneNumber = foundNumber
                } else {
                    // Open dialer with searched name/number
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${Uri.encode(target)}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                    return AutomationResult(
                        handled = true,
                        actionType = "CALL",
                        responseText = "सर, कांटेक्ट सूची में '$target' नहीं मिला। डायलर खोला जा रहा है।",
                        isSuccess = true
                    )
                }
            }

            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val callIntent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            context.startActivity(callIntent)
            return AutomationResult(
                handled = true,
                actionType = "CALL",
                responseText = "बिल्कुल सर, $contactName को कॉल लगाया जा रहा है।",
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e("PhoneAutomation", "Call error: ${e.message}")
            return AutomationResult(
                handled = true,
                actionType = "CALL",
                responseText = "कॉल लगाने में समस्या आई: ${e.localizedMessage ?: "अनुमति की जांच करें"}",
                isSuccess = false
            )
        }
    }

    private fun searchContactNumber(nameQuery: String): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameQuery%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }

    fun handleWhatsAppCommand(command: String): AutomationResult {
        val clean = command
            .replace("whatsapp pe message bhejo", "")
            .replace("whatsapp par message bhejo", "")
            .replace("whatsapp message", "")
            .replace("whatsapp open karo", "")
            .replace("whatsapp kholo", "")
            .replace("whatsapp", "")
            .replace("व्हाट्सएप", "")
            .trim()

        try {
            val intent = if (clean.isBlank()) {
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    `package` = "com.whatsapp"
                    putExtra(Intent.EXTRA_TEXT, clean)
                }
            }

            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return AutomationResult(
                    handled = true,
                    actionType = "WHATSAPP",
                    responseText = "जी सर, व्हाट्सएप खोला जा रहा है।",
                    isSuccess = true
                )
            } else {
                // Fallback: browser WhatsApp link or general share
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(clean)}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                return AutomationResult(
                    handled = true,
                    actionType = "WHATSAPP",
                    responseText = "व्हाट्सएप लिंक खोला जा रहा है।",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            return AutomationResult(
                handled = true,
                actionType = "WHATSAPP",
                responseText = "व्हाट्सएप खोलने में समस्या आई: ${e.message}",
                isSuccess = false
            )
        }
    }

    fun handleSmsCommand(command: String): AutomationResult {
        val clean = command
            .replace("sms bhejo", "")
            .replace("message bhejo", "")
            .replace("मैसेज भेजो", "")
            .replace("संदेश भेजो", "")
            .trim()

        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                if (clean.isNotBlank()) {
                    putExtra("sms_body", clean)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
            return AutomationResult(
                handled = true,
                actionType = "SMS",
                responseText = "सर, मैसेज टाइप कर दिया गया है, भेजने के लिए तैयार है।",
                isSuccess = true
            )
        } catch (e: Exception) {
            return AutomationResult(
                handled = true,
                actionType = "SMS",
                responseText = "मैसेज एप खोलने में समस्या आई: ${e.message}",
                isSuccess = false
            )
        }
    }

    fun copyToClipboard(text: String): AutomationResult {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Jarvis Copied Text", text)
            clipboard.setPrimaryClip(clip)
            return AutomationResult(
                handled = true,
                actionType = "TYPING",
                responseText = "सर, टेक्स्ट को क्लिपबोर्ड पर टाइप और कॉपी कर दिया गया है। आप इसे कहीं भी पेस्ट कर सकते हैं।",
                isSuccess = true
            )
        } catch (e: Exception) {
            return AutomationResult(
                handled = true,
                actionType = "TYPING",
                responseText = "क्लिपबोर्ड पर टाइप करने में समस्या: ${e.message}",
                isSuccess = false
            )
        }
    }

    fun setTorch(enable: Boolean): AutomationResult {
        try {
            val mgr = cameraManager ?: return AutomationResult(
                handled = true,
                actionType = "TORCH",
                responseText = "डिवाइस पर कैमरा फ्लैशलाइट उपलब्ध नहीं है।",
                isSuccess = false
            )

            val cameraId = mgr.cameraIdList.firstOrNull { id ->
                val chars = mgr.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facingBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                flashAvailable && facingBack
            } ?: mgr.cameraIdList.firstOrNull()

            if (cameraId != null) {
                mgr.setTorchMode(cameraId, enable)
                isTorchOn = enable
                val response = if (enable) "टॉर्च चालू कर दी गई है, सर।" else "टॉर्च बंद कर दी गई है, सर।"
                return AutomationResult(handled = true, actionType = "TORCH", responseText = response, isSuccess = true)
            } else {
                return AutomationResult(
                    handled = true,
                    actionType = "TORCH",
                    responseText = "फ्लैशलाइट हार्डवेयर नहीं मिला।",
                    isSuccess = false
                )
            }
        } catch (e: Exception) {
            return AutomationResult(
                handled = true,
                actionType = "TORCH",
                responseText = "टॉर्च नियंत्रित करने में समस्या: ${e.message}",
                isSuccess = false
            )
        }
    }

    private fun changeVolume(direction: Int): AutomationResult {
        return try {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val text = if (direction == AudioManager.ADJUST_RAISE) "वॉल्यूम बढ़ा दिया गया है, सर।" else "वॉल्यूम कम कर दिया गया है, सर।"
            AutomationResult(handled = true, actionType = "VOLUME", responseText = text, isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "VOLUME", responseText = "वॉल्यूम बदलने में असमर्थ।", isSuccess = false)
        }
    }

    private fun muteAudio(): AutomationResult {
        return try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            AutomationResult(handled = true, actionType = "VOLUME", responseText = "मीडिया वॉल्यूम म्यूट कर दिया गया है, सर।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "VOLUME", responseText = "म्यूट करने में असमर्थ।", isSuccess = false)
        }
    }

    private fun setMaxVolume(): AutomationResult {
        return try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            AutomationResult(handled = true, actionType = "VOLUME", responseText = "वॉल्यूम 100% फुल कर दिया गया है, सर।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "VOLUME", responseText = "वॉल्यूम बदलने में असमर्थ।", isSuccess = false)
        }
    }

    fun getBatteryStatus(): AutomationResult {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = batteryManager?.isCharging ?: false

            val statusText = if (level >= 0) {
                if (isCharging) {
                    "सर, वर्तमान बैटरी स्तर $level% है और फोन चार्ज हो रहा है।"
                } else {
                    "सर, वर्तमान बैटरी स्तर $level% है।"
                }
            } else {
                "सर, बैटरी स्तर की जानकारी प्राप्त नहीं हो सकी।"
            }
            AutomationResult(handled = true, actionType = "BATTERY", responseText = statusText, isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "BATTERY", responseText = "बैटरी स्टेटस नहीं मिला: ${e.message}", isSuccess = false)
        }
    }

    private fun openAlarm(): AutomationResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AutomationResult(handled = true, actionType = "ALARM", responseText = "अलार्म सेटिंग्स खोली जा रही हैं, सर।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "ALARM", responseText = "अलार्म खोलने में असमर्थ।", isSuccess = false)
        }
    }

    private fun openTimer(): AutomationResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, 300)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AutomationResult(handled = true, actionType = "ALARM", responseText = "टाइमर खोला जा रहा है, सर।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "ALARM", responseText = "टाइमर खोलने में असमर्थ।", isSuccess = false)
        }
    }

    fun handleAppLaunch(command: String): AutomationResult {
        val target = command
            .replace("open karo", "")
            .replace("open", "")
            .replace("kholo", "")
            .replace("खोलो", "")
            .replace("launch", "")
            .replace("chalao", "")
            .replace("चलाओ", "")
            .replace("app", "")
            .replace("एप", "")
            .trim()

        if (target.isBlank()) {
            return AutomationResult(handled = false, actionType = "APP_LAUNCH", responseText = "")
        }

        // Common apps mapping
        when {
            target.contains("youtube") || target.contains("यूट्यूब") -> {
                return launchPackageOrIntent("com.google.android.youtube", "यूट्यूब")
            }
            target.contains("camera") || target.contains("कैमरा") -> {
                return try {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "कैमरा खोला जा रहा है, सर।", isSuccess = true)
                } catch (e: Exception) {
                    AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "कैमरा खोलने में असमर्थ।", isSuccess = false)
                }
            }
            target.contains("settings") || target.contains("सेटिंग्स") -> {
                return try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "सेटिंग्स खोली जा रही हैं, सर।", isSuccess = true)
                } catch (e: Exception) {
                    AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "सेटिंग्स खोलने में असमर्थ।", isSuccess = false)
                }
            }
            target.contains("chrome") || target.contains("क्रोम") || target.contains("browser") -> {
                return launchPackageOrIntent("com.android.chrome", "गूगल क्रोम")
            }
            target.contains("calculator") || target.contains("कैलकुलेटर") -> {
                return launchPackageOrIntent("com.google.android.calculator", "कैलकुलेटर")
            }
            target.contains("maps") || target.contains("नक्शा") -> {
                return launchPackageOrIntent("com.google.android.apps.maps", "गूगल मैप्स")
            }
            target.contains("instagram") || target.contains("इंस्टाग्राम") -> {
                return launchPackageOrIntent("com.instagram.android", "इंस्टाग्राम")
            }
            target.contains("spotify") || target.contains("स्पॉटिफ़ाई") -> {
                return launchPackageOrIntent("com.spotify.music", "स्पॉटिफ़ाई")
            }
        }

        // Try searching installed apps by label
        return searchAndLaunchInstalledApp(target)
    }

    private fun searchAndLaunchInstalledApp(appNameQuery: String): AutomationResult {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages) {
                val label = pm.getApplicationLabel(appInfo).toString()
                if (label.contains(appNameQuery, ignoreCase = true) || appInfo.packageName.contains(appNameQuery, ignoreCase = true)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        return AutomationResult(
                            handled = true,
                            actionType = "APP_LAUNCH",
                            responseText = "जी सर, $label खोला जा रहा है।",
                            isSuccess = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneAutomation", "App search error: ${e.message}")
        }

        return AutomationResult(
            handled = false,
            actionType = "APP_LAUNCH",
            responseText = ""
        )
    }

    private fun launchPackageOrIntent(packageName: String, displayName: String): AutomationResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "जी सर, $displayName खोला जा रहा है।", isSuccess = true)
            } else {
                AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "सर, $displayName आपके फोन में इंस्टॉल नहीं मिला।", isSuccess = false)
            }
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "APP_LAUNCH", responseText = "$displayName खोलने में समस्या: ${e.message}", isSuccess = false)
        }
    }

    private fun searchGoogle(query: String): AutomationResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AutomationResult(handled = true, actionType = "SEARCH", responseText = "सर, गूगल पर '$query' खोजा जा रहा है।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "SEARCH", responseText = "खोजने में समस्या: ${e.message}", isSuccess = false)
        }
    }

    private fun searchYouTube(query: String): AutomationResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AutomationResult(handled = true, actionType = "SEARCH", responseText = "सर, यूट्यूब पर '$query' खोला जा रहा है।", isSuccess = true)
        } catch (e: Exception) {
            AutomationResult(handled = true, actionType = "SEARCH", responseText = "यूट्यूब खोलने में समस्या: ${e.message}", isSuccess = false)
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
