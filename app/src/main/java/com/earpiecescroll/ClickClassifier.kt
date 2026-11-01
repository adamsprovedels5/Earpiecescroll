package com.earpiecescroll

import android.os.SystemClock
import android.view.KeyEvent

object ClickClassifier {

    private const val DEBOUNCE_MS = 350L
    private const val LONG_PRESS_MS = 600L

    private val clickTimes = mutableListOf<Long>()
    private var downTime = 0L

    // 1 = Scroll Down, 2 = Scroll Up, 3 = Page Down, 4 = Page Up
    var onAction: ((Int) -> Unit)? = null

    fun onMediaKey(keyCode: Int, action: Int) {
        val now = SystemClock.elapsedRealtime()

        // Volume buttons: instant, no classification needed
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(1)
            return
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(2)
            return
        }

        if (keyCode != KeyEvent.KEYCODE_HEADSETHOOK) return

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                // Record the start time of the press (only on first DOWN, ignore repeats)
                if (downTime == 0L) downTime = now
            }

            KeyEvent.ACTION_UP -> {
                if (downTime == 0L) return // Spurious UP

                val pressDuration = now - downTime
                downTime = 0L

                // Long press detected HERE on UP — this is where we know the duration
                // (The old code checked this inside ACTION_DOWN which was always ~0ms)
                if (pressDuration >= LONG_PRESS_MS) {
                    clickTimes.clear()
                    onAction?.invoke(3) // Page Down
                    return
                }

                // Short click: register in the debounce window
                clickTimes.add(now)
                clickTimes.removeAll { now - it > DEBOUNCE_MS }

                when (clickTimes.size) {
                    1 -> onAction?.invoke(1) // Single click → Scroll Down
                    2 -> onAction?.invoke(2) // Double click → Scroll Up
                    3 -> {
                        clickTimes.clear()
                        onAction?.invoke(4) // Triple click → Page Up
                    }
                }
            }
        }
    }
}
