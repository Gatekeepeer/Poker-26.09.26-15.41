package com.example.poker.parser

import com.example.poker.model.ActionType
import com.example.poker.model.Card
import com.example.poker.model.HandAction
import com.example.poker.model.HandWinner
import com.example.poker.model.PlayerInitialState
import com.example.poker.model.PokerHand
import com.example.poker.model.PositionRole
import com.example.poker.model.Rank
import com.example.poker.model.Street
import com.example.poker.model.Suit
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parser for RedStar / iPoker XML hand history files.
 * Handles single or multiple <root><general>...<game>...</game></root> XML blocks.
 */
object RedStarParser {

    fun isRedStarFormat(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("<") && (trimmed.contains("<game ") || trimmed.contains("<general>") || trimmed.contains("gamecode=")))
    }

    fun parseHandHistory(text: String): ParseResult {
        val hands = mutableListOf<PokerHand>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        // In RedStar/iPoker exports, multiple <root>...</root> blocks or <game>...</game> blocks may be concatenated
        val gameBlocks = extractGameBlocks(text)
        if (gameBlocks.isEmpty()) {
            return ParseResult(emptyList(), listOf("Не найдено блоков раздач <game>"), 0, 0)
        }

        var handIndex = 0
        for (gameData in gameBlocks) {
            try {
                val hand = parseSingleGame(gameData, handIndex)
                if (hand != null) {
                    hands.add(hand)
                    handIndex++
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                warnings.add("Ошибка парсинга раздачи ${gameData.gameCode}: ${e.message}")
                skipped++
            }
        }

        return ParseResult(
            hands = hands,
            warnings = warnings,
            totalParsedCount = hands.size,
            skippedCount = skipped
        )
    }

    private data class RawGameData(
        val gameCode: String,
        val tournamentName: String,
        val tableName: String,
        val heroNickname: String,
        val tableSize: Int,
        val gameXml: String
    )

    private fun extractGameBlocks(text: String): List<RawGameData> {
        val result = mutableListOf<RawGameData>()

        // Split text by <root> or match <root>...</root> or standalone <game>...</game>
        // Many files have: <root><general>...</general><game gamecode="...">...</game></root>
        val rootRegex = Regex("""<root>(.*?)</root>""", RegexOption.DOT_MATCHES_ALL)
        val rootMatches = rootRegex.findAll(text).toList()

        if (rootMatches.isNotEmpty()) {
            for (match in rootMatches) {
                val rootContent = match.groupValues[1]
                val heroName = extractTagValue(rootContent, "nickname")
                val tournamentName = extractTagValue(rootContent, "tournamentname").ifEmpty {
                    extractTagValue(rootContent, "tablename")
                }
                val tableName = extractTagValue(rootContent, "tablename")
                val tableSize = extractTagValue(rootContent, "tablesize").toIntOrNull() ?: 7

                val gameRegex = Regex("""<game\s+gamecode="([^"]+)">(.*?)</game>""", RegexOption.DOT_MATCHES_ALL)
                val gameMatches = gameRegex.findAll(rootContent).toList()

                for (gMatch in gameMatches) {
                    val gCode = gMatch.groupValues[1]
                    val gXml = "<game gamecode=\"$gCode\">${gMatch.groupValues[2]}</game>"
                    result.add(
                        RawGameData(
                            gameCode = gCode,
                            tournamentName = tournamentName,
                            tableName = tableName,
                            heroNickname = heroName,
                            tableSize = tableSize,
                            gameXml = gXml
                        )
                    )
                }
            }
        } else {
            // Standalone <game> blocks without <root>
            val gameRegex = Regex("""<game\s+gamecode="([^"]+)">(.*?)</game>""", RegexOption.DOT_MATCHES_ALL)
            val gameMatches = gameRegex.findAll(text).toList()
            val heroName = extractTagValue(text, "nickname")
            val tournamentName = extractTagValue(text, "tournamentname")
            val tableName = extractTagValue(text, "tablename")
            val tableSize = extractTagValue(text, "tablesize").toIntOrNull() ?: 7

            for (gMatch in gameMatches) {
                val gCode = gMatch.groupValues[1]
                val gXml = "<game gamecode=\"$gCode\">${gMatch.groupValues[2]}</game>"
                result.add(
                    RawGameData(
                        gameCode = gCode,
                        tournamentName = tournamentName,
                        tableName = tableName,
                        heroNickname = heroName,
                        tableSize = tableSize,
                        gameXml = gXml
                    )
                )
            }
        }

        return result
    }

    private data class RawPlayer(
        val seat: Int,
        val name: String,
        val chips: Long,
        val dealer: Boolean,
        val bet: Long,
        val win: Long
    )

    private fun extractTagValue(xml: String, tag: String): String {
        val regex = Regex("""<$tag>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(xml)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun parseSingleGame(data: RawGameData, handIndex: Int): PokerHand? {
        val factory = DocumentBuilderFactory.newInstance()
        // Disable external entities for safety
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (_: Exception) {}

        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(data.gameXml.toByteArray(Charsets.UTF_8)))
        val gameElem = doc.documentElement

        val gameCode = gameElem.getAttribute("gamecode").ifEmpty { data.gameCode }

        // <general> within <game>
        val gameGeneralList = gameElem.getElementsByTagName("general")
        if (gameGeneralList.length == 0) return null
        val generalElem = gameGeneralList.item(0) as Element

        val startDate = getChildText(generalElem, "startdate")
        val smallBlind = parseChipAmount(getChildText(generalElem, "smallblind"))
        val bigBlind = parseChipAmount(getChildText(generalElem, "bigblind"))
        val ante = parseChipAmount(getChildText(generalElem, "ante"))

        val rawPlayers = mutableListOf<RawPlayer>()
        val playersNodes = generalElem.getElementsByTagName("player")
        for (i in 0 until playersNodes.length) {
            val pElem = playersNodes.item(i) as Element
            val seat = pElem.getAttribute("seat").toIntOrNull() ?: (i + 1)
            val name = pElem.getAttribute("name")
            val chips = parseChipAmount(pElem.getAttribute("chips"))
            val dealer = pElem.getAttribute("dealer") == "1"
            val bet = parseChipAmount(pElem.getAttribute("bet"))
            val win = parseChipAmount(pElem.getAttribute("win"))
            rawPlayers.add(RawPlayer(seat, name, chips, dealer, bet, win))
        }

        if (rawPlayers.isEmpty()) return null

        val buttonSeat = rawPlayers.firstOrNull { it.dealer }?.seat ?: rawPlayers.first().seat
        val heroName = data.heroNickname.ifEmpty {
            // If nickname was not in root, see if a player has reg_code or guess
            rawPlayers.firstOrNull { it.name.equals("Hero", ignoreCase = true) }?.name ?: rawPlayers.first().name
        }

        // Parse rounds and actions
        val roundNodes = gameElem.getElementsByTagName("round")
        val actions = mutableListOf<HandAction>()
        var heroCards = emptyList<Card>()
        val boardFlop = mutableListOf<Card>()
        var boardTurn: Card? = null
        var boardRiver: Card? = null

        val currentBoard = mutableListOf<Card>()
        var actionCounter = 1

        val antesPosted = mutableMapOf<String, Long>()
        val blindsPosted = mutableMapOf<String, Long>()
        val revealedOpponents = mutableMapOf<String, List<Card>>()

        for (rIdx in 0 until roundNodes.length) {
            val roundElem = roundNodes.item(rIdx) as Element
            val roundNo = roundElem.getAttribute("no").toIntOrNull() ?: rIdx

            var hasActionInRound = false

            // Check for cards tags in this round
            val children = roundElem.childNodes
            for (cIdx in 0 until children.length) {
                val node = children.item(cIdx)
                if (node.nodeType != Node.ELEMENT_NODE) continue
                val elem = node as Element

                if (elem.tagName == "cards") {
                    val playerAttr = elem.getAttribute("player")
                    val typeAttr = elem.getAttribute("type")
                    val cardsText = elem.textContent.trim()
                    val parsedCards = parseRedStarCards(cardsText)

                    when {
                        typeAttr.equals("Pocket", ignoreCase = true) -> {
                            if (playerAttr.equals(heroName, ignoreCase = true) || playerAttr.equals("Hero", ignoreCase = true)) {
                                if (parsedCards.isNotEmpty()) {
                                    heroCards = parsedCards
                                }
                            } else if (parsedCards.isNotEmpty() && !cardsText.contains("X")) {
                                revealedOpponents[playerAttr] = parsedCards
                            }
                        }
                        typeAttr.equals("Flop", ignoreCase = true) -> {
                            boardFlop.clear()
                            boardFlop.addAll(parsedCards)
                            currentBoard.clear()
                            currentBoard.addAll(parsedCards)
                        }
                        typeAttr.equals("Turn", ignoreCase = true) -> {
                            boardTurn = parsedCards.firstOrNull()
                            boardTurn?.let { currentBoard.add(it) }
                        }
                        typeAttr.equals("River", ignoreCase = true) -> {
                            boardRiver = parsedCards.firstOrNull()
                            boardRiver?.let { currentBoard.add(it) }
                        }
                    }
                } else if (elem.tagName == "action") {
                    hasActionInRound = true
                    val pName = elem.getAttribute("player")
                    val sum = parseChipAmount(elem.getAttribute("sum"))
                    val typeCode = elem.getAttribute("type").toIntOrNull() ?: -1
                    val seat = rawPlayers.firstOrNull { it.name == pName }?.seat ?: 0

                    if (roundNo == 0) {
                        // Posting stage: type 15=ante, 1=SB, 2=BB
                        when (typeCode) {
                            15 -> antesPosted[pName] = (antesPosted[pName] ?: 0L) + sum
                            1 -> blindsPosted[pName] = (blindsPosted[pName] ?: 0L) + sum
                            2 -> blindsPosted[pName] = (blindsPosted[pName] ?: 0L) + sum
                            else -> {
                                if (sum == smallBlind) blindsPosted[pName] = sum
                                else if (sum == bigBlind) blindsPosted[pName] = sum
                                else if (sum == ante) antesPosted[pName] = sum
                            }
                        }
                    } else {
                        // Betting streets
                        val street = mapRoundNoToStreet(roundNo)
                        val pStartingChips = rawPlayers.firstOrNull { it.name == pName }?.chips ?: 100_000L
                        val pAnte = antesPosted[pName] ?: 0L
                        val pBlind = blindsPosted[pName] ?: 0L
                        val pRemaining = (pStartingChips - pAnte - pBlind).coerceAtLeast(0L)

                        val handAction = parseRedStarAction(
                            actionId = actionCounter++,
                            street = street,
                            playerName = pName,
                            seatNumber = seat,
                            typeCode = typeCode,
                            sum = sum,
                            currentBoard = currentBoard.toList(),
                            playerStartingChips = pRemaining
                        )
                        if (handAction != null) {
                            actions.add(handAction)
                        }
                    }
                }
            }

            // If a post-flop round had community cards dealt but NO player actions (e.g. all-in runout),
            // add a street deal event so the board card(s) and street transition are visible in the replayer.
            if (!hasActionInRound) {
                when (roundNo) {
                    2 -> {
                        if (boardFlop.isNotEmpty()) {
                            actions.add(
                                HandAction(
                                    id = actionCounter++,
                                    street = Street.FLOP,
                                    playerName = "",
                                    seatNumber = 0,
                                    actionType = ActionType.CHECK,
                                    amountChips = 0L,
                                    totalToChips = 0L,
                                    shownCards = boardFlop.toList(),
                                    rawText = "Флоп: ${boardFlop.joinToString(" ") { it.displayText }}",
                                    streetBoardCards = currentBoard.toList()
                                )
                            )
                        }
                    }
                    3 -> {
                        if (boardTurn != null) {
                            actions.add(
                                HandAction(
                                    id = actionCounter++,
                                    street = Street.TURN,
                                    playerName = "",
                                    seatNumber = 0,
                                    actionType = ActionType.CHECK,
                                    amountChips = 0L,
                                    totalToChips = 0L,
                                    shownCards = listOf(boardTurn),
                                    rawText = "Терн: ${boardTurn.displayText}",
                                    streetBoardCards = currentBoard.toList()
                                )
                            )
                        }
                    }
                    4 -> {
                        if (boardRiver != null) {
                            actions.add(
                                HandAction(
                                    id = actionCounter++,
                                    street = Street.RIVER,
                                    playerName = "",
                                    seatNumber = 0,
                                    actionType = ActionType.CHECK,
                                    amountChips = 0L,
                                    totalToChips = 0L,
                                    shownCards = listOf(boardRiver),
                                    rawText = "Ривер: ${boardRiver.displayText}",
                                    streetBoardCards = currentBoard.toList()
                                )
                            )
                        }
                    }
                }
            }
        }

        // Determine player positions
        val playerStates = calculatePlayerPositions(
            rawPlayers = rawPlayers,
            buttonSeat = buttonSeat,
            antesPosted = antesPosted,
            blindsPosted = blindsPosted,
            heroName = heroName,
            heroCards = heroCards
        )

        // Calculate total pot and winners
        val winners = mutableListOf<HandWinner>()
        for (rp in rawPlayers) {
            if (rp.win > 0) {
                winners.add(HandWinner(rp.name, rp.win))
            }
        }

        // If winners is empty (e.g. everyone folded to 1 player, but win attr was 0)
        if (winners.isEmpty()) {
            val foldedPlayers = actions.filter { it.actionType == ActionType.FOLD }.map { it.playerName }.toSet()
            val activePlayers = rawPlayers.filter { it.name !in foldedPlayers }
            if (activePlayers.size == 1) {
                val soleWinner = activePlayers.first()
                val pot = rawPlayers.sumOf { it.bet }
                winners.add(HandWinner(soleWinner.name, pot))
            }
        }

        val totalPot = if (winners.isNotEmpty()) {
            winners.sumOf { it.amountChips }
        } else {
            rawPlayers.sumOf { it.bet }
        }

        // Reveal opponent cards at Showdown if any cards were revealed
        if (revealedOpponents.isNotEmpty()) {
            for ((oppName, oppCards) in revealedOpponents) {
                val seat = rawPlayers.firstOrNull { it.name == oppName }?.seat ?: 0
                actions.add(
                    HandAction(
                        id = actionCounter++,
                        street = Street.SHOWDOWN,
                        playerName = oppName,
                        seatNumber = seat,
                        actionType = ActionType.SHOWS,
                        amountChips = 0L,
                        totalToChips = 0L,
                        shownCards = oppCards,
                        rawText = "$oppName: SHOWS [${oppCards.joinToString(" ") { it.displayText }}]",
                        streetBoardCards = currentBoard.toList()
                    )
                )
            }
        }

        // Add COLLECTED action for each winner to complete the hand and show the winner
        val showdownOrEndStreet = if (boardFlop.isNotEmpty() || revealedOpponents.isNotEmpty()) Street.SHOWDOWN else Street.PREFLOP
        for (winner in winners) {
            val seat = rawPlayers.firstOrNull { it.name == winner.playerName }?.seat ?: 0
            val bbVal = if (bigBlind > 0L) winner.amountChips.toDouble() / bigBlind.toDouble() else 0.0
            val bbFormatted = if (bigBlind > 0L) {
                if (bbVal >= 100 || bbVal >= 10) String.format(Locale.US, "%.1f BB", bbVal)
                else if (bbVal == bbVal.toLong().toDouble()) "${bbVal.toLong()} BB"
                else String.format(Locale.US, "%.2f BB", bbVal)
            } else {
                "${winner.amountChips}"
            }

            actions.add(
                HandAction(
                    id = actionCounter++,
                    street = showdownOrEndStreet,
                    playerName = winner.playerName,
                    seatNumber = seat,
                    actionType = ActionType.COLLECTED,
                    amountChips = winner.amountChips,
                    totalToChips = 0L,
                    rawText = "🏆 ${winner.playerName} WINS $bbFormatted (${String.format(Locale.US, "%,d", winner.amountChips)})",
                    streetBoardCards = currentBoard.toList()
                )
            )
        }

        val levelNumber = handIndex + 1

        val cleanTableNumber = if (data.tableName.contains(",")) {
            data.tableName.substringAfterLast(",").trim()
        } else {
            data.tableName.trim()
        }.ifEmpty { "1" }

        val cleanTournamentName = if (data.tournamentName.contains(",")) {
            data.tournamentName.substringBefore(",").trim().ifEmpty { data.tournamentName }
        } else {
            data.tournamentName
        }.ifEmpty { "RedStar Tournament" }

        return PokerHand(
            handId = gameCode,
            tournamentId = extractTournamentCode(data.tournamentName),
            tournamentName = cleanTournamentName,
            levelNumber = levelNumber,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            timestamp = startDate,
            tableNumber = cleanTableNumber,
            maxSeats = data.tableSize,
            buttonSeat = buttonSeat,
            players = playerStates,
            heroName = heroName,
            heroCards = heroCards,
            boardFlop = boardFlop,
            boardTurn = boardTurn,
            boardRiver = boardRiver,
            actions = actions,
            totalPotChips = totalPot,
            winners = winners,
            summaryNotes = listOf("Рум: RedStar Poker (iPoker XML)")
        )
    }

    private fun mapRoundNoToStreet(roundNo: Int): Street = when (roundNo) {
        0, 1 -> Street.PREFLOP
        2 -> Street.FLOP
        3 -> Street.TURN
        4 -> Street.RIVER
        else -> Street.SHOWDOWN
    }

    /**
     * Maps RedStar / iPoker numeric action types to domain ActionType:
     * 0 = Fold
     * 1 = SB (preflop posting)
     * 2 = BB (preflop posting)
     * 3 = Call
     * 4 = Check
     * 5 = Bet
     * 7 = All-in
     * 15 = Ante
     * 23 = Raise
     */
    private fun parseRedStarAction(
        actionId: Int,
        street: Street,
        playerName: String,
        seatNumber: Int,
        typeCode: Int,
        sum: Long,
        currentBoard: List<Card>,
        playerStartingChips: Long
    ): HandAction? {
        val (actionType, isAllIn) = when (typeCode) {
            0 -> Pair(ActionType.FOLD, false)
            4 -> Pair(ActionType.CHECK, false)
            3 -> {
                val allIn = sum >= playerStartingChips && sum > 0
                Pair(if (allIn) ActionType.ALL_IN else ActionType.CALL, allIn)
            }
            5 -> {
                val allIn = sum >= playerStartingChips && sum > 0
                Pair(if (allIn) ActionType.ALL_IN else ActionType.BET, allIn)
            }
            23 -> {
                val allIn = sum >= playerStartingChips && sum > 0
                Pair(if (allIn) ActionType.ALL_IN else ActionType.RAISE, allIn)
            }
            7 -> Pair(ActionType.ALL_IN, true)
            else -> {
                // Unknown action code fallback
                if (sum == 0L) Pair(ActionType.CHECK, false)
                else Pair(ActionType.CALL, false)
            }
        }

        return HandAction(
            id = actionId,
            street = street,
            playerName = playerName,
            seatNumber = seatNumber,
            actionType = actionType,
            amountChips = sum,
            totalToChips = sum,
            isAllIn = isAllIn,
            rawText = "$playerName: ${actionType.name} $sum",
            streetBoardCards = currentBoard
        )
    }

    /**
     * Parses card strings formatted like:
     * "D3 C5" -> 3♦ 5♣
     * "S10 H3" -> 10♠ 3♥
     * "CQ D5" -> Q♣ 5♦
     * "DK H6 C10" -> K♦ 6♥ 10♣
     * Suit is first char: D = Diamonds, C = Clubs, H = Hearts, S = Spades
     * Rank is rest of string: 2..10, J, Q, K, A
     */
    fun parseRedStarCards(raw: String): List<Card> {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "X X" || trimmed == "X") return emptyList()

        val tokens = trimmed.split("\\s+".toRegex())
        val result = mutableListOf<Card>()

        for (token in tokens) {
            val t = token.trim()
            if (t.equals("X", ignoreCase = true) || t.length < 2) continue
            val suitChar = t[0]
            val rankStr = t.substring(1)

            val suit = when (suitChar.uppercaseChar()) {
                'D' -> Suit.DIAMONDS
                'C' -> Suit.CLUBS
                'H' -> Suit.HEARTS
                'S' -> Suit.SPADES
                else -> null
            } ?: continue

            val rank = when (rankStr.uppercase()) {
                "2" -> Rank.TWO
                "3" -> Rank.THREE
                "4" -> Rank.FOUR
                "5" -> Rank.FIVE
                "6" -> Rank.SIX
                "7" -> Rank.SEVEN
                "8" -> Rank.EIGHT
                "9" -> Rank.NINE
                "10", "T" -> Rank.TEN
                "J" -> Rank.JACK
                "Q" -> Rank.QUEEN
                "K" -> Rank.KING
                "A" -> Rank.ACE
                else -> null
            } ?: continue

            result.add(Card(rank, suit))
        }

        return result
    }

    private fun parseChipAmount(raw: String): Long {
        if (raw.isBlank()) return 0L
        val clean = raw.replace(",", "").replace("€", "").replace("$", "").trim()
        return clean.toLongOrNull() ?: 0L
    }

    private fun getChildText(elem: Element, tag: String): String {
        val list = elem.getElementsByTagName(tag)
        if (list.length == 0) return ""
        return list.item(0).textContent.trim()
    }

    private fun extractTournamentCode(name: String): String {
        val numMatch = Regex("""\b\d{7,12}\b""").find(name)
        return numMatch?.value ?: "RedStarTourney"
    }

    private fun calculatePlayerPositions(
        rawPlayers: List<RawPlayer>,
        buttonSeat: Int,
        antesPosted: Map<String, Long>,
        blindsPosted: Map<String, Long>,
        heroName: String,
        heroCards: List<Card>
    ): List<PlayerInitialState> {
        val players = rawPlayers.map {
            Triple(it.seat, it.name, it.chips)
        }.sortedBy { it.first }

        if (players.isEmpty()) return emptyList()

        val allSeats = players.map { it.first }

        // Find SB & BB seats
        val btnSeat = buttonSeat
        val sbPlayer = blindsPosted.entries.firstOrNull { it.value > 0 && it.value < (blindsPosted.values.maxOrNull() ?: 0L) }?.key
        val bbPlayer = blindsPosted.entries.maxByOrNull { it.value }?.key

        val sbSeat = players.firstOrNull { it.second == sbPlayer }?.first
            ?: getNextSeatClockwise(btnSeat, allSeats)

        val bbSeat = players.firstOrNull { it.second == bbPlayer }?.first
            ?: getNextSeatClockwise(sbSeat, allSeats)

        // Seats clockwise after BB up to BTN
        val otherSeatsClockwise = mutableListOf<Int>()
        var curr = getNextSeatClockwise(bbSeat, allSeats)
        var guard = 0
        while (curr != btnSeat && curr != sbSeat && curr != bbSeat && guard < 10) {
            otherSeatsClockwise.add(curr)
            curr = getNextSeatClockwise(curr, allSeats)
            guard++
        }

        val earlyMidRoles = listOf(
            PositionRole.UTG2,
            PositionRole.MP1,
            PositionRole.MP2,
            PositionRole.HJ,
            PositionRole.CO
        )

        val positionAssignment = mutableMapOf<Int, PositionRole>()
        positionAssignment[btnSeat] = PositionRole.BTN
        positionAssignment[sbSeat] = PositionRole.SB
        positionAssignment[bbSeat] = PositionRole.BB

        for ((idx, s) in otherSeatsClockwise.withIndex()) {
            if (idx < earlyMidRoles.size) {
                positionAssignment[s] = earlyMidRoles[idx]
            } else {
                positionAssignment[s] = PositionRole.CO
            }
        }

        return players.map { (seatNum, pName, startingChips) ->
            val isHero = pName.equals(heroName, ignoreCase = true) || pName.equals("Hero", ignoreCase = true)
            val pos = positionAssignment[seatNum] ?: PositionRole.UNKNOWN
            PlayerInitialState(
                seatNumber = seatNum,
                playerName = pName,
                startingChips = startingChips,
                isHero = isHero,
                holeCards = if (isHero && heroCards.isNotEmpty()) heroCards else null,
                position = pos,
                antePosted = antesPosted[pName] ?: 0L,
                blindPosted = blindsPosted[pName] ?: 0L
            )
        }
    }

    private fun getNextSeatClockwise(currentSeat: Int, sortedSeats: List<Int>): Int {
        val idx = sortedSeats.indexOf(currentSeat)
        if (idx == -1) return sortedSeats.firstOrNull() ?: currentSeat
        return sortedSeats[(idx + 1) % sortedSeats.size]
    }
}
