/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.screens.settings

import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sxanalys.innertube.models.WatchEndpoint
import com.sxanalys.music.BuildConfig
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.LocalPlayerConnection
import com.sxanalys.music.R
import com.sxanalys.music.playback.PlayerConnection
import com.sxanalys.music.playback.queues.YouTubeQueue
import com.sxanalys.music.ui.component.IconButton
import com.sxanalys.music.ui.component.Material3SettingsGroup
import com.sxanalys.music.ui.component.Material3SettingsItem
import com.sxanalys.music.ui.utils.backToMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

private data class Contributor(
    val name: String,
    val roleRes: Int,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
    val sponsorUrl: String? = null,
    val polygon: RoundedPolygon? = null,
    val favoriteSongVideoId: String? = null
)

private data class CommunityLink(
    val labelRes: Int,
    val iconRes: Int,
    val url: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val leadDeveloper = Contributor(
    name = "Sanz",
    roleRes = R.string.credits_lead_developer,
    githubHandle = "",
    polygon = MaterialShapes.Cookie9Sided,
    favoriteSongVideoId = null
)

private val communityLinks = listOf(
    CommunityLink(R.string.credits_view_repo, R.drawable.github, "https://github.com/sanz-dev-x/sx-analys"),
    CommunityLink(R.string.credits_license_name, R.drawable.info, "https://github.com/sanz-dev-x/sx-analys/blob/main/LICENSE")
)

private fun handleEasterEggClick(
    clickCount: Int,
    favoriteSongVideoId: String?,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    playerConnection: PlayerConnection?,
    wannaPlayStr: String,
    yeahStr: String,
    onCountUpdate: (Int) -> Unit
) {
    if (favoriteSongVideoId != null) {
        val newCount = clickCount + 1
        onCountUpdate(newCount)
        if (newCount >= 3) {
            onCountUpdate(0)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = wannaPlayStr,
                    actionLabel = yeahStr,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = favoriteSongVideoId)))
                }
            }
        }
    }
}

@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val fallback = painterResource(R.drawable.small_icon)
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.size(sizeDp.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
        )
    }
}

