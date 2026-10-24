package com.metrolist.music.ui.component

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

fun Modifier.liquidGlass(
    shape: Shape,
    fill: Color = Color.Transparent,
): Modifier = this
    .clip(shape)
    .background(fill)
    .background(
        brush = Brush.linearGradient(
            listOf(
                GlassPurple.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.08f),
                GlassAccent.copy(alpha = 0.26f),
            )
        ),
        shape = shape,
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                GlassPurple.copy(alpha = 0.15f),
                GlassAccent.copy(alpha = 0.50f),
            )
        ),
        shape = shape,
    )

@Composable
fun Modifier.glassPress(
    interactionSource: InteractionSource,
    shape: Shape = CircleShape,
    pressedScale: Float = 0.9f,
): Modifier {
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
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(GlassAccent.copy(alpha = 0.45f * g), Color.Transparent)
                    )
                )
            }
        }
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
) : Modifier.Node(), DrawModifierNode {
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
        val s = 1f - (if (large) 0.015f else 0.07f) * p
        val contentScope = this
        scale(s, s) { contentScope.drawContent() }
        val g = p.coerceIn(0f, 1f)
        if (g > 0f) {
            val origin = if (pressPosition.isSpecified) pressPosition else center
            val radius = (minOf(size.width, size.height) * 0.9f).coerceAtLeast(1f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(GlassAccent.copy(alpha = 0.30f * g), Color.Transparent),
                    center = origin,
                    radius = radius,
                )
            )
        }
    }
}
