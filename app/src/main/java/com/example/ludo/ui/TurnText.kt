package com.example.ludo.ui

import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor

/** Short text shown inside the active player's own home box. */
data class TurnMessage(val title: String, val subtitle: String)

fun turnMessage(s: LudoGameState): TurnMessage {
    val winner = s.winner
    if (winner != null) return TurnMessage("${winner.label} wins!", "")
    val bot = s.current in s.bots
    val solo = s.bots.isNotEmpty()
    val title = if (solo && !bot) "Your turn" else "${s.current.label}'s turn"
    val dice = s.dice
    val sub = when {
        s.noMove -> "No move (rolled $dice)"
        s.busy -> "Moving..."
        s.awaitingMove -> if (bot) "Rolled $dice" else "Tap a token"
        bot -> "Thinking..."
        else -> "Roll the dice"
    }
    return TurnMessage(title, sub)
}

/** Name line at the top of each home: "Green · You", "Red · Computer" (or just the colour in pass-and-play). */
fun seatLabel(s: LudoGameState, color: PlayerColor): String =
    if (s.bots.isEmpty()) color.label
    else "${color.label} · ${if (color in s.bots) "Computer" else "You"}"

private val BotNames = listOf("Hamza", "Ayesha", "Bilal")

/**
 * Name shown in the player strips and messages. Computers get human names (in board order),
 * you get the name from Settings, and in pass-and-play every seat is just its colour.
 */
fun seatName(s: LudoGameState, color: PlayerColor, humanName: String): String = when {
    s.bots.isEmpty() -> color.label
    color in s.bots -> BotNames[s.players.filter { it in s.bots }.indexOf(color).coerceAtLeast(0) % BotNames.size]
    else -> humanName
}
