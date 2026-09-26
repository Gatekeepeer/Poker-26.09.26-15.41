package com.example.poker.state

import com.example.poker.model.ActionType
import com.example.poker.model.Card
import com.example.poker.model.HandAction
import com.example.poker.model.HandStepState
import com.example.poker.model.PlayerReplayState
import com.example.poker.model.PokerHand
import com.example.poker.model.Street
import java.util.Locale

object HandReplayerEngine {

    /**
     * Precomputes all step states (0 to actions.size) for a given hand.
     */
    fun computeSteps(hand: PokerHand): List<HandStepState> {
        val totalSteps = hand.actions.size
        val result = ArrayList<HandStepState>(totalSteps + 1)

        val bb = hand.bigBlind

        // Internal mutable tracker per player
        class PlayerRuntime(
            val seatNumber: Int,
            val playerName: String,
            val isHero: Boolean,
            val position: com.example.poker.model.PositionRole,
            var chips: Long,
            var currentStreetBet: Long,
            var isFolded: Boolean = false,
            var isAllIn: Boolean = false,
            var revealedCards: List<Card>? = null,
            var lastActionText: String? = null,
            var isWinner: Boolean = false,
            var wonAmount: Long = 0L,
            val isButton: Boolean = false,
            val antePosted: Long = 0L,
            val blindPosted: Long = 0L
        )

        // Initialize players at Step 0
        val runtimePlayers = hand.players.map { p ->
            PlayerRuntime(
                seatNumber = p.seatNumber,
                playerName = p.playerName,
                isHero = p.isHero,
                position = p.position,
                chips = p.chipsAfterPosts,
                currentStreetBet = p.blindPosted,
                isFolded = false,
                isAllIn = p.chipsAfterPosts == 0L,
                revealedCards = if (p.isHero) p.holeCards else null,
                lastActionText = when {
                    p.blindPosted == hand.bigBlind -> "BB ${PlayerReplayState.formatBb(p.blindPosted, bb)}"
                    p.blindPosted == hand.smallBlind && p.blindPosted > 0L -> "SB ${PlayerReplayState.formatBb(p.blindPosted, bb)}"
                    p.antePosted > 0L -> "ANTE ${PlayerReplayState.formatBb(p.antePosted, bb)}"
                    else -> null
                },
                isWinner = false,
                wonAmount = 0L,
                isButton = p.seatNumber == hand.buttonSeat,
                antePosted = p.antePosted,
                blindPosted = p.blindPosted
            )
        }.toMutableList()

        var currentPotChips = hand.initialPotChips
        var currentStreet = Street.PREFLOP

        fun toPlayerReplayState(p: PlayerRuntime, actingNowName: String?): PlayerReplayState {
            return PlayerReplayState(
                seatNumber = p.seatNumber,
                playerName = p.playerName,
                isHero = p.isHero,
                position = p.position,
                currentChips = p.chips,
                currentChipsBb = if (bb > 0) p.chips.toDouble() / bb else 0.0,
                currentStreetBetChips = p.currentStreetBet,
                currentStreetBetBb = if (bb > 0) p.currentStreetBet.toDouble() / bb else 0.0,
                isFolded = p.isFolded,
                isAllIn = p.isAllIn,
                cards = p.revealedCards,
                isCardsVisible = p.isHero || (p.revealedCards != null && p.revealedCards!!.isNotEmpty()),
                isActingNow = p.playerName == actingNowName,
                lastActionText = p.lastActionText,
                isWinner = p.isWinner,
                wonAmountChips = p.wonAmount,
                isButton = p.isButton,
                antePostedChips = p.antePosted,
                blindPostedChips = p.blindPosted
            )
        }

        // Step 0: Initial state
        val step0Desc = buildString {
            append("Hand start. Blinds: ")
            append(PlayerReplayState.formatBb(hand.smallBlind, bb))
            append(" / ")
            append(PlayerReplayState.formatBb(hand.bigBlind, bb))
            if (hand.ante > 0) {
                append(" (ante: ")
                append(PlayerReplayState.formatBb(hand.ante, bb))
                append(")")
            }
        }

        result.add(
            HandStepState(
                stepIndex = 0,
                totalSteps = totalSteps,
                currentStreet = Street.PREFLOP,
                currentPotChips = currentPotChips,
                currentPotBb = if (bb > 0) currentPotChips.toDouble() / bb else 0.0,
                boardCards = emptyList(),
                players = runtimePlayers.map { toPlayerReplayState(it, null) },
                currentAction = null,
                actionDescription = step0Desc
            )
        )

        // Process each action for steps 1..N
        for ((idx, action) in hand.actions.withIndex()) {
            val stepNumber = idx + 1

            // If street transitioned, clear previous street bets into pot
            if (action.street != currentStreet) {
                currentStreet = action.street
                for (p in runtimePlayers) {
                    p.currentStreetBet = 0L
                    if (currentStreet == Street.FLOP || currentStreet == Street.TURN || currentStreet == Street.RIVER) {
                        p.lastActionText = null
                    }
                }
            }

            val actor = runtimePlayers.firstOrNull { it.playerName == action.playerName }
            var actionDesc = ""

            if (actor != null) {
                when (action.actionType) {
                    ActionType.FOLD -> {
                        actor.isFolded = true
                        actor.lastActionText = "FOLD"
                        actionDesc = "${actor.playerName} (${actor.position.roleName}): FOLD"
                    }
                    ActionType.CHECK -> {
                        actor.lastActionText = "CHECK"
                        actionDesc = "${actor.playerName} (${actor.position.roleName}): CHECK"
                    }
                    ActionType.CALL -> {
                        val callAmt = action.amountChips
                        val actualDeduct = callAmt.coerceAtMost(actor.chips)
                        actor.chips -= actualDeduct
                        actor.currentStreetBet += actualDeduct
                        currentPotChips += actualDeduct
                        if (action.isAllIn || actor.chips == 0L) {
                            actor.isAllIn = true
                            actor.lastActionText = "CALL ${PlayerReplayState.formatBb(callAmt, bb)} ALL-IN"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): CALL ${PlayerReplayState.formatBb(callAmt, bb)} ALL-IN"
                        } else {
                            actor.lastActionText = "CALL ${PlayerReplayState.formatBb(callAmt, bb)}"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): CALL ${PlayerReplayState.formatBb(callAmt, bb)}"
                        }
                    }
                    ActionType.BET -> {
                        val betAmt = action.amountChips
                        val actualDeduct = betAmt.coerceAtMost(actor.chips)
                        actor.chips -= actualDeduct
                        actor.currentStreetBet += actualDeduct
                        currentPotChips += actualDeduct
                        if (action.isAllIn || actor.chips == 0L) {
                            actor.isAllIn = true
                            actor.lastActionText = "BET ${PlayerReplayState.formatBb(betAmt, bb)} ALL-IN"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): BET ${PlayerReplayState.formatBb(betAmt, bb)} ALL-IN"
                        } else {
                            actor.lastActionText = "BET ${PlayerReplayState.formatBb(betAmt, bb)}"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): BET ${PlayerReplayState.formatBb(betAmt, bb)}"
                        }
                    }
                    ActionType.RAISE -> {
                        // totalToChips is the new total bet on this street
                        val toAmt = action.totalToChips
                        val additionalChips = (toAmt - actor.currentStreetBet).coerceAtLeast(action.amountChips).coerceAtMost(actor.chips)
                        actor.chips -= additionalChips
                        actor.currentStreetBet = toAmt
                        currentPotChips += additionalChips
                        if (action.isAllIn || actor.chips == 0L) {
                            actor.isAllIn = true
                            actor.lastActionText = "RAISE ${PlayerReplayState.formatBb(toAmt, bb)} ALL-IN"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): RAISE to ${PlayerReplayState.formatBb(toAmt, bb)} ALL-IN"
                        } else {
                            actor.lastActionText = "RAISE ${PlayerReplayState.formatBb(toAmt, bb)}"
                            actionDesc = "${actor.playerName} (${actor.position.roleName}): RAISE to ${PlayerReplayState.formatBb(toAmt, bb)}"
                        }
                    }
                    ActionType.ALL_IN -> {
                        val allInAmt = if (action.totalToChips > 0L) action.totalToChips else action.amountChips
                        val addChips = (allInAmt - actor.currentStreetBet).coerceAtLeast(0L).coerceAtMost(actor.chips)
                        actor.chips -= addChips
                        actor.currentStreetBet += addChips
                        currentPotChips += addChips
                        actor.isAllIn = true
                        actor.lastActionText = "ALL-IN ${PlayerReplayState.formatBb(actor.currentStreetBet, bb)}"
                        actionDesc = "${actor.playerName} (${actor.position.roleName}): ALL-IN ${PlayerReplayState.formatBb(actor.currentStreetBet, bb)}"
                    }
                    ActionType.SHOWS -> {
                        actor.revealedCards = action.shownCards
                        val cardsStr = action.shownCards.joinToString(" ") { it.displayText }
                        actor.lastActionText = "SHOWS $cardsStr"
                        actionDesc = "${actor.playerName} (${actor.position.roleName}): SHOWS [$cardsStr]"
                    }
                    ActionType.COLLECTED -> {
                        actor.isWinner = true
                        actor.wonAmount = action.amountChips
                        actor.chips += action.amountChips
                        actor.lastActionText = "WON ${PlayerReplayState.formatBb(action.amountChips, bb)}"
                        actionDesc = "🏆 ${actor.playerName} WINS ${PlayerReplayState.formatBb(action.amountChips, bb)}"
                    }
                }
            } else {
                actionDesc = action.rawText
            }

            // Visible board cards at this step
            val visibleBoard = buildList {
                if (currentStreet >= Street.FLOP) addAll(hand.boardFlop)
                if (currentStreet >= Street.TURN && hand.boardTurn != null) add(hand.boardTurn)
                if (currentStreet >= Street.RIVER && hand.boardRiver != null) add(hand.boardRiver)
            }

            result.add(
                HandStepState(
                    stepIndex = stepNumber,
                    totalSteps = totalSteps,
                    currentStreet = currentStreet,
                    currentPotChips = currentPotChips,
                    currentPotBb = if (bb > 0) currentPotChips.toDouble() / bb else 0.0,
                    boardCards = visibleBoard,
                    players = runtimePlayers.map { toPlayerReplayState(it, action.playerName) },
                    currentAction = action,
                    actionDescription = actionDesc,
                    winnerNotice = if (action.actionType == ActionType.COLLECTED) actionDesc else null
                )
            )
        }

        return result
    }
}
