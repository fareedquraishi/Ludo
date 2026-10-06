package com.example.ludo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ludo.viewmodel.LudoViewModel
import com.example.ludo.ui.BrandBackground
import com.example.ludo.ui.LudoScreen
import com.example.ludo.ui.LudoTheme

class MainActivity : ComponentActivity() {
    private lateinit var vm: LudoViewModel

    // Music and the turn timer pause while the app is not on screen.
    override fun onStart() {
        super.onStart()
        vm.setForeground(true)
    }

    override fun onStop() {
        vm.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[LudoViewModel::class.java]
        setContent {
            LudoTheme {
                BrandBackground { LudoScreen() }
            }
        }
    }
}
