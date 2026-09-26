package com.example.poker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.Card
import com.example.poker.model.Rank
import com.example.poker.model.Suit

// Colors strictly according to user request:
// Червы - просто красные карты
// Крести - зеленые
// Пики - серые
// Бубны - синие
val CardSpadesColor = Color(0xFF52525B)   // Solid Grey (Пики)
val CardHeartsColor = Color(0xFFB91C1C)   // Solid Red (Червы)
val CardDiamondsColor = Color(0xFF0284C7) // Solid Blue (Бубны)
val CardClubsColor = Color(0xFF15803D)    // Solid Green (Крести)
val CardBackColor = Color(0xFF475569)     // Solid Grey Card Back (Рубашка карт)

fun getCardBackgroundColor(suit: Suit): Color = when (suit) {
    Suit.SPADES -> CardSpadesColor
    Suit.HEARTS -> CardHeartsColor
    Suit.DIAMONDS -> CardDiamondsColor
    Suit.CLUBS -> CardClubsColor
}

@Composable
fun PlayingCardView(
    card: Card,
    modifier: Modifier = Modifier,
    width: Dp = 26.dp,
    height: Dp = 36.dp,
    elevation: Dp = 2.dp
) {
    val bgColor = getCardBackgroundColor(card.suit)
    val cornerRadius = (width * 0.16f).coerceIn(3.dp, 6.dp)
    val rankText = card.rank.display

    val fontSize = when {
        width <= 22.dp -> if (rankText.length > 1) 10.sp else 12.sp
        width <= 28.dp -> if (rankText.length > 1) 12.sp else 15.sp
        width <= 36.dp -> if (rankText.length > 1) 15.sp else 19.sp
        width <= 44.dp -> if (rankText.length > 1) 18.sp else 22.sp
        else -> if (rankText.length > 1) 22.sp else 26.sp
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rankText,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CardBackView(
    modifier: Modifier = Modifier,
    width: Dp = 26.dp,
    height: Dp = 36.dp,
    elevation: Dp = 2.dp
) {
    val cornerRadius = (width * 0.16f).coerceIn(3.dp, 6.dp)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(CardBackColor)
            .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(cornerRadius))
    )
}
