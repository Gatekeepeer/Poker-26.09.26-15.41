package com.example.poker.data

import com.example.poker.model.PokerHand
import com.example.poker.model.TournamentData
import com.example.poker.parser.GGPokerParser

object DemoTournaments {

    private const val DEMO_HAND_1 = """
Poker Hand #TM12345678: Tournament #998877, Bounty Hunters Big One $1.08 Hold'em No Limit - Level 26 (6,000 / 12,000, ante 1,800)
Table '124' 8-max Seat #3 is the button
Seat 1: 154133d5 (145,210 in chips, $2.14 bounty)
Seat 2: UTG_Solid (280,000 in chips)
Seat 3: 3cd7480d (199,680 in chips, $3.50 bounty)
Seat 4: 45e406a2 (521,880 in chips)
Seat 5: 9a1cc5e2 (311,040 in chips)
Seat 6: Hero_Shark (450,000 in chips, $5.00 bounty)
Seat 7: 77a88b9c (185,000 in chips)
Seat 8: 88bb99cc (220,000 in chips)
154133d5: posts the ante 1800
UTG_Solid: posts the ante 1800
3cd7480d: posts the ante 1800
45e406a2: posts the ante 1800
9a1cc5e2: posts the ante 1800
Hero_Shark: posts the ante 1800
77a88b9c: posts the ante 1800
88bb99cc: posts the ante 1800
45e406a2: posts small blind 6000
9a1cc5e2: posts big blind 12000
*** HOLE CARDS ***
Dealt to Hero_Shark [Ah Kh]
Hero_Shark: raises 14400 to 26400
77a88b9c: folds
88bb99cc: folds
154133d5: folds
UTG_Solid: folds
3cd7480d: calls 26400
45e406a2: folds
9a1cc5e2: calls 14400
*** FLOP *** [Kd 7h 2c]
9a1cc5e2: checks
Hero_Shark: bets 32000
3cd7480d: calls 32000
9a1cc5e2: folds
*** TURN *** [Kd 7h 2c] [As]
Hero_Shark: bets 68000
3cd7480d: calls 68000
*** RIVER *** [Kd 7h 2c As] [Jh]
Hero_Shark: bets 160000
3cd7480d: calls 73280 and is all-in
Hero_Shark: shows [Ah Kh] (two pair, Aces and Kings)
3cd7480d: shows [Kc Qc] (a pair of Kings)
Hero_Shark collected 442160 from pot
*** SUMMARY ***
Total pot 442160 | Rake 0
Board [Kd 7h 2c As Jh]
Seat 3: 3cd7480d (button) showed [Kc Qc] and lost with a pair of Kings
Seat 6: Hero_Shark showed [Ah Kh] and won (442160) with two pair, Aces and Kings
"""

    private const val DEMO_HAND_2 = """
Poker Hand #TM12345679: Tournament #998877, Bounty Hunters Big One $1.08 Hold'em No Limit - Level 27 (7,000 / 14,000, ante 2,000)
Table '124' 8-max Seat #4 is the button
Seat 1: 154133d5 (138,010 in chips)
Seat 2: UTG_Solid (272,000 in chips)
Seat 4: 45e406a2 (508,480 in chips)
Seat 5: 9a1cc5e2 (278,040 in chips)
Seat 6: Hero_Shark (712,480 in chips)
Seat 7: 77a88b9c (178,000 in chips)
Seat 8: 88bb99cc (213,000 in chips)
154133d5: posts the ante 2000
UTG_Solid: posts the ante 2000
45e406a2: posts the ante 2000
9a1cc5e2: posts the ante 2000
Hero_Shark: posts the ante 2000
77a88b9c: posts the ante 2000
88bb99cc: posts the ante 2000
9a1cc5e2: posts small blind 7000
Hero_Shark: posts big blind 14000
*** HOLE CARDS ***
Dealt to Hero_Shark [Qs Qd]
77a88b9c: folds
88bb99cc: raises 16000 to 30000
154133d5: folds
UTG_Solid: folds
45e406a2: folds
9a1cc5e2: folds
Hero_Shark: raises 65000 to 95000
88bb99cc: calls 65000
*** FLOP *** [Qc 9d 4s]
Hero_Shark: bets 55000
88bb99cc: raises 61000 to 116000 and is all-in
Hero_Shark: calls 61000
*** TURN *** [Qc 9d 4s] [8c]
*** RIVER *** [Qc 9d 4s 8c] [2d]
88bb99cc: shows [Jc Jh] (a pair of Jacks)
Hero_Shark: shows [Qs Qd] (three of a kind, Queens)
Hero_Shark collected 443000 from pot
*** SUMMARY ***
Total pot 443000
Board [Qc 9d 4s 8c 2d]
Seat 6: Hero_Shark showed [Qs Qd] and won (443000)
Seat 8: 88bb99cc showed [Jc Jh] and lost
"""

    fun getDemoTournament(): TournamentData {
        val hands = listOfNotNull(
            GGPokerParser.parseSingleHand(DEMO_HAND_1),
            GGPokerParser.parseSingleHand(DEMO_HAND_2)
        )
        return TournamentData(
            id = "demo_tournament",
            name = "Демо-турнир (Bounty Hunters)",
            hands = hands,
            sourceFormat = "GGPoker"
        )
    }
}
