package com.example.poker

import com.example.poker.model.ActionType
import com.example.poker.model.PositionRole
import com.example.poker.model.Rank
import com.example.poker.model.Street
import com.example.poker.model.Suit
import com.example.poker.parser.GGPokerParser
import com.example.poker.state.HandReplayerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GGPokerParserTest {

    private val sampleHandText = """
Poker Hand #TM6238545165: Tournament #301518242, Bounty Hunters Big One $1.08 Hold'em No Limit - Level27(8,000/16,000(2,500)) - 2026/07/30 16:54:16
Table '124' 8-max Seat #4 is the button
Seat 1: 9a1cc5e2 (278,352 in chips)
Seat 2: c79ea675 (780,616 in chips)
Seat 3: a3366cc0 (940,429 in chips)
Seat 4: Hero (296,807 in chips)
Seat 5: d31212e2 (379,782 in chips)
Seat 6: 151cc6e3 (789,912 in chips)
Seat 7: 3cd7480d (201,337 in chips)
Seat 8: 45e406a2 (488,791 in chips)
d31212e2: posts the ante 2,500
a3366cc0: posts the ante 2,500
45e406a2: posts the ante 2,500
3cd7480d: posts the ante 2,500
9a1cc5e2: posts the ante 2,500
Hero: posts the ante 2,500
c79ea675: posts the ante 2,500
151cc6e3: posts the ante 2,500
d31212e2: posts small blind 8,000
151cc6e3: posts big blind 16,000
*** HOLE CARDS ***
Dealt to 9a1cc5e2 
Dealt to c79ea675 
Dealt to a3366cc0 
Dealt to Hero [7c Kh]
Dealt to d31212e2 
Dealt to 151cc6e3 
Dealt to 3cd7480d 
Dealt to 45e406a2 
3cd7480d: folds
45e406a2: folds
9a1cc5e2: folds
c79ea675: folds
a3366cc0: folds
Hero: raises 278,307 to 294,307 and is all-in
d31212e2: calls 286,307
151cc6e3: folds
Hero: shows [7c Kh]
d31212e2: shows [9d 9c]
*** FLOP *** [2c As Qs]
*** TURN *** [2c As Qs] [2d]
*** RIVER *** [2c As Qs 2d] [2s]
*** SHOWDOWN ***
d31212e2 collected 624,614 from pot
*** SUMMARY ***
Total pot 624,614 | Rake 0 | Jackpot 0 | Bingo 0 | Fortune 0 | Tax 0
Board [2c As Qs 2d 2s]
Seat 1: 9a1cc5e2 folded before Flop
Seat 2: c79ea675 folded before Flop
Seat 3: a3366cc0 folded before Flop
Seat 4: Hero (button) showed [7c Kh] and lost with three of a kind, Twos
Seat 5: d31212e2 (small blind) showed [9d 9c] and won (624,614) with a full house, Twos full of Nines
Seat 6: 151cc6e3 (big blind) folded before Flop
Seat 7: 3cd7480d folded before Flop
Seat 8: 45e406a2 folded before Flop
    """.trimIndent()

    @Test
    fun testParseSingleHand() {
        val result = GGPokerParser.parseHandHistory(sampleHandText)
        assertEquals(1, result.hands.size)

        val hand = result.hands[0]
        assertEquals("TM6238545165", hand.handId)
        assertEquals("301518242", hand.tournamentId)
        assertEquals(27, hand.levelNumber)
        assertEquals(8000L, hand.smallBlind)
        assertEquals(16000L, hand.bigBlind)
        assertEquals(2500L, hand.ante)
        assertEquals("124", hand.tableNumber)
        assertEquals(8, hand.maxSeats)
        assertEquals(4, hand.buttonSeat)

        // Hero verification
        assertEquals("Hero", hand.heroName)
        assertEquals(2, hand.heroCards.size)
        assertEquals(Rank.SEVEN, hand.heroCards[0].rank)
        assertEquals(Suit.CLUBS, hand.heroCards[0].suit)
        assertEquals(Rank.KING, hand.heroCards[1].rank)
        assertEquals(Suit.HEARTS, hand.heroCards[1].suit)

        // Board cards
        assertEquals(3, hand.boardFlop.size)
        assertNotNull(hand.boardTurn)
        assertNotNull(hand.boardRiver)
        assertEquals(5, hand.allBoardCards.size)

        // Positions check according to Section 5:
        // Seat 4 = BTN (6)
        // Seat 5 = SB (7)
        // Seat 6 = BB (8)
        // Seat 7 = UTG2 (1)
        // Seat 8 = MP1 (2)
        // Seat 1 = MP2 (3)
        // Seat 2 = HJ (4)
        // Seat 3 = CO (5)
        val pMap = hand.players.associateBy { it.seatNumber }
        assertEquals(PositionRole.BTN, pMap[4]?.position)
        assertEquals(PositionRole.SB, pMap[5]?.position)
        assertEquals(PositionRole.BB, pMap[6]?.position)
        assertEquals(PositionRole.UTG2, pMap[7]?.position)
        assertEquals(PositionRole.MP1, pMap[8]?.position)
        assertEquals(PositionRole.MP2, pMap[1]?.position)
        assertEquals(PositionRole.HJ, pMap[2]?.position)
        assertEquals(PositionRole.CO, pMap[3]?.position)
    }

    @Test
    fun testReplayerSteps() {
        val result = GGPokerParser.parseHandHistory(sampleHandText)
        val hand = result.hands[0]
        val steps = HandReplayerEngine.computeSteps(hand)

        // Step 0: Initial state
        val step0 = steps[0]
        assertEquals(0, step0.stepIndex)
        assertFalse(step0.canStepBack)
        assertTrue(step0.canStepForward)
        assertEquals(Street.PREFLOP, step0.currentStreet)
        assertEquals(0, step0.boardCards.size)
        // Initial pot = 8 antes (2500*8 = 20,000) + SB (8,000) + BB (16,000) = 44,000
        assertEquals(44000L, step0.currentPotChips)

        // Final step: End of hand
        val lastStep = steps.last()
        assertEquals(steps.size - 1, lastStep.stepIndex)
        assertTrue(lastStep.canStepBack)
        assertFalse(lastStep.canStepForward)
        assertEquals(5, lastStep.boardCards.size)

        // Winner verification
        val winnerPlayer = lastStep.players.firstOrNull { it.isWinner }
        assertNotNull(winnerPlayer)
        assertEquals("d31212e2", winnerPlayer?.playerName)
        assertEquals(624614L, winnerPlayer?.wonAmountChips)
    }
}
