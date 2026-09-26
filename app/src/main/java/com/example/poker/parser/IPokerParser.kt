package com.example.poker.parser

import com.example.poker.model.ActionType
import com.example.poker.model.Card
import com.example.poker.model.HandStep
import com.example.poker.model.PlayerAction
import com.example.poker.model.PlayerState
import com.example.poker.model.PokerHand
import com.example.poker.model.Position
import com.example.poker.model.Street

object IPokerParser {

    fun parseTournamentHands(xmlText: String): List<PokerHand> {
        val hands = mutableListOf<PokerHand>()
        val gameBlocks = Regex("""<game\s+([^>]+)>([\s\S]*?)</game>""").findAll(xmlText)

        for (match in gameBlocks) {
            val gameTag = match.groupValues[1]
            val content = match.groupValues[2]
            parseSingleGame(gameTag, content)?.let { hands.add(it) }
        }
        return hands
    }

    private fun parseSingleGame(gameTag: String, content: String): PokerHand? {
        try {
            val gameCode = Regex("""gamecode="([^"]+)"""").find(gameTag)?.groupValues?.get(1) ?: "1"
            val tableName = "iPoker / RedStar Table"
            var sb = 0.0
            var bb = 0.0
            var ante = 0.0
            var buttonSeat = 1

            // Parse general info
            Regex("""<general>([\s\S]*?)</general>""").find(content)?.let { genMatch ->
                val genContent = genMatch.groupValues[1]
                Regex("""<smallblind>([^<]+)</smallblind>""").find(genContent)?.let { sb = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                Regex("""<bigblind>([^<]+)</bigblind>""").find(genContent)?.let { bb = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                Regex("""<ante>([^<]+)</ante>""").find(genContent)?.let { ante = it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                Regex("""<dealer>([^<]+)</dealer>""").find(genContent)?.let { buttonSeat = it.groupValues[1].toIntOrNull() ?: 1 }
            }

            if (bb == 0.0) bb = 1.0

            // Parse players
            val playersMap = mutableMapOf<Int, PlayerState>()
            val playerMatches = Regex("""<player\s+seat="(\d+)"\s+name="([^"]+)"\s+chips="([^"]+)"(?:\s+dealer="([^"]+)")?(?:\s+win="([^"]+)")?(?:\s+bet="([^"]+)")?""").findAll(content)

            for (pm in playerMatches) {
                val seat = pm.groupValues[1].toInt()
                val name = pm.groupValues[2]
                val chips = pm.groupValues[3].toDoubleOrNull() ?: 0.0
                playersMap[seat] = PlayerState(
                    name = name,
                    seatNumber = seat,
                    startingChips = chips,
                    currentChips = chips
                )
            }

            if (playersMap.isEmpty()) return null

            // Assign Positions
            val sortedSeats = playersMap.keys.sorted()
            val buttonIdx = sortedSeats.indexOf(buttonSeat).let { if (it >= 0) it else 0 }
            val playerCount = sortedSeats.size

            val initializedPlayers = playersMap.values.map { p ->
                val pIdx = sortedSeats.indexOf(p.seatNumber)
                val relPos = (pIdx - buttonIdx + playerCount) % playerCount
                val pos = computePosition(relPos, playerCount)
                p.copy(
                    position = pos,
                    isButton = p.seatNumber == buttonSeat
                )
            }

            // Parse rounds / actions
            val rounds = Regex("""<round\s+no="(\d+)">([\s\S]*?)</round>""").findAll(content)
            val actions = mutableListOf<Pair<Street, PlayerAction>>()
            val communityCards = mutableListOf<Card>()
            val winners = mutableListOf<Pair<String, Double>>()

            for (r in rounds) {
                val roundNo = r.groupValues[1].toInt()
                val rContent = r.groupValues[2]
                val street = when (roundNo) {
                    0 -> Street.PREFLOP
                    1 -> Street.PREFLOP
                    2 -> Street.FLOP
                    3 -> Street.TURN
                    4 -> Street.RIVER
                    else -> Street.SHOWDOWN
                }

                // Cards on board
                val cardMatches = Regex("""<cards\s+type="([^"]+)"\s+cards="([^"]+)"""").findAll(rContent)
                for (cm in cardMatches) {
                    val cardsStr = cm.groupValues[2]
                    val parsedCards = cardsStr.split(",").mapNotNull { Card.parse(it.trim()) }
                    communityCards.addAll(parsedCards)
                }

                // Actions
                val actMatches = Regex("""<action\s+no="(\d+)"\s+player="([^"]+)"\s+type="(\d+)"\s+sum="([^"]+)"""").findAll(rContent)
                for (am in actMatches) {
                    val pName = am.groupValues[2]
                    val typeCode = am.groupValues[3].toInt()
                    val sum = am.groupValues[4].toDoubleOrNull() ?: 0.0
                    val actionType = mapActionType(typeCode)
                    actions.add(street to PlayerAction(pName, actionType, sum, street = street))
                }
            }

            val steps = buildSimpleSteps(initializedPlayers, actions, communityCards, winners)

            return PokerHand(
                handId = gameCode,
                tournamentName = "iPoker / RedStar Tournament",
                tableName = tableName,
                levelText = "Blinds: $sb/$bb",
                smallBlind = sb,
                bigBlind = bb,
                ante = ante,
                heroName = null,
                buttonSeat = buttonSeat,
                initialPlayers = initializedPlayers,
                communityCards = communityCards.distinct(),
                steps = steps,
                winners = winners,
                totalPot = steps.lastOrNull()?.currentTotalPot ?: 0.0,
                rawText = content
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun mapActionType(code: Int): ActionType {
        return when (code) {
            0 -> ActionType.FOLD
            1 -> ActionType.CHECK
            2 -> ActionType.POST_SB
            3 -> ActionType.POST_BB
            4 -> ActionType.CALL
            5 -> ActionType.BET
            6 -> ActionType.RAISE
            7 -> ActionType.ALL_IN
            8 -> ActionType.POST_ANTE
            else -> ActionType.CALL
        }
    }

    private fun computePosition(relPos: Int, totalPlayers: Int): Position {
        return when (relPos) {
            0 -> Position.BTN
            1 -> Position.SB
            2 -> Position.BB
            3 -> if (totalPlayers <= 6) Position.UTG else Position.UTG
            4 -> if (totalPlayers <= 6) Position.MP1 else Position.UTG1
            5 -> if (totalPlayers <= 6) Position.CO else Position.MP1
            else -> Position.CO
        }
    }

    private fun buildSimpleSteps(
        initialPlayers: List<PlayerState>,
        actions: List<Pair<Street, PlayerAction>>,
        communityCards: List<Card>,
        winners: List<Pair<String, Double>>
    ): List<HandStep> {
        val steps = mutableListOf<HandStep>()
        val currentPlayers = initialPlayers.associateBy { it.name }.toMutableMap()
        var currentPot = 0.0
        var stepIndex = 0

        steps.add(
            HandStep(
                stepIndex = stepIndex++,
                street = Street.PREFLOP,
                description = "Начало раздачи",
                communityCards = emptyList(),
                mainPot = 0.0,
                currentTotalPot = 0.0,
                players = currentPlayers.toMap()
            )
        )

        for ((street, action) in actions) {
            val p = currentPlayers[action.playerName]
            if (p != null) {
                var newChips = p.currentChips
                var newBet = p.currentBet
                var isFolded = p.isFolded

                when (action.actionType) {
                    ActionType.FOLD -> isFolded = true
                    ActionType.CHECK -> {}
                    else -> {
                        val amt = minOf(action.amount, newChips)
                        newChips -= amt
                        newBet += amt
                        currentPot += amt
                    }
                }

                val updatedPlayer = p.copy(
                    currentChips = newChips,
                    currentBet = newBet,
                    isFolded = isFolded,
                    lastAction = action
                )
                currentPlayers[action.playerName] = updatedPlayer

                val boardForStreet = when (street) {
                    Street.PREFLOP -> emptyList()
                    Street.FLOP -> communityCards.take(3)
                    Street.TURN -> communityCards.take(4)
                    else -> communityCards.take(5)
                }

                steps.add(
                    HandStep(
                        stepIndex = stepIndex++,
                        street = street,
                        description = "${action.playerName}: ${action.actionType.displayName} ${action.amount}",
                        communityCards = boardForStreet,
                        mainPot = currentPot,
                        currentTotalPot = currentPot,
                        players = currentPlayers.toMap(),
                        activePlayerName = action.playerName,
                        lastAction = action
                    )
                )
            }
        }
        return steps
    }
}
