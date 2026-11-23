package com.example.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import com.example.domain.model.GlassMaterialConfig

object LiquidGlassShader {

    private const val AGSL_SOURCE = """
        uniform shader content;
        uniform float2 uSize;
        uniform float uCornerRadius;
        uniform float uRefraction;
        uniform float uDispersion;
        uniform float uHighlight;
        uniform float uTransparency;
        uniform float4 uTint;
        uniform float2 uTouch;
        uniform float uPressed;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + float2(r);
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = max(uSize * 0.5, float2(1.0));
            float radius = min(uCornerRadius, min(halfSize.x, halfSize.y));
            float2 p = fragCoord - halfSize;

            float sd = sdRoundedBox(p, halfSize, radius);
            float dx = sdRoundedBox(p + float2(1.5, 0.0), halfSize, radius) - sd;
            float dy = sdRoundedBox(p + float2(0.0, 1.5), halfSize, radius) - sd;
            float2 normal = normalize(float2(dx, dy) + float2(0.0001));

            float bevelWidth = max(18.0, min(halfSize.x, halfSize.y) * 0.28);
            float distInside = max(0.0, -sd);
            float bevel = clamp(1.0 - distInside / bevelWidth, 0.0, 1.0);

            // Convex edge refraction + subtle central lens magnification
            float edgeBend = pow(bevel, 1.7) * uRefraction * 26.0;
            float2 domeBend = -p * (0.022 * uRefraction * (1.0 + 0.45 * uPressed));
            float2 offset = normal * edgeBend + domeBend;

            // Chromatic dispersion around curved edges
            float disp = pow(bevel, 1.45) * uDispersion * 5.0;
            float2 sampleR = clamp(fragCoord + offset + normal * disp, float2(0.0), uSize);
            float2 sampleG = clamp(fragCoord + offset, float2(0.0), uSize);
            float2 sampleB = clamp(fragCoord + offset - normal * disp, float2(0.0), uSize);

            float r = content.eval(sampleR).r;
            float g = content.eval(sampleG).g;
            float b = content.eval(sampleB).b;
            float3 refracted = float3(r, g, b);

            // Subtle internal reflection & Fresnel specular rim lighting
            float topLeftLight = clamp((-normal.x * 0.65 - normal.y * 0.76) * 0.5 + 0.5, 0.0, 1.0);
            float bottomRightLight = clamp((normal.x * 0.55 + normal.y * 0.65) * 0.5 + 0.25, 0.0, 1.0);

            float outerRim = smoothstep(2.6, 0.0, abs(distInside - 1.1));
            float specularRim = outerRim * (0.18 + 0.52 * pow(topLeftLight, 2.2) + 0.18 * bottomRightLight) * uHighlight;
            float internalArc = smoothstep(bevelWidth, 1.5, distInside) * pow(topLeftLight, 3.0) * 0.075 * uHighlight;

            // Touch-reactive optical sheen
            float touchDist = length(fragCoord - uTouch);
            float touchSheen = exp(-touchDist * 0.008) * (0.045 + 0.09 * uPressed) * uHighlight;

            // Preserve crystal clarity: 99% transparency -> 0.01 tint weight
            float tintAlpha = clamp(1.0 - uTransparency, 0.0, 0.92);
            float3 tinted = mix(refracted, uTint.rgb, tintAlpha);

            float3 warmSpecular = float3(1.0, 0.97, 0.92) * (specularRim + internalArc + touchSheen);
            float3 finalColor = clamp(tinted + warmSpecular, float3(0.0), float3(1.0));

            return half4(finalColor, 1.0);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShader(): RuntimeShader? = runCatching {
        RuntimeShader(AGSL_SOURCE)
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun buildRenderEffect(
        shader: RuntimeShader,
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        config: GlassMaterialConfig,
        touchX: Float,
        touchY: Float,
        pressedAmount: Float
    ): ComposeRenderEffect? {
        if (widthPx <= 2f || heightPx <= 2f) return null
        return runCatching {
            shader.setFloatUniform("uSize", widthPx, heightPx)
            shader.setFloatUniform("uCornerRadius", cornerRadiusPx)
            shader.setFloatUniform("uRefraction", config.refractionStrength.coerceIn(0f, 1.5f))
            shader.setFloatUniform("uDispersion", config.dispersionStrength.coerceIn(0f, 1.5f))
            shader.setFloatUniform("uHighlight", config.edgeHighlightStrength.coerceIn(0f, 1.5f))
            shader.setFloatUniform("uTransparency", config.transparency.coerceIn(0f, 1f))
            val tint: Color = config.tintColor
            shader.setFloatUniform("uTint", tint.red, tint.green, tint.blue, 1f)
            shader.setFloatUniform("uTouch", touchX, touchY)
            shader.setFloatUniform("uPressed", pressedAmount)

            val runtimeEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
            val combined = if (config.blurRadiusDp > 0.5f && config.transparency < 0.96f) {
                val blurPx = config.blurRadiusDp * 1.5f
                val blurEffect = RenderEffect.createBlurEffect(
                    blurPx,
                    blurPx,
                    android.graphics.Shader.TileMode.CLAMP
                )
                RenderEffect.createChainEffect(runtimeEffect, blurEffect)
            } else {
                runtimeEffect
            }
            combined.asComposeRenderEffect()
        }.getOrNull()
    }
}
