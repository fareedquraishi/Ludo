package com.example.ludo.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.audio.Fx
import com.example.ludo.audio.SoundManager
import com.example.ludo.data.Settings
import com.example.ludo.data.SettingsStore
import com.example.ludo.ui.seatName
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.random.Random

/** "Ayesha beat Fareed!" / "Hamza's token is home!" pop-up shown over the board for a moment. */
data class CaptureBanner(val id: Long, val text: String, val color: PlayerColor)

/** Funny capture lines. All past tense, so they read right with "You" as well as with a name. */
private val CaptureLines: List<(String, String) -> String> = listOf(
    { a, b -> "$a beat $b!" },
    { a, b -> "$a sent $b back to base!" },
    { a, b -> "Boom! $a knocked out $b!" },
    { a, b -> "Ouch! $b got caught by $a" },
    { a, b -> "Not today, $b! - $a" },
    { a, b -> "$a sent $b packing!" },
    { a, b -> "Sorry $b, $a needed that square!" },
    { a, b -> "$a stole the square from $b!" },
    { a, b -> "$b took a trip home, thanks to $a" },
    { a, b -> "Gotcha, $b! - $a" },
)

/** Identifies one "waiting for the human" moment: the timer restarts whenever this changes. */
private data class TurnKey(val turnIndex: Int, val awaitingMove: Boolean, val logSize: Int)

class LudoViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        // Pacing ("Normal" speed), in milliseconds
        const val THINK = 600L       // banner shows, computer "thinks" before rolling
        const val PICK = 500L        // computer looks at its dice row before choosing a token
        const val NO_MOVE = 1100L    // "rolled N: no move" stays visible before the turn passes
        const val STEP = 230L        // time per square while a token hops
        const val CAPTURE = 350L     // beat at the destination when something is captured
        const val SETTLE = 120L      // beat at the destination otherwise
        const val ROLL = 700L        // dice shuffle animation before the number appears
    }

    private val rng = Random.Default
    private val _state = MutableStateFlow<LudoGameState?>(null) // null = setup screen
    val state: StateFlow<LudoGameState?> = _state.asStateFlow()
    private var job: Job? = null

    private val settingsStore = SettingsStore(app)
    val settings: StateFlow<Settings> = settingsStore.flow
    private val sound = SoundManager(app)

    /** Fraction of the turn timer left (1 -> 0) while a human is to act; null = no timer running. */
    private val _timer = MutableStateFlow<Float?>(null)
    val timer: StateFlow<Float?> = _timer.asStateFlow()
    /** True while the dice is shuffling (the strip shows rolling faces). */
    private val _rolling = MutableStateFlow(false)
    val rolling: StateFlow<Boolean> = _rolling.asStateFlow()

    private val _banner = MutableStateFlow<CaptureBanner?>(null)
    val banner: StateFlow<CaptureBanner?> = _banner.asStateFlow()

    private var settingsOpen = false
    private var inBackground = false

    init {
        viewModelScope.launch { settings.collect { sound.apply(it) } }
        viewModelScope.launch {
            _state.map { turnKey(it) }.distinctUntilChanged().collectLatest { key -> runTurnTimer(key) }
        }
    }

    fun updateSettings(change: (Settings) -> Settings) = settingsStore.update(change)
    fun click() = sound.play(Fx.CLICK)

    /** The settings dialog pauses the turn timer while it is open. */
    fun setSettingsOpen(open: Boolean) { settingsOpen = open }

    /** Called from the activity: music and the timer pause while the app is in the background. */
    fun setForeground(foreground: Boolean) {
        inBackground = !foreground
        sound.setForeground(foreground)
    }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }

    private fun turnKey(s: LudoGameState?): TurnKey? {
        if (s == null || s.winner != null || s.current in s.bots || s.busy || s.noMove) return null
        return TurnKey(s.turnIndex, s.awaitingMove, s.log.size)
    }

    private suspend fun runTurnTimer(key: TurnKey?) {
        _timer.value = null
        if (key == null) return
        val st = _state.value ?: return
        if (!key.awaitingMove && st.bots.isNotEmpty()) sound.play(Fx.TURN)
        val seconds = settings.value.timerSeconds
        if (seconds <= 0) return

        val total = seconds * 1000L
        var left = total
        var last = SystemClock.elapsedRealtime()
        var lastTick = Int.MAX_VALUE
        while (left > 0) {
            val now = SystemClock.elapsedRealtime()
            if (!settingsOpen && !inBackground) left -= now - last
            last = now
            _timer.value = (left.coerceAtLeast(0L).toFloat() / total)
            val secLeft = ceil(left / 1000.0).toInt()
            if (secLeft in 1..5 && secLeft < lastTick) {
                lastTick = secLeft
                sound.play(Fx.TICK)
            }
            delay(50)
        }
        _timer.value = 0f
        onTimeout(key)
    }

    private fun onTimeout(key: TurnKey) {
        val s = _state.value ?: return
        if (turnKey(s) != key) return
        if (!settings.value.autoPlayOnTimeout) {
            val who = seatName(s, s.current, settings.value.displayName)
            _state.update { it?.let { st -> LudoEngine.skipTurn(st, "$who ran out of time") } }
            launchSequence { }
        } else if (s.awaitingMove) {
            val pick = LudoEngine.chooseAiMove(s) ?: return
            launchSequence { doMove(pick.first.index, pick.second) }
        } else {
            launchSequence { doRoll() }
        }
    }

    fun start(players: List<PlayerColor>, rules: Rules, bots: Set<PlayerColor>) {
        val first = rng.nextInt(players.size)
        val who = players[first]
        val game = LudoEngine.newGame(players, rules, bots, first)
        val name = seatName(game, who, settings.value.displayName)
        val notice = if (bots.isNotEmpty() && who !in bots) "You start (random draw)" else "$name starts (random draw)"
        _state.value = game.copy(
            notice = notice,
            log = listOf("Random draw: $name starts"),
        )
        launchSequence { }   // if a computer starts, the bot loop takes over
    }

    fun reset() {
        job?.cancel()
        _rolling.value = false
        _banner.value = null
        _state.value = null
    }

    /** Human taps "Roll dice". */
    fun rollDice() {
        val s = _state.value ?: return
        if (s.current in s.bots || !s.canRoll || _rolling.value) return
        launchSequence { doRoll() }
    }

    /** Human taps a highlighted token: it spends the selected number from the dice row. */
    fun onTokenTap(token: Token) {
        val s = _state.value ?: return
        if (s.current in s.bots || !s.awaitingMove || s.busy || token.color != s.current) return
        val value = s.selected ?: return
        launchSequence { doMove(token.index, value) }
    }

    /** Human taps a die in the dice row: the next token tap spends that number. */
    fun selectDie(value: Int) {
        val s = _state.value ?: return
        if (s.current in s.bots || !s.awaitingMove || s.busy) return
        click()
        _state.update { it?.let { st -> LudoEngine.select(st, value) } }
    }

    private fun launchSequence(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            block()
            runBots()
        }
    }

    private suspend fun doRoll() {
        val first = _state.value ?: return
        if (!first.canRoll) return
        sound.play(Fx.DICE)
        _rolling.value = true
        try {
            delay(ROLL)
        } finally {
            _rolling.value = false
        }
        val value = rng.nextInt(1, 7)
        _state.update { it?.let { st -> LudoEngine.roll(st, value, autoPass = false) } }
        if (value == 6) sound.play(Fx.SIX)
        passIfNothingToPlay()
    }

    /** "No move" stays on screen for a moment, then the turn passes. */
    private suspend fun passIfNothingToPlay() {
        val after = _state.value ?: return
        if (after.noMove) {
            delay(NO_MOVE)
            _state.update { it?.let { st -> LudoEngine.passAfterNoMove(st) } }
        }
    }

    /** Hops the token square by square, then applies the real move (capture, bonus roll, turn change, win). */
    private suspend fun doMove(tokenIndex: Int, value: Int) {
        val base = _state.value ?: return
        val token = base.tokens.firstOrNull { it.color == base.current && it.index == tokenIndex } ?: return
        if (!base.awaitingMove || value !in base.queue || !LudoEngine.canMove(token, value)) return

        val target = if (token.inBase) 1 else token.progress + value
        val result = LudoEngine.move(base, tokenIndex, value)
        // The spent number leaves the dice row as soon as the token starts to hop.
        val hopBase = base.copy(queue = LudoEngine.withoutValue(base.queue, value), selected = null)

        for (p in (token.progress + 1)..target) {
            _state.value = LudoEngine.withTokenProgress(hopBase, token.color, token.index, p)
            sound.play(if (token.inBase) Fx.LEAVE else Fx.STEP)
            delay(STEP)
        }
        val captured = result.tokens.zip(base.tokens).any { (after, before) ->
            after.color != token.color && after.progress != before.progress
        }
        val reachedHome = result.tokens.any { it.color == token.color && it.index == token.index && it.isHome }
        if (captured) announceCapture(base, result, token.color)
        if (reachedHome) announceHome(result, token.color)
        when {
            captured -> sound.play(Fx.CAPTURE)
            reachedHome && result.winner == null -> sound.play(Fx.HOME)
        }
        delay(if (captured) CAPTURE else SETTLE)
        _state.value = result
        result.winner?.let { w ->
            sound.play(if (result.bots.isEmpty() || w !in result.bots) Fx.WIN else Fx.LOSE)
        }
        passIfNothingToPlay()
    }

    private fun announceCapture(before: LudoGameState, after: LudoGameState, attacker: PlayerColor) {
        val victims = after.tokens.zip(before.tokens)
            .filter { (a, b) -> a.color != attacker && a.progress == Token.BASE && b.progress != Token.BASE }
            .map { it.first.color }
            .distinct()
        if (victims.isEmpty()) return
        val me = settings.value.displayName
        val a = seatName(after, attacker, me)
        val b = victims.joinToString(" & ") { seatName(after, it, me) }
        _banner.value = CaptureBanner(System.nanoTime(), CaptureLines.random(rng)(a, b), attacker)
    }

    /** "Hamza's token is home! (2 of 4)" - also a line for the winner when the last token arrives. */
    private fun announceHome(after: LudoGameState, color: PlayerColor) {
        val count = after.tokens.count { it.color == color && it.isHome }
        val name = seatName(after, color, settings.value.displayName)
        val who = if (name == "You") "Your" else "$name's"
        _banner.value = CaptureBanner(System.nanoTime(), "$who token is home! ($count of 4)", color)
    }

    /** Plays computer turns, one after another, with pauses, until it is a human's turn. */
    private suspend fun runBots() {
        while (true) {
            val s = _state.value ?: return
            if (s.winner != null || s.current !in s.bots) return
            delay(THINK)
            val st = _state.value ?: return
            when {
                st.canRoll -> doRoll()                       // throws every earned roll first
                st.awaitingMove -> {
                    delay(PICK)
                    val pick = LudoEngine.chooseAiMove(st) ?: return
                    doMove(pick.first.index, pick.second)
                }
                else -> return
            }
        }
    }
}
