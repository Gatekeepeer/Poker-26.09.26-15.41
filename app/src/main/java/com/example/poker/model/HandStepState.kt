package com.example.poker.model

import java.util.Locale

data class PlayerReplayState(
    val seatNumber: Int,
    val playerName: String,
    val isHero: Boolean,
    val position: PositionRole,
    val currentChips: Long,
    val currentChipsBb: Double,
    val currentStreetBetChips: Long,
    val currentStreetBetBb: Double,
    val isFolded: Boolean,
    val isAllIn: Boolean,
    val cards: List<Card>?,
    val isCardsVisible: Boolean,
    val isActingNow: Boolean,
    val lastActionText: String?,
    val isWinner: Boolean = false,
    val wonAmountChips: Long = 0L,
    val isButton: Boolean = false,
    val antePostedChips: Long = 0L,
    val blindPostedChips: Long = 0L
) {
    fun formatChips(chips: Long, bb: Long): String {
        return formatBb(chips, bb)
    }

    companion object {
        fun formatBb(chips: Long, bb: Long): String {
            if (bb <= 0L) return "$chips"
            val bbVal = chips.toDouble() / bb.toDouble()
            return if (bbVal >= 100) {
                String.format(Locale.US, "%.1f BB", bbVal)
            } else if (bbVal >= 10) {
                String.format(Locale.US, "%.1f BB", bbVal)
            } else if (bbVal == bbVal.toLong().toDouble()) {
                "${bbVal.toLong()} BB"
            } else {
                String.format(Locale.US, "%.2f BB", bbVal)
            }
        }

        fun formatChipCount(amount: Long): String {
            return String.format(Locale.US, "%,d", amount)
        }
    }
}

data class HandStepState(
    val stepIndex: Int,
    val totalSteps: Int,
    val currentStreet: Street,
    val currentPotChips: Long,
    val currentPotBb: Double,
    val boardCards: List<Card>,
    val players: List<PlayerReplayState>,
    val currentAction: HandAction?,
    val actionDescription: String,
    val uncalledBetInfo: String? = null,
    val winnerNotice: String? = null
) {
    val canStepBack: Boolean get() = stepIndex > 0
    val canStepForward: Boolean get() = stepIndex < totalSteps
}
