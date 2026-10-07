package com.example.ludo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.ludo.R
import com.example.ludo.data.Settings

enum class Fx(val res: Int, val volume: Float = 1f) {
    DICE(R.raw.sfx_dice),
    STEP(R.raw.sfx_step, 0.7f),
    LEAVE(R.raw.sfx_leave),
    CAPTURE(R.raw.sfx_capture),
    HOME(R.raw.sfx_home),
    WIN(R.raw.sfx_win),
    LOSE(R.raw.sfx_lose),
    TURN(R.raw.sfx_turn, 0.8f),
    TICK(R.raw.sfx_tick, 0.6f),
    CLICK(R.raw.sfx_click, 0.7f),
    SIX(R.raw.sfx_six, 0.55f),
}

/** Short effects through SoundPool, background music through a looping MediaPlayer. */
class SoundManager(context: Context) {
    private val app = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids: Map<Fx, Int> = Fx.entries.associateWith { pool.load(app, it.res, 1) }
    private var music: MediaPlayer? = null

    private var sfxOn = true
    private var musicOn = true
    private var musicVolume = 0.5f
    private var foreground = true

    fun apply(s: Settings) {
        sfxOn = s.sfxOn
        musicOn = s.musicOn
        musicVolume = s.musicVolume
        refreshMusic()
    }

    /** False while the app is in the background: the music pauses. */
    fun setForeground(value: Boolean) {
        foreground = value
        refreshMusic()
    }

    private var sixStream = 0

    fun play(fx: Fx) {
        if (!sfxOn) return
        val id = ids[fx] ?: return
        // A new six restarts the cheer instead of stacking on top of the last one.
        if (fx == Fx.SIX && sixStream != 0) pool.stop(sixStream)
        val stream = pool.play(id, fx.volume, fx.volume, 1, 0, 1f)
        if (fx == Fx.SIX) sixStream = stream
    }

    private fun refreshMusic() {
        try {
            val want = musicOn && foreground && musicVolume > 0f
            if (want) {
                val p = music ?: MediaPlayer.create(app, R.raw.music_loop)?.also {
                    it.isLooping = true
                    music = it
                }
                p?.setVolume(musicVolume * 0.6f, musicVolume * 0.6f)
                if (p != null && !p.isPlaying) p.start()
            } else {
                music?.takeIf { it.isPlaying }?.pause()
            }
        } catch (_: Exception) {
            // audio problems must never crash the game
        }
    }

    fun release() {
        try {
            music?.release()
            pool.release()
        } catch (_: Exception) {
        }
        music = null
    }
}
