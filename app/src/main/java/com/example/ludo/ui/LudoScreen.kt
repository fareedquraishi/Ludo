package com.example.ludo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.R
import com.example.ludo.data.Settings
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Seats
import com.example.ludo.viewmodel.CaptureBanner
import com.example.ludo.viewmodel.LudoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LudoScreen(viewModel: LudoViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(showSettings) { viewModel.setSettingsOpen(showSettings) }
    val s = state

    Box(Modifier.fillMaxSize()) {
        if (s == null) {
            SetupPanel(onStart = { players, rules, bots ->
                viewModel.click()
                viewModel.start(players, rules, bots)
            })
        } else {
            GameContent(s, viewModel, settings)
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(LudoColors.NavyDeep)
                .border(1.dp, LudoColors.Teal, CircleShape)
                .clickable {
                    viewModel.click()
                    showSettings = true
                },
            contentAlignment = Alignment.Center,
        ) { Text("\u2699", fontSize = 22.sp, color = LudoColors.OnNavy) }
        if (showSettings) {
            SettingsDialog(settings, onChange = viewModel::updateSettings, onClose = { showSettings = false })
        }
    }
}

@Composable
private fun PanelCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LudoColors.NavyDeep),
        border = BorderStroke(1.dp, LudoColors.Teal),
    ) { content() }
}

