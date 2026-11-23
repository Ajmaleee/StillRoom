package com.example.ui.ambient

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.AmbientSceneType
import com.example.domain.model.SceneTransitionType
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomObsidian
import com.example.ui.theme.StillroomPlum
import com.example.ui.theme.StillroomSage
import com.example.ui.theme.StillroomSlateBlue
import com.example.ui.theme.StillroomTerracotta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AmbientBackgroundEngine(
    sceneType: AmbientSceneType,
    transitionType: SceneTransitionType,
    customImageUri: String?,
    motionSpeed: Float,
    warmthTint: Float,
    grainOpacity: Float,
    dimmingOverlay: Float,
    nightModeTint: Boolean,
    reducedMotion: Boolean,
    lowPowerMode: Boolean,
    isIdle: Boolean,
    audioEnergy: Float,
    modifier: Modifier = Modifier
) {
    // Master continuous phase clock throttled gracefully in idle / low-power / reduced-motion
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reducedMotion, lowPowerMode, isIdle, motionSpeed) {
        if (reducedMotion) return@LaunchedEffect
        var lastNanos = 0L
        val effectiveSpeed = motionSpeed.coerceIn(0.2f, 2.0f) * when {
            lowPowerMode -> 0.25f
            isIdle -> 0.55f
            else -> 1.0f
        }
        while (true) {
            withFrameNanos { frameTime ->
                if (lastNanos != 0L) {
                    val dt = ((frameTime - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    phase += dt * effectiveSpeed
                }
                lastNanos = frameTime
            }
        }
    }

    AnimatedContent(
        targetState = sceneType,
        transitionSpec = { buildSceneTransition(transitionType, reducedMotion) },
        label = "ambient_scene_transition",
        modifier = modifier
    ) { activeScene ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StillroomObsidian)
        ) {
            when (activeScene) {
                AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT -> SoftAtmosphericLightScene(phase, warmthTint, audioEnergy)
                AmbientSceneType.LANDSCAPE_RIDGE -> DrawableLandscapeScene(R.drawable.img_ambient_ridge, phase, reducedMotion)
                AmbientSceneType.LANDSCAPE_BOTANICAL -> DrawableLandscapeScene(R.drawable.img_ambient_botanical, phase, reducedMotion)
                AmbientSceneType.PARTICLE_FIELD -> ParticleFieldScene(phase, audioEnergy)
                AmbientSceneType.ABSTRACT_SHAPES -> AbstractShapesScene(phase, warmthTint)
                AmbientSceneType.SPACE_STARFIELD -> StarfieldScene(phase)
                AmbientSceneType.MATRIX_RAIN -> MatrixRainScene(phase)
                AmbientSceneType.RAIN_ON_GLASS -> RainOnGlassScene(phase)
                AmbientSceneType.OCEAN_FLOW -> OceanFlowScene(phase, audioEnergy)
                AmbientSceneType.AURORA_MOTION -> AuroraMotionScene(phase, audioEnergy)
                AmbientSceneType.MINIMAL_GEOMETRIC -> MinimalGeometricScene(phase, audioEnergy)
                AmbientSceneType.CUSTOM_IMAGE -> CustomImageScene(customImageUri, phase, reducedMotion)
            }

            // Subtle film grain + warmth veil + idle/night dimming
            AmbientAtmosphereOverlay(
                warmthTint = warmthTint,
                grainOpacity = if (lowPowerMode) 0f else grainOpacity,
                dimmingOverlay = dimmingOverlay,
                nightModeTint = nightModeTint
            )
        }
    }
}

private fun buildSceneTransition(
    type: SceneTransitionType,
    reducedMotion: Boolean
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(tween(250)) togetherWith fadeOut(tween(250))
    }
    return when (type) {
        SceneTransitionType.CROSSFADE ->
            fadeIn(tween(1100)) togetherWith fadeOut(tween(1100))
        SceneTransitionType.DISSOLVE ->
            fadeIn(tween(1500)) togetherWith fadeOut(tween(1400))
        SceneTransitionType.GENTLE_ZOOM ->
            (fadeIn(tween(1100)) + scaleIn(initialScale = 1.06f, animationSpec = tween(1200))) togetherWith
                (fadeOut(tween(900)) + scaleOut(targetScale = 0.96f, animationSpec = tween(900)))
        SceneTransitionType.LIQUID ->
            (fadeIn(tween(1300)) + scaleIn(initialScale = 0.94f, animationSpec = tween(1300))) togetherWith
                (fadeOut(tween(1100)) + scaleOut(targetScale = 1.05f, animationSpec = tween(1100)))
        SceneTransitionType.SLIDE ->
            (slideInHorizontally(tween(900)) { it / 6 } + fadeIn(tween(900))) togetherWith
                (slideOutHorizontally(tween(900)) { -it / 6 } + fadeOut(tween(900)))
    }
}

