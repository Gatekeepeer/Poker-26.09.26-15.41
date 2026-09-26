package com.example.poker.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedStarParserTest {

    private val sampleRedStarXml = """
<root><general>
  <client_version>25.9.1.23</client_version>
  <mode>real</mode>
  <gametype>Holdem NL</gametype>
  <tablename>€2 MYSTERY [7-Max], 1153144089</tablename>
  <tournamentcurrency>EUR</tournamentcurrency>
  <duration>00:08:45</duration>
  <gamecount>9</gamecount>
  <startdate>2026-02-09 11:01:24</startdate>
  <currency>EUR</currency>
  <nickname>Adaptera</nickname>
  <bets>42,250</bets>
  <wins>0</wins>
  <chipsin>198,382</chipsin>
  <chipsout>156,432</chipsout>
  <statuspoints>0.00</statuspoints>
  <awardpoints>0.00</awardpoints>
  <ipoints>0.00</ipoints>
  <tablesize>7</tablesize>
  <tournamentcode>1152283265</tournamentcode>
  <tournamentname>€2 MYSTERY [7-Max]</tournamentname>
  <place>N/A</place>
  <buyin>€0.91 + €0.91 + €0.18</buyin>
  <totalbuyin>€2</totalbuyin>
  <win>N/A</win>
 </general><game gamecode="12033536056">
  <general>
   <startdate>2026-02-09 11:01:24</startdate>
   <smallblind>250</smallblind>
   <bigblind>500</bigblind>
   <ante>75</ante>
   <players>
    <player addon="0" bet="75" chips="100,000" dealer="1" name="dicerough18" rebuy="0" reg_code="" seat="1" win="0"/>
    <player addon="0" bet="325" chips="100,000" dealer="0" name="Agent71" rebuy="0" reg_code="" seat="3" win="0"/>
    <player addon="0" bet="575" chips="100,000" dealer="0" name="Adaptera" rebuy="0" reg_code="8566605031" seat="4" win="0"/>
    <player addon="0" bet="1,075" chips="102,700" dealer="0" name="godfreed1" rebuy="0" reg_code="" seat="9" win="0"/>
    <player addon="0" bet="3,000" chips="99,925" dealer="0" name="PokeRUShka01" rebuy="0" reg_code="" seat="10" win="3,125"/>
   </players>
  </general>
  <round no="0">
   <action no="1" player="Agent71" sum="75" type="15"/>
   <action no="2" player="Adaptera" sum="75" type="15"/>
   <action no="3" player="godfreed1" sum="75" type="15"/>
   <action no="4" player="PokeRUShka01" sum="75" type="15"/>
   <action no="5" player="dicerough18" sum="75" type="15"/>
   <action no="6" player="Agent71" sum="250" type="1"/>
   <action no="7" player="Adaptera" sum="500" type="2"/>
  </round>
  <round no="1">
   <cards player="godfreed1" type="Pocket">X X</cards>
   <action no="8" player="godfreed1" sum="1,000" type="23"/>
   <cards player="PokeRUShka01" type="Pocket">X X</cards>
   <action no="9" player="PokeRUShka01" sum="3,000" type="23"/>
   <cards player="dicerough18" type="Pocket">X X</cards>
   <action no="10" player="dicerough18" sum="0" type="0"/>
   <cards player="Agent71" type="Pocket">X X</cards>
   <action no="11" player="Agent71" sum="0" type="0"/>
   <cards player="Adaptera" type="Pocket">D3 C5</cards>
   <action no="12" player="Adaptera" sum="0" type="0"/>
   <action no="13" player="godfreed1" sum="0" type="0"/>
  </round>
 </game></root>
    """.trimIndent()

    @Test
    fun testIsRedStarFormat() {
        assertTrue(RedStarParser.isRedStarFormat(sampleRedStarXml))
    }

    @Test
    fun testParseRedStarCards() {
        val cards = RedStarParser.parseRedStarCards("D3 C5")
        assertEquals(2, cards.size)
        assertEquals("3d", cards[0].code)
        assertEquals("5c", cards[1].code)

        val cards10 = RedStarParser.parseRedStarCards("S10 H3")
        assertEquals(2, cards10.size)
        assertEquals("Ts", cards10[0].code)
        assertEquals("3h", cards10[1].code)
    }

    @Test
    fun testParseRedStarSingleGame() {
        val result = RedStarParser.parseHandHistory(sampleRedStarXml)
        assertEquals(1, result.hands.size)

        val hand = result.hands[0]
        assertEquals("12033536056", hand.handId)
        assertEquals(250L, hand.smallBlind)
        assertEquals(500L, hand.bigBlind)
        assertEquals(75L, hand.ante)
        assertEquals("Adaptera", hand.heroName)

        assertEquals(2, hand.heroCards.size)
        assertEquals("3d", hand.heroCards[0].code)
        assertEquals("5c", hand.heroCards[1].code)

        val heroState = hand.players.firstOrNull { it.isHero }
        assertNotNull(heroState)
        assertEquals("Adaptera", heroState?.playerName)
        assertEquals(500L, heroState?.blindPosted)

        // Verify winner and final action
        assertEquals(1, hand.winners.size)
        assertEquals("PokeRUShka01", hand.winners[0].playerName)
        assertEquals(3125L, hand.winners[0].amountChips)

        val lastAction = hand.actions.lastOrNull()
        assertNotNull(lastAction)
        assertEquals(com.example.poker.model.ActionType.COLLECTED, lastAction?.actionType)
        assertEquals("PokeRUShka01", lastAction?.playerName)
        assertEquals(3125L, lastAction?.amountChips)
    }

    @Test
    fun testParseRedStarAllInRunoutWithShowdownAndWinner() {
        val allInXml = """
<root><general>
  <client_version>25.9.1.23</client_version>
  <mode>real</mode>
  <gametype>Holdem NL</gametype>
  <tablename>€2 MYSTERY [7-Max], 1153144089</tablename>
  <tournamentcurrency>EUR</tournamentcurrency>
  <duration>00:08:45</duration>
  <gamecount>1</gamecount>
  <startdate>2026-02-09 11:01:24</startdate>
  <currency>EUR</currency>
  <nickname>Adaptera</nickname>
  <tablesize>7</tablesize>
  <tournamentcode>1152283265</tournamentcode>
  <tournamentname>€2 MYSTERY [7-Max]</tournamentname>
  <totalbuyin>€2</totalbuyin>
 </general><game gamecode="12033583763">
  <general>
   <startdate>2026-02-09 11:22:38</startdate>
   <smallblind>350</smallblind>
   <bigblind>700</bigblind>
   <ante>100</ante>
   <players>
    <player addon="0" bet="9,180" chips="195,180" dealer="1" name="Tolik12" rebuy="0" reg_code="" seat="1" win="20,960"/>
    <player addon="0" bet="9,080" chips="9,180" dealer="0" name="BISHBASHBOSH73" rebuy="0" reg_code="" seat="3" win="0"/>
    <player addon="0" bet="800" chips="99,720" dealer="0" name="totone007" rebuy="0" reg_code="" seat="4" win="0"/>
    <player addon="0" bet="100" chips="87,960" dealer="0" name="sicjoker1" rebuy="0" reg_code="" seat="6" win="0"/>
    <player addon="0" bet="100" chips="85,169" dealer="0" name="Adaptera" rebuy="0" reg_code="8566605031" seat="8" win="0"/>
    <player addon="0" bet="100" chips="114,039" dealer="0" name="Agent71" rebuy="0" reg_code="" seat="9" win="0"/>
    <player addon="0" bet="1,500" chips="168,012" dealer="0" name="VitaliyDZ" rebuy="0" reg_code="" seat="10" win="0"/>
   </players>
  </general>
  <round no="0">
   <action no="1" player="BISHBASHBOSH73" sum="100" type="15"/>
   <action no="2" player="totone007" sum="100" type="15"/>
   <action no="3" player="sicjoker1" sum="100" type="15"/>
   <action no="4" player="Adaptera" sum="100" type="15"/>
   <action no="5" player="Agent71" sum="100" type="15"/>
   <action no="6" player="VitaliyDZ" sum="100" type="15"/>
   <action no="7" player="Tolik12" sum="100" type="15"/>
   <action no="8" player="BISHBASHBOSH73" sum="350" type="1"/>
   <action no="9" player="totone007" sum="700" type="2"/>
  </round>
  <round no="1">
   <cards player="sicjoker1" type="Pocket">X X</cards>
   <action no="10" player="sicjoker1" sum="0" type="0"/>
   <cards player="Adaptera" type="Pocket">D3 SK</cards>
   <action no="11" player="Adaptera" sum="0" type="0"/>
   <cards player="Agent71" type="Pocket">X X</cards>
   <action no="12" player="Agent71" sum="0" type="0"/>
   <cards player="VitaliyDZ" type="Pocket">X X</cards>
   <action no="13" player="VitaliyDZ" sum="1,400" type="23"/>
   <cards player="Tolik12" type="Pocket">C6 HK</cards>
   <action no="14" player="Tolik12" sum="1,400" type="3"/>
   <cards player="BISHBASHBOSH73" type="Pocket">S3 S8</cards>
   <action no="15" player="BISHBASHBOSH73" sum="9,080" type="23"/>
   <cards player="totone007" type="Pocket">X X</cards>
   <action no="16" player="totone007" sum="0" type="0"/>
   <action no="17" player="VitaliyDZ" sum="0" type="0"/>
   <action no="18" player="Tolik12" sum="7,680" type="3"/>
  </round>
  <round no="2">
   <cards type="Flop">S7 C7 S10</cards>
  </round>
  <round no="3">
   <cards type="Turn">CA</cards>
  </round>
  <round no="4">
   <cards type="River">CK</cards>
  </round>
 </game></root>
        """.trimIndent()

        val parsed = RedStarParser.parseHandHistory(allInXml)
        assertEquals(1, parsed.hands.size)
        val hand = parsed.hands[0]

        // Community cards were parsed
        assertEquals(3, hand.boardFlop.size)
        assertNotNull(hand.boardTurn)
        assertNotNull(hand.boardRiver)

        // Actions contain flop, turn, river deals, showdown, and winner collection
        assertTrue(hand.actions.any { it.street == com.example.poker.model.Street.FLOP })
        assertTrue(hand.actions.any { it.street == com.example.poker.model.Street.TURN })
        assertTrue(hand.actions.any { it.street == com.example.poker.model.Street.RIVER })
        assertTrue(hand.actions.any { it.actionType == com.example.poker.model.ActionType.SHOWS })
        
        val lastAction = hand.actions.last()
        assertEquals(com.example.poker.model.ActionType.COLLECTED, lastAction.actionType)
        assertEquals("Tolik12", lastAction.playerName)
        assertEquals(20960L, lastAction.amountChips)

        // HandReplayerEngine generates steps with all-in runout and final winner step
        val steps = com.example.poker.state.HandReplayerEngine.computeSteps(hand)
        val finalStep = steps.last()
        val winnerPlayer = finalStep.players.firstOrNull { it.playerName == "Tolik12" }
        assertNotNull(winnerPlayer)
        assertTrue(winnerPlayer?.isWinner == true)
        assertEquals(5, finalStep.boardCards.size)
    }
}
