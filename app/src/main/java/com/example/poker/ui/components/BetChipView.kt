package com.example.poker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws an authentic casino poker chip with edge dashes and inner ring,
 * matching the user's reference image.
 */
@Composable
fun CasinoChip(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    label: String? = null
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // 1. Outer main color circle
        drawCircle(
            color = color,
            radius = radius,
            center = center
        )

        // 2. White outer border
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.2f)
        )

        // 3. Casino rim edge stripes (6 stripes around the perimeter)
        val numStripes = 6
        for (i in 0 until numStripes) {
            val angle = (i * 2.0 * Math.PI / numStripes).toFloat()
            val startDist = radius * 0.72f
            val endDist = radius
            val start = Offset(
                center.x + startDist * cos(angle),
                center.y + startDist * sin(angle)
            )
            val end = Offset(
                center.x + endDist * cos(angle),
                center.y + endDist * sin(angle)
            )
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = 2.2f
            )
        }

        // 4. Inner groove ring
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = radius * 0.55f,
            center = center,
            style = Stroke(width = 1.2f)
        )

        // 5. Center circle
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = radius * 0.38f,
            center = center
        )
    }
}

/**
 * Displays two overlapping chips (e.g. 100 black + 25 green, or purple + gold)
 */
@Composable
fun ChipsStack(
    primaryColor: Color = Color(0xFF1E293B), // Dark/black chip
    secondaryColor: Color = Color(0xFF15803D), // Green chip
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        CasinoChip(
            color = primaryColor,
            size = 13.dp
        )
        Box(modifier = Modifier.padding(start = 7.dp)) {
            CasinoChip(
                color = secondaryColor,
                size = 13.dp
            )
        }
    }
}

/**
 * Component showing the player's current bet amount placed on the table felt next to them,
 * matching the user reference photo: e.g. "0.5 [chip][chip]" or "[chip] 2".
 */
@Composable
fun PlayerBetChipsView(
    betText: String,
    chipsOnLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090D16).copy(alpha = 0.88f))
            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        if (chipsOnLeft) {
            ChipsStack()
            Text(
                text = betText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Text(
                text = betText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            ChipsStack()
        }
    }
}

/**
 * Formats a bet amount in BB (e.g., "0.5", "1", "2", "18.4") or in chip values.
 */
fun formatBetNumber(chips: Long, bb: Long, useBbUnits: Boolean): String {
    if (!useBbUnits || bb <= 0L) {
        return if (chips >= 1_000_000) {
            String.format(java.util.Locale.US, "%.1fM", chips / 1_000_000.0)
        } else if (chips >= 10_000) {
            String.format(java.util.Locale.US, "%.1fk", chips / 1_000.0)
        } else {
            String.format(java.util.Locale.US, "%,d", chips)
        }
    }
    val bbVal = chips.toDouble() / bb.toDouble()
    return if (bbVal == bbVal.toLong().toDouble()) {
        "${bbVal.toLong()}"
    } else if (bbVal < 1.0) {
        val s = String.format(java.util.Locale.US, "%.1f", bbVal)
        if (s == "0.0") String.format(java.util.Locale.US, "%.2f", bbVal) else s
    } else {
        String.format(java.util.Locale.US, "%.1f", bbVal)
    }
}
