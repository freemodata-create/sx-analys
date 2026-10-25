package com.sxanalys.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxanalys.music.LocalDatabase
import com.sxanalys.music.R
import com.sxanalys.music.utils.computeListeningStreak
import java.time.LocalDate

@Composable
fun ListeningStreakCard(modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val rawDays by database.listeningDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val today = LocalDate.now()
    val streak =
        remember(rawDays, today) {
            computeListeningStreak(rawDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }, today)
        }
    if (rawDays.isEmpty()) return

    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.streak_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = streak.current.toString(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = pluralStringResource(R.plurals.streak_days, streak.current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            streak.lastSevenDays.forEachIndexed { index, listened ->
                val isToday = index == streak.lastSevenDays.lastIndex
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(
                                when {
                                    listened -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(2.dp),
                            ),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text =
                if (streak.listenedToday || streak.current == 0) {
                    stringResource(R.string.streak_best, streak.best)
                } else {
                    stringResource(R.string.streak_keep_alive, streak.best)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
