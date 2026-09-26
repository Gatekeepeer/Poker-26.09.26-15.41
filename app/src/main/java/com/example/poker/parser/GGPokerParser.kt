package com.example.poker.parser

import com.example.poker.model.ActionType
import com.example.poker.model.Card
import com.example.poker.model.HandStep
import com.example.poker.model.PlayerAction
import com.example.poker.model.PlayerState
import com.example.poker.model.PokerHand
import com.example.poker.model.Position
import com.example.poker.model.Street

object GGPokerParser {

    fun parseTournamentHands(text: String): List<PokerHand> {
        val rawHands = splitIntoHands(text)
        return rawHands.mapNotNull { parseSingleHand(it) }
    }

    private fun splitIntoHands(text: String): List<String> {
        val lines = text.lines()
        val handBlocks = mutableListOf<String>()
        val currentBlock = StringBuilder()

        for (line in lines) {
            if (line.startsWith("Poker Hand #") || line.startsWith("GG Poker Hand #")) {
                if (currentBlock.isNotBlank()) {
                    handBlocks.add(currentBlock.toString())
                    currentBlock.clear()
                }
            }
            currentBlock.appendLine(line)
        }
        if (currentBlock.isNotBlank()) {
            handBlocks.add(currentBlock.toString())
        }
        return handBlocks
    }

