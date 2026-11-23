package com.example.ui.glass

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.domain.model.GlassMaterialConfig

val LocalBackdropLayer = compositionLocalOf<GraphicsLayer?> { null }
val LocalGlassConfig = compositionLocalOf { GlassMaterialConfig() }
val LocalReducedMotion = compositionLocalOf { false }
val LocalLowPowerMode = compositionLocalOf { false }

/**
 * Hosts the ambient backdrop scene once per frame into a shared GraphicsLayer so every
 * floating LiquidGlassSurface can sample and optically refract live pixels beneath it.
 */
@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    glassConfig: GlassMaterialConfig,
    reducedMotion: Boolean,
    lowPowerMode: Boolean,
    backdrop: @Composable () -> Unit,
    overlay: @Composable BoxScope.() -> Unit
) {
    val backdropLayer = rememberGraphicsLayer()

    CompositionLocalProvider(
        LocalBackdropLayer provides backdropLayer,
        LocalGlassConfig provides glassConfig,
        LocalReducedMotion provides reducedMotion,
        LocalLowPowerMode provides lowPowerMode
    ) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        backdropLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(backdropLayer)
                    }
            ) {
                backdrop()
            }
            overlay()
        }
    }
}

/**
 * Genuine optical Liquid Glass surface:
 * - On Android 13+ (API 33+) with live backdrop available and low-power disabled:
 *   uses AGSL RuntimeShader RenderEffect to visibly refract, curve, and disperse
 *   live background pixels underneath the glass while preserving crystal-clear transparency.
 * - On API 24–32 or in low-power/reduced-motion mode:
 *   uses multi-layer optical specular rims, internal Fresnel arcs, and crisp glass depth.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    config: GlassMaterialConfig = LocalGlassConfig.current,
    cornerRadius: Dp = config.cornerRadiusDp.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val backdropLayer = LocalBackdropLayer.current
    val reducedMotion = LocalReducedMotion.current
    val lowPowerMode = LocalLowPowerMode.current
    val density = LocalDensity.current

    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    var touchPoint by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    val pressedAnim by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = if (reducedMotion) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_press"
    )

    val elasticScale = if (reducedMotion || lowPowerMode) {
        1f
    } else {
        1f - pressedAnim * (0.028f * config.interactionElasticity)
    }

    val shape = RoundedCornerShape(cornerRadius)
    val cornerPx = with(density) { cornerRadius.toPx() }
    val shadowPx = with(density) { config.shadowElevationDp.dp.toPx() }

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidGlassShader.createShader()
        } else null
    }

    val useOpticalShader = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        shader != null &&
        backdropLayer != null &&
        !lowPowerMode &&
        config.refractionStrength > 0.02f

    val composeRenderEffect = remember(
        useOpticalShader,
        componentSize,
        cornerPx,
        config,
        touchPoint,
        pressedAnim
    ) {
        if (useOpticalShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
            LiquidGlassShader.buildRenderEffect(
                shader = shader,
                widthPx = componentSize.width.toFloat(),
                heightPx = componentSize.height.toFloat(),
                cornerRadiusPx = cornerPx,
                config = config,
                touchX = if (touchPoint == Offset.Zero) componentSize.width * 0.35f else touchPoint.x,
                touchY = if (touchPoint == Offset.Zero) componentSize.height * 0.25f else touchPoint.y,
                pressedAmount = if (reducedMotion) 0f else pressedAnim
            )
        } else null
    }

    val interactiveModifier = if (onClick != null || onLongClick != null) {
        Modifier.pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = { offset ->
                    touchPoint = offset
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick?.invoke() },
                onLongPress = { onLongClick?.invoke() }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                positionInRoot = coords.positionInRoot()
                componentSize = coords.size
            }
            .graphicsLayer {
                scaleX = elasticScale
                scaleY = elasticScale
                this.shadowElevation = shadowPx
                this.shape = shape
                this.clip = false
            }
            .then(interactiveModifier)
    ) {
        // Layer 1: Optical refracted backdrop slab (never affects foreground text sharpness)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (useOpticalShader && composeRenderEffect != null && backdropLayer != null) {
                        Modifier
                            .graphicsLayer {
                                this.renderEffect = composeRenderEffect
                                this.clip = true
                                this.shape = shape
                            }
                            .drawBehind {
                                translate(left = -positionInRoot.x, top = -positionInRoot.y) {
                                    drawLayer(backdropLayer)
                                }
                            }
                    } else {
                        Modifier
                    }
                )
        )

        // Layer 2: Fallback optical surface tint + Fresnel specular rim & internal edge reflections
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val cr = CornerRadius(cornerPx, cornerPx)

            // When shader isn't active (API < 33 or low-power), paint calibrated crystal tint
            val tintAlpha = if (useOpticalShader && composeRenderEffect != null) {
                0f
            } else {
                (1f - config.transparency).coerceIn(0.01f, 0.88f)
            }
            if (tintAlpha > 0f) {
                drawRoundRect(
                    color = config.tintColor.copy(alpha = tintAlpha),
                    cornerRadius = cr
                )
            }

            // Subtle convex internal specular gradient (keeps center crystal clear)
            val hl = config.edgeHighlightStrength.coerceIn(0f, 1.5f)
            if (hl > 0.01f) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f * hl),
                            Color.Transparent,
                            Color(0xFFD9A66C).copy(alpha = 0.03f * hl)
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    ),
                    cornerRadius = cr
                )

                // Hairline outer reflective rim
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.42f * hl).coerceAtMost(0.85f)),
                            Color.White.copy(alpha = (0.08f * hl).coerceAtMost(0.3f)),
                            Color(0xFFEBD2B0).copy(alpha = (0.24f * hl).coerceAtMost(0.6f))
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    cornerRadius = cr,
                    style = Stroke(width = 1.15.dp.toPx())
                )

                // Fine inner caustic bevel highlight
                val inset = 1.6.dp.toPx()
                if (w > inset * 4 && h > inset * 4) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.16f * hl).coerceAtMost(0.45f)),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2, h - inset * 2),
                        cornerRadius = CornerRadius((cornerPx - inset).coerceAtLeast(0f)),
                        style = Stroke(width = 0.75.dp.toPx())
                    )
                }
            }
        }

        // Layer 3: Crisp foreground content (typography, icons, interactive widgets)
        content()
    }
}
