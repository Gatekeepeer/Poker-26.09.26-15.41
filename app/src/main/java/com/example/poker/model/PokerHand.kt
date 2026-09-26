package com.example.poker.model

data class PlayerInitialState(
    val seatNumber: Int,
    val playerName: String,
    val startingChips: Long,
    val isHero: Boolean,
    val holeCards: List<Card>?,
    val position: PositionRole,
    val antePosted: Long = 0L,
    val blindPosted: Long = 0L
) {
    val chipsAfterPosts: Long
        get() = (startingChips - antePosted - blindPosted).coerceAtLeast(0L)
}

data class HandWinner(
    val playerName: String,
    val amountChips: Long,
    val winningHandDesc: String? = null
)

data class PokerHand(
    val handId: String,
    val tournamentId: String,
    val tournamentName: String,
    val levelNumber: Int,
    val smallBlind: Long,
    val bigBlind: Long,
    val ante: Long,
    val timestamp: String,
    val tableNumber: String,
    val maxSeats: Int,
    val buttonSeat: Int,
    val players: List<PlayerInitialState>,
    val heroName: String,
    val heroCards: List<Card>,
    val boardFlop: List<Card> = emptyList(),
    val boardTurn: Card? = null,
    val boardRiver: Card? = null,
    val actions: List<HandAction> = emptyList(),
    val totalPotChips: Long = 0L,
    val winners: List<HandWinner> = emptyList(),
    val summaryNotes: List<String> = emptyList()
) {
    val allBoardCards: List<Card>
        get() = buildList {
            addAll(boardFlop)
            boardTurn?.let { add(it) }
            boardRiver?.let { add(it) }
        }

    val initialPotChips: Long
        get() = players.sumOf { it.antePosted + it.blindPosted }
}
