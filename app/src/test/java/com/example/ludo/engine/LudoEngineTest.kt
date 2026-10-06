package com.example.ludo.engine

import com.example.ludo.model.GameMode
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoEngineTest {

    private fun game(rules: Rules = Rules()) = LudoEngine.newGame(GameMode.FOUR, rules)

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
        s = LudoEngine.move(s, 0)
        assertEquals(2, s.tokens.first { it.color == PlayerColor.GREEN && it.index == 0 }.trackSquare)
        assertEquals(PlayerColor.GREEN, s.current)             // bonus roll
    }

    @Test fun capturesOnNormalSquare() {
        // green p=4 -> square 5; roll 3 lands on square 8. red p=46 is on square 8.
        var s = game().put(PlayerColor.GREEN, 0, 4).put(PlayerColor.RED, 0, 46)
        s = LudoEngine.move(LudoEngine.roll(s, 3), 0)
        assertEquals(Token.BASE, s.tokens.first { it.color == PlayerColor.RED && it.index == 0 }.progress)
        assertEquals(PlayerColor.RED, s.current)               // no capture bonus by default
    }

    @Test fun noCaptureOnSafeSquare() {
        // green p=6 -> square 7; roll 3 lands on square 10 (safe). red p=48 is on square 10.
        var s = game().put(PlayerColor.GREEN, 0, 6).put(PlayerColor.RED, 0, 48)
        s = LudoEngine.move(LudoEngine.roll(s, 3), 0)
        assertEquals(48, s.tokens.first { it.color == PlayerColor.RED && it.index == 0 }.progress)
    }

    @Test fun exactRollNeededToFinish() {
        val s = game().put(PlayerColor.GREEN, 0, 55)
        assertNull(LudoEngine.movableTokens(LudoEngine.roll(s, 3)).firstOrNull { it.index == 0 })
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

    @Test fun sixLimitPassesTurn() {
        var s = game(Rules(sixLimit = 2))
        s = LudoEngine.move(LudoEngine.roll(s, 6), 0)          // streak 1 -> bonus
        assertEquals(PlayerColor.GREEN, s.current)
        s = LudoEngine.move(LudoEngine.roll(s, 6), 1)          // streak 2 -> limit reached
        assertEquals(PlayerColor.RED, s.current)
    }
}