@Composable
private fun SoftAtmosphericLightScene(phase: Float, warmthTint: Float, audioEnergy: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pulse = 1f + audioEnergy * 0.14f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF13110F), Color(0xFF1B1612), Color(0xFF0E0D0B))
            )
        )

        val x1 = w * (0.32f + 0.14f * sin(phase * 0.18f))
        val y1 = h * (0.38f + 0.10f * cos(phase * 0.13f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    StillroomAmber.copy(alpha = (0.34f + warmthTint * 0.14f) * pulse.coerceAtMost(1.2f)),
                    StillroomTerracotta.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(x1, y1),
                radius = w * 0.52f
            ),
            radius = w * 0.52f,
            center = Offset(x1, y1)
        )

        val x2 = w * (0.74f + 0.12f * cos(phase * 0.14f))
        val y2 = h * (0.62f + 0.12f * sin(phase * 0.16f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    StillroomSage.copy(alpha = 0.22f),
                    StillroomSlateBlue.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(x2, y2),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(x2, y2)
        )

        val x3 = w * (0.52f + 0.18f * sin(phase * 0.09f + 2.1f))
        val y3 = h * (0.24f + 0.08f * cos(phase * 0.11f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    StillroomTerracotta.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(x3, y3),
                radius = w * 0.36f
            ),
            radius = w * 0.36f,
            center = Offset(x3, y3)
        )
    }
}

@Composable
private fun DrawableLandscapeScene(drawableId: Int, phase: Float, reducedMotion: Boolean) {
    val scale = if (reducedMotion) 1.03f else 1.05f + 0.03f * sin(phase * 0.08f)
    val driftX = if (reducedMotion) 0f else 18f * cos(phase * 0.06f)
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = "Ambient landscape backdrop",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = driftX
            }
    )
}

@Composable
private fun CustomImageScene(uriString: String?, phase: Float, reducedMotion: Boolean) {
    if (uriString.isNullOrBlank()) {
        SoftAtmosphericLightScene(phase, 0.3f, 0f)
    } else {
        val context = LocalContext.current
        val scale = if (reducedMotion) 1.02f else 1.04f + 0.025f * sin(phase * 0.07f)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(uriString))
                .crossfade(true)
                .build(),
            contentDescription = "Custom ambient background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun ParticleFieldScene(phase: Float, audioEnergy: Float) {
    val motes = remember {
        val rng = Random(42)
        List(72) {
            FloatArray(5).apply {
                this[0] = rng.nextFloat() // x
                this[1] = rng.nextFloat() // y
                this[2] = 0.4f + rng.nextFloat() * 0.9f // depth speed
                this[3] = 2f + rng.nextFloat() * 8f // radius
                this[4] = rng.nextFloat() * 6.28f // phase offset
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF100F0D), Color(0xFF191612), Color(0xFF0E0D0B))
            )
        )
        motes.forEachIndexed { idx, m ->
            val px = ((m[0] + phase * 0.012f * m[2] + 0.03f * sin(phase * 0.2f + m[4])) % 1.06f) * size.width
            val py = ((m[1] - phase * 0.008f * m[2] + 1.0f) % 1.0f) * size.height
            val alpha = (0.14f + 0.26f * (0.5f + 0.5f * sin(phase * 0.5f + m[4])) + audioEnergy * 0.25f)
                .coerceIn(0.08f, 0.7f)
            val color = if (idx % 3 == 0) StillroomSage else StillroomAmber
            val r = m[3] * (1f + audioEnergy * 0.4f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(px, py),
                    radius = r * 3.2f
                ),
                radius = r * 3.2f,
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun AbstractShapesScene(phase: Float, warmthTint: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF12100E))
        val colors = listOf(
            StillroomTerracotta.copy(alpha = 0.26f + warmthTint * 0.1f),
            StillroomAmber.copy(alpha = 0.22f),
            StillroomSlateBlue.copy(alpha = 0.20f),
            StillroomSage.copy(alpha = 0.18f)
        )
        for (i in 0 until 4) {
            val path = Path()
            val cx = size.width * (0.25f + i * 0.18f + 0.08f * sin(phase * 0.12f + i))
            val cy = size.height * (0.45f + 0.16f * cos(phase * 0.10f + i * 1.4f))
            val baseR = size.minDimension * (0.24f + i * 0.04f)
            val steps = 36
            for (s in 0..steps) {
                val a = (s.toFloat() / steps) * 2f * PI.toFloat()
                val wobble = 1f + 0.18f * sin(a * 3f + phase * 0.28f + i) + 0.10f * cos(a * 2f - phase * 0.22f)
                val x = cx + cos(a) * baseR * wobble
                val y = cy + sin(a) * baseR * wobble
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = colors[i])
        }
    }
}

