package com.example.poker.data

import android.content.Context
import android.content.SharedPreferences

class ReplayerPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastSourceUri: String?
        get() = prefs.getString(KEY_LAST_SOURCE_URI, null)
        set(value) = prefs.edit().putString(KEY_LAST_SOURCE_URI, value).apply()

    var lastFileName: String?
        get() = prefs.getString(KEY_LAST_FILE_NAME, null)
        set(value) = prefs.edit().putString(KEY_LAST_FILE_NAME, value).apply()

    var lastHandIndex: Int
        get() = prefs.getInt(KEY_LAST_HAND_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_HAND_INDEX, value).apply()

    var lastStepIndex: Int
        get() = prefs.getInt(KEY_LAST_STEP_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_STEP_INDEX, value).apply()

    var lastHandId: String?
        get() = prefs.getString(KEY_LAST_HAND_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_HAND_ID, value).apply()

    var useBbUnits: Boolean
        get() = prefs.getBoolean(KEY_USE_BB_UNITS, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_BB_UNITS, value).apply()

    var autoPlaySpeedMs: Long
        get() = prefs.getLong(KEY_AUTOPLAY_SPEED, 1500L)
        set(value) = prefs.edit().putLong(KEY_AUTOPLAY_SPEED, value).apply()

    var isFullAccessPurchased: Boolean
        get() = prefs.getBoolean(KEY_FULL_ACCESS_PURCHASED, false)
        set(value) = prefs.edit().putBoolean(KEY_FULL_ACCESS_PURCHASED, value).apply()

    fun saveReplayerState(handIndex: Int, stepIndex: Int, handId: String?) {
        prefs.edit()
            .putInt(KEY_LAST_HAND_INDEX, handIndex)
            .putInt(KEY_LAST_STEP_INDEX, stepIndex)
            .putString(KEY_LAST_HAND_ID, handId)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "poker_replayer_prefs"
        private const val KEY_LAST_SOURCE_URI = "last_source_uri"
        private const val KEY_LAST_FILE_NAME = "last_file_name"
        private const val KEY_LAST_HAND_INDEX = "last_hand_index"
        private const val KEY_LAST_STEP_INDEX = "last_step_index"
        private const val KEY_LAST_HAND_ID = "last_hand_id"
        private const val KEY_USE_BB_UNITS = "use_bb_units"
        private const val KEY_AUTOPLAY_SPEED = "autoplay_speed_ms"
        private const val KEY_FULL_ACCESS_PURCHASED = "full_access_purchased"
    }
}
