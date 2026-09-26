package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.Card
import com.example.poker.model.HandStep
import com.example.poker.model.PlayerState
import com.example.poker.model.PokerHand
import com.example.poker.ui.theme.CardBackground
import com.example.poker.ui.theme.CardBlack
import com.example.poker.ui.theme.CardRed
import com.example.poker.ui.theme.GoldAccent
import com.example.poker.ui.theme.HeroHighlight
import com.example.poker.ui.theme.PokerTableFeltDark
import com.example.poker.ui.theme.PokerTableGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PokerTableView(
    hand: PokerHand,
    step: HandStep?,
    displayInBB: Boolean,
    modifier: Modifier = Modifier
) {
    val bb = if (hand.bigBlind > 0.0) hand.bigBlind else 1.0
    val players = step?.players?.values?.toList() ?: hand.initialPlayers
    val boardCards = step?.communityCards ?: emptyList()
    val totalPot = step?.currentTotalPot ?: 0.0
    val activePlayer = step?.activePlayerName

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("poker_table_view"),
        contentAlignment = Alignment.Center
    ) {
        val tableWidth = maxWidth * 0.94f
        val tableHeight = maxHeight * 0.72f

        // Table Outer Leather Border
        Box(
            modifier = Modifier
                .size(width = tableWidth, height = tableHeight)
                .shadow(16.dp, RoundedCornerShape(percent = 50))
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF0F172A))
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Table Inner Felt (Green)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PokerTableGreen, PokerTableFeltDark)
                        )
                    )
                    .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.35f), RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center
            ) {
                // Table Center: Pot & Board Cards
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pot Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Банк: ",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatChips(totalPot, bb, displayInBB),
                                color = GoldAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Community Cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 5) {
                            if (i < boardCards.size) {
                                PokerCardView(card = boardCards[i])
                            } else {
                                EmptyCardSlot()
                            }
                        }
                    }
                }
            }
        }

        // Render Players seated in oval
        val seatCount = players.size
        val rx = (tableWidth.value / 2f) * 0.86f
        val ry = (tableHeight.value / 2f) * 0.82f

        players.forEachIndexed { index, player ->
            // Arrange positions around oval
            val angle = Math.toRadians((index.toDouble() / seatCount.toDouble() * 360.0) + 90.0)
            val posX = (rx * cos(angle)).dp
            val posY = (ry * sin(angle)).dp

            Box(
                modifier = Modifier.offset(x = posX, y = posY),
                contentAlignment = Alignment.Center
            ) {
                PlayerSeatView(
                    player = player,
                    bb = bb,
                    displayInBB = displayInBB,
                    isActive = player.name == activePlayer
                )
            }
        }
    }
}

@Composable
fun PlayerSeatView(
    player: PlayerState,
    bb: Double,
    displayInBB: Boolean,
    isActive: Boolean
) {
    val borderColor = when {
        isActive -> GoldAccent
        player.isHero -> HeroHighlight
        player.isFolded -> Color(0xFF334155)
        else -> Color(0xFF475569)
    }

    val backgroundColor = when {
        player.isFolded -> Color(0xFF0F172A).copy(alpha = 0.6f)
        player.isHero -> Color(0xFF064E3B).copy(alpha = 0.95f)
        else -> Color(0xFF1E293B).copy(alpha = 0.95f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dealer Button / Position Badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (player.isButton) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("D", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            player.position?.let { pos ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        pos.displayName,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Hole Cards if visible
        if (player.holeCards.isNotEmpty() && !player.isFolded) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                player.holeCards.forEach { card ->
                    PokerCardView(card = card, isSmall = true)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Player Info Box
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
            modifier = Modifier.width(78.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = player.name,
                    color = if (player.isFolded) Color(0xFF64748B) else Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatChips(player.currentChips, bb, displayInBB),
                    color = if (player.isFolded) Color(0xFF64748B) else GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Current Bet / Action Bubble
        if (player.currentBet > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0284C7),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    text = formatChips(player.currentBet, bb, displayInBB),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        } else if (player.isFolded) {
            Text("Fold", color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PokerCardView(
    card: Card,
    isSmall: Boolean = false
) {
    val width = if (isSmall) 22.dp else 34.dp
    val height = if (isSmall) 30.dp else 46.dp
    val rankSize = if (isSmall) 9.sp else 13.sp
    val suitSize = if (isSmall) 9.sp else 12.sp
    val textColor = if (card.suit.isRed) CardRed else CardBlack

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = CardBackground,
        border = BorderStroke(0.75.dp, Color(0xFF94A3B8)),
        shadowElevation = 2.dp,
        modifier = Modifier.size(width = width, height = height)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = card.rank.symbol,
                color = textColor,
                fontSize = rankSize,
                fontWeight = FontWeight.Bold,
                lineHeight = rankSize
            )
            Text(
                text = card.suit.symbol,
                color = textColor,
                fontSize = suitSize,
                lineHeight = suitSize
            )
        }
    }
}

@Composable
fun EmptyCardSlot() {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 46.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.4f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
    )
}

fun formatChips(chips: Double, bb: Double, displayInBB: Boolean): String {
    if (displayInBB) {
        val bbValue = chips / bb
        return "%.1f BB".format(bbValue)
    }
    return if (chips >= 1000000) {
        "%.2fM".format(chips / 1000000.0)
    } else if (chips >= 1000) {
        "%.1fK".format(chips / 1000.0)
    } else {
        if (chips % 1.0 == 0.0) chips.toLong().toString() else "%.1f".format(chips)
    }
}
