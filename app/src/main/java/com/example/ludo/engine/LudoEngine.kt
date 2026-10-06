package com.example.ludo.engine

import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Token

/** Pure Kotlin, no Android/Compose imports: easy to unit test. Dice value is passed in. */
object LudoEngine {

    /** Squares with data-blocksp="1" in the original: the four starts plus four "star" squares. */
    val SAFE_SQUARES = setOf(2, 10, 15, 23, 28, 36, 41, 49)

    fun newGame(
        players: List<PlayerColor>,
        rules: Rules = Rules(),
        bots: Set<PlayerColor> = emptySet(),
        startIndex: Int = 0,
    ) = LudoGameState(
        players = players,
        rules = rules,
        bots = bots,
        tokens = players.flatMap { c -> List(4) { Token(c, it) } },
        turnIndex = startIndex,
    )

    fun canMove(t: Token, dice: Int): Boolean = when {
        t.isHome -> false
        t.inBase -> dice == 6                      // need a six to leave base
        else -> t.progress + dice <= Token.HOME    // exact roll needed to finish
    }

    fun movableTokens(s: LudoGameState): List<Token> {
        val dice = s.dice ?: return emptyList()
        if (!s.awaitingMove) return emptyList()
        return s.tokens.filter { it.color == s.current && canMove(it, dice) }
    }

    /**
     * autoPass = true: with no legal move the turn passes immediately (used by tests).
     * autoPass = false: the state is marked noMove so the UI can show the roll for a moment,
     * then call passAfterNoMove().
     */
    fun roll(s: LudoGameState, value: Int, autoPass: Boolean = true): LudoGameState {
        require(value in 1..6)
        if (!s.canRoll) return s
        val rolled = s.copy(
            dice = value,
            diceBy = s.current,
            sixStreak = if (value == 6) s.sixStreak + 1 else 0,
            notice = null,
        )
        val hasMove = s.tokens.any { it.color == s.current && canMove(it, value) }
        return when {
            hasMove -> rolled.copy(awaitingMove = true, log = s.log + "${s.current.label} rolled $value")
            autoPass -> passTurn(rolled.copy(log = s.log + "${s.current.label} rolled $value - no move"))
            else -> rolled.copy(noMove = true, log = s.log + "${s.current.label} rolled $value - no move")
        }
    }

    fun passAfterNoMove(s: LudoGameState): LudoGameState = if (s.noMove) passTurn(s) else s

    /** One hop of the move animation: shows the token at [progress]; nothing else changes yet. */
    fun withTokenProgress(s: LudoGameState, color: PlayerColor, index: Int, progress: Int) = s.copy(
        tokens = s.tokens.map { if (it.color == color && it.index == index) it.copy(progress = progress) else it },
        awaitingMove = false,
        busy = true,
    )

    fun move(s: LudoGameState, tokenIndex: Int): LudoGameState {
        val dice = s.dice ?: return s
        if (!s.awaitingMove || s.winner != null) return s
        val token = s.tokens.firstOrNull { it.color == s.current && it.index == tokenIndex } ?: return s
        if (!canMove(token, dice)) return s

        val moved = token.copy(progress = if (token.inBase) 1 else token.progress + dice)
        val square = moved.trackSquare
        val victims = if (square == null || square in SAFE_SQUARES) emptyList()
        else s.tokens.filter { it.color != token.color && it.trackSquare == square }

        val tokens = s.tokens.map { t ->
            when {
                t.color == token.color && t.index == token.index -> moved
                t in victims -> t.copy(progress = Token.BASE)
                else -> t
            }
        }
        var log = s.log
        if (victims.isNotEmpty()) log = log + "${s.current.label} captured ${victims.size} token(s)"
        if (moved.isHome) log = log + "${s.current.label} token ${token.index + 1} is home"

        if (tokens.filter { it.color == s.current }.all { it.isHome }) {
            return s.copy(
                tokens = tokens, awaitingMove = false, winner = s.current,
                log = log + "${s.current.label} wins!",
            )
        }

        val limit = s.rules.sixLimit
        val sixBonus = dice == 6 && !(limit != null && s.sixStreak >= limit)
        val extra = sixBonus ||
            (victims.isNotEmpty() && s.rules.captureBonus) ||
            (moved.isHome && s.rules.homeBonus)

        val next = s.copy(tokens = tokens, awaitingMove = false, log = log)
        return if (extra) next else passTurn(next)
    }

    private fun passTurn(s: LudoGameState) = s.copy(
        turnIndex = (s.turnIndex + 1) % s.players.size,
        awaitingMove = false,
        noMove = false,
        sixStreak = 0,
    )

    /** Simple bot: capture > finish > leave base > safe square > furthest token. */
    fun chooseAiMove(s: LudoGameState): Token? {
        val dice = s.dice ?: return null
        return movableTokens(s).maxByOrNull { t ->
            val np = if (t.inBase) 1 else t.progress + dice
            val sq = t.copy(progress = np).trackSquare
            var score = np
            if (sq != null && sq !in SAFE_SQUARES &&
                s.tokens.any { it.color != t.color && it.trackSquare == sq }
            ) score += 100
            if (np == Token.HOME) score += 80
            if (t.inBase) score += 60
            if (sq != null && sq in SAFE_SQUARES) score += 30
            score
        }
    }
}
