package com.example.ludo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ludo.R
import com.example.ludo.engine.LudoEngine
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token

// Slightly richer than before so they stand out on the navy background.
val PlayerColor.tint: Color
    get() = when (this) {
        PlayerColor.GREEN -> Color(0xFF22A04B)
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
        PlayerColor.YELLOW -> Color(0xFFFFD21F)
    }

// Frame: 3dp outer line + 3dp gap + 0.5dp inner line + 2.5dp white mat = 9dp before the board cells.
private val OuterLine = 3.dp
private val InnerLine = 0.5.dp
private val LineGap = 3.dp
private val BoardPad = 9.dp

// The 4 uncoloured stopover/safe squares and the colour of the arm they sit in.
private val StopoverSquares = linkedMapOf(
    10 to PlayerColor.RED,
    23 to PlayerColor.BLUE,
    36 to PlayerColor.YELLOW,
    49 to PlayerColor.GREEN,
)
@Composable
fun LudoBoard(
    state: LudoGameState,
    onTokenTap: (Token) -> Unit,
    modifier: Modifier = Modifier,
) {
    val movable = LudoEngine.movableTokens(state)
    val spots = BoardGeometry.layoutTokens(state.tokens)

    // Overlay logos are positioned via cell coordinates. We need pixel size to place them:
    // use BoxWithConstraints-free approach by measuring the Box inside the layout.
    Box(
        modifier = modifier.aspectRatio(1f),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    detectTapGestures { tap ->
                        val pad = BoardPad.toPx()
                        val cell = (size.width - 2 * pad) / BoardGeometry.GRID
                        val tx = (tap.x - pad) / cell
                        val ty = (tap.y - pad) / cell
                        val hit = movable.minByOrNull { spots.getValue(it).dist(tx, ty) }
                        if (hit != null && spots.getValue(hit).dist(tx, ty) < 0.6f) onTokenTap(hit)
                    }
                },
        ) { drawBoard(state, movable, spots) }

        BoardLogos(state)
    }
}

/**
 * Overlays the small white Euro logo on the 4 coloured entry squares and the big coloured
 * Euro logo on the centre home. Uses a Layout-free approach: measure the box's width to
 * derive the cell size, then offset each Image.
 */
@Composable
private fun BoardLogos(state: LudoGameState) {
    val padDp = BoardPad
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val boxPx = with(LocalDensity.current) { maxWidth.toPx() }
        val padPx = with(LocalDensity.current) { padDp.toPx() }
        val cellPx = (boxPx - 2 * padPx) / BoardGeometry.GRID

        // White Euro logo on each stopover square (the tile itself is now coloured)
        StopoverSquares.forEach { (sq, colour) ->
            val (c, r) = BoardGeometry.trackCells.getValue(sq)
            val logoPx = cellPx * 0.72f
            val centreX = padPx + (c + 0.5f) * cellPx
            val centreY = padPx + (r + 0.5f) * cellPx
            val dim = if (colour in state.players) 1.0f else 0.4f

            Image(
                painter = painterResource(R.drawable.euro_logo_white_cell),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = dim,
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) { (centreX - logoPx / 2f).toDp() },
                        y = with(LocalDensity.current) { (centreY - logoPx / 2f).toDp() },
                    )
                    .size(with(LocalDensity.current) { logoPx.toDp() }),
            )
        }

        // Big coloured Euro logo in the centre home
        val centrePx = cellPx * 2.4f
        val leftPx = padPx + 7.5f * cellPx - centrePx / 2f
        val topPx  = padPx + 7.5f * cellPx - centrePx / 2f
        Image(
            painter = painterResource(R.drawable.euro_logo_colour_centre),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { leftPx.toDp() },
                    y = with(LocalDensity.current) { topPx.toDp() },
                )
                .size(with(LocalDensity.current) { centrePx.toDp() }),
        )
    }
}

private fun DrawScope.drawBoard(state: LudoGameState, movable: List<Token>, spots: Map<Token, Pt>) {
    drawFrame()
    inset(BoardPad.toPx()) { drawCells(state, movable, spots) }
}

/** White mat with a dark #002D78 outline: thick outer line, thin parallel inner line. */
private fun DrawScope.drawFrame() {
    val w = size.width
    val outer = OuterLine.toPx()
    val hair = InnerLine.toPx()
    val gap = LineGap.toPx()
    val radius = 8.dp.toPx()

    drawRoundRect(Color.White, size = size, cornerRadius = CornerRadius(radius))
    drawRoundRect(
        LudoColors.Frame,
        topLeft = Offset(outer / 2, outer / 2),
        size = Size(w - outer, w - outer),
        cornerRadius = CornerRadius(radius - outer / 2),
        style = Stroke(outer),
    )
    val inset = outer + gap + hair / 2
    drawRoundRect(
        LudoColors.Frame,
        topLeft = Offset(inset, inset),
        size = Size(w - 2 * inset, w - 2 * inset),
        cornerRadius = CornerRadius((radius - inset).coerceAtLeast(1.dp.toPx())),
        style = Stroke(hair),
    )
}

private fun DrawScope.drawCells(state: LudoGameState, movable: List<Token>, spots: Map<Token, Pt>) {
    val cell = size.width / BoardGeometry.GRID
    val active = state.players.toSet()
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
        val entryOwner = PlayerColor.entries.firstOrNull { it.startSquare == sq }
        val stopoverColour = StopoverSquares[sq]
        val fill = when {
            stopoverColour != null -> stopoverColour.tint
            entryOwner != null -> entryOwner.tint
            else -> Color.White
        }
        cellRect(rc.first, rc.second, fill)
        // small safe-square dot, but skip on stopover tiles (the logo sits there now)
        if (sq in LudoEngine.SAFE_SQUARES && stopoverColour == null) {
            drawCircle(
                if (entryOwner != null) Color(0xAAFFFFFF) else Color(0x66000000),
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

    // Tokens: soft shadow, dark rim, glossy radial fill. Movable ones get a #002D78 ring.
    state.tokens.forEach { t ->
        val c = spots.getValue(t).let { Offset(it.x * cell, it.y * cell) }
        val r = cell * 0.34f
        val tint = t.color.tint
        drawCircle(Color(0x55000000), r, c + Offset(0f, cell * 0.05f))
        drawCircle(Color(0xFF111111), r + cell * 0.04f, c)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lerp(tint, Color.White, 0.55f), tint, lerp(tint, Color.Black, 0.25f)),
                center = c - Offset(r * 0.35f, r * 0.35f),
                radius = r * 1.6f,
            ),
            radius = r,
            center = c,
        )
        drawCircle(Color(0x66FFFFFF), r * 0.26f, c - Offset(r * 0.3f, r * 0.36f))
        if (t in movable) drawCircle(LudoColors.Frame, cell * 0.47f, c, style = Stroke(cell * 0.09f))
    }
}


