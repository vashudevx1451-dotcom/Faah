package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class JarvisState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

@Composable
fun ArcReactorView(
    state: JarvisState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.PROCESSING -> 3000
                    JarvisState.LISTENING -> 5000
                    JarvisState.SPEAKING -> 6000
                    else -> 16000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation for counter-ring
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.PROCESSING -> 4000
                    JarvisState.LISTENING -> 7000
                    else -> 20000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Breathing pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 800
                    JarvisState.SPEAKING -> 1100
                    JarvisState.PROCESSING -> 600
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Primary State Color
    val stateColor = when (state) {
        JarvisState.IDLE -> JarvisCyan
        JarvisState.LISTENING -> JarvisGreen
        JarvisState.PROCESSING -> JarvisGold
        JarvisState.SPEAKING -> JarvisCyanBright
        JarvisState.ERROR -> JarvisRed
    }

    val stateDimColor = when (state) {
        JarvisState.IDLE -> JarvisCyanDim
        JarvisState.LISTENING -> Color(0xFF00C853)
        JarvisState.PROCESSING -> Color(0xFFFFB300)
        JarvisState.SPEAKING -> JarvisCyan
        JarvisState.ERROR -> Color(0xFFD50000)
    }

    // Audio reactive boost (rms scale 0-10 -> 0.0-0.2)
    val rmsScale = (audioRms.coerceIn(0f, 10f) / 50f)
    val totalScale = pulse + (if (state == JarvisState.LISTENING) rmsScale else 0f)

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("arc_reactor_core")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.85f * totalScale

            // 1. Ambient Background Glow Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateColor.copy(alpha = 0.25f),
                        stateDimColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.25f,
                center = center
            )

            // 2. Outermost Segmented Arc Ring (rotating clockwise)
            rotate(degrees = rotation, pivot = center) {
                val segments = 8
                val sweep = 360f / segments
                for (i in 0 until segments) {
                    val startAngle = i * sweep + 4f
                    drawArc(
                        color = stateColor.copy(alpha = 0.8f),
                        startAngle = startAngle,
                        sweepAngle = sweep - 12f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 2),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Middle Counter-Rotating Track Ring
            val middleRadius = baseRadius * 0.76f
            rotate(degrees = reverseRotation, pivot = center) {
                drawCircle(
                    color = stateDimColor.copy(alpha = 0.35f),
                    radius = middleRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // 12 power nodes along the middle track
                for (i in 0 until 12) {
                    val angle = (i * 30f) * (PI.toFloat() / 180f)
                    val nodeX = center.x + middleRadius * cos(angle)
                    val nodeY = center.y + middleRadius * sin(angle)
                    drawCircle(
                        color = if (i % 3 == 0) stateColor else stateDimColor.copy(alpha = 0.6f),
                        radius = if (i % 3 == 0) 4.5f else 2.5f,
                        center = Offset(nodeX, nodeY)
                    )
                }
            }

            // 4. Inner Cybernetic Ring with 10 Triangular Teeth / Spokes
            val innerRingRadius = baseRadius * 0.54f
            rotate(degrees = rotation * 0.7f, pivot = center) {
                drawCircle(
                    color = stateColor,
                    radius = innerRingRadius,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                val teethCount = 10
                for (i in 0 until teethCount) {
                    val angle = (i * (360f / teethCount)) * (PI.toFloat() / 180f)
                    val innerX = center.x + (innerRingRadius - 10f) * cos(angle)
                    val innerY = center.y + (innerRingRadius - 10f) * sin(angle)
                    val outerX = center.x + (innerRingRadius + 6f) * cos(angle)
                    val outerY = center.y + (innerRingRadius + 6f) * sin(angle)

                    drawLine(
                        color = stateColor,
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Central Glowing Core Sphere
            val coreRadius = baseRadius * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        stateColor,
                        stateDimColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 6. Innermost Power Dot
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.4f,
                center = center
            )
        }
    }
}
