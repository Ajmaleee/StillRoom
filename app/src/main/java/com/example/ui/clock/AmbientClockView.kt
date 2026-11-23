package com.example.ui.clock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockStyle
import com.example.domain.model.ClockTypography
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomLinen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientClockView(
    config: ClockConfig,
    isIdle: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (config.style == ClockStyle.HIDDEN) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(config.showSeconds, isIdle) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = if (config.showSeconds && !isIdle) 250L else 1000L
            delay(delayMs)
        }
    }

    val fontFamily = when (config.typography) {
        ClockTypography.SERIF -> CormorantFontFamily
        ClockTypography.MONO -> JetBrainsMonoFontFamily
        ClockTypography.SANS -> PlusJakartaFontFamily
    }

    val dateObj = remember(nowMillis / 1000L) { Date(nowMillis) }
    val cal = remember(nowMillis / 1000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        when (config.style) {
            ClockStyle.EDITORIAL_DIGITAL -> {
                val pattern = if (config.is24Hour) "HH:mm" else "h:mm"
                val mainTime = SimpleDateFormat(pattern, Locale.getDefault()).format(dateObj)
                val secStr = SimpleDateFormat("ss", Locale.getDefault()).format(dateObj)
                val amPm = if (config.is24Hour) "" else SimpleDateFormat("a", Locale.getDefault()).format(dateObj)

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = mainTime,
                        fontFamily = fontFamily,
                        fontSize = (92f * config.size.scale).sp,
                        lineHeight = (94f * config.size.scale).sp,
                        fontWeight = FontWeight.Normal,
                        color = StillroomLinen.copy(alpha = if (isIdle) 0.85f else 0.96f),
                        letterSpacing = (-2).sp
                    )
                    if (config.showSeconds || !config.is24Hour) {
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.padding(bottom = 14.dp)) {
                            if (config.showSeconds) {
                                Text(
                                    text = secStr,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = (22f * config.size.scale).sp,
                                    color = StillroomAmber.copy(alpha = 0.88f)
                                )
                            }
                            if (!config.is24Hour) {
                                Text(
                                    text = amPm.uppercase(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = (13f * config.size.scale).sp,
                                    color = StillroomBone.copy(alpha = 0.72f),
                                    letterSpacing = 1.6.sp
                                )
                            }
                        }
                    }
                }
            }

            ClockStyle.MINIMAL_ANALOG -> {
                val dialSize = (176f * config.size.scale).dp
                val hour = cal.get(Calendar.HOUR)
                val minute = cal.get(Calendar.MINUTE)
                val second = cal.get(Calendar.SECOND)
                val ms = cal.get(Calendar.MILLISECOND)
                val smoothSec = second + (if (reducedMotion || isIdle) 0f else ms / 1000f)

                Box(modifier = Modifier.size(dialSize), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val r = size.minDimension / 2f
                        val c = Offset(size.width / 2f, size.height / 2f)

                        drawCircle(
                            color = StillroomLinen.copy(alpha = 0.28f),
                            radius = r - 2f,
                            center = c,
                            style = Stroke(width = 1.6f)
                        )

                        for (tick in 0 until 60) {
                            val angle = (tick * 6.0 - 90.0) * PI / 180.0
                            val isHour = tick % 5 == 0
                            val inner = if (isHour) r * 0.84f else r * 0.91f
                            val outer = r * 0.96f
                            drawLine(
                                color = if (isHour) StillroomLinen.copy(alpha = 0.72f) else StillroomBone.copy(alpha = 0.3f),
                                start = Offset((c.x + cos(angle) * inner).toFloat(), (c.y + sin(angle) * inner).toFloat()),
                                end = Offset((c.x + cos(angle) * outer).toFloat(), (c.y + sin(angle) * outer).toFloat()),
                                strokeWidth = if (isHour) 2.2f else 1f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Hour hand
                        val hAngle = (((hour + minute / 60f) * 30f) - 90f) * PI / 180.0
                        drawLine(
                            color = StillroomLinen,
                            start = c,
                            end = Offset((c.x + cos(hAngle) * r * 0.50f).toFloat(), (c.y + sin(hAngle) * r * 0.50f).toFloat()),
                            strokeWidth = 4.5f,
                            cap = StrokeCap.Round
                        )

                        // Minute hand
                        val mAngle = (((minute + smoothSec / 60f) * 6f) - 90f) * PI / 180.0
                        drawLine(
                            color = StillroomLinen.copy(alpha = 0.92f),
                            start = c,
                            end = Offset((c.x + cos(mAngle) * r * 0.74f).toFloat(), (c.y + sin(mAngle) * r * 0.74f).toFloat()),
                            strokeWidth = 2.8f,
                            cap = StrokeCap.Round
                        )

                        // Second hand
                        if (config.showSeconds) {
                            val sAngle = ((smoothSec * 6f) - 90f) * PI / 180.0
                            drawLine(
                                color = StillroomAmber,
                                start = Offset((c.x - cos(sAngle) * r * 0.16f).toFloat(), (c.y - sin(sAngle) * r * 0.16f).toFloat()),
                                end = Offset((c.x + cos(sAngle) * r * 0.82f).toFloat(), (c.y + sin(sAngle) * r * 0.82f).toFloat()),
                                strokeWidth = 1.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        drawCircle(color = StillroomAmber, radius = 4.5f, center = c)
                    }
                }
            }

            ClockStyle.FLIP_CLOCK -> {
                val hStr = SimpleDateFormat(if (config.is24Hour) "HH" else "hh", Locale.getDefault()).format(dateObj)
                val mStr = SimpleDateFormat("mm", Locale.getDefault()).format(dateObj)
                val sStr = SimpleDateFormat("ss", Locale.getDefault()).format(dateObj)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlipDigitTile(hStr, config.size.scale, reducedMotion)
                    Text(
                        text = ":",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = (46f * config.size.scale).sp,
                        color = StillroomAmber
                    )
                    FlipDigitTile(mStr, config.size.scale, reducedMotion)
                    if (config.showSeconds) {
                        FlipDigitTile(sStr, config.size.scale * 0.62f, reducedMotion)
                    }
                }
            }

            ClockStyle.HIDDEN -> Unit
        }

        if (config.showDate) {
            Spacer(Modifier.height(10.dp))
            val dateLine = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(dateObj)
            Text(
                text = dateLine.uppercase(),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = (13f * config.size.scale.coerceAtLeast(0.85f)).sp,
                letterSpacing = 2.4.sp,
                color = StillroomBone.copy(alpha = 0.86f)
            )
        }

        if (config.worldZones.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                config.worldZones.take(4).forEach { zone ->
                    val fmt = SimpleDateFormat(if (config.is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone(zone.zoneId)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = zone.label,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = StillroomAmber.copy(alpha = 0.85f),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = fmt.format(dateObj),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = StillroomBone.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipDigitTile(
    value: String,
    scale: Float,
    reducedMotion: Boolean
) {
    val tileShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(tileShape)
            .background(Color(0xFF1A1714).copy(alpha = 0.68f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), tileShape)
            .padding(horizontal = (16f * scale).dp, vertical = (10f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                } else {
                    (slideInVertically(tween(260)) { -it / 2 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(220)))
                }
            },
            label = "flip_digit"
        ) { target ->
            Text(
                text = target,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = (54f * scale).sp,
                fontWeight = FontWeight.Medium,
                color = StillroomLinen
            )
        }
        // Horizontal split-flap hinge line
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .width((64f * scale).dp)
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
    }
}
