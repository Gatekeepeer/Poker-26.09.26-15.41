package com.example.poker.parser

import com.example.poker.model.ActionType
import com.example.poker.model.Card
import com.example.poker.model.HandAction
import com.example.poker.model.HandWinner
import com.example.poker.model.PlayerInitialState
import com.example.poker.model.PokerHand
import com.example.poker.model.PositionRole
import com.example.poker.model.Street
import java.text.SimpleDateFormat
import java.util.Locale

data class ParseResult(
    val hands: List<PokerHand>,
    val warnings: List<String>,
    val totalParsedCount: Int,
    val skippedCount: Int
)

object GGPokerParser {

    private val HAND_HEADER_REGEX = Regex(
        """Poker Hand #([A-Za-z0-9]+):\s*Tournament #(\d+),\s*(.*?)\s*-\s*Level(\d+)\(([\d,]+)/([\d,]+)(?:\(([\d,]+)\))?\)\s*-\s*([\d/]+ [\d:]+)"""
    )
    private val TABLE_REGEX = Regex(
        """Table '([^']+)'\s*(\d+)-max\s*Seat #(\d+)\s*is the button"""
    )
    private val SEAT_REGEX = Regex(
        """Seat (\d+):\s*(.+?)\s*\(([\d,]+)\s*in chips\)"""
    )
    private val POST_ANTE_REGEX = Regex(
        """^(.+?):\s*posts the ante\s*([\d,]+)"""
    )
    private val POST_SB_REGEX = Regex(
        """^(.+?):\s*posts small blind\s*([\d,]+)"""
    )
    private val POST_BB_REGEX = Regex(
        """^(.+?):\s*posts big blind\s*([\d,]+)"""
    )
    private val DEALT_HERO_REGEX = Regex(
        """Dealt to (.+?)\s*\[(.*?)\]"""
    )
    private val FLOP_REGEX = Regex(
        """\*\*\* FLOP \*\*\*\s*\[(.*?)\]"""
    )
    private val TURN_REGEX = Regex(
        """\*\*\* TURN \*\*\*\s*\[.*?\]\s*\[(.*?)\]"""
    )
    private val RIVER_REGEX = Regex(
        """\*\*\* RIVER \*\*\*\s*\[.*?\]\s*\[(.*?)\]"""
    )
    private val COLLECTED_REGEX = Regex(
        """^(.+?)\s+collected\s+([\d,]+)\s+from pot"""
    )
    private val SHOWS_REGEX = Regex(
        """^(.+?):\s*shows\s*\[(.*?)\](?:\s*\((.*?)\))?"""
    )
    private val UNCALLED_BET_REGEX = Regex(
        """Uncalled bet \(([\d,]+)\) returned to (.+)"""
    )

    fun parseHandHistory(text: String): ParseResult {
        if (text.isBlank()) {
            return ParseResult(emptyList(), listOf("Файл пуст"), 0, 0)
        }

        val rawHands = splitIntoHandChunks(text)
        val hands = mutableListOf<PokerHand>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        for ((index, chunk) in rawHands.withIndex()) {
            try {
                val hand = parseSingleHand(chunk)
                if (hand != null) {
                    hands.add(hand)
                } else {
                    skipped++
                    warnings.add("Раздача #${index + 1} пропущена (не удалось распарсить структуру)")
                }
            } catch (e: Exception) {
                skipped++
                warnings.add("Ошибка при парсинге раздачи #${index + 1}: ${e.message}")
            }
        }

        // Sort chronologically from earliest to latest (section 4.1)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
        val sortedHands = hands.sortedWith { h1, h2 ->
            try {
                val d1 = dateFormat.parse(h1.timestamp)
                val d2 = dateFormat.parse(h2.timestamp)
                if (d1 != null && d2 != null) d1.compareTo(d2) else 0
            } catch (e: Exception) {
                0
            }
        }

        return ParseResult(
            hands = sortedHands,
            warnings = warnings,
            totalParsedCount = sortedHands.size,
            skippedCount = skipped
        )
    }

