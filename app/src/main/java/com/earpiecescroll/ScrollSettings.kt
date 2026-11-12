package com.earpiecescroll

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent

/** Persistent user controls shared by the activity and accessibility service. */
object ScrollSettings {
    const val ACTION_SCROLL_DOWN = ClickClassifier.ACTION_SCROLL_DOWN
    const val ACTION_SCROLL_UP = ClickClassifier.ACTION_SCROLL_UP
    const val ACTION_PAGE_DOWN = ClickClassifier.ACTION_PAGE_DOWN
    const val ACTION_PAGE_UP = ClickClassifier.ACTION_PAGE_UP
    const val ACTION_SELECT = ClickClassifier.ACTION_SELECT
    const val ACTION_BACK = ClickClassifier.ACTION_BACK

    private const val PREFS = "scroll_settings"
    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun p(context: Context): SharedPreferences {
        if (!::prefs.isInitialized) initialize(context)
        return prefs
    }

    fun volumeKeysEnabled(context: Context) = p(context).getBoolean("volume_enabled", true)
    fun setVolumeKeysEnabled(context: Context, enabled: Boolean) {
        p(context).edit().putBoolean("volume_enabled", enabled).apply()
    }

    fun useGestures(context: Context) = p(context).getBoolean("use_gestures", false)
    fun setUseGestures(context: Context, enabled: Boolean) {
        p(context).edit().putBoolean("use_gestures", enabled).apply()
    }

    fun scrollDistance(context: Context) = p(context).getInt("distance", 48)
    fun setScrollDistance(context: Context, value: Int) {
        p(context).edit().putInt("distance", value.coerceIn(20, 85)).apply()
    }

    fun gestureDuration(context: Context) = p(context).getInt("duration", 260)
    fun setGestureDuration(context: Context, value: Int) {
        p(context).edit().putInt("duration", value.coerceIn(100, 800)).apply()
    }

    fun doubleTapMs(context: Context) = p(context).getInt("double_tap_ms", 300)
    fun setDoubleTapMs(context: Context, value: Int) {
        p(context).edit().putInt("double_tap_ms", value.coerceIn(180, 700)).apply()
    }

    fun holdMs(context: Context) = p(context).getInt("hold_ms", 700)
    fun setHoldMs(context: Context, value: Int) {
        p(context).edit().putInt("hold_ms", value.coerceIn(350, 1500)).apply()
    }

    fun singleAction(context: Context, keyCode: Int): Int? {
        val key = actionKey(keyCode) ?: return null
        if (!p(context).contains(key)) return null
        return p(context).getInt(key, ClickClassifier.ACTION_SCROLL_DOWN)
    }

    fun setSingleAction(context: Context, keyCode: Int, action: Int) {
        actionKey(keyCode)?.let { key -> p(context).edit().putInt(key, action).apply() }
    }

    private fun actionKey(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> "single_volume_up"
        KeyEvent.KEYCODE_VOLUME_DOWN -> "single_volume_down"
        KeyEvent.KEYCODE_HEADSETHOOK -> "single_headset"
        else -> null
    }
}
