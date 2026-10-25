/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.sxanalys.music.ui.theme.LocalLargeNav
import com.sxanalys.music.ui.theme.LocalGlassAccent
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.sxanalys.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(32.dp)
    val glassBase = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
    val idleColor = if (pureBlack) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val glassAccent = LocalGlassAccent.current
    val large = LocalLargeNav.current

    val count = navigationItems.size.coerceAtLeast(1)
    val selectedIndex =
        navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }

    var hoverIndex by remember { mutableIntStateOf(-1) }
    var pendingIndex by remember { mutableIntStateOf(-1) }
    var dragging by remember { mutableStateOf(false) }
    var innerWidthPx by remember { mutableFloatStateOf(0f) }
    val itemWidthPx = innerWidthPx / count

    LaunchedEffect(selectedIndex) { pendingIndex = -1 }

    val shownIndex =
        when {
            hoverIndex >= 0 -> hoverIndex
            pendingIndex >= 0 -> pendingIndex
            else -> selectedIndex
        }

    val lensX = remember { Animatable(0f) }
    var lensPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(shownIndex, itemWidthPx) {
        if (shownIndex < 0 || itemWidthPx <= 0f) return@LaunchedEffect
        val target = shownIndex * itemWidthPx
        if (!lensPlaced) {
            lensX.snapTo(target)
            lensPlaced = true
        } else {
            lensX.animateTo(
                target,
                spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
            )
        }
    }
    val lensScale by animateFloatAsState(
        targetValue = if (dragging) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lensScale",
    )

    val latestItems by rememberUpdatedState(navigationItems)
    val latestSelected by rememberUpdatedState(selectedIndex)
    val latestOnClick by rememberUpdatedState(onItemClick)
    val latestSearchLong by rememberUpdatedState(onSearchLongClick)
    val latestHomeHold by rememberUpdatedState(onHomeLongHold)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (large) 16.dp else 28.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                    ).liquidGlass(shape, fill = glassBase.copy(alpha = 0.55f))
                    .padding(6.dp)
                    .onSizeChanged { innerWidthPx = it.width.toFloat() }
                    .pointerInput(count) {
                        coroutineScope {
                            val scope = this
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val w = size.width.toFloat().coerceAtLeast(1f)

                                fun indexAt(x: Float): Int = ((x / w) * count).toInt().coerceIn(0, count - 1)

                                val downIndex = indexAt(down.position.x)
                                val downItem = latestItems.getOrNull(downIndex)
                                val longAction: (() -> Unit)? =
                                    when {
                                        downItem == Screens.Search -> latestSearchLong
                                        downItem == Screens.Home -> latestHomeHold
                                        else -> null
                                    }
                                val longDelay =
                                    if (downItem == Screens.Home) 15_000L else viewConfiguration.longPressTimeoutMillis

                                var longFired = false
                                var dragged = false
                                var cancelled = false
                                var lastIndex = downIndex
                                hoverIndex = downIndex

                                val longJob =
                                    longAction?.let { action ->
                                        scope.launch {
                                            delay(longDelay)
                                            longFired = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            action()
                                        }
                                    }

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        cancelled = true
                                        break
                                    }
                                    if (change.changedToUpIgnoreConsumed()) {
                                        change.consume()
                                        break
                                    }
                                    if (!dragged && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                                        dragged = true
                                        dragging = true
                                        longJob?.cancel()
                                    }
                                    if (dragged) {
                                        val idx = indexAt(change.position.x)
                                        if (idx != lastIndex) {
                                            lastIndex = idx
                                            hoverIndex = idx
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        change.consume()
                                    }
                                }

                                longJob?.cancel()
                                dragging = false
                                val finalIndex = if (dragged) lastIndex else downIndex
                                if (!cancelled && !longFired) {
                                    pendingIndex = finalIndex
                                    latestItems.getOrNull(finalIndex)?.let { screen ->
                                        latestOnClick(screen, finalIndex == latestSelected)
                                    }
                                }
                                hoverIndex = -1
                            }
                        }
                    },
        ) {
            if (shownIndex >= 0 && itemWidthPx > 0f) {
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier =
                            Modifier
                                .offset { IntOffset(lensX.value.roundToInt(), 0) }
                                .width(with(density) { itemWidthPx.toDp() })
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                                .graphicsLayer {
                                    val stretch = 1f + (abs(lensX.velocity) / 9000f).coerceAtMost(0.16f)
                                    scaleX = lensScale * stretch
                                    scaleY = lensScale
                                }.glassButton3D(RoundedCornerShape(26.dp), glassAccent, if (large) 0.26f else 0.32f),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                navigationItems.forEachIndexed { index, screen ->
                    val active = index == shownIndex
                    val iconRes = if (active) screen.iconIdActive else screen.iconIdInactive
                    val fg by animateColorAsState(
                        targetValue = if (active) glassAccent else idleColor,
                        label = "glassItemFg",
                    )
                    val title = stringResource(screen.titleId)
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    selected = index == selectedIndex
                                    onClick(label = title) {
                                        latestOnClick(screen, index == latestSelected)
                                        true
                                    }
                                }.padding(vertical = if (slimNav) 12.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = title,
                            tint = fg,
                            modifier = Modifier.size(if (slimNav) 30.dp else if (large) 26.dp else 24.dp),
                        )
                        if (!slimNav) {
                            Text(
                                text = title,
                                color = fg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
