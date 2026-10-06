package com.example.ludo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ludo.engine.LudoEngine
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token

val PlayerColor.tint: Color
    get() = when (this) {
        PlayerColor.GREEN -> Color(0xFF2E7D32)
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
    }

@Composable
fun LudoBoard(
    state: LudoGameState,
    onTokenTap: (Token) -> Unit,
    modifier: Modifier = Modifier,
) {
    val movable = LudoEngine.movableTokens(state)
    val spots = BoardGeometry.layoutTokens(state.tokens)

    Canvas(
        modifier = modifier
            .aspectRatio(1f) // scales with the available width: phones, tablets, foldables
            .pointerInput(state) {
                detectTapGestures { tap ->
                    val cell = size.width / BoardGeometry.GRID
                    val hit = movable.minByOrNull { spots.getValue(it).dist(tap.x / cell, tap.y / cell) }
                    if (hit != null && spots.getValue(hit).dist(tap.x / cell, tap.y / cell) < 0.6f) {
                        onTokenTap(hit)
                    }
                }
            },
    ) { drawBoard(state, movable, spots) }
}

private fun DrawScope.drawBoard(state: LudoGameState, movable: List<Token>, spots: Map<Token, Pt>) {
    val cell = size.width / BoardGeometry.GRID
    val active = state.mode.players.toSet()
    val line = Color(0x33000000)
    fun cellRect(c: Int, r: Int, fill: Color) {
        drawRect(fill, Offset(c * cell, r * cell), Size(cell, cell))
        drawRect(line, Offset(c * cell, r * cell), Size(cell, cell), style = Stroke(1f))
    }

    // Bases (inactive colours are dimmed)
    PlayerColor.entries.forEach { color ->
        val (ox, oy) = BoardGeometry.baseOrigin(color)
        drawRect(color.tint.copy(alpha = if (color in active) 1f else 0.3f), Offset(ox * cell, oy * cell), Size(6 * cell, 6 * cell))
        drawRoundRect(Color.White, Offset((ox + 1) * cell, (oy + 1) * cell), Size(4 * cell, 4 * cell), CornerRadius(cell * 0.4f))
    }

    // Main track: start squares tinted, safe squares marked with a dot
    BoardGeometry.trackCells.forEach { (sq, rc) ->
        val owner = PlayerColor.entries.firstOrNull { it.startSquare == sq }
        cellRect(rc.first, rc.second, owner?.tint ?: Color.White)
        if (sq in LudoEngine.SAFE_SQUARES) {
            drawCircle(
                if (owner != null) Color(0xAAFFFFFF) else Color(0x66000000),
                cell * 0.18f,
                Offset((rc.first + 0.5f) * cell, (rc.second + 0.5f) * cell),
            )
        }
    }

    // Home columns
    PlayerColor.entries.forEach { color ->
        for (step in 0..4) BoardGeometry.columnCell(color, step).let { cellRect(it.first, it.second, color.tint) }
    }

    // Centre triangles
    fun p(x: Float, y: Float) = Offset(x * cell, y * cell)
    fun tri(color: Color, a: Offset, b: Offset) {
        val path = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(p(7.5f, 7.5f).x, p(7.5f, 7.5f).y); close() }
        drawPath(path, color)
    }
    tri(PlayerColor.GREEN.tint, p(6f, 6f), p(6f, 9f))
    tri(PlayerColor.RED.tint, p(6f, 6f), p(9f, 6f))
    tri(PlayerColor.BLUE.tint, p(9f, 6f), p(9f, 9f))
    tri(PlayerColor.YELLOW.tint, p(6f, 9f), p(9f, 9f))

    // Tokens (movable ones get a ring)
    state.tokens.forEach { t ->
        val c = spots.getValue(t).let { Offset(it.x * cell, it.y * cell) }
        drawCircle(Color.Black, cell * 0.38f, c)
        drawCircle(t.color.tint, cell * 0.32f, c)
        if (t in movable) drawCircle(Color.Black, cell * 0.47f, c, style = Stroke(cell * 0.08f))
    }
}
