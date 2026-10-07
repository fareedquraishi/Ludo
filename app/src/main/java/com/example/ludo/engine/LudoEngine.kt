package com.example.ludo.engine

import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Token

/** Pure Kotlin, no Android/Compose imports: easy to unit test. Dice values are passed in. */
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

    /** True when the player on turn has at least one token that could use [value]. */
    private fun usable(s: LudoGameState, value: Int): Boolean =
        s.tokens.any { it.color == s.current && canMove(it, value) }

    /** Tokens that can spend the selected number right now (they get the highlight ring). */
    fun movableTokens(s: LudoGameState): List<Token> {
        val value = s.selected ?: return emptyList()
        if (!s.awaitingMove) return emptyList()
        return s.tokens.filter { it.color == s.current && canMove(it, value) }
    }

    /**
     * Throws one roll and keeps the number in the queue. A six earns another roll (unless the
     * six limit is reached). When no roll is left the spend phase begins.
     *
     * autoPass = true: with nothing playable the turn passes immediately (used by tests).
     * autoPass = false: the state is marked noMove so the UI can show it for a moment,
     * then call passAfterNoMove().
     */
    fun roll(s: LudoGameState, value: Int, autoPass: Boolean = true): LudoGameState {
        require(value in 1..6)
        if (!s.canRoll) return s
        val streak = if (value == 6) s.sixStreak + 1 else 0
        val limit = s.rules.sixLimit
        val sixBonus = value == 6 && !(limit != null && streak >= limit)
        val pending = s.pendingRolls - 1 + (if (sixBonus) 1 else 0)
        val rolled = s.copy(
            queue = s.queue + value,
            lastRoll = value,
            sixStreak = streak,
            pendingRolls = pending,
            notice = null,
            log = s.log + "${s.current.label} rolled $value",
        )
        return if (pending > 0) rolled else beginSpend(rolled, autoPass)
    }

    /** No roll left: pick the first number that can be used, or end the turn if none can. */
    private fun beginSpend(s: LudoGameState, autoPass: Boolean): LudoGameState {
        val first = s.queue.firstOrNull { usable(s, it) }
        return when {
            first != null -> s.copy(awaitingMove = true, selected = first)
            autoPass -> passTurn(s.copy(log = s.log + "${s.current.label}: no move"))
            else -> s.copy(awaitingMove = false, noMove = true, selected = null, log = s.log + "${s.current.label}: no move")
        }
    }

    /** The player taps a die in the dice row: the next token tap spends that number. */
    fun select(s: LudoGameState, value: Int): LudoGameState =
        if (s.awaitingMove && !s.busy && value in s.queue && usable(s, value)) s.copy(selected = value) else s

    /** Used when a human's turn timer runs out and the setting is "skip turn". */
    fun skipTurn(s: LudoGameState, reason: String): LudoGameState =
        if (s.winner != null) s else passTurn(s.copy(log = s.log + reason))

    fun passAfterNoMove(s: LudoGameState): LudoGameState = if (s.noMove) passTurn(s) else s

    /** One hop of the move animation: shows the token at [progress]; nothing else changes yet. */
    fun withTokenProgress(s: LudoGameState, color: PlayerColor, index: Int, progress: Int) = s.copy(
        tokens = s.tokens.map { if (it.color == color && it.index == index) it.copy(progress = progress) else it },
        awaitingMove = false,
        busy = true,
    )

    /** Removes one copy of [value] from the dice row. */
    fun withoutValue(queue: List<Int>, value: Int): List<Int> {
        val i = queue.indexOf(value)
        return if (i < 0) queue else queue.filterIndexed { idx, _ -> idx != i }
    }

    /** Spends [value] (default: the selected number) on the token with [tokenIndex]. */
    fun move(s: LudoGameState, tokenIndex: Int, value: Int? = s.selected): LudoGameState {
        val v = value ?: return s
        if (!s.awaitingMove || s.winner != null || v !in s.queue) return s
        val token = s.tokens.firstOrNull { it.color == s.current && it.index == tokenIndex } ?: return s
        if (!canMove(token, v)) return s

        val moved = token.copy(progress = if (token.inBase) 1 else token.progress + v)
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
        val queue = withoutValue(s.queue, v)
        var log = s.log
        if (victims.isNotEmpty()) log = log + "${s.current.label} captured ${victims.size} token(s)"
        if (moved.isHome) log = log + "${s.current.label} token ${token.index + 1} is home"

        if (tokens.filter { it.color == s.current }.all { it.isHome }) {
            return s.copy(
                tokens = tokens, queue = emptyList(), selected = null, awaitingMove = false,
                pendingRolls = 0, winner = s.current,
                log = log + "${s.current.label} wins!",
            )
        }

        // A capture or a token reaching home can earn another roll (if those rules are on).
        var bonus = 0
        if (victims.isNotEmpty() && s.rules.captureBonus) bonus++
        if (moved.isHome && s.rules.homeBonus) bonus++

        val next = s.copy(
            tokens = tokens, queue = queue, selected = null, awaitingMove = false,
            pendingRolls = bonus, log = log,
        )
        return when {
            bonus > 0 -> next                      // roll first; the numbers left in the row wait
            queue.isEmpty() -> passTurn(next)
            else -> beginSpend(next, autoPass = false)
        }
    }

    private fun passTurn(s: LudoGameState) = s.copy(
        turnIndex = (s.turnIndex + 1) % s.players.size,
        queue = emptyList(),
        pendingRolls = 1,
        selected = null,
        lastRoll = null,
        awaitingMove = false,
        noMove = false,
        sixStreak = 0,
    )

    /** Simple bot: capture > finish > leave base > safe square > furthest token. Returns (token, number). */
    fun chooseAiMove(s: LudoGameState): Pair<Token, Int>? {
        if (!s.awaitingMove) return null
        var best: Pair<Token, Int>? = null
        var bestScore = Int.MIN_VALUE
        for (value in s.queue.distinct()) {
            for (t in s.tokens) {
                if (t.color != s.current || !canMove(t, value)) continue
                val np = if (t.inBase) 1 else t.progress + value
                val sq = t.copy(progress = np).trackSquare
                var score = np
                if (sq != null && sq !in SAFE_SQUARES &&
                    s.tokens.any { it.color != t.color && it.trackSquare == sq }
                ) score += 100
                if (np == Token.HOME) score += 80
                if (t.inBase) score += 60
                if (sq != null && sq in SAFE_SQUARES) score += 30
                if (score > bestScore) {
                    bestScore = score
                    best = t to value
                }
            }
        }
        return best
    }
}
