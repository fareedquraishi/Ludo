package com.example.ludo.model

/**
 * Board numbering matches the original web game (index.php / functions.js):
 * main track squares 1..52, start squares G=2 R=15 B=28 Y=41,
 * turn order Green -> Red -> Blue -> Yellow (clockwise).
 */
enum class PlayerColor(val label: String, val startSquare: Int) {
    GREEN("Green", 2),
    RED("Red", 15),
    BLUE("Blue", 28),
    YELLOW("Yellow", 41),
}

/**
 * Which colours play, derived from the number of players and the colour the user picked.
 * 4 = everyone, 2 = you + the colour opposite you, 3 = you + the next two clockwise.
 * The result is always in board order (Green, Red, Blue, Yellow) = turn order.
 */
object Seats {
    fun of(count: Int, human: PlayerColor): List<PlayerColor> {
        val all = PlayerColor.entries
        val chosen = when (count) {
            2 -> setOf(human, all[(human.ordinal + 2) % all.size])
            3 -> setOf(human, all[(human.ordinal + 1) % all.size], all[(human.ordinal + 2) % all.size])
            else -> all.toSet()
        }
        return all.filter { it in chosen }
    }
}

/**
 * progress: 0 = base, 1 = start square, 1..51 = main track,
 * 52..56 = own home column (original blocks x00..x04), 57 = home (x05).
 * Immutable on purpose: every change creates a new Token so Compose sees it.
 */
data class Token(val color: PlayerColor, val index: Int, val progress: Int = BASE) {
    val inBase: Boolean get() = progress == BASE
    val isHome: Boolean get() = progress == HOME

    /** Main-track square 1..52 the token is on, or null (base / home column / home). */
    val trackSquare: Int?
        get() = if (progress in 1..MAIN_END) ((color.startSquare - 1 + progress - 1) % 52) + 1 else null

    /** 0..4 when inside the home column, else null. */
    val columnStep: Int?
        get() = if (progress in COLUMN_START..COLUMN_END) progress - COLUMN_START else null

    companion object {
        const val BASE = 0
        const val MAIN_END = 51
        const val COLUMN_START = 52
        const val COLUMN_END = 56
        const val HOME = 57
    }
}

/** Defaults reproduce the original: a six gives another roll, nothing else does. */
data class Rules(
    val sixLimit: Int? = null,        // original "Limited Sixes" (2..10); null = unlimited
    val captureBonus: Boolean = false,
    val homeBonus: Boolean = false,
)

/**
 * A turn has two phases. First the player throws every roll they have earned (a six earns another
 * roll) and each number is kept in [queue]. Once no roll is left ([pendingRolls] == 0) the player
 * spends the numbers on tokens, in any order, until the queue is empty.
 */
data class LudoGameState(
    val players: List<PlayerColor> = PlayerColor.entries.toList(),
    val rules: Rules = Rules(),
    val bots: Set<PlayerColor> = emptySet(),
    val tokens: List<Token> = emptyList(),
    val turnIndex: Int = 0,
    val queue: List<Int> = emptyList(),   // numbers rolled this turn and not yet spent (the "dice row")
    val pendingRolls: Int = 1,            // rolls the player on turn still has to throw
    val selected: Int? = null,            // the number the next token tap will spend
    val lastRoll: Int? = null,            // the most recent roll of this turn, for the player strip
    val awaitingMove: Boolean = false,    // spend phase: waiting for a token to be chosen
    val noMove: Boolean = false,          // nothing left can move: shown for a moment, then the turn passes
    val busy: Boolean = false,            // a token is hopping: ignore taps
    val sixStreak: Int = 0,
    val winner: PlayerColor? = null,
    val notice: String? = null,       // e.g. "Blue starts (random draw)"
    val log: List<String> = emptyList(),
) {
    val current: PlayerColor get() = players[turnIndex]
    val canRoll: Boolean get() = winner == null && pendingRolls > 0 && !awaitingMove && !noMove && !busy
}