@Composable
private fun AnimatedDeveloperAvatar(
    sizeDp: Int,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "developerAvatar")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bob"
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes<Float> {
                durationMillis = 3200
                0f at 0
                0f at 2900
                1f at 2970
                0f at 3040
            }
        ),
        label = "blink"
    )
    val notes by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "notes"
    )

    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension
            val beat = sin(bob * 2f * PI.toFloat())
            val cx = s * 0.5f
            val cy = s * 0.47f
            val headR = s * 0.23f
            val skin = Color(0xFFF1C7A0)
            val hair = Color(0xFF211C28)
            val ink = Color(0xFF2A2230)

            translate(top = beat * s * 0.012f) {
                drawRoundRect(
                    color = primary.copy(alpha = 0.9f),
                    topLeft = Offset(cx - s * 0.30f, s * 0.74f),
                    size = Size(s * 0.60f, s * 0.50f),
                    cornerRadius = CornerRadius(s * 0.20f)
                )
                drawRoundRect(
                    color = skin,
                    topLeft = Offset(cx - s * 0.05f, cy + headR * 0.8f),
                    size = Size(s * 0.10f, s * 0.16f),
                    cornerRadius = CornerRadius(s * 0.03f)
                )
                rotate(degrees = beat * 2.5f, pivot = Offset(cx, cy + headR)) {
                    drawCircle(color = skin, radius = headR, center = Offset(cx, cy))
                    drawArc(
                        color = hair,
                        startAngle = 188f,
                        sweepAngle = 164f,
                        useCenter = true,
                        topLeft = Offset(cx - headR * 1.04f, cy - headR * 1.06f),
                        size = Size(headR * 2.08f, headR * 2.08f)
                    )
                    val eyeScale = 1f - 0.9f * blink.coerceIn(0f, 1f)
                    for (dx in listOf(-0.42f, 0.42f)) {
                        drawOval(
                            color = ink,
                            topLeft = Offset(
                                cx + headR * dx - s * 0.017f,
                                cy + headR * 0.12f - s * 0.026f * eyeScale
                            ),
                            size = Size(s * 0.034f, s * 0.052f * eyeScale)
                        )
                    }
                    drawArc(
                        color = Color(0xFF8A4A3A),
                        startAngle = 15f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(cx - s * 0.045f, cy + headR * 0.42f),
                        size = Size(s * 0.09f, s * 0.05f),
                        style = Stroke(width = s * 0.012f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - headR * 1.12f, cy - headR * 1.12f),
                        size = Size(headR * 2.24f, headR * 2.24f),
                        style = Stroke(width = s * 0.034f, cap = StrokeCap.Round)
                    )
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(cx - headR * 1.12f - s * 0.036f, cy - s * 0.035f),
                        size = Size(s * 0.072f, s * 0.13f),
                        cornerRadius = CornerRadius(s * 0.03f)
                    )
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(cx + headR * 1.12f - s * 0.036f, cy - s * 0.035f),
                        size = Size(s * 0.072f, s * 0.13f),
                        cornerRadius = CornerRadius(s * 0.03f)
                    )
                }
            }

            for (k in 0..1) {
                val p = (notes + k * 0.5f) % 1f
                val alpha = (1f - p).coerceIn(0f, 1f)
                val nx = cx + headR * 1.35f + s * 0.04f * sin(p * 2f * PI.toFloat())
                val ny = cy - headR * 0.4f - p * s * 0.30f
                drawCircle(
                    color = primary.copy(alpha = alpha),
                    radius = s * 0.022f,
                    center = Offset(nx, ny)
                )
                drawLine(
                    color = primary.copy(alpha = alpha),
                    start = Offset(nx + s * 0.02f, ny),
                    end = Offset(nx + s * 0.02f, ny - s * 0.09f),
                    strokeWidth = s * 0.01f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun DeveloperSocials(
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://tiktok.com/@sanz_crypt") },
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.tiktok),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
        }
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://www.instagram.com/snzxvs") },
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(painterResource(R.drawable.instagram), contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val wannaPlayStr = stringResource(R.string.wanna_play_favorite_song)
    val yeahStr = stringResource(R.string.yeah)
    
    val windowInsets = LocalPlayerAwareWindowInsets.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Top)
            )
        )

        Spacer(Modifier.height(16.dp))

        // App Header Section
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.small_icon),
                    contentDescription = stringResource(R.string.sxanalys),
                    colorFilter = ColorFilter.tint(
                        color = MaterialTheme.colorScheme.primary,
                        blendMode = BlendMode.SrcIn,
                    ),
                    modifier = Modifier.size(64.dp)
                )
        
                Spacer(Modifier.width(20.dp))
        
                Column {
                    val sxanalysName = stringResource(R.string.sxanalys)
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    Text(
                        text = sxanalysName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
            
                    Spacer(Modifier.height(8.dp))
            
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = BuildConfig.ARCHITECTURE.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (BuildConfig.DEBUG) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "DEBUG",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Lead Developer Hero Card
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedDeveloperAvatar(
                        sizeDp = 110,
                        shape = leadDeveloper.polygon?.toShape() ?: CircleShape
                    )

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = leadDeveloper.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = stringResource(R.string.credits_lead_developer),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                DeveloperSocials(uriHandler)
                
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Community & Info using standard Group
        Material3SettingsGroup(
            title = stringResource(R.string.community_and_info),
            items = communityLinks.map { link ->
                Material3SettingsItem(
                    icon = painterResource(link.iconRes),
                    title = { Text(stringResource(link.labelRes), fontWeight = FontWeight.SemiBold) },
                    description = if (link.labelRes == R.string.credits_license_name) {
                        { Text(stringResource(R.string.credits_license_desc)) }
                    } else null,
                    onClick = { uriHandler.openUri(link.url) }
                )
            }
        )

        Spacer(Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.stands_with_palestine),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        Spacer(Modifier.height(48.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        }
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        )
    }
}
