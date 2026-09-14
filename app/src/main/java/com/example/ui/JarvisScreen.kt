package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.components.CommandHistorySheet
import com.example.ui.components.InstallGuideDialog
import com.example.ui.components.QuickActionChips
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBgVariant
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.commandHistory.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInstallGuideDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }

    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (audioGranted) {
            if (uiState.isWakeWordActive) {
                viewModel.startWakeWordListening()
            } else {
                viewModel.startListening()
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("वॉयस कंट्रोल और 'Jarvis' वेक-वर्ड के लिए माइक्रोफ़ोन अनुमति आवश्यक है।")
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val audioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (audioPerm != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS
                )
            )
        } else if (uiState.isWakeWordActive) {
            viewModel.startWakeWordListening()
        }
    }

    fun checkAndStartListening() {
        val audioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        val phonePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
        val contactPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)

        val needed = mutableListOf<String>()
        if (audioPerm != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (phonePerm != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CALL_PHONE)
        if (contactPerm != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_CONTACTS)

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            viewModel.toggleListening()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = JarvisDarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top HUD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(JarvisCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "J.A.R.V.I.S.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = JarvisCyan
                        )
                    }
                    Text(
                        text = "VOICE AI PHONE ASSISTANT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // API Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (uiState.isApiKeyConfigured) JarvisGreen.copy(alpha = 0.15f)
                                else JarvisGold.copy(alpha = 0.15f)
                            )
                            .border(
                                0.5.dp,
                                if (uiState.isApiKeyConfigured) JarvisGreen.copy(alpha = 0.4f)
                                else JarvisGold.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isApiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.isApiKeyConfigured) JarvisGreen else JarvisGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isApiKeyConfigured) "AI ACTIVE" else "KEY NEEDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isApiKeyConfigured) JarvisGreen else JarvisGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Install Guide Button
                    IconButton(
                        onClick = { showInstallGuideDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("install_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Install Guide",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // History Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Central Interactive Arc Reactor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorView(
                    state = uiState.state,
                    audioRms = uiState.audioRms,
                    onClick = { checkAndStartListening() }
                )
            }

            // 3. State Status Pill
            val statePillColor by animateColorAsState(
                targetValue = when (uiState.state) {
                    JarvisState.IDLE -> JarvisCyan
                    JarvisState.LISTENING -> JarvisGreen
                    JarvisState.PROCESSING -> JarvisGold
                    JarvisState.SPEAKING -> JarvisCyanBright
                    JarvisState.ERROR -> JarvisRed
                },
                label = "pill_color"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statePillColor.copy(alpha = 0.12f))
                    .border(1.dp, statePillColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (uiState.state) {
                        JarvisState.IDLE -> "⚫ STANDBY (सक्रिय होने के लिए तैयार)"
                        JarvisState.LISTENING -> "🟢 LISTENING... (सुन रहा हूँ, बोलिए)"
                        JarvisState.PROCESSING -> "🟡 PROCESSING... (प्रोसेस हो रहा है)"
                        JarvisState.SPEAKING -> "🔵 SPEAKING... (जार्विस बोल रहा है)"
                        JarvisState.ERROR -> "🔴 ERROR (त्रुटि)"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statePillColor,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Live Interaction Card (Recognized Text & Hindi Male Voice Response)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("live_interaction_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // User query section
                    if (uiState.lastUserCommand.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👤 आपकी आज्ञा:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisGold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${uiState.lastUserCommand}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(JarvisBorder.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Jarvis response section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(JarvisCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🤖 जार्विस का उत्तर (Hindi Voice):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisCyan
                            )
                        }

                        Row {
                            if (uiState.state == JarvisState.SPEAKING) {
                                IconButton(
                                    onClick = { viewModel.stopSpeaking() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop voice",
                                        tint = JarvisRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.speakText(uiState.lastJarvisResponse) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak voice",
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.lastJarvisResponse,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 21.sp
                    )

                    if (uiState.errorMessage != null && uiState.state == JarvisState.ERROR) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 सुझाव: सेटिंग्स (Settings ⚙️) में जाकर Google AI Studio API Key जोड़ें।",
                            fontSize = 11.sp,
                            color = JarvisGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4.5. Wake-Word "JARVIS" Detection & Voice Test Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isWakeWordActive) JarvisCardBgVariant else JarvisCardBg
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isWakeWordActive) JarvisGreen.copy(alpha = 0.5f) else JarvisBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("wake_word_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (uiState.isWakeWordActive) JarvisGreen else TextMuted,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isWakeWordActive) "🟢 'Jarvis' वेक-वर्ड सक्रिय" else "⚪ 'Jarvis' वेक-वर्ड बंद",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isWakeWordActive) JarvisGreen else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isWakeWordActive) "मुंह से 'Jarvis' बोलते ही तुरंत सक्रिय होगा" else "बोलकर जगाने के लिए चालू करें",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice Test Button
                        Button(
                            onClick = { viewModel.testJarvisVoice() },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanDim.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Test Voice",
                                tint = JarvisCyanBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("आवाज़ टेस्ट", fontSize = 10.sp, color = JarvisCyanBright, fontWeight = FontWeight.SemiBold)
                        }

                        // Toggle Switch
                        androidx.compose.material3.Switch(
                            checked = uiState.isWakeWordActive,
                            onCheckedChange = { viewModel.toggleWakeWordMode() },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = JarvisGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = JarvisCardBg
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Quick Action Chips Header & Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "त्वरित आदेश (Quick Actions)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                if (uiState.isTorchActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = null,
                            tint = JarvisGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("टॉर्च चालू", fontSize = 10.sp, color = JarvisGold)
                    }
                }
            }

            QuickActionChips(
                onActionClick = { cmd ->
                    viewModel.triggerQuickAction(cmd)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Bottom Controls: Big Futuristic Push-to-Talk Mic & Keyboard Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Keyboard Type button
                IconButton(
                    onClick = { showTextInputDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(JarvisCardBg)
                        .border(1.dp, JarvisBorder, CircleShape)
                        .testTag("type_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Type command",
                        tint = JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Giant Glowing Push-To-Talk Mic
                val micTransition = rememberInfiniteTransition(label = "mic_pulse")
                val micScale by micTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (uiState.state == JarvisState.LISTENING) 1.15f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_scale"
                )

                Box(
                    modifier = Modifier
                        .scale(micScale)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (uiState.state == JarvisState.LISTENING) listOf(JarvisGreen, Color(0xFF00C853))
                                else listOf(JarvisCyan, JarvisCyanDim)
                            )
                        )
                        .border(
                            2.dp,
                            if (uiState.state == JarvisState.LISTENING) Color.White else JarvisCyanBright,
                            CircleShape
                        )
                        .clickable { checkAndStartListening() }
                        .testTag("main_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.state == JarvisState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = JarvisDarkBg,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Install Guide quick button
                IconButton(
                    onClick = { showInstallGuideDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(JarvisCardBg)
                        .border(1.dp, JarvisBorder, CircleShape)
                        .testTag("quick_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Install Guide",
                        tint = JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = if (uiState.state == JarvisState.LISTENING) "सुन रहा हूँ... अपनी आज्ञा बोलें" else "माइक दबाकर बोलें या ऊपर Arc Reactor पर टैप करें",
                fontSize = 12.sp,
                color = if (uiState.state == JarvisState.LISTENING) JarvisGreen else TextMuted,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            preferences = viewModel.preferences,
            onDismiss = { showSettingsDialog = false },
            onTestVoice = { pitch, speed ->
                viewModel.testMaleVoice(pitch, speed)
            },
            onSave = { apiKey, userName, pitch, speed, autoSpeak ->
                viewModel.saveSettings(apiKey, userName, pitch, speed, autoSpeak)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("सेटिंग्स सुरक्षित कर दी गई हैं।")
                }
            }
        )
    }

    // Install Guide Dialog
    if (showInstallGuideDialog) {
        InstallGuideDialog(
            onDismiss = { showInstallGuideDialog = false }
        )
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        CommandHistorySheet(
            history = history,
            sheetState = historySheetState,
            onDismiss = { showHistorySheet = false },
            onSpeak = { text -> viewModel.speakText(text) },
            onClearHistory = { viewModel.clearHistory() }
        )
    }

    // Manual Text Input Command Dialog
    if (showTextInputDialog) {
        Dialog(onDismissRequest = { showTextInputDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = JarvisCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("text_input_dialog")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⌨️ कमांड टाइप करें (Type Command)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "जैसे: 'Call 9876543210', 'टॉर्च चालू करो', 'Open YouTube'",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = { Text("अपनी आज्ञा यहाँ लिखें...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = JarvisCyan,
                            focusedContainerColor = JarvisCardBgVariant,
                            unfocusedContainerColor = JarvisCardBgVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_command_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    viewModel.processUserCommand(manualInputText)
                                    manualInputText = ""
                                    showTextInputDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisDarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("send_manual_command_button")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("भेजें (Send)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
