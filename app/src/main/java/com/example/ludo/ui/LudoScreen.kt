package com.example.ludo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.model.Seats
import com.example.ludo.viewmodel.LudoViewModel

@Composable
fun LudoScreen(viewModel: LudoViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val s = state
    if (s == null) SetupPanel(onStart = viewModel::start) else GameContent(s, viewModel)
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
private fun GameContent(s: LudoGameState, vm: LudoViewModel) {
    Column(Modifier.fillMaxSize()) {
        GameBody(s, vm, Modifier.weight(1f).fillMaxWidth())
        EuroWatermark(Modifier.fillMaxWidth().height(84.dp).padding(bottom = 10.dp), alpha = 0.22f)
    }
    s.winner?.let { w ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (s.bots.isNotEmpty() && w !in s.bots) "You win!" else "${w.label} wins!") },
            confirmButton = { TextButton(onClick = vm::reset) { Text("New game") } },
        )
    }
}

@Composable
private fun GameBody(s: LudoGameState, vm: LudoViewModel, modifier: Modifier) {
    val humanTurn = s.current !in s.bots
    val youColor = if (s.bots.isNotEmpty()) s.players.firstOrNull { it !in s.bots } else null
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandTitle(size = 30.sp)
        youColor?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(12.dp).background(c.tint, CircleShape))
                Text("You are ${c.label}", fontSize = 13.sp, color = LudoColors.Muted)
            }
        }
        StatusCard(s, humanTurn)
        LudoBoard(s, onTokenTap = vm::onTokenTap, modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = vm::rollDice, enabled = s.canRoll && humanTurn) { Text("Roll dice", fontWeight = FontWeight.Bold) }
            DiceFace(s.dice, s.diceBy?.tint ?: LudoColors.Frame)
        }
        Column(Modifier.fillMaxWidth()) {
            s.log.takeLast(3).forEach { Text(it, fontSize = 13.sp, color = LudoColors.Muted) }
        }
    }
}

@Composable
private fun StatusCard(s: LudoGameState, humanTurn: Boolean) {
    val solo = s.bots.isNotEmpty()
    val name = s.current.label
    val who = when {
        !humanTurn -> "$name (computer)"
        solo -> "You"
        else -> name
    }
    val line = when {
        s.winner != null -> "${s.winner.label} won"
        s.noMove -> "$who rolled ${s.dice}: no move"
        !humanTurn -> "$name (computer) is playing..."
        s.busy -> "Moving..."
        s.awaitingMove -> if (solo) "Your turn: tap a highlighted token" else "$name: tap a highlighted token"
        else -> if (solo) "Your turn: roll the dice" else "$name's turn: roll the dice"
    }
    PanelCard(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(18.dp).background(s.current.tint, CircleShape))
            Column {
                Text(line, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                s.notice?.let { Text(it, fontSize = 13.sp, color = LudoColors.Muted) }
            }
        }
    }
}

@Composable
private fun DiceFace(value: Int?, ring: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(3.dp, ring),
        modifier = Modifier.size(56.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                value?.toString() ?: "-",
                color = LudoColors.Frame,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
