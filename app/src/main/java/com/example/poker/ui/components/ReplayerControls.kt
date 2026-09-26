package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.HandStep
import com.example.poker.model.PokerHand
import com.example.poker.model.Street
import com.example.poker.ui.theme.GoldAccent

@Composable
fun ReplayerControls(
    hand: PokerHand,
    currentStep: HandStep?,
    currentStepIndex: Int,
    isPlaying: Boolean,
    playbackSpeedMs: Long,
    onPlayPause: () -> Unit,
    onStepPrev: () -> Unit,
    onStepNext: () -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onJumpStreet: (Street) -> Unit,
    onSpeedChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSteps = hand.steps.size
    val currentStreet = currentStep?.street ?: Street.PREFLOP

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step progress & description
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentStep?.description ?: "Ожидание действия...",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${currentStepIndex + 1} / $totalSteps",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Street Jump Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val streets = listOf(Street.PREFLOP, Street.FLOP, Street.TURN, Street.RIVER, Street.SUMMARY)
            streets.forEach { st ->
                val isSelected = currentStreet == st
                val hasStreet = hand.steps.any { it.street == st }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> GoldAccent
                                hasStreet -> Color(0xFF1E293B)
                                else -> Color(0xFF0F172A).copy(alpha = 0.5f)
                            }
                        )
                        .clickable(enabled = hasStreet) { onJumpStreet(st) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = st.displayName,
                        color = if (isSelected) Color(0xFF1E1B4B) else if (hasStreet) Color.White else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onJumpStart,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_jump_start")
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "В начало", tint = Color(0xFF94A3B8))
            }

            IconButton(
                onClick = onStepPrev,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_step_prev")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Шаг назад", tint = Color.White)
            }

            // Central Play/Pause Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .clickable { onPlayPause() }
                    .testTag("btn_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                    tint = Color(0xFF1E1B4B),
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onStepNext,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_step_next")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Шаг вперёд", tint = Color.White)
            }

            IconButton(
                onClick = onJumpEnd,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_jump_end")
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "В конец", tint = Color(0xFF94A3B8))
            }
        }
    }
}
