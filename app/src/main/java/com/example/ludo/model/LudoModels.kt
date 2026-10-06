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

/** Same four modes as the original "Number of Players" select (2, 3A, 3B, 4). */
enum class GameMode(val label: String, val players: List<PlayerColor>) {
    TWO("2 players", listOf(PlayerColor.GREEN, PlayerColor.BLUE)),
    THREE_A("3 players (A)", listOf(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.BLUE)),
    THREE_B("3 players (B)", listOf(PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.YELLOW)),
    FOUR("4 players", PlayerColor.entries.toList()),
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

data class LudoGameState(
    val mode: GameMode = GameMode.FOUR,
    val rules: Rules = Rules(),
    val bots: Set<PlayerColor> = emptySet(),
    val tokens: List<Token> = emptyList(),
    val turnIndex: Int = 0,
    val dice: Int? = null,
    val diceBy: PlayerColor? = null,
    val awaitingMove: Boolean = false,
    val sixStreak: Int = 0,
    val winner: PlayerColor? = null,
    val log: List<String> = emptyList(),
) {
    val current: PlayerColor get() = mode.players[turnIndex]
    val canRoll: Boolean get() = winner == null && !awaitingMove
}
