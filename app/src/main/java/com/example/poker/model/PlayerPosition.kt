package com.example.poker.model

/**
 * 8-max fixed position system where position number = preflop action order:
 * 1 = UTG2
 * 2 = MP1
 * 3 = MP2
 * 4 = HJ
 * 5 = CO
 * 6 = BTN
 * 7 = SB
 * 8 = BB
 */
enum class PositionRole(
    val roleName: String,
    val positionNumber: Int,
    val descriptionRu: String
) {
    UTG2("UTG2", 1, "UTG2 (Позиция 1)"),
    MP1("MP1", 2, "MP1 (Позиция 2)"),
    MP2("MP2", 3, "MP2 (Позиция 3)"),
    HJ("HJ", 4, "HJ Хайджек (Позиция 4)"),
    CO("CO", 5, "CO Кат-офф (Позиция 5)"),
    BTN("BTN", 6, "BTN Баттон (Позиция 6)"),
    SB("SB", 7, "SB Малый блайнд (Позиция 7)"),
    BB("BB", 8, "BB Большой блайнд (Позиция 8)"),
    UNKNOWN("UNK", 0, "Неизвестная позиция");

    val shortLabel: String get() = roleName
}