    private fun splitIntoHandChunks(text: String): List<String> {
        val lines = text.lines()
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (line in lines) {
            if (line.startsWith("Poker Hand #")) {
                if (currentChunk.isNotBlank()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotBlank()) {
            chunks.add(currentChunk.toString().trim())
        }
        return chunks
    }

    private fun parseSingleHand(chunk: String): PokerHand? {
        val lines = chunk.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val headerMatch = HAND_HEADER_REGEX.find(lines[0]) ?: return null
        val handId = headerMatch.groupValues[1]
        val tournamentId = headerMatch.groupValues[2]
        val tournamentName = headerMatch.groupValues[3].trim()
        val levelNumber = headerMatch.groupValues[4].toIntOrNull() ?: 1
        val smallBlind = headerMatch.groupValues[5].replace(",", "").toLongOrNull() ?: 0L
        val bigBlind = headerMatch.groupValues[6].replace(",", "").toLongOrNull() ?: 1L
        val ante = headerMatch.groupValues.getOrNull(7)?.replace(",", "")?.toLongOrNull() ?: 0L
        val timestamp = headerMatch.groupValues[8]

        var tableNumber = "1"
        var maxSeats = 8
        var buttonSeat = 1

        val rawSeats = mutableListOf<Triple<Int, String, Long>>()
        val antesPosted = mutableMapOf<String, Long>()
        val blindsPosted = mutableMapOf<String, Long>()

        var heroName = ""
        var heroCards = emptyList<Card>()

        val flopCards = mutableListOf<Card>()
        var turnCard: Card? = null
        var riverCard: Card? = null

        var currentStreet = Street.PREFLOP
        val actions = mutableListOf<HandAction>()
        var actionIdCounter = 1

        val winners = mutableListOf<HandWinner>()
        val summaryNotes = mutableListOf<String>()
        var totalPotChips = 0L

        var inSummary = false

        for (line in lines) {
            // Table line
            val tableMatch = TABLE_REGEX.find(line)
            if (tableMatch != null) {
                tableNumber = tableMatch.groupValues[1]
                maxSeats = tableMatch.groupValues[2].toIntOrNull() ?: 8
                buttonSeat = tableMatch.groupValues[3].toIntOrNull() ?: 1
                continue
            }

            // Seat lines
            val seatMatch = SEAT_REGEX.find(line)
            if (seatMatch != null && !inSummary) {
                val seatNum = seatMatch.groupValues[1].toInt()
                val pName = seatMatch.groupValues[2].trim()
                val chips = seatMatch.groupValues[3].replace(",", "").toLongOrNull() ?: 0L
                rawSeats.add(Triple(seatNum, pName, chips))
                continue
            }

            // Post ante
            val anteMatch = POST_ANTE_REGEX.find(line)
            if (anteMatch != null) {
                val pName = anteMatch.groupValues[1].trim()
                val amt = anteMatch.groupValues[2].replace(",", "").toLongOrNull() ?: 0L
                antesPosted[pName] = (antesPosted[pName] ?: 0L) + amt
                continue
            }

            // Post small blind
            val sbMatch = POST_SB_REGEX.find(line)
            if (sbMatch != null) {
                val pName = sbMatch.groupValues[1].trim()
                val amt = sbMatch.groupValues[2].replace(",", "").toLongOrNull() ?: 0L
                blindsPosted[pName] = (blindsPosted[pName] ?: 0L) + amt
                continue
            }

            // Post big blind
            val bbMatch = POST_BB_REGEX.find(line)
            if (bbMatch != null) {
                val pName = bbMatch.groupValues[1].trim()
                val amt = bbMatch.groupValues[2].replace(",", "").toLongOrNull() ?: 0L
                blindsPosted[pName] = (blindsPosted[pName] ?: 0L) + amt
                continue
            }

            // Hole cards dealt
            val dealtHeroMatch = DEALT_HERO_REGEX.find(line)
            if (dealtHeroMatch != null) {
                val targetPlayer = dealtHeroMatch.groupValues[1].trim()
                val cardsStr = dealtHeroMatch.groupValues[2].trim()
                if (cardsStr.isNotBlank()) {
                    val parsed = Card.parseCards(cardsStr)
                    if (parsed.isNotEmpty()) {
                        heroName = targetPlayer
                        heroCards = parsed
                    }
                }
                continue
            }

            // Street transitions
            if (line.startsWith("*** FLOP ***")) {
                currentStreet = Street.FLOP
                val flopMatch = FLOP_REGEX.find(line)
                if (flopMatch != null) {
                    flopCards.clear()
                    flopCards.addAll(Card.parseCards(flopMatch.groupValues[1]))
                }
                continue
            }

            if (line.startsWith("*** TURN ***")) {
                currentStreet = Street.TURN
                val turnMatch = TURN_REGEX.find(line)
                if (turnMatch != null) {
                    turnCard = Card.fromString(turnMatch.groupValues[1])
                }
                continue
            }

            if (line.startsWith("*** RIVER ***")) {
                currentStreet = Street.RIVER
                val riverMatch = RIVER_REGEX.find(line)
                if (riverMatch != null) {
                    riverCard = Card.fromString(riverMatch.groupValues[1])
                }
                continue
            }

            if (line.startsWith("*** SHOWDOWN ***")) {
                currentStreet = Street.SHOWDOWN
                continue
            }

            if (line.startsWith("*** SUMMARY ***")) {
                inSummary = true
                continue
            }

            if (inSummary) {
                if (line.startsWith("Total pot")) {
                    val potStr = Regex("""Total pot ([\d,]+)""").find(line)?.groupValues?.get(1)
                    if (potStr != null) {
                        totalPotChips = potStr.replace(",", "").toLongOrNull() ?: totalPotChips
                    }
                }
                summaryNotes.add(line)
                continue
            }

            // Ignore uncalled bet lines as active action events (handled per 4.4)
            if (UNCALLED_BET_REGEX.containsMatchIn(line)) {
                continue
            }

            // Hand action events (section 4.6)
            val action = parseActionLine(
                line = line,
                actionId = actionIdCounter++,
                street = currentStreet,
                seats = rawSeats,
                currentBoard = buildList {
                    if (currentStreet >= Street.FLOP) addAll(flopCards)
                    if (currentStreet >= Street.TURN && turnCard != null) add(turnCard)
                    if (currentStreet >= Street.RIVER && riverCard != null) add(riverCard)
                }
            )
            if (action != null) {
                actions.add(action)
                if (action.actionType == ActionType.COLLECTED) {
                    winners.add(HandWinner(action.playerName, action.amountChips))
                }
            }
        }

        // If Hero was not detected by name, check if any player is literally named "Hero"
        if (heroName.isBlank()) {
            val heroSeat = rawSeats.firstOrNull { it.second.equals("Hero", ignoreCase = true) }
            if (heroSeat != null) {
                heroName = heroSeat.second
            } else {
                // Section 8: Hand without hero -> skip
                return null
            }
        }

        // Determine player positions
        val playerStates = calculatePlayerPositions(
            rawSeats = rawSeats,
            buttonSeat = buttonSeat,
            antesPosted = antesPosted,
            blindsPosted = blindsPosted,
            heroName = heroName,
            heroCards = heroCards
        )

        return PokerHand(
            handId = handId,
            tournamentId = tournamentId,
            tournamentName = tournamentName,
            levelNumber = levelNumber,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            timestamp = timestamp,
            tableNumber = tableNumber,
            maxSeats = maxSeats,
            buttonSeat = buttonSeat,
            players = playerStates,
            heroName = heroName,
            heroCards = heroCards,
            boardFlop = flopCards,
            boardTurn = turnCard,
            boardRiver = riverCard,
            actions = actions,
            totalPotChips = totalPotChips,
            winners = winners,
            summaryNotes = summaryNotes
        )
    }

    private fun parseActionLine(
        line: String,
        actionId: Int,
        street: Street,
        seats: List<Triple<Int, String, Long>>,
        currentBoard: List<Card>
    ): HandAction? {
        val colonIdx = line.indexOf(':')
        if (colonIdx > 0) {
            val playerName = line.substring(0, colonIdx).trim()
            val actionRest = line.substring(colonIdx + 1).trim()
            val seatNum = seats.firstOrNull { it.second == playerName }?.first ?: 0

            // 1. folds
            if (actionRest.startsWith("folds")) {
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = ActionType.FOLD,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }

            // 2. checks
            if (actionRest.startsWith("checks")) {
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = ActionType.CHECK,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }

            // 3. raises X to Y [and is all-in]
            val raiseMatch = Regex("""raises\s+([\d,]+)\s+to\s+([\d,]+)(?:\s+and is all-in)?""").find(actionRest)
            if (raiseMatch != null) {
                val amt = raiseMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
                val totalTo = raiseMatch.groupValues[2].replace(",", "").toLongOrNull() ?: 0L
                val isAllIn = actionRest.contains("all-in")
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = if (isAllIn) ActionType.ALL_IN else ActionType.RAISE,
                    amountChips = amt,
                    totalToChips = totalTo,
                    isAllIn = isAllIn,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }

            // 4. calls X [and is all-in]
            val callMatch = Regex("""calls\s+([\d,]+)(?:\s+and is all-in)?""").find(actionRest)
            if (callMatch != null) {
                val amt = callMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
                val isAllIn = actionRest.contains("all-in")
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = if (isAllIn) ActionType.ALL_IN else ActionType.CALL,
                    amountChips = amt,
                    totalToChips = amt,
                    isAllIn = isAllIn,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }

            // 5. bets X [and is all-in]
            val betMatch = Regex("""bets\s+([\d,]+)(?:\s+and is all-in)?""").find(actionRest)
            if (betMatch != null) {
                val amt = betMatch.groupValues[1].replace(",", "").toLongOrNull() ?: 0L
                val isAllIn = actionRest.contains("all-in")
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = if (isAllIn) ActionType.ALL_IN else ActionType.BET,
                    amountChips = amt,
                    totalToChips = amt,
                    isAllIn = isAllIn,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }

            // 6. shows [..]
            val showsMatch = SHOWS_REGEX.find(line)
            if (showsMatch != null) {
                val cards = Card.parseCards(showsMatch.groupValues[2])
                return HandAction(
                    id = actionId,
                    street = street,
                    playerName = playerName,
                    seatNumber = seatNum,
                    actionType = ActionType.SHOWS,
                    shownCards = cards,
                    rawText = line,
                    streetBoardCards = currentBoard
                )
            }
        }

        // Collected from pot
        val colMatch = COLLECTED_REGEX.find(line)
        if (colMatch != null) {
            val pName = colMatch.groupValues[1].trim()
            val amt = colMatch.groupValues[2].replace(",", "").toLongOrNull() ?: 0L
            val seatNum = seats.firstOrNull { it.second == pName }?.first ?: 0
            return HandAction(
                id = actionId,
                street = Street.SHOWDOWN,
                playerName = pName,
                seatNumber = seatNum,
                actionType = ActionType.COLLECTED,
                amountChips = amt,
                rawText = line,
                streetBoardCards = currentBoard
            )
        }

        return null
    }

    /**
     * Determines 8-max / 7-max logical position roles based on section 5:
     * 1. BTN - Seat with button
     * 2. SB - Player who posted small blind
     * 3. BB - Player who posted big blind
     * 4. Remaining seats are assigned clockwise from BB:
     *    UTG2 (1) -> MP1 (2) -> MP2 (3) -> HJ (4) -> CO (5) -> BTN (6) -> SB (7) -> BB (8)
     */
    private fun calculatePlayerPositions(
        rawSeats: List<Triple<Int, String, Long>>,
        buttonSeat: Int,
        antesPosted: Map<String, Long>,
        blindsPosted: Map<String, Long>,
        heroName: String,
        heroCards: List<Card>
    ): List<PlayerInitialState> {
        val sortedSeats = rawSeats.sortedBy { it.first }
        if (sortedSeats.isEmpty()) return emptyList()

        // Map seat numbers to players
        val seatMap = sortedSeats.associateBy { it.first }
        val allSeatNums = sortedSeats.map { it.first }

        // Determine BTN, SB, BB
        val btnSeat = buttonSeat
        val sbPlayer = blindsPosted.entries.firstOrNull { it.value > 0 && it.value < (blindsPosted.values.maxOrNull() ?: 0L) }?.key
            ?: sortedSeats.firstOrNull { it.second != seatMap[btnSeat]?.second }?.second.orEmpty()
        val bbPlayer = blindsPosted.entries.maxByOrNull { it.value }?.key
            ?: sortedSeats.lastOrNull()?.second.orEmpty()

        val sbSeat = sortedSeats.firstOrNull { it.second == sbPlayer }?.first ?: (
            // clockwise after btn
            getNextSeatClockwise(btnSeat, allSeatNums)
        )
        val bbSeat = sortedSeats.firstOrNull { it.second == bbPlayer }?.first ?: (
            // clockwise after sb
            getNextSeatClockwise(sbSeat, allSeatNums)
        )

        // Seats clockwise after BB up to BTN
        val otherSeatsClockwise = mutableListOf<Int>()
        var curr = getNextSeatClockwise(bbSeat, allSeatNums)
        var guard = 0
        while (curr != btnSeat && curr != sbSeat && curr != bbSeat && guard < 10) {
            otherSeatsClockwise.add(curr)
            curr = getNextSeatClockwise(curr, allSeatNums)
            guard++
        }

        // Logical roles available for preflop action between BB and BTN
        val earlyMidRoles = listOf(
            PositionRole.UTG2, // 1
            PositionRole.MP1,  // 2
            PositionRole.MP2,  // 3
            PositionRole.HJ,   // 4
            PositionRole.CO    // 5
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

        return sortedSeats.map { (seatNum, pName, startingChips) ->
            val isHero = pName.equals(heroName, ignoreCase = true) || pName.equals("Hero", ignoreCase = true)
            val pos = positionAssignment[seatNum] ?: PositionRole.UNKNOWN
            PlayerInitialState(
                seatNumber = seatNum,
                playerName = pName,
                startingChips = startingChips,
                isHero = isHero,
                holeCards = if (isHero) heroCards else null,
                position = pos,
                antePosted = antesPosted[pName] ?: 0L,
                blindPosted = blindsPosted[pName] ?: 0L
            )
        }
    }

    private fun getNextSeatClockwise(currentSeat: Int, activeSeats: List<Int>): Int {
        val sorted = activeSeats.sorted()
        val next = sorted.firstOrNull { it > currentSeat }
        return next ?: sorted.first()
    }
}
