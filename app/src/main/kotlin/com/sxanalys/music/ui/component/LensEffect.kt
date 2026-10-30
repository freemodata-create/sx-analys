package com.sxanalys.music.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val LENS_AGSL =
    """
    uniform shader content;
    uniform float2 center;
    uniform float radius;
    uniform float mag;

    half4 main(float2 p) {
        float2 d = p - center;
        float dist = length(d);
        if (dist >= radius || mag <= 1.001) {
            return content.eval(p);
        }
        float t = dist / radius;
        float k = 1.0 - (1.0 - 1.0 / mag) * (1.0 - t * t);
        float ca = 0.02 * t * t;
        half4 base = content.eval(center + d * k);
        half r = content.eval(center + d * (k + ca)).r;
        half b = content.eval(center + d * (k - ca)).b;
        return half4(r, base.g, b, base.a);
    }
    """

@RequiresApi(33)
private class LensShaderHolder {
    val shader: RuntimeShader? = runCatching { RuntimeShader(LENS_AGSL) }.getOrNull()
}

/**
 * Membesarkan isi di sekitar titik lensa (ikon dan label), dengan tepi sedikit berwarna.
 * Aktif hanya di Android 13+; di versi lebih rendah tidak melakukan apa-apa.
 */
fun Modifier.lensMagnify(
    centerX: () -> Float,
    amount: () -> Float,
    radius: () -> Float,
): Modifier = if (Build.VERSION.SDK_INT >= 33) lensMagnifyApi33(centerX, amount, radius) else this

@RequiresApi(33)
private fun Modifier.lensMagnifyApi33(
    centerX: () -> Float,
    amount: () -> Float,
    radius: () -> Float,
): Modifier =
    composed {
        val holder = remember { LensShaderHolder() }
        graphicsLayer {
            val shader = holder.shader
            val a = amount()
            if (shader != null && a > 0.01f) {
                shader.setFloatUniform("center", centerX(), size.height / 2f)
                shader.setFloatUniform("radius", radius())
                shader.setFloatUniform("mag", 1f + 0.45f * a)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            } else {
                renderEffect = null
            }
        }
    }

/** Gelembung kaca 3D dengan bevel, kilau spekular, dan tepi pelangi tipis, dipakai saat nav bar digeser. */
fun Modifier.liquidBubble(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.02f),
                    Color.White.copy(alpha = 0.08f),
                ),
            ),
        ).glassVolume(shape, gloss = 1.2f, bevel = 1f, caustic = Color(0xFF7FE7FF))
        .border(
            1.5.dp,
            Brush.sweepGradient(
                listOf(
                    Color(0xFF7FE7FF).copy(alpha = 0.75f),
                    Color(0xFFFF7FD5).copy(alpha = 0.65f),
                    Color(0xFFFFE97F).copy(alpha = 0.65f),
                    Color(0xFF7FE7FF).copy(alpha = 0.75f),
                ),
            ),
            shape,
        ).border(
            0.75.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.10f))),
            shape,
        )