    fun parseSingleHand(handText: String): PokerHand? {
        try {
            val lines = handText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return null

            var handId = ""
            var tournamentName = "GGPoker Tournament"
            var tableName = "Table 1"
            var levelText = ""
            var sb = 0.0
            var bb = 0.0
            var ante = 0.0
            var buttonSeat = 1
            var heroName: String? = null

            val playersMap = mutableMapOf<Int, PlayerState>()
            val holeCardsMap = mutableMapOf<String, List<Card>>()
            val parsedActions = mutableListOf<Pair<Street, PlayerAction>>()
            val streetCards = mutableMapOf<Street, List<Card>>()
            val winners = mutableListOf<Pair<String, Double>>()

            var currentStreet = Street.PREFLOP

            val headerRegex = Regex("""Poker Hand #([A-Za-z0-9]+):\s+Tournament #\d+,\s+(.+?)\s+-\s+Level\s+([^(]+)\(([^)]+)\)""")
            val blindsRegex = Regex("""(\d+(?:,\d+)*(?:\.\d+)?)/(\d+(?:,\d+)*(?:\.\d+)?)""")
            val anteRegex = Regex("""ante\s+(\d+(?:,\d+)*(?:\.\d+)?)""")
            val seatRegex = Regex("""Seat (\d+):\s+(.+?)\s+\((\d+(?:,\d+)*(?:\.\d+)?)\s+in chips(?:,\s+[$€₽]?(\d+(?:,\d+)*(?:\.\d+)?)\s+bounty)?\)""")
            val buttonRegex = Regex("""Seat #(\d+) is the button""")
            val heroRegex = Regex("""Dealt to (.+?)\s+\[([^\]]+)\]""")
            val cardRegex = Regex("""\[([^\]]+)\]""")

            for (line in lines) {
                when {
                    line.startsWith("Poker Hand #") || line.startsWith("GG Poker Hand #") -> {
                        val match = headerRegex.find(line)
                        if (match != null) {
                            handId = match.groupValues[1]
                            tournamentName = match.groupValues[2]
                            levelText = "Level " + match.groupValues[3].trim()
                            val blindsPart = match.groupValues[4]
                            blindsRegex.find(blindsPart)?.let { bMatch ->
                                sb = parseNumber(bMatch.groupValues[1])
                                bb = parseNumber(bMatch.groupValues[2])
                            }
                            anteRegex.find(blindsPart)?.let { aMatch ->
                                ante = parseNumber(aMatch.groupValues[1])
                            }
                        } else {
                            handId = line.substringAfter("#").substringBefore(":").trim()
                        }
                    }
                    line.startsWith("Table '") -> {
                        tableName = line.substringAfter("Table '").substringBefore("'")
                        buttonRegex.find(line)?.let {
                            buttonSeat = it.groupValues[1].toIntOrNull() ?: 1
                        }
                    }
                    line.startsWith("Seat ") && line.contains("in chips") -> {
                        seatRegex.find(line)?.let { sm ->
                            val sNum = sm.groupValues[1].toInt()
                            val pName = sm.groupValues[2].trim()
                            val chips = parseNumber(sm.groupValues[3])
                            val bounty = if (sm.groupValues[4].isNotEmpty()) parseNumber(sm.groupValues[4]) else 0.0
                            playersMap[sNum] = PlayerState(
                                name = pName,
                                seatNumber = sNum,
                                startingChips = chips,
                                currentChips = chips,
                                bounty = bounty
                            )
                        }
                    }
                    line.startsWith("Dealt to ") -> {
                        heroRegex.find(line)?.let { hm ->
                            val hName = hm.groupValues[1].trim()
                            heroName = hName
                            val cardsStr = hm.groupValues[2].trim()
                            val cards = cardsStr.split(" ").mapNotNull { Card.parse(it) }
                            holeCardsMap[hName] = cards
                        }
                    }
                    line.startsWith("*** FLOP ***") -> {
                        currentStreet = Street.FLOP
                        cardRegex.find(line)?.let { cm ->
                            val flopCards = cm.groupValues[1].split(" ").mapNotNull { Card.parse(it) }
                            streetCards[Street.FLOP] = flopCards
                        }
                    }
                    line.startsWith("*** TURN ***") -> {
                        currentStreet = Street.TURN
                        val cards = extractCardsFromLine(line)
                        streetCards[Street.TURN] = cards
                    }
                    line.startsWith("*** RIVER ***") -> {
                        currentStreet = Street.RIVER
                        val cards = extractCardsFromLine(line)
                        streetCards[Street.RIVER] = cards
                    }
                    line.startsWith("*** SHOWDOWN ***") || line.startsWith("*** SHOW DOWN ***") -> {
                        currentStreet = Street.SHOWDOWN
                    }
                    line.startsWith("*** SUMMARY ***") -> {
                        currentStreet = Street.SUMMARY
                    }
                    line.contains("posts the ante") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        parsedActions.add(Street.PREFLOP to PlayerAction(pName, ActionType.POST_ANTE, amt, street = Street.PREFLOP))
                    }
                    line.contains("posts small blind") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        parsedActions.add(Street.PREFLOP to PlayerAction(pName, ActionType.POST_SB, amt, street = Street.PREFLOP))
                    }
                    line.contains("posts big blind") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        parsedActions.add(Street.PREFLOP to PlayerAction(pName, ActionType.POST_BB, amt, street = Street.PREFLOP))
                    }
                    line.contains(": folds") -> {
                        val pName = line.substringBefore(":")
                        parsedActions.add(currentStreet to PlayerAction(pName, ActionType.FOLD, street = currentStreet))
                    }
                    line.contains(": checks") -> {
                        val pName = line.substringBefore(":")
                        parsedActions.add(currentStreet to PlayerAction(pName, ActionType.CHECK, street = currentStreet))
                    }
                    line.contains(": calls") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        val isAllIn = line.contains("all-in")
                        parsedActions.add(currentStreet to PlayerAction(pName, if (isAllIn) ActionType.ALL_IN else ActionType.CALL, amt, isAllIn = isAllIn, street = currentStreet))
                    }
                    line.contains(": bets") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        val isAllIn = line.contains("all-in")
                        parsedActions.add(currentStreet to PlayerAction(pName, if (isAllIn) ActionType.ALL_IN else ActionType.BET, amt, isAllIn = isAllIn, street = currentStreet))
                    }
                    line.contains(": raises") -> {
                        val pName = line.substringBefore(":")
                        val amt = parseAmountFromLine(line)
                        val toAmt = parseToAmountFromLine(line)
                        val isAllIn = line.contains("all-in")
                        parsedActions.add(currentStreet to PlayerAction(pName, if (isAllIn) ActionType.ALL_IN else ActionType.RAISE, amt, toAmount = toAmt, isAllIn = isAllIn, street = currentStreet))
                    }
                    line.contains("collected") && (line.contains("from pot") || line.contains("from main pot") || line.contains("from side pot")) -> {
                        val pName = line.substringBefore(" collected")
                        val amt = parseAmountFromLine(line)
                        winners.add(pName to amt)
                    }
                    line.contains("shows [") -> {
                        val pName = line.substringBefore(":")
                        cardRegex.find(line)?.let { cm ->
                            val cards = cm.groupValues[1].split(" ").mapNotNull { Card.parse(it) }
                            holeCardsMap[pName] = cards
                        }
                    }
                }
            }

            if (playersMap.isEmpty()) return null
            if (bb == 0.0) bb = 1.0

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
                    isButton = p.seatNumber == buttonSeat,
                    isHero = p.name == heroName,
                    holeCards = holeCardsMap[p.name] ?: emptyList()
                )
            }

            // Build Simulation Steps
            val allCommunityCards = mutableListOf<Card>()
            streetCards[Street.FLOP]?.let { allCommunityCards.addAll(it) }
            streetCards[Street.TURN]?.let { allCommunityCards.addAll(it.takeLast(1)) }
            streetCards[Street.RIVER]?.let { allCommunityCards.addAll(it.takeLast(1)) }

            val steps = buildSteps(
                initialPlayers = initializedPlayers,
                actions = parsedActions,
                streetCards = streetCards,
                winners = winners,
                ante = ante,
                sb = sb,
                bb = bb
            )

            val totalPot = steps.lastOrNull()?.currentTotalPot ?: 0.0

            return PokerHand(
                handId = handId.ifEmpty { "1" },
                tournamentName = tournamentName,
                tableName = tableName,
                levelText = levelText.ifEmpty { "Level 1" },
                smallBlind = sb,
                bigBlind = bb,
                ante = ante,
                heroName = heroName,
                buttonSeat = buttonSeat,
                initialPlayers = initializedPlayers,
                communityCards = allCommunityCards,
                steps = steps,
                winners = winners,
                totalPot = totalPot,
                rawText = handText
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun computePosition(relPos: Int, totalPlayers: Int): Position {
        if (totalPlayers == 2) {
            return if (relPos == 0) Position.BTN else Position.BB
        }
        return when (relPos) {
            0 -> Position.BTN
            1 -> Position.SB
            2 -> Position.BB
            3 -> if (totalPlayers <= 6) Position.UTG else Position.UTG
            4 -> if (totalPlayers <= 6) Position.MP1 else Position.UTG1
            5 -> if (totalPlayers <= 6) Position.CO else Position.MP1
            6 -> if (totalPlayers <= 7) Position.CO else Position.MP2
            7 -> Position.HJ
            8 -> Position.CO
            else -> Position.MP1
        }
    }

    private fun buildSteps(
        initialPlayers: List<PlayerState>,
        actions: List<Pair<Street, PlayerAction>>,
        streetCards: Map<Street, List<Card>>,
        winners: List<Pair<String, Double>>,
        ante: Double,
        sb: Double,
        bb: Double
    ): List<HandStep> {
        val steps = mutableListOf<HandStep>()
        val currentPlayers = initialPlayers.associateBy { it.name }.toMutableMap()
        var currentPot = 0.0
        val currentBoard = mutableListOf<Card>()
        var stepIndex = 0

        // Step 0: Initial state before actions
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

        var lastStreet = Street.PREFLOP

        for ((street, action) in actions) {
            // Check street transition
            if (street != lastStreet) {
                // Collect bets to main pot at street end
                for ((pName, pState) in currentPlayers) {
                    if (pState.currentBet > 0.0) {
                        currentPlayers[pName] = pState.copy(currentBet = 0.0)
                    }
                }
                if (street == Street.FLOP) {
                    streetCards[Street.FLOP]?.let { currentBoard.addAll(it) }
                } else if (street == Street.TURN) {
                    streetCards[Street.TURN]?.let { if (it.isNotEmpty()) currentBoard.add(it.last()) }
                } else if (street == Street.RIVER) {
                    streetCards[Street.RIVER]?.let { if (it.isNotEmpty()) currentBoard.add(it.last()) }
                }
                lastStreet = street
            }

            val p = currentPlayers[action.playerName]
            if (p != null) {
                var newChips = p.currentChips
                var newBet = p.currentBet
                var isFolded = p.isFolded
                var isAllIn = p.isAllIn

                when (action.actionType) {
                    ActionType.POST_ANTE -> {
                        val amount = minOf(action.amount, newChips)
                        newChips -= amount
                        currentPot += amount
                    }
                    ActionType.POST_SB -> {
                        val amount = minOf(action.amount, newChips)
                        newChips -= amount
                        newBet += amount
                        currentPot += amount
                    }
                    ActionType.POST_BB -> {
                        val amount = minOf(action.amount, newChips)
                        newChips -= amount
                        newBet += amount
                        currentPot += amount
                    }
                    ActionType.FOLD -> {
                        isFolded = true
                    }
                    ActionType.CHECK -> {}
                    ActionType.CALL -> {
                        val amount = minOf(action.amount, newChips)
                        newChips -= amount
                        newBet += amount
                        currentPot += amount
                        if (action.isAllIn || newChips <= 0.0) isAllIn = true
                    }
                    ActionType.BET -> {
                        val amount = minOf(action.amount, newChips)
                        newChips -= amount
                        newBet += amount
                        currentPot += amount
                        if (action.isAllIn || newChips <= 0.0) isAllIn = true
                    }
                    ActionType.RAISE -> {
                        val additional = if (action.toAmount > 0) action.toAmount - newBet else action.amount
                        val amount = minOf(maxOf(additional, action.amount), newChips)
                        newChips -= amount
                        newBet = if (action.toAmount > 0) action.toAmount else newBet + amount
                        currentPot += amount
                        if (action.isAllIn || newChips <= 0.0) isAllIn = true
                    }
                    ActionType.ALL_IN -> {
                        val amount = newChips
                        newChips = 0.0
                        newBet += amount
                        currentPot += amount
                        isAllIn = true
                    }
                    else -> {}
                }

                val updatedPlayer = p.copy(
                    currentChips = maxOf(0.0, newChips),
                    currentBet = newBet,
                    isFolded = isFolded,
                    isAllIn = isAllIn,
                    lastAction = action
                )
                currentPlayers[action.playerName] = updatedPlayer

                val actionDesc = formatActionDescription(action, updatedPlayer)
                steps.add(
                    HandStep(
                        stepIndex = stepIndex++,
                        street = street,
                        description = actionDesc,
                        communityCards = currentBoard.toList(),
                        mainPot = currentPot,
                        currentTotalPot = currentPot,
                        players = currentPlayers.toMap(),
                        activePlayerName = action.playerName,
                        lastAction = action
                    )
                )
            }
        }

        // Final Winner Step
        if (winners.isNotEmpty()) {
            for ((wName, wAmt) in winners) {
                val p = currentPlayers[wName]
                if (p != null) {
                    currentPlayers[wName] = p.copy(
                        currentChips = p.currentChips + wAmt,
                        wonAmount = wAmt,
                        currentBet = 0.0
                    )
                }
            }
            val winNames = winners.joinToString(", ") { "${it.first} (+${formatChips(it.second)})" }
            steps.add(
                HandStep(
                    stepIndex = stepIndex++,
                    street = Street.SUMMARY,
                    description = "Победитель: $winNames",
                    communityCards = currentBoard.toList(),
                    mainPot = currentPot,
                    currentTotalPot = currentPot,
                    players = currentPlayers.toMap(),
                    activePlayerName = winners.firstOrNull()?.first
                )
            )
        }

        return steps
    }

    private fun formatActionDescription(action: PlayerAction, player: PlayerState): String {
        val posStr = player.position?.displayName?.let { " ($it)" } ?: ""
        return when (action.actionType) {
            ActionType.POST_ANTE -> "${action.playerName}$posStr ставит анте ${formatChips(action.amount)}"
            ActionType.POST_SB -> "${action.playerName}$posStr ставит малый блайнд ${formatChips(action.amount)}"
            ActionType.POST_BB -> "${action.playerName}$posStr ставит большой блайнд ${formatChips(action.amount)}"
            ActionType.FOLD -> "${action.playerName}$posStr сбрасывает (Fold)"
            ActionType.CHECK -> "${action.playerName}$posStr чекает (Check)"
            ActionType.CALL -> "${action.playerName}$posStr коллирует ${formatChips(action.amount)}${if (action.isAllIn) " [All-in]" else ""}"
            ActionType.BET -> "${action.playerName}$posStr ставит ${formatChips(action.amount)}${if (action.isAllIn) " [All-in]" else ""}"
            ActionType.RAISE -> "${action.playerName}$posStr повышает до ${formatChips(if (action.toAmount > 0) action.toAmount else action.amount)}${if (action.isAllIn) " [All-in]" else ""}"
            ActionType.ALL_IN -> "${action.playerName}$posStr идёт ва-банк (All-in) ${formatChips(action.amount)}"
            else -> "${action.playerName}: ${action.actionType.displayName}"
        }
    }

    private fun extractCardsFromLine(line: String): List<Card> {
        val bracketMatches = Regex("""\[([^\]]+)\]""").findAll(line)
        val allCards = mutableListOf<Card>()
        for (m in bracketMatches) {
            val parts = m.groupValues[1].split(" ")
            parts.forEach { cStr -> Card.parse(cStr)?.let { allCards.add(it) } }
        }
        return allCards
    }

    private fun parseNumber(str: String): Double {
        val clean = str.replace(",", "").replace("$", "").replace("€", "").replace("₽", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun parseAmountFromLine(line: String): Double {
        val match = Regex("""(\d+(?:,\d+)*(?:\.\d+)?)""").findAll(line)
        return match.lastOrNull()?.let { parseNumber(it.groupValues[1]) } ?: 0.0
    }

    private fun parseToAmountFromLine(line: String): Double {
        val match = Regex("""to\s+(\d+(?:,\d+)*(?:\.\d+)?)""").find(line)
        return match?.let { parseNumber(it.groupValues[1]) } ?: 0.0
    }

    private fun formatChips(amount: Double): String {
        return if (amount % 1.0 == 0.0) amount.toLong().toString() else "%.1f".format(amount)
    }
}
