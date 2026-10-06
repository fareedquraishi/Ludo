package com.example.ludo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.engine.LudoEngine
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel : ViewModel() {

    private companion object {
        // Pacing ("Normal" speed), in milliseconds
        const val THINK = 1000L      // banner shows, computer "thinks" before rolling
        const val DICE = 1000L       // computer's dice number stays visible before it moves
        const val NO_MOVE = 1100L    // "rolled N: no move" stays visible before the turn passes
        const val STEP = 230L        // time per square while a token hops
        const val CAPTURE = 350L     // beat at the destination when something is captured
        const val SETTLE = 120L      // beat at the destination otherwise
    }

    private val rng = Random.Default
    private val _state = MutableStateFlow<LudoGameState?>(null) // null = setup screen
    val state: StateFlow<LudoGameState?> = _state.asStateFlow()
    private var job: Job? = null

    fun start(players: List<PlayerColor>, rules: Rules, bots: Set<PlayerColor>) {
        val first = rng.nextInt(players.size)
        val who = players[first]
        val label = when {
            who in bots -> "${who.label} (computer)"
            bots.isNotEmpty() -> "You (${who.label})"
            else -> who.label
        }
        _state.value = LudoEngine.newGame(players, rules, bots, first).copy(
            notice = "$label starts (random draw)",
            log = listOf("Random draw: ${who.label} starts"),
        )
        launchSequence { }   // if a computer starts, the bot loop takes over
    }

    fun reset() {
        job?.cancel()
        _state.value = null
    }

    /** Human taps "Roll dice". */
    fun rollDice() {
        val s = _state.value ?: return
        if (s.current in s.bots || !s.canRoll) return
        launchSequence { doRoll() }
    }

    /** Human taps a highlighted token. */
    fun onTokenTap(token: Token) {
        val s = _state.value ?: return
        if (s.current in s.bots || !s.awaitingMove || s.busy || token.color != s.current) return
        launchSequence { doMove(token.index) }
    }

    private fun launchSequence(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            block()
            runBots()
        }
    }

    private suspend fun doRoll() {
        _state.update { it?.let { st -> LudoEngine.roll(st, rng.nextInt(1, 7), autoPass = false) } }
        val after = _state.value ?: return
        if (after.noMove) {
            delay(NO_MOVE)
            _state.update { it?.let { st -> LudoEngine.passAfterNoMove(st) } }
        }
    }

    /** Hops the token square by square, then applies the real move (capture, turn change, win). */
    private suspend fun doMove(tokenIndex: Int) {
        val base = _state.value ?: return
        val dice = base.dice ?: return
        val token = base.tokens.firstOrNull { it.color == base.current && it.index == tokenIndex } ?: return
        if (!base.awaitingMove || !LudoEngine.canMove(token, dice)) return

        val target = if (token.inBase) 1 else token.progress + dice
        val result = LudoEngine.move(base, tokenIndex)

        for (p in (token.progress + 1)..target) {
            _state.value = LudoEngine.withTokenProgress(base, token.color, token.index, p)
            delay(STEP)
        }
        val captured = result.tokens.zip(base.tokens).any { (after, before) ->
            after.color != token.color && after.progress != before.progress
        }
        delay(if (captured) CAPTURE else SETTLE)
        _state.value = result
    }

    /** Plays computer turns, one after another, with pauses, until it is a human's turn. */
    private suspend fun runBots() {
        while (true) {
            val s = _state.value ?: return
            if (s.winner != null || s.current !in s.bots) return
            delay(THINK)
            doRoll()
            val after = _state.value ?: return
            if (after.awaitingMove) {
                delay(DICE)
                val pick = LudoEngine.chooseAiMove(after) ?: return
                doMove(pick.index)
            }
        }
    }
}