@Composable
private fun SetupPanel(onStart: (List<PlayerColor>, Rules, Set<PlayerColor>) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        EuroWatermark(Modifier.align(Alignment.Center).fillMaxWidth(), alpha = 0.14f)
        SetupContent(onStart)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupContent(onStart: (List<PlayerColor>, Rules, Set<PlayerColor>) -> Unit) {
    var count by remember { mutableStateOf(4) }
    var human by remember { mutableStateOf(PlayerColor.GREEN) }
    var sixLimit by remember { mutableStateOf<Int?>(null) }
    var vsComputer by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        BrandTitle(size = 64.sp)
        PanelCard(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Number of players", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { n ->
                        FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n players") })
                    }
                }
                Text("Your colour", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerColor.entries.forEach { c ->
                        FilterChip(
                            selected = human == c,
                            onClick = { human = c },
                            label = { Text(c.label) },
                            leadingIcon = { Box(Modifier.size(14.dp).background(c.tint, CircleShape)) },
                        )
                    }
                }
                Text(
                    "Playing: " + Seats.of(count, human).joinToString(", ") { it.label },
                    fontSize = 13.sp,
                    color = LudoColors.Muted,
                )
                Text("Consecutive sixes allowed", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<Int?>(null, 2, 3, 4, 5).forEach { n ->
                        FilterChip(selected = sixLimit == n, onClick = { sixLimit = n }, label = { Text(n?.toString() ?: "Unlimited") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = vsComputer, onCheckedChange = { vsComputer = it })
                    Text("Computer plays the other colours")
                }
            }
        }
        Button(
            onClick = {
                val players = Seats.of(count, human)
                val bots = if (vsComputer) players.filter { it != human }.toSet() else emptySet()
                onStart(players, Rules(sixLimit = sixLimit), bots)
            },
        ) { Text("Start game", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun GameContent(s: LudoGameState, vm: LudoViewModel, settings: Settings) {
    Column(Modifier.fillMaxSize()) {
        GameBody(s, vm, settings, Modifier.weight(1f).fillMaxWidth())
        EuroWatermark(Modifier.fillMaxWidth().height(84.dp).padding(bottom = 10.dp), alpha = 0.22f)
    }
    s.winner?.let { w ->
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(if (s.bots.isNotEmpty() && w !in s.bots) "You win!" else "${seatName(s, w, settings.displayName)} wins!")
            },
            confirmButton = { TextButton(onClick = vm::reset) { Text("New game") } },
        )
    }
}

@Composable
private fun GameBody(s: LudoGameState, vm: LudoViewModel, settings: Settings, modifier: Modifier) {
    val humanTurn = s.current !in s.bots
    val timer = vm.timer.collectAsState()
    val rolling by vm.rolling.collectAsState()
    val banner by vm.banner.collectAsState()
    @Composable
    fun Seat(color: PlayerColor, seatModifier: Modifier) {
        if (color in s.players) {
            PlayerStrip(
                color = color,
                name = seatName(s, color, settings.displayName),
                active = s.winner == null && s.current == color,
                rolling = rolling && s.current == color,
                // Numbers only show for the player on turn, once they have rolled; otherwise the Euro "e".
                shownDice = s.dice.takeIf {
                    s.winner == null && s.current == color && s.diceBy == color &&
                        (s.awaitingMove || s.noMove || s.busy)
                },
                canRoll = s.current == color && humanTurn && s.canRoll && !rolling,
                onRoll = vm::rollDice,
                timer = timer,
                modifier = seatModifier,
            )
        } else {
            Spacer(seatModifier)   // colour not playing: keep the slot empty
        }
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandTitle(size = 30.sp)
        TurnHint(s, humanTurn, seatName(s, s.current, settings.displayName))

        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Seat(PlayerColor.GREEN, Modifier.weight(1f))
                Seat(PlayerColor.RED, Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                LudoBoard(s, onTokenTap = vm::onTokenTap, modifier = Modifier.fillMaxWidth())
                CaptureBannerView(banner, Modifier.padding(top = 36.dp, start = 24.dp, end = 24.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Seat(PlayerColor.YELLOW, Modifier.weight(1f))
                Seat(PlayerColor.BLUE, Modifier.weight(1f))
            }
        }

        Button(onClick = vm::rollDice, enabled = s.canRoll && humanTurn) { Text("Roll dice", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TurnHint(s: LudoGameState, humanTurn: Boolean, currentName: String) {
    val solo = s.bots.isNotEmpty()
    val line = when {
        s.winner != null -> ""
        s.noMove -> "${if (humanTurn && solo) "You" else currentName} rolled ${s.dice}: no move"
        !humanTurn -> "$currentName is playing..."
        s.busy -> "Moving..."
        s.awaitingMove -> if (solo) "Your turn: tap a highlighted token" else "$currentName: tap a highlighted token"
        else -> if (solo) "Your turn: roll the dice" else "$currentName's turn: roll the dice"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(line, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(s.notice.orEmpty(), fontSize = 12.sp, color = LudoColors.Muted, maxLines = 1)
    }
}

/** Avatar, name and last roll of one player, with the turn timer bar when it is their turn. */
@Composable
private fun PlayerStrip(
    color: PlayerColor,
    name: String,
    active: Boolean,
    rolling: Boolean,
    shownDice: Int?,
    canRoll: Boolean,
    onRoll: () -> Unit,
    timer: State<Float?>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(LudoColors.NavyDeep)
            .border(if (active) 2.dp else 1.dp, if (active) color.tint else LudoColors.EuroBlue, shape)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(34.dp).background(color.tint, CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (color == PlayerColor.YELLOW) LudoColors.Frame else Color.White,
                )
            }
            Text(
                name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) LudoColors.OnNavy else LudoColors.Muted,
            )
            DieFace(
                rolling = rolling,
                value = shownDice,
                ring = color.tint,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = canRoll, onClick = onRoll),
            )
        }
        Spacer(Modifier.height(6.dp))
        // The bar is drawn from the timer state in the draw phase, so it never recomposes the screen.
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x22FFFFFF))
                .drawBehind {
                    val f = if (active) timer.value else null
                    if (f != null) {
                        val low = f < 0.25f
                        drawRect(if (low) Color(0xFFFF5252) else color.tint, size = Size(size.width * f, size.height))
                    }
                },
        )
    }
}

/**
 * The dice in a player strip: the Euro "e" when idle, shuffling faces while rolling,
 * and the number once the player on turn has rolled.
 */
