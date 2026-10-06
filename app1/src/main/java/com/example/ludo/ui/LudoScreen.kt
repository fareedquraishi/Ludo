package com.example.ludo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.model.GameMode
import com.example.ludo.model.LudoGameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Rules
import com.example.ludo.viewmodel.LudoViewModel

@Composable
fun LudoScreen(viewModel: LudoViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val s = state
    if (s == null) SetupPanel(onStart = viewModel::start) else GameContent(s, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupPanel(onStart: (GameMode, Rules, Set<PlayerColor>) -> Unit) {
    var mode by remember { mutableStateOf(GameMode.FOUR) }
    var sixLimit by remember { mutableStateOf<Int?>(null) }
    var vsComputer by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ludo", style = MaterialTheme.typography.headlineLarge)
        Text("Players")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMode.entries.forEach { m ->
                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
            }
        }
        Text("Consecutive sixes allowed")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Int?>(null, 2, 3, 4, 5).forEach { n ->
                FilterChip(selected = sixLimit == n, onClick = { sixLimit = n }, label = { Text(n?.toString() ?: "Unlimited") })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = vsComputer, onCheckedChange = { vsComputer = it })
            Text("Computer plays everyone except Green")
        }
        Button(
            onClick = {
                val bots = if (vsComputer) mode.players.drop(1).toSet() else emptySet()
                onStart(mode, Rules(sixLimit = sixLimit), bots)
            },
        ) { Text("Start game") }
    }
}

@Composable
private fun GameContent(s: LudoGameState, vm: LudoViewModel) {
    val humanTurn = s.current !in s.bots
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusCard(s, humanTurn)
        LudoBoard(s, onTokenTap = vm::onTokenTap, modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = vm::rollDice, enabled = s.canRoll && humanTurn) { Text("Roll dice") }
            DiceFace(s.dice)
        }
        Column(Modifier.fillMaxWidth()) { s.log.takeLast(4).forEach { Text(it, fontSize = 13.sp) } }
    }
    s.winner?.let { w ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("${w.label} wins!") },
            confirmButton = { TextButton(onClick = vm::reset) { Text("New game") } },
        )
    }
}

@Composable
private fun StatusCard(s: LudoGameState, humanTurn: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Colour chip + normal text colour, so yellow stays readable on a light card
            Box(Modifier.size(18.dp).background(s.current.tint, CircleShape))
            Text(
                when {
                    s.winner != null -> "${s.winner.label} won"
                    !humanTurn -> "${s.current.label} (computer) is playing..."
                    s.awaitingMove -> "${s.current.label}: tap a highlighted token"
                    else -> "${s.current.label}'s turn: roll the dice"
                },
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun DiceFace(value: Int?) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.DarkGray, modifier = Modifier.size(56.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(value?.toString() ?: "-", color = Color.White, fontSize = 24.sp)
        }
    }
}
