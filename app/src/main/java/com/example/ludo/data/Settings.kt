package com.example.ludo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val playerName: String = "You",
    val timerSeconds: Int = 15,          // 0 = off
    val autoPlayOnTimeout: Boolean = true, // false = skip the turn instead
    val musicOn: Boolean = true,
    val musicVolume: Float = 0.5f,
    val sfxOn: Boolean = true,
) {
    val displayName: String get() = playerName.trim().ifEmpty { "You" }

    companion object {
        val TimerChoices = listOf(0, 10, 15, 20, 30)
    }
}

/** Saves settings on the phone (SharedPreferences) so they persist between sessions. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ludo_settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<Settings> = _flow.asStateFlow()

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            playerName = prefs.getString("name", d.playerName) ?: d.playerName,
            timerSeconds = prefs.getInt("timer", d.timerSeconds),
            autoPlayOnTimeout = prefs.getBoolean("autoplay", d.autoPlayOnTimeout),
            musicOn = prefs.getBoolean("music", d.musicOn),
            musicVolume = prefs.getFloat("musicVol", d.musicVolume),
            sfxOn = prefs.getBoolean("sfx", d.sfxOn),
        )
    }

    fun update(change: (Settings) -> Settings) {
        val s = change(_flow.value)
        _flow.value = s
        prefs.edit()
            .putString("name", s.playerName)
            .putInt("timer", s.timerSeconds)
            .putBoolean("autoplay", s.autoPlayOnTimeout)
            .putBoolean("music", s.musicOn)
            .putFloat("musicVol", s.musicVolume)
            .putBoolean("sfx", s.sfxOn)
            .apply()
    }
}
