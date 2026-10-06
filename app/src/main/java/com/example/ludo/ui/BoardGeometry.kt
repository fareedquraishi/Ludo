package com.example.ludo.ui

import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import kotlin.math.hypot

/** A point in grid-cell units (cell centre = col + 0.5, row + 0.5) on the 15x15 board. */
data class Pt(val x: Float, val y: Float) {
    fun dist(px: Float, py: Float) = hypot(x - px, y - py)
}

/**
 * Cell (col,row) for every original block number, taken from the block order in index.php:
 * left arm rows 6..8, top arm cols 6..8, right arm rows 6..8, bottom arm cols 6..8.
 */
/** Token slots inside a 6x6 base: a tighter 2x2 cluster around the middle of the inner square. */
object BaseSlot {
    private const val NEAR = 2.0f
    private const val FAR = 4.0f
    fun x(index: Int) = if (index % 2 == 0) NEAR else FAR
    fun y(index: Int) = if (index < 2) NEAR else FAR
}

object BoardGeometry {
    const val GRID = 15

    val trackCells: Map<Int, Pair<Int, Int>> = buildMap {
        for (i in 0..5) put(1 + i, i to 6)            // 1..6   left arm, top row, left->right
        put(52, 0 to 7)                                // 52     left arm, middle row, leftmost
        for (i in 0..5) put(51 - i, i to 8)           // 51..46 left arm, bottom row, left->right
        for (i in 0..5) put(7 + i, 6 to 5 - i)        // 7..12  top arm, left column, going up
        put(13, 7 to 0); put(14, 8 to 0)              // 13,14  top arm, top row
        for (i in 0..4) put(15 + i, 8 to 1 + i)       // 15..19 top arm, right column, going down
        for (i in 0..5) put(20 + i, 9 + i to 6)       // 20..25 right arm, top row
        put(26, 14 to 7)                               // 26     right arm, middle row, rightmost
        for (i in 0..5) put(27 + i, 14 - i to 8)      // 27..32 right arm, bottom row, right->left
        for (i in 0..4) put(33 + i, 8 to 9 + i)       // 33..37 bottom arm, right column, going down
        put(38, 8 to 14); put(39, 7 to 14); put(40, 6 to 14)
        for (i in 0..4) put(41 + i, 6 to 13 - i)      // 41..45 bottom arm, left column, going up
    }

    /** Home-column cell, step 0..4 (original blocks 100..104, 200..204, 300..304, 400..404). */
    fun columnCell(color: PlayerColor, step: Int): Pair<Int, Int> = when (color) {
        PlayerColor.GREEN -> (1 + step) to 7
        PlayerColor.RED -> 7 to (1 + step)
        PlayerColor.BLUE -> (13 - step) to 7
        PlayerColor.YELLOW -> 7 to (13 - step)
    }

    /** Top-left cell of each 6x6 base: green TL, red TR, yellow BL, blue BR. */
    fun baseOrigin(color: PlayerColor): Pair<Int, Int> = when (color) {
        PlayerColor.GREEN -> 0 to 0
        PlayerColor.RED -> 9 to 0
        PlayerColor.YELLOW -> 0 to 9
        PlayerColor.BLUE -> 9 to 9
    }

    private fun centre(c: Int, r: Int) = Pt(c + 0.5f, r + 0.5f)

    fun position(t: Token): Pt {
        val sq = t.trackSquare
        return when {
            t.inBase -> {
                val (ox, oy) = baseOrigin(t.color)
                Pt(ox + BaseSlot.x(t.index), oy + BaseSlot.y(t.index))
            }
            t.isHome -> {
                val jitter = ((t.index % 2) - 0.5f) * 0.22f
                when (t.color) {
                    PlayerColor.GREEN -> Pt(6.8f, 7.5f + jitter)
                    PlayerColor.RED -> Pt(7.5f + jitter, 6.8f)
                    PlayerColor.BLUE -> Pt(8.2f, 7.5f + jitter)
                    PlayerColor.YELLOW -> Pt(7.5f + jitter, 8.2f)
                }
            }
            sq != null -> trackCells.getValue(sq).let { centre(it.first, it.second) }
            else -> columnCell(t.color, t.columnStep!!).let { centre(it.first, it.second) }
        }
    }

    /** Positions for all tokens; tokens sharing a cell are fanned out sideways so all stay visible. */
    fun layoutTokens(tokens: List<Token>): Map<Token, Pt> {
        val groups = tokens.map { it to position(it) }
            .groupBy { (it.second.x * 100).toInt() to (it.second.y * 100).toInt() }
        val out = LinkedHashMap<Token, Pt>()
        for (group in groups.values) {
            group.forEachIndexed { i, (token, p) ->
                val shift = (i - (group.size - 1) / 2f) * 0.26f
                out[token] = if (group.size > 1) Pt(p.x + shift, p.y) else p
            }
        }
        return out
    }
}