@Composable
private fun StarfieldScene(phase: Float) {
    val stars = remember {
        val rng = Random(108)
        List(150) {
            FloatArray(4).apply {
                this[0] = rng.nextFloat()
                this[1] = rng.nextFloat()
                this[2] = 0.8f + rng.nextFloat() * 2.2f
                this[3] = rng.nextFloat() * 6.28f
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF07090E), Color(0xFF0E121B), Color(0xFF131115))
            )
        )
        // Subtle warm dust nebula
        drawCircle(
            brush = Brush.radialGradient(
                listOf(StillroomPlum.copy(alpha = 0.16f), StillroomAmber.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width * 0.68f, size.height * 0.36f),
                radius = size.width * 0.45f
            ),
            radius = size.width * 0.45f,
            center = Offset(size.width * 0.68f, size.height * 0.36f)
        )
        stars.forEachIndexed { idx, s ->
            val sx = ((s[0] + phase * 0.002f * s[2]) % 1.0f) * size.width
            val sy = s[1] * size.height
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(phase * 1.1f + s[3]))
            val color = if (idx % 9 == 0) StillroomAmber else Color(0xFFEDE8DF)
            drawCircle(
                color = color.copy(alpha = twinkle * 0.78f),
                radius = s[2],
                center = Offset(sx, sy)
            )
        }
    }
}

@Composable
private fun MatrixRainScene(phase: Float) {
    val textMeasurer = rememberTextMeasurer()
    val glyphs = remember { listOf("0", "1", "7", "A", "E", "K", "S", "T", "R", "M", "9", "4") }
    val columns = remember {
        val rng = Random(77)
        List(34) {
            FloatArray(3).apply {
                this[0] = rng.nextFloat() // initial offset
                this[1] = 0.35f + rng.nextFloat() * 0.55f // speed
                this[2] = (6 + rng.nextInt(9)).toFloat() // trail length
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF0A0D0B))
        val colWidth = size.width / columns.size
        val rowHeight = 26f
        val rows = (size.height / rowHeight).toInt().coerceAtLeast(10)

        columns.forEachIndexed { cIdx, col ->
            val headRow = ((col[0] * rows) + phase * col[1] * 8f) % (rows + col[2])
            val trail = col[2].toInt()
            for (t in 0 until trail) {
                val r = (headRow - t).toInt()
                if (r in 0 until rows) {
                    val alpha = (1f - t.toFloat() / trail) * 0.52f
                    val charIdx = (cIdx * 7 + r + (phase * 2).toInt()) % glyphs.size
                    val color = if (t == 0) StillroomAmber.copy(alpha = 0.78f) else StillroomSage.copy(alpha = alpha)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = glyphs[charIdx.coerceAtLeast(0)],
                        topLeft = Offset(cIdx * colWidth + 6f, r * rowHeight),
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = color
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RainOnGlassScene(phase: Float) {
    val drops = remember {
        val rng = Random(303)
        List(44) {
            FloatArray(4).apply {
                this[0] = rng.nextFloat()
                this[1] = rng.nextFloat()
                this[2] = 0.04f + rng.nextFloat() * 0.08f
                this[3] = 2.5f + rng.nextFloat() * 5.5f
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Warm city evening bokeh backdrop behind wet glass
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0F1318), Color(0xFF181A1E), Color(0xFF14110F))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(StillroomAmber.copy(alpha = 0.22f), Color.Transparent)),
            radius = size.width * 0.24f,
            center = Offset(size.width * 0.25f, size.height * 0.65f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(StillroomTerracotta.copy(alpha = 0.18f), Color.Transparent)),
            radius = size.width * 0.20f,
            center = Offset(size.width * 0.72f, size.height * 0.52f)
        )
        drops.forEachIndexed { idx, d ->
            val x = (d[0] + 0.005f * sin(phase * 1.4f + idx)) * size.width
            val y = ((d[1] + phase * d[2]) % 1.05f) * size.height
            val trailLen = 28f + d[3] * 10f
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.20f)),
                    startY = y - trailLen,
                    endY = y
                ),
                start = Offset(x, y - trailLen),
                end = Offset(x, y),
                strokeWidth = d[3] * 0.45f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = d[3],
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun OceanFlowScene(phase: Float, audioEnergy: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0B1117), Color(0xFF121C24), Color(0xFF0A0F14))
            )
        )
        val waveColors = listOf(
            StillroomSlateBlue.copy(alpha = 0.20f),
            StillroomSage.copy(alpha = 0.16f),
            Color(0xFF2B4152).copy(alpha = 0.28f),
            StillroomAmber.copy(alpha = 0.10f)
        )
        for (layer in 0 until 4) {
            val path = Path()
            val baseY = size.height * (0.42f + layer * 0.12f)
            val amp = (26f + layer * 10f) * (1f + audioEnergy * 0.4f)
            path.moveTo(0f, size.height)
            path.lineTo(0f, baseY)
            val steps = 40
            for (i in 0..steps) {
                val x = (i.toFloat() / steps) * size.width
                val y = baseY +
                    sin(i * 0.24f + phase * (0.45f - layer * 0.06f) + layer) * amp +
                    cos(i * 0.12f - phase * 0.3f) * (amp * 0.45f)
                path.lineTo(x, y)
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, waveColors[layer])
        }
    }
}

