package com.example.poker.model

enum class Street(val displayNameRu: String) {
    PREFLOP("Префлоп"),
    FLOP("Флоп"),
    TURN("Терн"),
    RIVER("Ривер"),
    SHOWDOWN("Шоудаун")
}

enum class ActionType(val labelRu: String, val labelEn: String) {
    FOLD("FOLD", "FOLD"),
    CHECK("CHECK", "CHECK"),
    CALL("CALL", "CALL"),
    BET("BET", "BET"),
    RAISE("RAISE", "RAISE"),
    ALL_IN("ALL-IN", "ALL-IN"),
    SHOWS("SHOWS", "SHOWS"),
    COLLECTED("COLLECTED", "COLLECTED")
}

data class HandAction(
    val id: Int,
    val street: Street,
    val playerName: String,
    val seatNumber: Int,
    val actionType: ActionType,
    val amountChips: Long = 0L,
    val totalToChips: Long = 0L, // for "raises X to Y"
    val isAllIn: Boolean = false,
    val shownCards: List<Card> = emptyList(),
    val rawText: String = "",
    val streetBoardCards: List<Card> = emptyList() // Board cards visible up to this street
)
