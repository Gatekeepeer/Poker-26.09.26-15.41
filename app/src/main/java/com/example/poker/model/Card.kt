package com.example.poker.model

enum class Suit(val charCode: Char, val symbol: String, val russianName: String) {
    SPADES('s', "♠", "Пики"),
    HEARTS('h', "♥", "Червы"),
    DIAMONDS('d', "♦", "Бубны"),
    CLUBS('c', "♣", "Трефы");

    companion object {
        fun fromChar(c: Char): Suit = when (c.lowercaseChar()) {
            's' -> SPADES
            'h' -> HEARTS
            'd' -> DIAMONDS
            'c' -> CLUBS
            else -> SPADES
        }
    }
}

enum class Rank(val charCode: Char, val display: String, val value: Int) {
    TWO('2', "2", 2),
    THREE('3', "3", 3),
    FOUR('4', "4", 4),
    FIVE('5', "5", 5),
    SIX('6', "6", 6),
    SEVEN('7', "7", 7),
    EIGHT('8', "8", 8),
    NINE('9', "9", 9),
    TEN('T', "10", 10),
    JACK('J', "J", 11),
    QUEEN('Q', "Q", 12),
    KING('K', "K", 13),
    ACE('A', "A", 14);

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
            else -> TWO
        }
    }
}

data class Card(
    val rank: Rank,
    val suit: Suit
) {
    val code: String get() = "${rank.charCode}${suit.charCode}"
    val displayText: String get() = "${rank.display}${suit.symbol}"

    companion object {
        fun fromString(str: String): Card? {
            val trimmed = str.trim()
            if (trimmed.length < 2) return null
            val rankChar = trimmed[0]
            val suitChar = trimmed[1]
            return Card(Rank.fromChar(rankChar), Suit.fromChar(suitChar))
        }

        fun parseCards(raw: String): List<Card> {
            val clean = raw.replace("[", "").replace("]", "").trim()
            if (clean.isBlank()) return emptyList()
            return clean.split("\\s+".toRegex()).mapNotNull { fromString(it) }
        }
    }
}
