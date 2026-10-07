package com.example.ludo.engine

import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Seats
import com.example.ludo.model.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoEngineTest {

    private fun game(rules: Rules = Rules()) = LudoEngine.newGame(Seats.of(4, PlayerColor.GREEN), rules)

    private fun LudoGameState.put(color: PlayerColor, index: Int, progress: Int) =
        copy(tokens = tokens.map { if (it.color == color && it.index == index) it.copy(progress = progress) else it })

    @Test fun lastTrackSquareBeforeEachHomeColumn() {
        // matches the original: green 52 -> 100.., red 13 -> 200.., blue 26 -> 300.., yellow 39 -> 400..
        val expected = mapOf(PlayerColor.GREEN to 52, PlayerColor.RED to 13, PlayerColor.BLUE to 26, PlayerColor.YELLOW to 39)
        expected.forEach { (c, sq) -> assertEquals(sq, Token(c, 0, 51).trackSquare) }
        PlayerColor.entries.forEach { assertEquals(it.startSquare, Token(it, 0, 1).trackSquare) }
    }

    @Test fun needsSixToLeaveBase_andSixGivesAnotherRoll() {
        var s = LudoEngine.roll(game(), 3)
        assertEquals(PlayerColor.RED, s.current)               // no move -> turn passed
        s = LudoEngine.roll(game(), 6)
        assertEquals(PlayerColor.GREEN, s.current)             // a six earns another roll first
        assertEquals(1, s.pendingRolls)
        assertFalse(s.awaitingMove)                            // no moving until every roll is thrown
        s = LudoEngine.roll(s, 3)
        assertEquals(listOf(6, 3), s.queue)
        assertTrue(s.awaitingMove)
        s = LudoEngine.move(s, 0)                              // the 6 (selected first) leaves base
        assertEquals(2, s.tokens.first { it.color == PlayerColor.GREEN && it.index == 0 }.trackSquare)
        assertEquals(listOf(3), s.queue)
        assertEquals(PlayerColor.GREEN, s.current)             // the 3 is still to spend
        s = LudoEngine.move(s, 0)
        assertEquals(PlayerColor.RED, s.current)               // row empty -> turn passes
    }

    @Test fun valuesStayInTheRowUntilUsed() {
        var s = game().put(PlayerColor.GREEN, 0, 10)
        s = LudoEngine.roll(s, 6)
        s = LudoEngine.roll(s, 4)
        assertEquals(listOf(6, 4), s.queue)
        assertEquals(6, s.selected)
        s = LudoEngine.select(s, 4)
        assertEquals(4, s.selected)
        s = LudoEngine.move(s, 0)                              // spends the 4 on token 0
        assertEquals(14, s.tokens.first { it.color == PlayerColor.GREEN && it.index == 0 }.progress)
        assertEquals(listOf(6), s.queue)
        assertEquals(PlayerColor.GREEN, s.current)
        assertTrue(s.awaitingMove)
    }

    @Test fun capturesOnNormalSquare() {
        // green p=4 -> square 5; roll 3 lands on square 8. red p=46 is on square 8.
        var s = game().put(PlayerColor.GREEN, 0, 4).put(PlayerColor.RED, 0, 46)
        s = LudoEngine.move(LudoEngine.roll(s, 3), 0)
        assertEquals(Token.BASE, s.tokens.first { it.color == PlayerColor.RED && it.index == 0 }.progress)
        assertEquals(PlayerColor.RED, s.current)               // no capture bonus by default
    }

    @Test fun captureBonusGivesAnotherRoll() {
        var s = game(Rules(captureBonus = true)).put(PlayerColor.GREEN, 0, 4).put(PlayerColor.RED, 0, 46)
        s = LudoEngine.move(LudoEngine.roll(s, 3), 0)
        assertEquals(PlayerColor.GREEN, s.current)
        assertEquals(1, s.pendingRolls)
        assertTrue(s.canRoll)
    }

    @Test fun homeBonusGivesAnotherRoll() {
        var s = game(Rules(homeBonus = true)).put(PlayerColor.GREEN, 0, 55)
        s = LudoEngine.move(LudoEngine.roll(s, 2), 0)
        assertTrue(s.tokens.first { it.color == PlayerColor.GREEN && it.index == 0 }.isHome)
        assertEquals(PlayerColor.GREEN, s.current)
        assertTrue(s.canRoll)
    }

    @Test fun noCaptureOnSafeSquare() {
        // green p=6 -> square 7; roll 3 lands on square 10 (safe). red p=48 is on square 10.
        var s = game().put(PlayerColor.GREEN, 0, 6).put(PlayerColor.RED, 0, 48)
        s = LudoEngine.move(LudoEngine.roll(s, 3), 0)
        assertEquals(48, s.tokens.first { it.color == PlayerColor.RED && it.index == 0 }.progress)
    }

    @Test fun exactRollNeededToFinish() {
        val s = game().put(PlayerColor.GREEN, 0, 55)
        assertTrue(LudoEngine.movableTokens(LudoEngine.roll(s, 3)).isEmpty())
        val ok = LudoEngine.roll(s, 2)
        assertEquals(listOf(0), LudoEngine.movableTokens(ok).map { it.index })
    }

    @Test fun winnerWhenAllFourAreHome() {
        var s = game()
        (0..2).forEach { s = s.put(PlayerColor.GREEN, it, 57) }
        s = s.put(PlayerColor.GREEN, 3, 56)
        s = LudoEngine.move(LudoEngine.roll(s, 1), 3)
        assertEquals(PlayerColor.GREEN, s.winner)
    }

    @Test fun sixLimitStopsFurtherRolls() {
        var s = game(Rules(sixLimit = 2))
        s = LudoEngine.roll(s, 6)                              // streak 1 -> another roll
        assertEquals(1, s.pendingRolls)
        s = LudoEngine.roll(s, 6)                              // streak 2 -> limit reached, no more rolls
        assertEquals(0, s.pendingRolls)
        assertEquals(listOf(6, 6), s.queue)
        assertTrue(s.awaitingMove)
        s = LudoEngine.move(s, 0)
        s = LudoEngine.move(s, 1)
        assertEquals(PlayerColor.RED, s.current)
    }

    @Test fun seatsFollowChosenColour() {
        assertEquals(listOf(PlayerColor.RED, PlayerColor.YELLOW), Seats.of(2, PlayerColor.RED))
        assertEquals(listOf(PlayerColor.GREEN, PlayerColor.BLUE), Seats.of(2, PlayerColor.BLUE))
        assertEquals(listOf(PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW), Seats.of(3, PlayerColor.BLUE))
        assertEquals(listOf(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW), Seats.of(3, PlayerColor.YELLOW))
        assertEquals(4, Seats.of(4, PlayerColor.RED).size)
    }

    @Test fun noMoveWaitsBeforePassing() {
        val s = LudoEngine.roll(game(), 3, autoPass = false)
        assertTrue(s.noMove)
        assertEquals(PlayerColor.GREEN, s.current)                       // still green while the roll is shown
        assertEquals(PlayerColor.RED, LudoEngine.passAfterNoMove(s).current)
    }

    @Test fun startIndexDecidesWhoGoesFirst() {
        val s = LudoEngine.newGame(Seats.of(4, PlayerColor.GREEN), startIndex = 2)
        assertEquals(PlayerColor.BLUE, s.current)
    }
}
