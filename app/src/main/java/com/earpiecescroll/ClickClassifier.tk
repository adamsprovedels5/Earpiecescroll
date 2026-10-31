package com.earpiecescroll

import android.os.SystemClock
import android.view.KeyEvent

object ClickClassifier {

    private const val DEBOUNCE_MS = 400L
    private const val LONG_PRESS_MS = 500L // Tuned to beat the Assistant
    
    private val clickTimes = mutableListOf<Long>()
    private var downTime = 0L

    // 1 = Scroll Down, 2 = Scroll Up, 3 = Page Down, 4 = Page Up
    var onAction: ((Int) -> Unit)? = null

    fun onMediaKey(keyCode: Int, action: Int) {
        val now = SystemClock.elapsedRealtime()

        // Handle immediate volume buttons (instant scroll)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(1)
            return
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(2)
            return
        }

        // Handle Center Button (HEADSETHOOK)
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (downTime == 0L) downTime = now
                
                // If it's a long press, trigger Page Down and reset
                if (now - downTime > LONG_PRESS_MS) {
                    onAction?.invoke(3) 
                    downTime = 0L
                    clickTimes.clear()
                }
            } else if (action == KeyEvent.ACTION_UP) {
                val pressDuration = now - downTime
                downTime = 0L

                if (pressDuration > LONG_PRESS_MS) return // Already handled

                // Register a click
                clickTimes.add(now)
                clickTimes.removeAll { now - it > DEBOUNCE_MS }

                val count = clickTimes.size
                when (count) {
                    1 -> onAction?.invoke(1) // Scroll Down
                    2 -> onAction?.invoke(2) // Scroll Up
                    3 -> onAction?.invoke(4) // Page Up (Triple click)
                }
            }
        }
    }
}
