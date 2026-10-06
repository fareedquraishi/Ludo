package com.example.ludo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.engine.LudoEngine
import com.example.ludo.model.GameMode
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

    private val rng = Random.Default
    private val _state = MutableStateFlow<LudoGameState?>(null) // null = setup screen
    val state: StateFlow<LudoGameState?> = _state.asStateFlow()
    private var botJob: Job? = null

    fun start(mode: GameMode, rules: Rules, bots: Set<PlayerColor>) {
        _state.value = LudoEngine.newGame(mode, rules, bots)
        scheduleBots()
    }

    fun reset() {
        botJob?.cancel()
        _state.value = null
    }

    fun rollDice() {
        val s = _state.value ?: return
        if (s.current in s.bots) return
        _state.update { it?.let { st -> LudoEngine.roll(st, rng.nextInt(1, 7)) } }
        scheduleBots() // a no-move roll passes the turn immediately
    }

    fun onTokenTap(token: Token) {
        val s = _state.value ?: return
        if (s.current in s.bots || token.color != s.current) return
        _state.update { it?.let { st -> LudoEngine.move(st, token.index) } }
        scheduleBots()
    }

    private fun scheduleBots() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            while (true) {
                val s = _state.value ?: return@launch
                if (s.winner != null || s.current !in s.bots) return@launch
                delay(700)
                _state.update { it?.let { st -> LudoEngine.roll(st, rng.nextInt(1, 7)) } }
                val after = _state.value ?: return@launch
                if (after.awaitingMove) {
                    delay(600)
                    LudoEngine.chooseAiMove(after)?.let { pick ->
                        _state.update { it?.let { st -> LudoEngine.move(st, pick.index) } }
                    }
                }
            }
        }
    }
}
