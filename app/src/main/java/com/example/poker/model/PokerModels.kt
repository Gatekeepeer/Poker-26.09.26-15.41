package com.example.poker.model

enum class Suit(val symbol: String, val isRed: Boolean) {
    HEARTS("♥", true),
    DIAMONDS("♦", true),
    CLUBS("♣", false),
    SPADES("♠", false);

    companion object {
        fun fromChar(c: Char): Suit = when (c.lowercaseChar()) {
            'h' -> HEARTS
            'd' -> DIAMONDS
            'c' -> CLUBS
            's' -> SPADES
            else -> SPADES
        }
    }
}

enum class Rank(val symbol: String, val value: Int) {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("T", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14);

    companion object {
        fun fromChar(c: Char): Rank = when (c.uppercaseChar()) {
            '2' -> TWO
            '3' -> THREE
            '4' -> FOUR
            '5' -> FIVE
            '6' -> SIX
            '7' -> SEVEN
            '8' -> EIGHT
            '9' -> NINE
            'T', '1' -> TEN
            'J' -> JACK
            'Q' -> QUEEN
            'K' -> KING
            'A' -> ACE
            else -> ACE
        }
    }
}

data class Card(val rank: Rank, val suit: Suit) {
    override fun toString(): String = "${rank.symbol}${suit.symbol}"

    companion object {
        fun parse(str: String): Card? {
            val s = str.trim()
            if (s.length < 2) return null
            val rChar = s[0]
            val sChar = s[s.length - 1]
            return Card(Rank.fromChar(rChar), Suit.fromChar(sChar))
        }
    }
}

enum class Position(val displayName: String) {
    BTN("BTN"),
    SB("SB"),
    BB("BB"),
    UTG("UTG"),
    UTG1("UTG+1"),
    UTG2("UTG+2"),
    MP1("MP1"),
    MP2("MP2"),
    HJ("HJ"),
    CO("CO")
}

enum class ActionType(val displayName: String) {
    POST_SB("Small Blind"),
    POST_BB("Big Blind"),
    POST_ANTE("Ante"),
    FOLD("Fold"),
    CHECK("Check"),
    CALL("Call"),
    BET("Bet"),
    RAISE("Raise"),
    ALL_IN("All-in"),
    UNCALLED_BET("Возврат"),
    COLLECT_POT("Банк"),
    SHOWS("Показывает"),
    MUCKS("Сбрасывает");

    val isAggressive: Boolean get() = this == BET || this == RAISE || this == ALL_IN
    val isFold: Boolean get() = this == FOLD
}

data class PlayerAction(
    val playerName: String,
    val actionType: ActionType,
    val amount: Double = 0.0,
    val toAmount: Double = 0.0,
    val isAllIn: Boolean = false,
    val street: Street = Street.PREFLOP
)

enum class Street(val displayName: String) {
    PREFLOP("Preflop"),
    FLOP("Flop"),
    TURN("Turn"),
    RIVER("River"),
    SHOWDOWN("Showdown"),
    SUMMARY("Summary")
}

data class PlayerState(
    val name: String,
    val seatNumber: Int,
    val startingChips: Double,
    val currentChips: Double,
    val currentBet: Double = 0.0,
    val position: Position? = null,
    val isButton: Boolean = false,
    val isHero: Boolean = false,
    val holeCards: List<Card> = emptyList(),
    val isFolded: Boolean = false,
    val isAllIn: Boolean = false,
    val isSittingOut: Boolean = false,
    val lastAction: PlayerAction? = null,
    val wonAmount: Double = 0.0,
    val bounty: Double = 0.0
)

data class HandStep(
    val stepIndex: Int,
    val street: Street,
    val description: String,
    val communityCards: List<Card>,
    val mainPot: Double,
    val sidePots: List<Double> = emptyList(),
    val currentTotalPot: Double,
    val players: Map<String, PlayerState>,
    val activePlayerName: String? = null,
    val lastAction: PlayerAction? = null
)

data class PokerHand(
    val handId: String,
    val tournamentName: String,
    val tableName: String,
    val levelText: String,
    val smallBlind: Double,
    val bigBlind: Double,
    val ante: Double = 0.0,
    val heroName: String?,
    val buttonSeat: Int,
    val initialPlayers: List<PlayerState>,
    val communityCards: List<Card>,
    val steps: List<HandStep>,
    val winners: List<Pair<String, Double>>,
    val totalPot: Double,
    val rawText: String = ""
)

data class TournamentData(
    val id: String,
    val name: String,
    val hands: List<PokerHand>,
    val sourceFormat: String = "GGPoker"
)
