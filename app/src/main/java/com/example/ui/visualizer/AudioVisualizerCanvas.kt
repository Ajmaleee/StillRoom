package com.example.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AudioCaptureStatus
import com.example.data.audio.AudioFrameData
import com.example.domain.model.VisualizerConfig
import com.example.domain.model.VisualizerMode
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomLinen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AudioVisualizerCanvas(
    config: VisualizerConfig,
    frame: AudioFrameData,
    controlsVisible: Boolean,
    onRequestAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!config.enabled) return

    val primary = Color(config.colorPreset.primaryArgb).copy(alpha = config.alpha)
    val secondary = Color(config.colorPreset.secondaryArgb).copy(alpha = config.alpha * 0.72f)

    val particleSeeds = remember {
        val rng = Random(204)
        List(48) { FloatArray(3).apply { this[0] = rng.nextFloat(); this[1] = rng.nextFloat(); this[2] = rng.nextFloat() } }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Real-time audio visualizer" }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val bands = frame.bands
            val wave = frame.waveform
            val scale = config.sizeScale.coerceIn(0.5f, 1.5f)

            when (config.mode) {
                VisualizerMode.SPECTRUM_BARS -> {
                    val count = bands.size
                    val totalW = w * 0.86f
                    val startX = (w - totalW) / 2f
                    val slotW = totalW / count
                    val barW = slotW * 0.56f
                    val maxBarH = h * 0.78f * scale
                    for (i in 0 until count) {
                        val mag = bands[i].coerceIn(0.02f, 1f)
                        val barH = (mag * maxBarH).coerceAtLeast(3.dp.toPx())
                        val x = startX + i * slotW + (slotW - barW) / 2f
                        val y = h - barH
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(primary, secondary), startY = y, endY = h),
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }

                VisualizerMode.CIRCULAR_SPECTRUM -> {
                    val center = Offset(w / 2f, h / 2f)
                    val baseR = size.minDimension * 0.24f * scale
                    val count = bands.size
                    for (i in 0 until count) {
                        val angle = (i.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f
                        val mag = bands[i].coerceIn(0.02f, 1f)
                        val len = baseR * (0.12f + mag * 0.95f)
                        val p0 = Offset(center.x + cos(angle) * baseR, center.y + sin(angle) * baseR)
                        val p1 = Offset(center.x + cos(angle) * (baseR + len), center.y + sin(angle) * (baseR + len))
                        drawLine(
                            color = if (i % 2 == 0) primary else secondary,
                            start = p0,
                            end = p1,
                            strokeWidth = 3.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = primary.copy(alpha = 0.25f),
                        radius = baseR * (0.94f + frame.bassEnergy * 0.16f),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                VisualizerMode.WAVEFORM -> {
                    val path = Path()
                    val midY = h * 0.55f
                    val amp = h * 0.36f * scale
                    for (i in wave.indices) {
                        val x = (i.toFloat() / (wave.size - 1)) * w
                        val y = midY + wave[i] * amp
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(secondary, primary, secondary)),
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                VisualizerMode.FREQUENCY_RINGS -> {
                    val center = Offset(w / 2f, h / 2f)
                    val maxR = size.minDimension * 0.42f * scale
                    val ringEnergies = listOf(frame.bassEnergy, frame.midEnergy, frame.trebleEnergy, frame.overallRms)
                    ringEnergies.forEachIndexed { idx, energy ->
                        val r = maxR * (0.28f + idx * 0.21f) * (0.85f + energy * 0.45f)
                        drawCircle(
                            color = if (idx % 2 == 0) primary else secondary,
                            radius = r,
                            center = center,
                            style = Stroke(width = (3.5f - idx * 0.5f).dp.toPx())
                        )
                    }
                }

                VisualizerMode.AUDIO_PARTICLES -> {
                    particleSeeds.forEachIndexed { idx, seed ->
                        val bandVal = bands[idx % bands.size]
                        val px = seed[0] * w
                        val py = h * (0.85f - seed[1] * 0.65f - bandVal * 0.22f * scale)
                        val r = (2.5f + seed[2] * 4f + bandVal * 10f) * scale
                        drawCircle(
                            color = (if (idx % 3 == 0) secondary else primary).copy(alpha = (0.25f + bandVal * 0.65f).coerceAtMost(0.9f)),
                            radius = r,
                            center = Offset(px, py)
                        )
                    }
                }

                VisualizerMode.MINIMAL_PULSE -> {
                    val center = Offset(w / 2f, h * 0.55f)
                    val radius = (size.minDimension * 0.16f + frame.overallRms * size.minDimension * 0.28f) * scale
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(primary.copy(alpha = 0.48f), secondary.copy(alpha = 0.15f), Color.Transparent),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.65f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }

                VisualizerMode.LIQUID_WAVES -> {
                    for (layer in 0 until 3) {
                        val path = Path()
                        val baseY = h * (0.58f + layer * 0.10f)
                        path.moveTo(0f, h)
                        path.lineTo(0f, baseY)
                        for (i in bands.indices) {
                            val x = (i.toFloat() / (bands.size - 1)) * w
                            val energy = bands[i] * scale
                            val y = baseY - energy * h * (0.42f - layer * 0.09f) - sin(i * 0.45f + layer) * 6f
                            path.lineTo(x, y)
                        }
                        path.lineTo(w, h)
                        path.close()
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                listOf(
                                    (if (layer == 0) primary else secondary).copy(alpha = 0.34f - layer * 0.08f),
                                    Color.Transparent
                                ),
                                startY = h * 0.2f,
                                endY = h
                            )
                        )
                    }
                }

                VisualizerMode.DUAL_BAND -> {
                    val midY = h * 0.55f
                    val half = bands.size / 2
                    val slotW = (w * 0.84f) / half
                    val startX = w * 0.08f
                    for (i in 0 until half) {
                        val low = bands[i] * h * 0.38f * scale
                        val high = bands[i + half] * h * 0.32f * scale
                        val x = startX + i * slotW
                        drawLine(
                            color = primary,
                            start = Offset(x, midY),
                            end = Offset(x, midY - low.coerceAtLeast(3f)),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = secondary,
                            start = Offset(x, midY + 4f),
                            end = Offset(x, midY + 4f + high.coerceAtLeast(3f)),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Honest communication when RECORD_AUDIO isn't granted or unavailable
        if (controlsVisible && (frame.status is AudioCaptureStatus.PermissionRequired || frame.status is AudioCaptureStatus.Unavailable)) {
            LiquidGlassSurface(
                onClick = onRequestAudioPermission,
                cornerRadius = 18.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (frame.status is AudioCaptureStatus.PermissionRequired) {
                            Icons.Outlined.GraphicEq
                        } else {
                            Icons.Outlined.MicOff
                        },
                        contentDescription = null,
                        tint = StillroomAmber
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (val st = frame.status) {
                            is AudioCaptureStatus.PermissionRequired -> "Enable Microphone for Live Audio Visualizer"
                            is AudioCaptureStatus.Unavailable -> st.reason
                            else -> ""
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = StillroomLinen
                    )
                }
            }
        }
    }
}
