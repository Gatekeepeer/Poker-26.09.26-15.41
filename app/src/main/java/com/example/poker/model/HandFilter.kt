package com.example.poker.model

enum class HandFilter(val label: String) {
    ALL("Все раздачи"),
    HERO_WON("Hero победил"),
    HERO_PLAYED("Смотрел флоп+"),
    HERO_FOLDED_PRE("Фолд префлоп");

    fun matches(hand: PokerHand): Boolean {
        val heroWon = hand.winners.any { it.playerName == hand.heroName }
        val heroFoldedPreflop = hand.actions.any {
            it.playerName == hand.heroName && it.actionType == ActionType.FOLD && it.street == Street.PREFLOP
        }
        val heroSawFlop = !heroFoldedPreflop && hand.boardFlop.isNotEmpty()

        return when (this) {
            ALL -> true
            HERO_WON -> heroWon
            HERO_PLAYED -> heroSawFlop || heroWon
            HERO_FOLDED_PRE -> heroFoldedPreflop
        }
    }
}
