package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poker.model.PokerHand
import com.example.poker.model.TournamentData
import com.example.poker.ui.theme.GoldAccent

@Composable
fun HandListDrawer(
    tournament: TournamentData?,
    currentHandIndex: Int,
    isFullAccess: Boolean,
    freeLimit: Int,
    onSelectHand: (Int) -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenPaywall: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hands = tournament?.hands ?: emptyList()

    Surface(
        color = Color(0xFF0F172A),
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .testTag("hand_list_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Список раздач",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color(0xFF94A3B8))
                }
            }

            Text(
                text = tournament?.name ?: "Турнир не выбран",
                color = GoldAccent,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Open File Button
            Button(
                onClick = onOpenFilePicker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("btn_load_file")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Выбрать турнир (.txt, .xml)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.75.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // List of Hands
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(hands) { index, hand ->
                    val isSelected = index == currentHandIndex
                    val isLocked = !isFullAccess && index >= freeLimit

                    HandListItem(
                        hand = hand,
                        index = index,
                        isSelected = isSelected,
                        isLocked = isLocked,
                        onClick = {
                            if (isLocked) {
                                onOpenPaywall()
                            } else {
                                onSelectHand(index)
                                onClose()
                            }
                        }
                    )
                }
            }

            if (!isFullAccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF854D0E).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPaywall() }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Разблокировать все раздачи", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandListItem(
    hand: PokerHand,
    index: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> Color(0xFF1E293B)
        isLocked -> Color(0xFF0F172A).copy(alpha = 0.5f)
        else -> Color(0xFF131D33)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = if (isSelected) BorderStroke(1.dp, GoldAccent) else BorderStroke(0.5.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Раздача #${index + 1}",
                        color = if (isSelected) GoldAccent else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = hand.levelText,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }

                val heroWin = hand.winners.firstOrNull()?.first
                Text(
                    text = "Победитель: ${heroWin ?: "—"}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Заблокировано",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
