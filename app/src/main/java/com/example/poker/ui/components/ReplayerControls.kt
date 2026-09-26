package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.HandFilter
import com.example.poker.model.HandStepState
import com.example.poker.model.PlayerReplayState
import com.example.poker.model.PokerHand
import com.example.poker.model.PositionRole

@Composable
fun ReplayerControls(
    currentHand: PokerHand,
    currentStepState: HandStepState,
    handIndex: Int,
    totalHands: Int,
    isPlaying: Boolean,
    activeFilter: HandFilter = HandFilter.ALL,
    selectedPositions: Set<PositionRole> = emptySet(),
    filteredCurrentIndex: Int = -1,
    totalFilteredHands: Int = totalHands,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onStepTo: (Int) -> Unit,
    onPrevHand: () -> Unit,
    onNextHand: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenHandList: () -> Unit,
    onClearFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current Action Description Banner (compact)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
                .testTag("action_description_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentStepState.actionDescription,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Step indicator badge (compact)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF334155))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${currentStepState.stepIndex} / ${currentStepState.totalSteps}",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Step Scrubber Slider
        if (currentStepState.totalSteps > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentStepState.stepIndex.toFloat(),
                    onValueChange = { onStepTo(it.toInt()) },
                    valueRange = 0f..currentStepState.totalSteps.toFloat(),
                    steps = (currentStepState.totalSteps - 1).coerceAtLeast(0),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("step_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF0284C7),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
                Text(
                    text = "${currentStepState.totalSteps}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val isFiltering = activeFilter != HandFilter.ALL || selectedPositions.isNotEmpty()

        val canGoPrev = if (!isFiltering) {
            handIndex > 0
        } else {
            filteredCurrentIndex > 0
        }

        val canGoNext = if (!isFiltering) {
            handIndex < totalHands - 1
        } else {
            filteredCurrentIndex != -1 && filteredCurrentIndex < totalFilteredHands - 1
        }

        // Action & Hand Navigation Controls (Full width, balanced 5-segment controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Hand (|<)
            Surface(
                onClick = onPrevHand,
                enabled = canGoPrev,
                shape = RoundedCornerShape(10.dp),
                color = if (canGoPrev) Color(0xFF334155) else Color(0xFF1E293B),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("prev_hand_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Предыдущая раздача",
                        tint = if (canGoPrev) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Step Back (<)
            Surface(
                onClick = onStepBack,
                enabled = currentStepState.canStepBack,
                shape = RoundedCornerShape(10.dp),
                color = if (currentStepState.canStepBack) Color(0xFF0284C7) else Color(0xFF1E293B),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("step_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = if (currentStepState.canStepBack) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Play / Pause auto replay
            Surface(
                onClick = onTogglePlay,
                shape = RoundedCornerShape(10.dp),
                color = if (isPlaying) Color(0xFFE11D48) else Color(0xFF16A34A),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("play_pause_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Step Forward (>)
            Surface(
                onClick = onStepForward,
                enabled = currentStepState.canStepForward,
                shape = RoundedCornerShape(10.dp),
                color = if (currentStepState.canStepForward) Color(0xFF0284C7) else Color(0xFF1E293B),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("step_forward_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Вперёд",
                        tint = if (currentStepState.canStepForward) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Next Hand (>|)
            Surface(
                onClick = onNextHand,
                enabled = canGoNext,
                shape = RoundedCornerShape(10.dp),
                color = if (canGoNext) Color(0xFF334155) else Color(0xFF1E293B),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("next_hand_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Следующая раздача",
                        tint = if (canGoNext) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hand Information Banner & Hand List Launcher (Full width, situated directly below navigation)
        Surface(
            onClick = onOpenHandList,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("hand_list_launcher_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = "Список раздач",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                val mainText = if (!isFiltering) {
                    "Раздача ${handIndex + 1} из $totalHands"
                } else {
                    val currentNum = if (filteredCurrentIndex >= 0) "${filteredCurrentIndex + 1}" else "—"
                    "Раздача $currentNum из $totalFilteredHands (#${handIndex + 1})"
                }
                Text(
                    text = mainText,
                    color = Color.White,
                    fontSize = if (!isFiltering) 15.sp else 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Active Filter indicator badge with quick clear button
        if (isFiltering) {
            Spacer(modifier = Modifier.height(4.dp))
            val filterDescription = buildString {
                if (activeFilter != HandFilter.ALL) {
                    append(activeFilter.label)
                }
                if (selectedPositions.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append("Позиции: ${selectedPositions.sortedBy { it.positionNumber }.joinToString { it.shortLabel }}")
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0369A1))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onClearFilter() }
                    .padding(horizontal = 8.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтр: $filterDescription",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Сбросить фильтр",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
