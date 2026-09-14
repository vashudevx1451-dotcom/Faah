package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.preferences.JarvisPreferences
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBgVariant
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    preferences: JarvisPreferences,
    onDismiss: () -> Unit,
    onTestVoice: (pitch: Float, speed: Float) -> Unit,
    onSave: (apiKey: String, userName: String, pitch: Float, speed: Float, autoSpeak: Boolean) -> Unit
) {
    var apiKey by remember { mutableStateOf(preferences.customApiKey) }
    var userName by remember { mutableStateOf(preferences.userName) }
    var voicePitch by remember { mutableFloatStateOf(preferences.voicePitch) }
    var voiceSpeed by remember { mutableFloatStateOf(preferences.voiceSpeed) }
    var autoSpeak by remember { mutableStateOf(preferences.autoSpeak) }
    var isKeyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = JarvisCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "जार्विस सेटिंग्स (Settings)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key Input
                Text(
                    text = "Google AI Studio API Key",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisCyan
                )
                Text(
                    text = "aistudio.google.com से अपनी मुफ्त API Key प्राप्त करें।",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("AI Studio Key दर्ज करें (AIzaSy...)", color = TextMuted, fontSize = 13.sp) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = JarvisCyan
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = JarvisCyan,
                        focusedContainerColor = JarvisCardBgVariant,
                        unfocusedContainerColor = JarvisCardBgVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Title Input
                Text(
                    text = "संबोधन (आपको क्या पुकारे?)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JarvisCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    placeholder = { Text("जैसे: सर, बॉस, या आपका नाम", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = JarvisCyan,
                        focusedContainerColor = JarvisCardBgVariant,
                        unfocusedContainerColor = JarvisCardBgVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Pitch Setting (Male tone)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisCardBgVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "पुरुष स्वर पिच (Male Voice Pitch)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (voicePitch <= 0.85f) "गहरी आवाज़ (Deep Male)" else "सामान्य (Normal)",
                                fontSize = 11.sp,
                                color = JarvisGold
                            )
                        }

                        Slider(
                            value = voicePitch,
                            onValueChange = { voicePitch = it },
                            valueRange = 0.65f..1.25f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisCyan,
                                activeTrackColor = JarvisCyan,
                                inactiveTrackColor = JarvisBorder
                            ),
                            modifier = Modifier.testTag("voice_pitch_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "बोलने की गति (Speech Rate)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${String.format("%.2f", voiceSpeed)}x",
                                fontSize = 11.sp,
                                color = JarvisCyan
                            )
                        }

                        Slider(
                            value = voiceSpeed,
                            onValueChange = { voiceSpeed = it },
                            valueRange = 0.7f..1.3f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisCyan,
                                activeTrackColor = JarvisCyan,
                                inactiveTrackColor = JarvisBorder
                            ),
                            modifier = Modifier.testTag("voice_speed_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Test Voice Button
                        OutlinedButton(
                            onClick = { onTestVoice(voicePitch, voiceSpeed) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("हिंदी पुरुष आवाज़ का परीक्षण करें (Test Male Voice)", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Speak Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "स्वचालित आवाज़ उत्तर (Auto Speak Replies)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "जार्विस बोलकर जवाब देगा।",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { autoSpeak = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyan,
                            checkedTrackColor = JarvisBorder,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = JarvisCardBgVariant
                        ),
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(apiKey, userName, voicePitch, voiceSpeed, autoSpeak)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = JarvisDarkBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("सेव करें (Save Settings)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
