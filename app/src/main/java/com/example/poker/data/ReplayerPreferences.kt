package com.example.poker.data

import android.content.Context
import android.content.SharedPreferences

class ReplayerPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("poker_replayer_prefs", Context.MODE_PRIVATE)

    var isDisplayInBigBlinds: Boolean
        get() = prefs.getBoolean(KEY_DISPLAY_BB, false)
        set(value) = prefs.edit().putBoolean(KEY_DISPLAY_BB, value).apply()

    var isFullAccessPurchased: Boolean
        get() = prefs.getBoolean(KEY_FULL_ACCESS_PURCHASED, false)
        set(value) = prefs.edit().putBoolean(KEY_FULL_ACCESS_PURCHASED, value).apply()

    var playbackSpeedMs: Long
        get() = prefs.getLong(KEY_PLAYBACK_SPEED, 1200L)
        set(value) = prefs.edit().putLong(KEY_PLAYBACK_SPEED, value).apply()

    fun getFreeHandLimit(): Int = 30

    companion object {
        private const val KEY_DISPLAY_BB = "display_in_bb"
        private const val KEY_FULL_ACCESS_PURCHASED = "full_access_purchased"
        private const val KEY_PLAYBACK_SPEED = "playback_speed_ms"
    }
}
