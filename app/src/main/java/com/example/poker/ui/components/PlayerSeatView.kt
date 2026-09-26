package com.example.poker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.PlayerReplayState
import com.example.poker.model.PositionRole

@Composable
fun PlayerSeatView(
    player: PlayerReplayState,
    bigBlindChips: Long,
    useBbUnits: Boolean,
    modifier: Modifier = Modifier
) {
    val isFolded = player.isFolded
    val isActing = player.isActingNow
    val isWinner = player.isWinner
    val isHero = player.isHero

    // Animated glow for active player
    val infiniteTransition = rememberInfiniteTransition(label = "acting_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val borderColor = when {
        isWinner -> Color(0xFFFFD700)
        isActing -> Color(0xFFFFC107).copy(alpha = pulseAlpha)
        isHero -> Color(0xFF38BDF8)
        isFolded -> Color(0xFF334155)
        else -> Color(0xFF475569)
    }

    val borderWidth = when {
        isActing || isWinner -> 2.dp
        isHero -> 1.5.dp
        else -> 1.dp
    }

    val cardWidth = if (isHero) 29.dp else 25.dp
    val cardHeight = if (isHero) 40.dp else 34.dp
    val seatBoxWidth = if (isHero) 60.dp else 52.dp

    Column(
        modifier = modifier
            .alpha(if (isFolded) 0.45f else 1f)
            .testTag("player_seat_${player.seatNumber}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player's Cards (spaced with 2.dp so they never overlap)
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cards = player.cards
            if (player.isCardsVisible && cards != null && cards.isNotEmpty()) {
                for (card in cards.take(2)) {
                    PlayingCardView(
                        card = card,
                        width = cardWidth,
                        height = cardHeight
                    )
                }
            } else if (!isFolded) {
                // Grey card backs
                CardBackView(width = cardWidth, height = cardHeight)
                CardBackView(width = cardWidth, height = cardHeight)
            } else {
                // Folded: dimmed card backs
                CardBackView(
                    modifier = Modifier.alpha(0.35f),
                    width = cardWidth,
                    height = cardHeight
                )
                CardBackView(
                    modifier = Modifier.alpha(0.35f),
                    width = cardWidth,
                    height = cardHeight
                )
            }
        }

        Spacer(modifier = Modifier.height(1.5.dp))

        // Compact Player Info Box (+30% scaled with snug proportions)
        Box(
            modifier = Modifier
                .shadow(if (isActing || isHero) 3.dp else 1.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isWinner -> Brush.verticalGradient(listOf(Color(0xFF5A4408), Color(0xFF2E2203)))
                        isHero -> Brush.verticalGradient(listOf(Color(0xFF0F2B48), Color(0xFF0A1B2E)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827)))
                    }
                )
                .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 2.5.dp, vertical = 1.5.dp)
                .width(seatBoxWidth)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Line 1: Nickname (and D button if button)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isHero) "HERO" else player.playerName,
                        color = if (isHero) Color(0xFF38BDF8) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )

                    if (player.isButton) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(0.5.dp, Color(0xFFDC2626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "D",
                                color = Color(0xFFDC2626),
                                style = TextStyle(
                                    fontSize = 6.5.sp,
                                    lineHeight = 6.5.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                }

                // Line 2: Position + Numeric Stack (e.g. "SB 22.01", NO "BB" word!)
                val stackText = if (useBbUnits) {
                    if (bigBlindChips > 0L) {
                        val bbVal = player.currentChips.toDouble() / bigBlindChips.toDouble()
                        String.format(java.util.Locale.US, "%.2f", bbVal)
                    } else {
                        "${player.currentChips}"
                    }
                } else {
                    String.format(java.util.Locale.US, "%,d", player.currentChips)
                }

                val positionColor = when (player.position) {
                    PositionRole.BTN -> Color(0xFFF59E0B) // Amber
                    PositionRole.SB -> Color(0xFF00E5FF)  // Vibrant Cyan matching user screenshot
                    PositionRole.BB -> Color(0xFFC084FC)  // Purple
                    else -> Color(0xFF00E5FF)             // Cyan
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = player.position.shortLabel,
                        color = positionColor,
                        style = TextStyle(
                            fontSize = 9.8.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stackText,
                        color = if (isWinner) Color(0xFFFDE047) else Color(0xFFE2FBD7),
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 9.8.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // Action status indicator (WINNER, ALL-IN, CHECK, BET, RAISE, CALL) in English uppercase
        val statusText = when {
            isFolded -> null
            player.isWinner -> "WINNER"
            player.isAllIn -> "ALL-IN"
            player.lastActionText?.contains("CHECK", ignoreCase = true) == true ||
            player.lastActionText?.contains("Чек", ignoreCase = true) == true -> "CHECK"
            player.lastActionText?.contains("BET", ignoreCase = true) == true ||
            player.lastActionText?.contains("Бет", ignoreCase = true) == true -> "BET"
            player.lastActionText?.contains("RAISE", ignoreCase = true) == true ||
            player.lastActionText?.contains("Рейз", ignoreCase = true) == true -> "RAISE"
            player.lastActionText?.contains("CALL", ignoreCase = true) == true ||
            player.lastActionText?.contains("Колл", ignoreCase = true) == true -> "CALL"
            else -> null
        }

        if (statusText != null) {
            Spacer(modifier = Modifier.height(2.dp))
            val actionTextColor = when (statusText) {
                "WINNER" -> Color(0xFFFBBF24)
                "ALL-IN" -> Color(0xFFEF4444)
                "CHECK" -> Color(0xFF38BDF8)
                "BET" -> Color(0xFFF43F5E)
                "RAISE" -> Color(0xFFFB923C)
                "CALL" -> Color(0xFF22C55E)
                else -> Color(0xFF94A3B8)
            }

            Text(
                text = statusText,
                color = actionTextColor,
                style = TextStyle(
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
