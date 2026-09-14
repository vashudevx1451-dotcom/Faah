package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.TextPrimary

data class QuickActionItem(
    val label: String,
    val command: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickActionItem("कॉल करो", "Call lagao", Icons.Default.Call, "chip_call"),
        QuickActionItem("टॉर्च चालू", "Flashlight on karo", Icons.Default.FlashlightOn, "chip_torch"),
        QuickActionItem("व्हाट्सएप", "Open WhatsApp", Icons.Default.Chat, "chip_whatsapp"),
        QuickActionItem("यूट्यूब", "Open YouTube", Icons.Default.PlayArrow, "chip_youtube"),
        QuickActionItem("कैमरा", "Open Camera", Icons.Default.CameraAlt, "chip_camera"),
        QuickActionItem("बैटरी स्टेटस", "Battery kitni hai", Icons.Default.BatteryChargingFull, "chip_battery"),
        QuickActionItem("वॉल्यूम बढ़ाओ", "Volume badhao", Icons.Default.VolumeUp, "chip_volume"),
        QuickActionItem("अलार्म", "Alarm lagao", Icons.Default.Alarm, "chip_alarm")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { item ->
            FilterChip(
                selected = false,
                onClick = { onActionClick(item.command) },
                label = { Text(item.label, color = TextPrimary, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = JarvisCardBg,
                    labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = JarvisBorder
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