@Composable
private fun DieFace(rolling: Boolean, value: Int?, ring: Color, modifier: Modifier = Modifier) {
    var shuffle by remember { mutableStateOf(1) }
    LaunchedEffect(rolling) {
        while (rolling) {
            shuffle = Random.nextInt(1, 7)
            delay(70)
        }
    }
    val wobble by rememberInfiniteTransition(label = "dieWobble").animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
        label = "wobble",
    )
    Box(modifier.graphicsLayer { rotationZ = if (rolling) wobble else 0f }, contentAlignment = Alignment.Center) {
        DicePips(if (rolling) shuffle else value, ring, Modifier.fillMaxSize())
        if (!rolling && value == null) {
            Image(
                painter = painterResource(R.drawable.euro_e_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(7.dp),
            )
        }
    }
}

/** "Ayesha beat Fareed!" pops in over the board, then fades out. */
@Composable
private fun CaptureBannerView(banner: CaptureBanner?, modifier: Modifier = Modifier) {
    if (banner == null) return
    key(banner.id) {
        val fade = remember { Animatable(0f) }
        val pop = remember { Animatable(0.5f) }
        LaunchedEffect(Unit) {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 260f)) }
            fade.animateTo(1f, tween(120))
            delay(1500)
            fade.animateTo(0f, tween(500))
        }
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier
                .graphicsLayer {
                    alpha = fade.value
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(shape)
                .background(banner.color.tint)
                .border(2.dp, Color.White, shape)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                banner.text,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = if (banner.color == PlayerColor.YELLOW) LudoColors.Frame else Color.White,
            )
        }
    }
}

private val DotPositions = mapOf(
    1 to listOf(1 to 1),
    2 to listOf(0 to 0, 2 to 2),
    3 to listOf(0 to 0, 1 to 1, 2 to 2),
    4 to listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2),
    5 to listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2),
    6 to listOf(0 to 0, 0 to 1, 0 to 2, 2 to 0, 2 to 1, 2 to 2),
)

@Composable
private fun DicePips(value: Int?, ring: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val corner = CornerRadius(size.width * 0.18f)
        drawRoundRect(Color.White, cornerRadius = corner)
        drawRoundRect(ring, cornerRadius = corner, style = Stroke(size.width * 0.08f))
        DotPositions[value]?.forEach { (c, r) ->
            drawCircle(
                LudoColors.Frame,
                size.width * 0.075f,
                Offset(size.width * (0.27f + 0.23f * c), size.height * (0.27f + 0.23f * r)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings") },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = settings.playerName,
                    onValueChange = { v -> onChange { it.copy(playerName = v.take(12)) } },
                    label = { Text("Your name") },
                    singleLine = true,
                )
                Text("Turn timer", fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Settings.TimerChoices.forEach { n ->
                        FilterChip(
                            selected = settings.timerSeconds == n,
                            onClick = { onChange { it.copy(timerSeconds = n) } },
                            label = { Text(if (n == 0) "Off" else "${n}s") },
                        )
                    }
                }
                Text("When time runs out", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.autoPlayOnTimeout,
                        onClick = { onChange { it.copy(autoPlayOnTimeout = true) } },
                        label = { Text("Auto-play") },
                    )
                    FilterChip(
                        selected = !settings.autoPlayOnTimeout,
                        onClick = { onChange { it.copy(autoPlayOnTimeout = false) } },
                        label = { Text("Skip turn") },
                    )
                }
                Text("The timer applies from your next turn.", fontSize = 12.sp, color = LudoColors.Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = settings.musicOn, onCheckedChange = { v -> onChange { it.copy(musicOn = v) } })
                    Text("Background music")
                }
                Slider(
                    value = settings.musicVolume,
                    onValueChange = { v -> onChange { it.copy(musicVolume = v) } },
                    enabled = settings.musicOn,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = settings.sfxOn, onCheckedChange = { v -> onChange { it.copy(sfxOn = v) } })
                    Text("Sound effects")
                }
            }
        },
    )
}
