package com.example.poker.parser

import com.example.poker.model.PokerHand
import com.example.poker.model.TournamentData
import java.util.UUID

object TournamentParser {

    fun parseTournament(content: String, fileName: String): TournamentData {
        val trimmed = content.trim()
        val isXml = trimmed.startsWith("<?xml") || trimmed.startsWith("<game") || trimmed.contains("<session")

        val hands: List<PokerHand> = if (isXml) {
            IPokerParser.parseTournamentHands(content)
        } else {
            GGPokerParser.parseTournamentHands(content)
        }

        val tournamentName = if (hands.isNotEmpty()) {
            hands.first().tournamentName.ifEmpty { fileName.substringBeforeLast(".") }
        } else {
            fileName.substringBeforeLast(".")
        }

        val format = if (isXml) "iPoker / RedStar" else "GGPoker"

        return TournamentData(
            id = UUID.randomUUID().toString(),
            name = tournamentName,
            hands = hands,
            sourceFormat = format
        )
    }
}
