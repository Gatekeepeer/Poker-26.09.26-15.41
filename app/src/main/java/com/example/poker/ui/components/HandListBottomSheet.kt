package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import com.example.poker.model.ActionType
import com.example.poker.model.HandFilter
import com.example.poker.model.PlayerReplayState
import com.example.poker.model.PokerHand
import com.example.poker.model.PositionRole
import com.example.poker.model.Street

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandListBottomSheet(
    sheetState: SheetState,
    hands: List<PokerHand>,
    selectedIndex: Int,
    activeFilter: HandFilter,
    selectedPositions: Set<PositionRole> = emptySet(),
    isFullAccessPurchased: Boolean = true,
    onSelectFilter: (HandFilter) -> Unit,
    onTogglePosition: (PositionRole) -> Unit,
    onClearPositions: () -> Unit,
    onSelectAllPositions: (Set<PositionRole>) -> Unit,
    onSelectHand: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isPositionsExpanded by remember { mutableStateOf(false) }

    val allPositions = remember {
        listOf(
            PositionRole.UTG2,
            PositionRole.MP1,
            PositionRole.MP2,
            PositionRole.HJ,
            PositionRole.CO,
            PositionRole.BTN,
            PositionRole.SB,
            PositionRole.BB
        )
    }

    val filteredIndexedHands = remember(hands, activeFilter, selectedPositions) {
        hands.mapIndexed { idx, hand -> Pair(idx, hand) }
            .filter { (_, hand) ->
                val matchesType = activeFilter.matches(hand)
                val matchesPos = if (selectedPositions.isEmpty()) {
                    true
                } else {
                    val heroPos = hand.players.firstOrNull { it.isHero }?.position
                    heroPos != null && heroPos in selectedPositions
                }
                matchesType && matchesPos
            }
    }

    val isFiltering = activeFilter != HandFilter.ALL || selectedPositions.isNotEmpty()
    val filterDesc = buildString {
        if (activeFilter != HandFilter.ALL) {
            append(activeFilter.label)
        }
        if (selectedPositions.isNotEmpty()) {
            if (isNotEmpty()) append(" • ")
            append("Позиции: ${selectedPositions.sortedBy { it.positionNumber }.joinToString { it.shortLabel }}")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Список всех раздач турнира",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (!isFiltering) {
                            "Всего раздач: ${hands.size} (по порядку от ранней к поздней)"
                        } else {
                            "Найдено: ${filteredIndexedHands.size} из ${hands.size} ($filterDesc)"
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filters
            val isPositionsActive = selectedPositions.isNotEmpty()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Main filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(HandFilter.ALL, HandFilter.HERO_WON, HandFilter.HERO_PLAYED).forEach { filter ->
                        val isFilterActive = activeFilter == filter
                        AssistChip(
                            onClick = { onSelectFilter(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isFilterActive) Color(0xFF0284C7) else Color(0xFF1E293B),
                                labelColor = if (isFilterActive) Color.White else Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isFilterActive) Color(0xFF38BDF8) else Color(0xFF334155)
                            )
                        )
                    }
                }

                // Row 2: "Фолд префлоп" + "Позиции" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Фолд префлоп" chip
                    val isFoldFilterActive = activeFilter == HandFilter.HERO_FOLDED_PRE
                    AssistChip(
                        onClick = { onSelectFilter(HandFilter.HERO_FOLDED_PRE) },
                        label = {
                            Text(
                                text = HandFilter.HERO_FOLDED_PRE.label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = if (isFoldFilterActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isFoldFilterActive) Color(0xFF0284C7) else Color(0xFF1E293B),
                            labelColor = if (isFoldFilterActive) Color.White else Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFoldFilterActive) Color(0xFF38BDF8) else Color(0xFF334155)
                        )
                    )

                    // "Позиции" button
                    val posLabel = if (selectedPositions.isEmpty()) {
                        if (isPositionsExpanded) "Позиции ▴" else "Позиции ▾"
                    } else {
                        val posNames = selectedPositions.sortedBy { it.positionNumber }.joinToString(",") { it.shortLabel }
                        if (isPositionsExpanded) "Позиции: $posNames ▴" else "Позиции: $posNames ▾"
                    }

                    AssistChip(
                        onClick = { isPositionsExpanded = !isPositionsExpanded },
                        label = {
                            Text(
                                text = posLabel,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = if (isPositionsActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        trailingIcon = if (selectedPositions.isNotEmpty()) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Сбросить позиции",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onClearPositions() }
                                )
                            }
                        } else null,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isPositionsActive) Color(0xFF0284C7) else if (isPositionsExpanded) Color(0xFF334155) else Color(0xFF1E293B),
                            labelColor = if (isPositionsActive || isPositionsExpanded) Color.White else Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isPositionsActive) Color(0xFF38BDF8) else if (isPositionsExpanded) Color(0xFF64748B) else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("filter_positions_button")
                    )
                }

                // Expandable Position Picker Panel
                AnimatedVisibility(visible = isPositionsExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                            .testTag("positions_selection_panel"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF131D2E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Выберите позиции Hero:",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { onSelectAllPositions(allPositions.toSet()) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Все", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (selectedPositions.isNotEmpty()) {
                                        TextButton(
                                            onClick = onClearPositions,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Сбросить", color = Color(0xFFF87171), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2 rows of 4 position buttons
                            val posRow1 = allPositions.take(4)
                            val posRow2 = allPositions.drop(4)

                            listOf(posRow1, posRow2).forEach { rowPositions ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowPositions.forEach { pos ->
                                        val isPosSelected = pos in selectedPositions
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isPosSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isPosSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { onTogglePosition(pos) }
                                                .testTag("pos_toggle_${pos.name}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pos.shortLabel,
                                                color = if (isPosSelected) Color.White else Color(0xFFCBD5E1),
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isPosSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filtered Hands list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredIndexedHands) { _, (origIdx, hand) ->
                    val isSelected = origIdx == selectedIndex
                    val heroState = hand.players.firstOrNull { it.isHero }
                    val heroWon = hand.winners.any { it.playerName == hand.heroName }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectHand(origIdx)
                                onDismiss()
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("hand_item_$origIdx"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D2E)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left column: Hand index, Table, Level, Blinds
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${origIdx + 1}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (!isFullAccessPurchased && origIdx >= 30) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFEAB308).copy(alpha = 0.2f))
                                                .border(0.8.dp, Color(0xFFEAB308), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Полный доступ",
                                                    tint = Color(0xFFFDE047),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "PRO",
                                                    color = Color(0xFFFDE047),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (heroWon) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF16A34A))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "ВЫИГРЫШ",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "Стол ${hand.tableNumber} (${hand.maxSeats}-max)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Уровень ${hand.levelNumber} • ${PlayerReplayState.formatBb(hand.smallBlind, hand.bigBlind)}/${PlayerReplayState.formatBb(hand.bigBlind, hand.bigBlind)} • Банк: ${PlayerReplayState.formatBb(hand.totalPotChips, hand.bigBlind)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${hand.timestamp} • ${hand.handId}",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Right column: Hero hole cards & position
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                if (heroState != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF334155))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = heroState.position.shortLabel,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    for (card in hand.heroCards) {
                                        PlayingCardView(
                                            card = card,
                                            width = 24.dp,
                                            height = 34.dp,
                                            elevation = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
