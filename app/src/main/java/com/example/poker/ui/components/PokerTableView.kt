package com.example.poker.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.HandStepState
import com.example.poker.model.PlayerReplayState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PokerTableView(
    stepState: HandStepState,
    bigBlindChips: Long,
    useBbUnits: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(400.dp)
) {
    // Reorder players so Hero is always at index 0 (bottom center)
    val rawPlayers = stepState.players
    val heroIdx = rawPlayers.indexOfFirst { it.isHero }
    val orderedPlayers = if (heroIdx >= 0) {
        rawPlayers.subList(heroIdx, rawPlayers.size) + rawPlayers.subList(0, heroIdx)
    } else {
        rawPlayers
    }

    val numPlayers = orderedPlayers.size.coerceAtLeast(1)

    val verticalShiftDp = 24.dp

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        val tableWidth = maxWidth
        val tableHeight = maxHeight

        // Outer Wood/Leather Table Rail
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -verticalShiftDp)
                .width(tableWidth * 0.94f)
                .height(tableHeight * 0.72f)
                .shadow(12.dp, RoundedCornerShape(120.dp))
                .clip(RoundedCornerShape(120.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2E1C0C), Color(0xFF170D06)),
                        radius = 450f
                    )
                )
                .border(3.5.dp, Color(0xFFD4AF37).copy(alpha = 0.65f), RoundedCornerShape(120.dp))
                .padding(7.dp)
        ) {
            // Felt surface (Dark Casino Forest Green)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(110.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF135338), // Lighter center
                                Color(0xFF0D3826),
                                Color(0xFF072116)  // Dark perimeter
                            )
                        )
                    )
                    .border(1.2.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(110.dp))
            ) {
                // Table center: Pot & Board cards
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pot: simple number in yellow color, without "BB" word
                    val potText = if (useBbUnits) {
                        if (bigBlindChips > 0L) {
                            val bbVal = stepState.currentPotChips.toDouble() / bigBlindChips.toDouble()
                            if (bbVal == bbVal.toLong().toDouble()) {
                                "${bbVal.toLong()}"
                            } else {
                                String.format(java.util.Locale.US, "%.1f", bbVal)
                            }
                        } else {
                            "${stepState.currentPotChips}"
                        }
                    } else {
                        String.format(java.util.Locale.US, "%,d", stepState.currentPotChips)
                    }

                    Text(
                        text = potText,
                        color = Color(0xFFFDE047),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Board cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val board = stepState.boardCards
                        for (i in 0 until 5) {
                            if (i < board.size) {
                                PlayingCardView(
                                    card = board[i],
                                    width = 28.dp,
                                    height = 38.dp,
                                    elevation = 3.dp
                                )
                            } else {
                                // Empty card slot
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF061A11).copy(alpha = 0.6f))
                                        .border(
                                            0.8.dp,
                                            Color(0xFF10B981).copy(alpha = 0.25f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Winner announcement banner positioned below the board without shifting board cards
                if (stepState.winnerNotice != null) {
                    val noticeText = stepState.winnerNotice
                    val cleanText = noticeText.removePrefix("🏆").trim()
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 52.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF854D0E).copy(alpha = 0.94f))
                            .border(0.9.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 8.5.sp,
                            lineHeight = 9.sp
                        )
                        Text(
                            text = cleanText,
                            color = Color(0xFFFEF08A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Position each player around the ellipse
        // Hero is at index 0 (bottom center)
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val shiftYPx = with(LocalDensity.current) { verticalShiftDp.toPx() }
        val centerX = widthPx / 2f
        val centerY = (heightPx / 2f) - shiftYPx
        val rx = (widthPx * 0.38f).coerceAtLeast(100f)
        val ry = (heightPx * 0.38f).coerceAtLeast(100f)

        for (i in 0 until numPlayers) {
            val player = orderedPlayers[i]
            val angleRad = (2.0 * Math.PI * i / numPlayers).toFloat()

            // i=0 is Hero at bottom center (x = centerX, y = centerY + ry)
            // As i increases: moves clockwise around the table
            val xPos = centerX - rx * sin(angleRad)
            val yPos = centerY + ry * cos(angleRad)

            val seatHalfWidth = if (player.isHero) 30.dp else 26.dp
            val seatHalfHeight = if (player.isHero) 22.dp else 19.dp

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (xPos - seatHalfWidth.toPx()).roundToInt(),
                            y = (yPos - seatHalfHeight.toPx()).roundToInt()
                        )
                    }
                    .wrapContentSize()
            ) {
                PlayerSeatView(
                    player = player,
                    bigBlindChips = bigBlindChips,
                    useBbUnits = useBbUnits
                )
            }

            // Player's Bet Chips displayed on the felt next to the player towards table center
            if (!player.isFolded && player.currentStreetBetChips > 0L) {
                val rxBet = rx * 0.58f
                val ryBet = ry * 0.54f
                val xBet = centerX - rxBet * sin(angleRad)
                val yBet = centerY + ryBet * cos(angleRad)

                val betText = formatBetNumber(
                    chips = player.currentStreetBetChips,
                    bb = bigBlindChips,
                    useBbUnits = useBbUnits
                )

                val chipsOnLeft = sin(angleRad) > 0.05f

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (xBet - 24.dp.toPx()).roundToInt(),
                                y = (yBet - 10.dp.toPx()).roundToInt()
                            )
                        }
                        .wrapContentSize()
                ) {
                    PlayerBetChipsView(
                        betText = betText,
                        chipsOnLeft = chipsOnLeft
                    )
                }
            }
        }
    }
}
