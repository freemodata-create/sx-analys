package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import com.sxanalys.music.ui.theme.GlassMaterial
import com.sxanalys.music.ui.theme.LocalGlassAccent
import com.sxanalys.music.ui.theme.LocalGlassMaterial
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val GlassAccent = Color(0xFFFF8A4C)
val GlassPurple = Color(0xFF7C5CFA)

private class GlassSpec(
    val fill: Color,
    val sheen: Brush?,
    val border: Brush,
)

private fun glassSpec(
    material: GlassMaterial,
    accent: Color,
    surface: Color,
    ink: Color,
    base: Color,
): GlassSpec =
    when (material) {
        GlassMaterial.CLEAR ->
            GlassSpec(
                base,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f), accent.copy(alpha = 0.06f)),
                ),
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f), accent.copy(alpha = 0.30f)),
                ),
            )
        GlassMaterial.FROSTED ->
            GlassSpec(
                surface.copy(alpha = 0.82f),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))),
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))),
            )
        GlassMaterial.TINTED ->
            GlassSpec(
                base,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f))),
                Brush.linearGradient(listOf(accent.copy(alpha = 0.70f), accent.copy(alpha = 0.20f))),
            )
        GlassMaterial.METAL ->
            GlassSpec(
                base,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.14f)),
                ),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.60f), ink.copy(alpha = 0.10f))),
            )
        GlassMaterial.MATTE ->
            GlassSpec(lerp(surface, ink, 0.08f), null, SolidColor(ink.copy(alpha = 0.10f)))
        GlassMaterial.OUTLINE ->
            GlassSpec(Color.Transparent, null, SolidColor(ink.copy(alpha = 0.30f)))
        GlassMaterial.INK ->
            GlassSpec(
                Color.Black.copy(alpha = 0.62f),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)),
                SolidColor(Color.White.copy(alpha = 0.14f)),
            )
    }

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    fill: Color = Color.Transparent,
): Modifier {
    val material = LocalGlassMaterial.current
    val accent = LocalGlassAccent.current
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val spec = remember(material, accent, surface, ink, fill) { glassSpec(material, accent, surface, ink, fill) }
    val sheen = spec.sheen
    return this
        .clip(shape)
        .background(spec.fill)
        .then(if (sheen != null) Modifier.background(brush = sheen, shape = shape) else Modifier)
        .border(width = 1.dp, brush = spec.border, shape = shape)
}

@Composable
fun Modifier.glassPress(
    interactionSource: InteractionSource,
    shape: Shape = CircleShape,
    pressedScale: Float = 0.94f,
): Modifier {
    val accent = LocalGlassAccent.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "glassPressScale",
    )
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "glassPressGlow",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .drawWithContent {
            drawContent()
            val g = glow.coerceIn(0f, 1f)
            if (g > 0f) {
                drawRect(color = accent.copy(alpha = 0.18f * g))
            }
        }
}

@Composable
fun Modifier.glassChip(selected: Boolean, shape: Shape): Modifier {
    val accent = LocalGlassAccent.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill = if (selected) accent.copy(alpha = 0.20f) else onSurface.copy(alpha = 0.06f)
    val line = if (selected) accent.copy(alpha = 0.60f) else onSurface.copy(alpha = 0.16f)
    return this
        .clip(shape)
        .background(fill)
        .border(1.dp, line, shape)
}

@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(if (small) 14.dp else 20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(if (small) 40.dp else 56.dp)
            .glassPress(interactionSource, shape)
            .liquidGlass(shape, fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            content = content,
        )
    }
}

object GlassIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlassIndicationNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

private class GlassIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val progress = Animatable(0f)
    private var pressPosition: Offset = Offset.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressPosition = interaction.pressPosition
                        launch {
                            progress.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        launch {
                            progress.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = progress.value
        val large = size.width > 280.dp.toPx() || size.height > 280.dp.toPx()
        val s = 1f - (if (large) 0.012f else 0.04f) * p
        val contentScope = this
        scale(s, s) { contentScope.drawContent() }
        val g = p.coerceIn(0f, 1f)
        if (g > 0f) {
            drawRect(color = currentValueOf(LocalContentColor).copy(alpha = 0.10f * g))
        }
    }
}
