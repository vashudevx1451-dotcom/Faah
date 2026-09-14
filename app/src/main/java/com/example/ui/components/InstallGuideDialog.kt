package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBgVariant
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class InstallStep(
    val stepNum: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun InstallGuideDialog(
    onDismiss: () -> Unit
) {
    val steps = listOf(
        InstallStep(
            stepNum = "1",
            title = "APK डाउनलोड करें (Download APK)",
            description = "AI Studio में ऊपर दाएं कोने (Top Right) के Settings/Export मेनू पर जाएं। 'Generate APK' या 'Export as ZIP' चुनें और .apk फाइल अपने फोन में डाउनलोड करें।",
            icon = Icons.Default.Download
        ),
        InstallStep(
            stepNum = "2",
            title = "अज्ञात स्रोत अनुमति दें (Allow Unknown Apps)",
            description = "मोबाइल Settings > Security / Apps > 'Install Unknown Apps' में जाएं और अपने ब्राउज़र या फाइल मैनेजर को APK इंस्टॉल करने की अनुमति दें।",
            icon = Icons.Default.Security
        ),
        InstallStep(
            stepNum = "3",
            title = "APK इंस्टॉल और खोलें (Install & Open)",
            description = "डाउनलोड की गई Jarvis.apk फाइल पर क्लिक करें और 'Install' दबाएं। इंस्टॉल होने के बाद ऐप खोलें।",
            icon = Icons.Default.Android
        ),
        InstallStep(
            stepNum = "4",
            title = "आवश्यक अनुमतियां प्रदान करें (Grant Permissions)",
            description = "ऐप खुलने पर माइक्रोफ़ोन (Microphone), फोन कॉल (Phone Calls), और कांटेक्ट्स (Contacts) की अनुमति दें ताकि जार्विस सीधे कॉल और वॉयस कमांड ले सके।",
            icon = Icons.Default.Mic
        ),
        InstallStep(
            stepNum = "5",
            title = "Google AI Studio API Key डालें (Add API Key)",
            description = "aistudio.google.com से अपनी मुफ्त API Key कॉपी करें और जार्विस सेटिंग्स (Settings ⚙️) में पेस्ट करके सेव करें।",
            icon = Icons.Default.VpnKey
        ),
        InstallStep(
            stepNum = "6",
            title = "जार्विस से बात करें (Ready to Command)",
            description = "अब Arc Reactor या माइक बटन पर टैप करें और बोलें: 'नमस्ते जार्विस', 'राहुल को कॉल करो', 'टॉर्च चालू करो', 'यूट्यूब खोलो'!",
            icon = Icons.Default.CheckCircle
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = JarvisCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("install_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = JarvisGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "मोबाइल पर इंस्टॉल करने की विधि",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_install_guide")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "अपने एंड्रॉइड फोन में जार्विस असिस्टेंट को चलाने के लिए नीचे दिए गए 6 आसान चरणों का पालन करें:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Steps List
                steps.forEach { step ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisCardBgVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(JarvisCyan.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = step.stepNum,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyanBright
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = step.description,
                                    fontSize = 11.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisDarkBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("install_guide_ok_button")
                ) {
                    Text("समझ गया (Got It)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