@Composable
private fun AuroraMotionScene(phase: Float, audioEnergy: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF090C11), Color(0xFF0E1419), Color(0xFF110F12))
            )
        )
        val ribbons = listOf(
            StillroomSage.copy(alpha = 0.24f + audioEnergy * 0.12f),
            StillroomSlateBlue.copy(alpha = 0.22f),
            StillroomAmber.copy(alpha = 0.16f + audioEnergy * 0.10f)
        )
        ribbons.forEachIndexed { idx, color ->
            val path = Path()
            val yCenter = size.height * (0.34f + idx * 0.12f)
            path.moveTo(0f, yCenter)
            val steps = 32
            for (i in 0..steps) {
                val x = (i.toFloat() / steps) * size.width
                val y = yCenter + sin(i * 0.22f + phase * 0.35f + idx * 1.3f) * (70f + idx * 18f)
                path.lineTo(x, y)
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(listOf(Color.Transparent, color, Color.Transparent)),
                style = Stroke(width = size.height * 0.24f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun MinimalGeometricScene(phase: Float, audioEnergy: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF12100E))
        val center = Offset(size.width * 0.56f, size.height * 0.5f)
        val maxR = size.minDimension * 0.46f
        for (i in 1..6) {
            val r = maxR * (i / 6f) * (1f + audioEnergy * 0.04f)
            drawCircle(
                color = StillroomAmber.copy(alpha = 0.06f + (6 - i) * 0.02f),
                radius = r,
                center = center,
                style = Stroke(width = 1.2f)
            )
            val angle = phase * (0.14f / i) + i * 0.9f
            val dotPos = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
            drawCircle(
                color = if (i % 2 == 0) StillroomAmber.copy(alpha = 0.45f) else StillroomSage.copy(alpha = 0.45f),
                radius = 4f,
                center = dotPos
            )
        }
    }
}

@Composable
private fun AmbientAtmosphereOverlay(
    warmthTint: Float,
    grainOpacity: Float,
    dimmingOverlay: Float,
    nightModeTint: Boolean
) {
    val grainSeeds = remember {
        val rng = Random(99)
        List(180) { Offset(rng.nextFloat(), rng.nextFloat()) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle editorial vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f
            )
        )
        if (warmthTint > 0.02f) {
            drawRect(color = StillroomAmber.copy(alpha = warmthTint * 0.07f))
        }
        if (nightModeTint) {
            drawRect(color = Color(0xFF2B1205).copy(alpha = 0.28f))
        }
        if (dimmingOverlay > 0.01f) {
            drawRect(color = Color.Black.copy(alpha = dimmingOverlay.coerceIn(0f, 0.75f)))
        }
        if (grainOpacity > 0.01f) {
            grainSeeds.forEach { pt ->
                drawCircle(
                    color = Color.White.copy(alpha = grainOpacity * 0.35f),
                    radius = 1.1f,
                    center = Offset(pt.x * size.width, pt.y * size.height)
                )
            }
        }
    }
}
